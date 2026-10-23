package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.PasswordUpdate;
import com.celcioblond.pms.dto.UserCreate;
import com.celcioblond.pms.dto.UserRegister;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.dto.UserUpdate;
import com.celcioblond.pms.exception.BadRequestException;
import com.celcioblond.pms.exception.ConflictException;
import com.celcioblond.pms.exception.ResourceNotFoundException;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Role;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.UserRepository;
import com.celcioblond.pms.security.AccessPolicy;
import com.celcioblond.pms.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VectorStoreService vectorStoreService;
    private final PasswordEncoder passwordEncoder;
    private final AccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(ResponseMapper::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return ResponseMapper.toUserResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return ResponseMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getColleaguesByProjectId(Long projectId, UserPrincipal currentUser) {
        accessPolicy.requireProjectAccess(projectId, currentUser);
        return userRepository.findColleaguesByProjectId(projectId, currentUser.getUsername()).stream()
                .map(ResponseMapper::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreate userCreate) {
        ensureUnique(userCreate.username(), userCreate.email());

        User user = new User();
        user.setName(userCreate.name());
        user.setUsername(userCreate.username());
        if (userCreate.age() != null) user.setAge(userCreate.age());
        user.setEmail(userCreate.email());
        user.setPassword(passwordEncoder.encode(userCreate.password()));
        user.setPosition(userCreate.position());
        user.setDepartment(userCreate.department());
        user.setRole(userCreate.role());

        return saveAndIndex(user);
    }

    /** Self-registration always creates an EMPLOYEE; admins are created by other admins. */
    @Transactional
    public UserResponse registerUser(UserRegister register) {
        ensureUnique(register.username(), register.email());

        User user = new User();
        user.setUsername(register.username());
        user.setEmail(register.email());
        user.setPassword(passwordEncoder.encode(register.password()));
        user.setName(register.username()); // shown as author until the profile is filled in
        user.setRole(Role.EMPLOYEE);

        return saveAndIndex(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdate userUpdate, UserPrincipal currentUser) {
        User user = findUser(id);

        if (userUpdate.name() != null) user.setName(userUpdate.name());
        if (userUpdate.age() != null) user.setAge(userUpdate.age());

        if (userUpdate.email() != null && !user.getEmail().equals(userUpdate.email())) {
            if (userRepository.existsByEmail(userUpdate.email())) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(userUpdate.email());
        }

        if (userUpdate.position() != null) user.setPosition(userUpdate.position());
        if (userUpdate.department() != null) user.setDepartment(userUpdate.department());

        if (userUpdate.role() != null) {
            if (isSelf(id, currentUser) && userUpdate.role() != Role.ADMIN) {
                throw new BadRequestException("You cannot remove your own admin role");
            }
            user.setRole(userUpdate.role());
        }

        return saveAndIndex(user);
    }

    /**
     * Admins can reset any password. Other users can only change their own and
     * must confirm their current password.
     */
    @Transactional
    public void updatePassword(Long id, PasswordUpdate passwordUpdate, UserPrincipal currentUser) {
        User user = findUser(id);

        if (!currentUser.isAdmin()) {
            if (!isSelf(id, currentUser)) {
                throw new AccessDeniedException("Cannot change another user's password");
            }
            if (passwordUpdate.currentPassword() == null
                    || !passwordEncoder.matches(passwordUpdate.currentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
        }

        user.setPassword(passwordEncoder.encode(passwordUpdate.newPassword()));
        userRepository.save(user);
        // No vector store update needed — passwords are never in the document content
    }

    @Transactional
    public void deleteUser(Long id, UserPrincipal currentUser) {
        if (isSelf(id, currentUser)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        User user = findUser(id);

        // Unassign from projects and tasks so the join rows don't block the delete
        user.getAssignedProjects().forEach(project -> project.getAssignedEmployees().remove(user));
        user.getAssignedTasks().forEach(task -> task.getAssignedEmployees().remove(user));

        vectorStoreService.deleteUser(id);
        userRepository.delete(user);
    }

    private void ensureUnique(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already in use");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }
    }

    private static boolean isSelf(Long id, UserPrincipal currentUser) {
        return Objects.equals(id, currentUser.getId());
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserResponse saveAndIndex(User user) {
        User savedUser = userRepository.save(user);
        vectorStoreService.upsertUser(savedUser);
        return ResponseMapper.toUserResponse(savedUser);
    }
}
