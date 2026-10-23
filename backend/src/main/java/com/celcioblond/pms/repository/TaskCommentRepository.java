package com.celcioblond.pms.repository;

import com.celcioblond.pms.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskId(Long taskId);
    List<TaskComment> findByAuthorId(Long authorId);
}
