package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.UserCreate;
import com.celcioblond.pms.dto.UserRegister;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.dto.UserUpdate;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VectorStoreService vectorStoreService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(ResponseMapper::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return ResponseMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return ResponseMapper.toUserResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found by email: " + email));
        return ResponseMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getColleaguesByProjectId(Long projectId, String username) {
        return userRepository.findColleaguesByProjectId(projectId, username).stream()
                .map(ResponseMapper::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreate userCreate) {
        if (userRepository.existsByEmail(userCreate.email())) {
            throw new RuntimeException("Email already in use: " + userCreate.email());
        }

        User user = new User();
        user.setName(userCreate.name());
        user.setUsername(userCreate.username());
        user.setAge(userCreate.age());
        user.setEmail(userCreate.email());
        user.setPassword(passwordEncoder.encode(userCreate.password()));
        user.setPosition(userCreate.position());
        user.setDepartment(userCreate.department());
        user.setRole(userCreate.role());

        return saveAndIndex(user);
    }

    @Transactional
    public UserResponse registerUser(UserRegister register) {
        if (userRepository.findByUsername(register.username()).isPresent()) {
            throw new RuntimeException("Username is already in use: " + register.username());
        }

        User user = new User();
        user.setUsername(register.username());
        user.setEmail(register.email());
        user.setPassword(passwordEncoder.encode(register.password()));
        user.setName("");
        user.setRole("EMPLOYEE");

        return saveAndIndex(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdate userUpdate) {
        User user = findUser(id);

        if (userUpdate.name() != null) user.setName(userUpdate.name());
        if (userUpdate.age() != null) user.setAge(userUpdate.age());

        if (userUpdate.email() != null) {
            if (!user.getEmail().equals(userUpdate.email()) && userRepository.existsByEmail(userUpdate.email())) {
                throw new RuntimeException("Email already exists: " + userUpdate.email());
            }
            user.setEmail(userUpdate.email());
        }

        if (userUpdate.position() != null) user.setPosition(userUpdate.position());
        if (userUpdate.department() != null) user.setDepartment(userUpdate.department());
        if (userUpdate.role() != null) user.setRole(userUpdate.role());

        return saveAndIndex(user);
    }

    @Transactional
    public void updatePassword(Long id, String newPassword) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // No vector store update needed — passwords are never in the document content
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        vectorStoreService.deleteUser(id);
        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    private UserResponse saveAndIndex(User user) {
        User savedUser = userRepository.save(user);
        vectorStoreService.upsertUser(savedUser);
        return ResponseMapper.toUserResponse(savedUser);
    }
}
