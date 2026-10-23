package com.celcioblond.pms.repository;

import com.celcioblond.pms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(String role);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.assignedProjects p " +
            "WHERE p.id = :projectId " +
            "AND u.username != :username")
    List<User> findColleaguesByProjectId(
            @Param("projectId") Long projectId,
            @Param("username") String username);
}