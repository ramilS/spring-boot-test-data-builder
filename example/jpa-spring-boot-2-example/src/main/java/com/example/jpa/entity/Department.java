package com.example.jpa.entity;

import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * Department entity using javax.persistence (Spring Boot 2.x).
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Department getParent() { return parent; }
    public void setParent(Department parent) { this.parent = parent; }
}
