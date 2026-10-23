package com.celcioblond.pms.service;

import com.celcioblond.pms.TestUsers;
import com.celcioblond.pms.dto.PasswordUpdate;
import com.celcioblond.pms.dto.UserCreate;
import com.celcioblond.pms.dto.UserRegister;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.dto.UserUpdate;
import com.celcioblond.pms.exception.BadRequestException;
import com.celcioblond.pms.exception.ConflictException;
import com.celcioblond.pms.model.Role;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.UserRepository;
import com.celcioblond.pms.security.AccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VectorStoreService vectorStoreService;
    @Mock
    private AccessPolicy accessPolicy;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, vectorStoreService, passwordEncoder, accessPolicy);
    }

    @Test
    void createUserStoresHashedPassword() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(new UserCreate("Dana", "dana01", 30, "dana@example.com",
                "Passw0rd!x", "Engineer", "R&D", Role.EMPLOYEE));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isNotEqualTo("Passw0rd!x");
        assertThat(passwordEncoder.matches("Passw0rd!x", saved.getValue().getPassword())).isTrue();
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("dana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new UserCreate("Dana", "dana01", 30,
                "dana@example.com", "Passw0rd!x", null, null, Role.EMPLOYEE)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registrationAlwaysCreatesAnEmployee() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.registerUser(new UserRegister("erin01", "erin@example.com", "Passw0rd!x"));

        assertThat(response.role()).isEqualTo("EMPLOYEE");
        assertThat(response.name()).isEqualTo("erin01");
    }

    @Test
    void employeeCannotChangeAnotherUsersPassword() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(TestUsers.user(9L, "other", Role.EMPLOYEE)));

        assertThatThrownBy(() -> userService.updatePassword(9L,
                new PasswordUpdate("whatever", "NewPassw0rd!"), TestUsers.employee(7L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void employeeMustConfirmCurrentPassword() {
        User self = TestUsers.user(7L, "employee7", Role.EMPLOYEE);
        self.setPassword(passwordEncoder.encode("OldPassw0rd!"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.updatePassword(7L,
                new PasswordUpdate("wrong", "NewPassw0rd!"), TestUsers.employee(7L)))
                .isInstanceOf(BadRequestException.class);

        userService.updatePassword(7L, new PasswordUpdate("OldPassw0rd!", "NewPassw0rd!"), TestUsers.employee(7L));
        assertThat(passwordEncoder.matches("NewPassw0rd!", self.getPassword())).isTrue();
    }

    @Test
    void adminCanResetPasswordWithoutCurrentOne() {
        User target = TestUsers.user(9L, "other", Role.EMPLOYEE);
        when(userRepository.findById(9L)).thenReturn(Optional.of(target));

        userService.updatePassword(9L, new PasswordUpdate(null, "NewPassw0rd!"), TestUsers.admin());

        assertThat(passwordEncoder.matches("NewPassw0rd!", target.getPassword())).isTrue();
    }

    @Test
    void adminCannotDeleteOwnAccount() {
        assertThatThrownBy(() -> userService.deleteUser(1L, TestUsers.admin()))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void adminCannotRemoveOwnAdminRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestUsers.user(1L, "admin", Role.ADMIN)));

        assertThatThrownBy(() -> userService.updateUser(1L,
                new UserUpdate(null, null, null, null, null, Role.EMPLOYEE), TestUsers.admin()))
                .isInstanceOf(BadRequestException.class);
    }
}
