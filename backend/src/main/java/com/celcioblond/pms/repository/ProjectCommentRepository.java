package com.celcioblond.pms.repository;

import com.celcioblond.pms.model.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    List<ProjectComment> findByProjectId(Long projectId);
    List<ProjectComment> findByAuthorId(Long authorId);
}
