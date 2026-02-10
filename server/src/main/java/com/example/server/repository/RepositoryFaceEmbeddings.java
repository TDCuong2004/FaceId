package com.example.server.repository;
import com.example.server.entity.EntityFaceEmbeddings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryFaceEmbeddings extends JpaRepository<EntityFaceEmbeddings, Integer> {
}
