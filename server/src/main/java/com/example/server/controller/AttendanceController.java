package com.example.server.controller;

import com.example.server.dto.AttendanceRequest;
import com.example.server.dto.AttendanceResponse;
import com.example.server.service.FaceRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @PostMapping("/attendance")
    public ResponseEntity<?> attendance(@RequestBody AttendanceRequest request) {
        try {
            System.out.println(">>> IMAGE LENGTH = " +
                    (request.getImage() == null ? "null" : request.getImage().length()));

            AttendanceResponse res =
                    faceRecognitionService.attendance(request.getImage());

            System.out.println(">>> RESPONSE = " + res);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 BẮT BUỘC
            return ResponseEntity.status(500).body(e.toString());
        }
    }

}
