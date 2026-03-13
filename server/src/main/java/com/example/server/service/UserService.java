package com.example.server.service;

import com.example.server.entity.EntityUser;
import com.example.server.repository.RepositoryUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private RepositoryUser repositoryUser;

    public String register(String username, String password, String email){

        Optional<EntityUser> existing = repositoryUser.findByUsername(username);

        if(existing.isPresent()){
            return "Username already exists";
        }

        EntityUser user = new EntityUser();
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setEmail(email);
        user.setRole("user");

        repositoryUser.save(user);

        return "Register success";
    }

    public boolean login(String username, String password){

        Optional<EntityUser> user = repositoryUser.findByUsername(username);

        if(user.isPresent()){
            return user.get().getPasswordHash().equals(password);
        }

        return false;
    }

}