package com.example.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceResponse {


    private boolean success;

    private String studentCode;
    private String fullName;
    private String className;

    private double similarity;

    private String message;

    private boolean alreadyCheckedIn;
}
