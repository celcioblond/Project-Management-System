package com.celcioblond.pms.repository;

import com.celcioblond.pms.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    @Query("SELECT t FROM Task t JOIN t.assignedEmployees u WHERE u.id = :employeeId")
    List<Task> findByAssignedEmployeeId(Long employeeId);
}
