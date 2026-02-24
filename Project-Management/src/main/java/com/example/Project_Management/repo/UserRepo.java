package com.example.Project_Management.repo;

import com.example.Project_Management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
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