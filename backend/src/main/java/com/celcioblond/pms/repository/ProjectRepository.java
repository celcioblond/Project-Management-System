package com.celcioblond.pms.repository;

import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("SELECT p FROM Project p JOIN p.assignedEmployees u WHERE u = :user")
    List<Project> findByAssignedEmployee(User user);
}
