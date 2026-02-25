package com.example.Project_Management.repo;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepo extends JpaRepository<Project, Long> {
    @Query("SELECT p FROM Project p JOIN p.assignedEmployees u WHERE u = :user")
    List<Project> findByAssignedEmployee(User user);
}
