package com.example.server.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "students")
@Data
public class EntityStudents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "age")
    private Integer age;
}
