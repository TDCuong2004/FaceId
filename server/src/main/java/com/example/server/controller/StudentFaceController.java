package com.example.server.controller;

import com.example.server.dto.StudentFaceRegisterRequest;
import com.example.server.entity.EntityFaceEmbeddings;
import com.example.server.entity.EntityStudents;
import com.example.server.repository.RepositoryFaceEmbeddings;
import com.example.server.repository.RepositoryStudents;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bytedeco.javacpp.BytePointer;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import org.bytedeco.opencv.opencv_core.Mat;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/studentface")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class StudentFaceController {
    private final RepositoryStudents studentRepository;
    private final RepositoryFaceEmbeddings faceEmbeddingRepository;

    @PostMapping("/register-with-face")
    @Transactional
    public ResponseEntity<?> registerWithFace(
            @RequestBody StudentFaceRegisterRequest request
    ) {

        if (request.getImages() == null || request.getImages().isEmpty()) {
            return ResponseEntity.badRequest().body("Chưa gửi ảnh khuôn mặt");
        }

        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            return ResponseEntity.badRequest().body("Mã sinh viên đã tồn tại");
        }

        // 1. Lưu sinh viên
        EntityStudents student = new EntityStudents();
        student.setStudentCode(request.getStudentCode());
        student.setFullName(request.getFullName());
        student.setAge(request.getAge());
        student.setClassName(request.getClassName());
        studentRepository.save(student);

        // 2. Lưu embedding giả lập
        for (String img : request.getImages()) {
            EntityFaceEmbeddings fe = new EntityFaceEmbeddings();
            fe.setStudentCode(request.getStudentCode());
            fe.setEmbedding("[0.1,0.2,0.3]");
            faceEmbeddingRepository.save(fe);
        }

        return ResponseEntity.ok("OK - test thành công");
    }

    /* ======= GIẢ LẬP ======= */

//    private Mat detectFace(Mat image) {
//        return image; // TODO: real detect
//    }
//
//    private String extractEmbedding(Mat face) {
//        return "[0.12,0.45,0.78]";
//    }
}
