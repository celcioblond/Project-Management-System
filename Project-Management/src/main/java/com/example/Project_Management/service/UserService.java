package com.example.Project_Management.service;

import com.example.Project_Management.model.User;
import com.example.Project_Management.model.dto.UserCreate;
import com.example.Project_Management.model.dto.UserResponse;
import com.example.Project_Management.model.dto.UserUpdate;
import com.example.Project_Management.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public List<UserResponse> getAllUsers(){
        return userRepo.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToUserResponse(user);
    }

    public UserResponse getUserByEmail(String email){
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found by email: " + email));
        return convertToUserResponse(user);
    }

    public List<UserResponse> getUsersByRole(String role){
        return userRepo.findByRole(role).stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse createUser(UserCreate userCreate){
        if(userRepo.existsByEmail(userCreate.email())) {
            throw new RuntimeException("Email already in use" + userCreate.email());
        }

        User user = new User();
        user.setName(userCreate.name());
        user.setAge(userCreate.age());
        user.setEmail(userCreate.email());
        user.setPassword(userCreate.password());  // TODO: Hash password when adding Spring Security
        user.setPosition(userCreate.position());
        user.setDepartment(userCreate.department());
        user.setRole(userCreate.role());

        User savedUser = userRepo.save(user);
        return convertToUserResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserUpdate userUpdate){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id" + id));

        if(userUpdate.name()!=null){
            user.setName(userUpdate.name());
        }

        if(userUpdate.age()!=null){
            user.setAge(userUpdate.age());
        }

        if (userUpdate.email() != null) {
            // Check if new email is already taken by another user
            if (!user.getEmail().equals(userUpdate.email()) && userRepo.existsByEmail(userUpdate.email())) {
                throw new RuntimeException("Email already exists: " + userUpdate.email());
            }
            user.setEmail(userUpdate.email());
        }

        if (userUpdate.position()!=null){
            user.setPosition(userUpdate.position());
        }

        if (userUpdate.department()!=null){
            user.setPosition(userUpdate.position());
        }

        if(userUpdate.role()!=null){
            user.setRole(userUpdate.role());
        }

        User savedUser = userRepo.save(user);

        return convertToUserResponse(savedUser);
    }

    public void deleteUser(Long id){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));

        userRepo.delete(user);
    }

    public void updatePassword(Long id, String newPassword){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));

        user.setPassword(newPassword);
        userRepo.save(user);
    }

    private UserResponse convertToUserResponse(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getAge(),
                user.getEmail(),
                user.getPosition(),
                user.getDepartment(),
                user.getRole()
        );
    }
}
