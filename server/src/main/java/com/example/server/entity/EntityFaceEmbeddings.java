package com.example.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_embeddings")
@Data
public class EntityFaceEmbeddings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_code")
    private String studentCode;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "student_code",
            referencedColumnName = "student_code",
            insertable = false,
            updatable = false
    )
    private EntityStudents student;
}

