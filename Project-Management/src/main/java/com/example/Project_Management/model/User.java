package com.example.Project_Management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="users")
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

    @OneToMany(mappedBy = "assignedEmployee", cascade = CascadeType.ALL)
    private List<Project> assignedProjects;

    @OneToMany(mappedBy = "assignedEmployee", cascade = CascadeType.ALL)
    private List<Task> assignedTasks;

}
