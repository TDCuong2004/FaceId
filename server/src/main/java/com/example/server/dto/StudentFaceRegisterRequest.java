package com.example.server.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudentFaceRegisterRequest {

    private String studentCode;
    private String fullName;
    private Integer age;
    private String className;
    // danh sách ảnh base64
    private List<String> images;
}
