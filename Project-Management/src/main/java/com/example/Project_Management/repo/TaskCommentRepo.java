package com.example.Project_Management.repo;

import com.example.Project_Management.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepo extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByAuthorId(Long authorId);
}
