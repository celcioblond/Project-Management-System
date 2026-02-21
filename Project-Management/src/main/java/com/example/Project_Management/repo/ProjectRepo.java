package com.example.Project_Management.repo;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepo extends JpaRepository<Project, Long> {
    List<Project> findByAssignedEmployee(User assignedEmployee);
}
