package com.example.server.controller;

import com.example.server.dto.LoginRequest;
import com.example.server.dto.RegisterRequest;
import com.example.server.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request){

        return userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request){

        boolean success = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        if(success){
            return "Login success";
        }

        return "Login failed";
    }

}