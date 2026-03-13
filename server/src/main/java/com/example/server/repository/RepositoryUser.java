package com.example.server.repository;

import com.example.server.entity.EntityUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryUser extends JpaRepository<EntityUser, Integer> {

    Optional<EntityUser> findByUsername(String username);

}