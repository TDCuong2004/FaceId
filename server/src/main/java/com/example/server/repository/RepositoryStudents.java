package com.example.server.repository;

import com.example.server.entity.EntityStudents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryStudents extends JpaRepository<EntityStudents, Integer>{
    boolean existsByStudentCode(String studentCode);
    Optional<EntityStudents> findByStudentCode(String studentCode);
}
