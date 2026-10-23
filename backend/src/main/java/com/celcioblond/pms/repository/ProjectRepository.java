package com.celcioblond.pms.repository;

import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("SELECT p FROM Project p JOIN p.assignedEmployees u WHERE u = :user")
    List<Project> findByAssignedEmployee(User user);

    /** Projects a user can see: assigned directly, or through one of the project's tasks. */
    @Query("""
            SELECT DISTINCT p.id FROM Project p
            LEFT JOIN p.assignedEmployees u
            LEFT JOIN p.tasks t
            LEFT JOIN t.assignedEmployees tu
            WHERE u.id = :userId OR tu.id = :userId
            """)
    List<Long> findAccessibleProjectIds(@Param("userId") Long userId);
}
