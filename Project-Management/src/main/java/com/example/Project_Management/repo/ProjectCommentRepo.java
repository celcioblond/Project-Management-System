package com.example.Project_Management.repo;

import com.example.Project_Management.model.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCommentRepo extends JpaRepository<ProjectComment, Long> {
    List<ProjectComment> findByProjectId(Long projectId);
    List<ProjectComment> findByAuthorId(Long authorId);
}
