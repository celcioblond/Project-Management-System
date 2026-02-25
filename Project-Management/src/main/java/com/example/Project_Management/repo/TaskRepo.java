package com.example.Project_Management.repo;

import com.example.Project_Management.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepo extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    @Query("SELECT t FROM Task t JOIN t.assignedEmployees u WHERE u.id = :employeeId")
    List<Task> findByAssignedEmployeeId(Long employeeId);
}
