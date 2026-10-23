package com.celcioblond.pms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    private int age;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String position;
    private String department;
    private String role;

    @ManyToMany(mappedBy = "assignedEmployees")
    private List<Project> assignedProjects = new ArrayList<>();

    @ManyToMany(mappedBy = "assignedEmployees")
    private List<Task> assignedTasks = new ArrayList<>();
}
