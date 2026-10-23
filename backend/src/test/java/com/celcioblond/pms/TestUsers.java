package com.celcioblond.pms;

import com.celcioblond.pms.model.Role;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.security.UserPrincipal;

/** Small factory for users and principals shared by the tests. */
public final class TestUsers {

    private TestUsers() {
    }

    public static User user(long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hashed");
        user.setRole(role);
        return user;
    }

    public static UserPrincipal admin() {
        return new UserPrincipal(user(1L, "admin", Role.ADMIN));
    }

    public static UserPrincipal employee(long id) {
        return new UserPrincipal(user(id, "employee" + id, Role.EMPLOYEE));
    }
}
