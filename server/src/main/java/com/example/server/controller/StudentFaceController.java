package com.example.server.controller;

import com.example.server.dto.StudentFaceRegisterRequest;
import com.example.server.entity.EntityFaceEmbeddings;
import com.example.server.entity.EntityStudents;
import com.example.server.repository.RepositoryFaceEmbeddings;
import com.example.server.repository.RepositoryStudents;
import com.example.server.service.FaceRecognitionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/studentface")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentFaceController {

    private final RepositoryStudents studentRepository;
    private final RepositoryFaceEmbeddings faceEmbeddingRepository;

    // Tiêm (Inject) Service nhận diện khuôn mặt vào để dùng
    private final FaceRecognitionService faceService;

    @PostMapping("/register-with-face")
    @Transactional
    public ResponseEntity<?> registerWithFace(
            @RequestBody StudentFaceRegisterRequest request
    ) {

        // 1. Kiểm tra đầu vào
        if (request.getImages() == null || request.getImages().isEmpty()) {
            return ResponseEntity.badRequest().body("Chưa gửi ảnh khuôn mặt");
        }

        if (studentRepository.existsByStudentCode(request.getStudentCode())) {
            return ResponseEntity.badRequest().body("Mã sinh viên đã tồn tại");
        }

        // 2. Lưu thông tin sinh viên
        EntityStudents student = new EntityStudents();
        student.setStudentCode(request.getStudentCode());
        student.setFullName(request.getFullName());
        student.setAge(request.getAge());
        student.setClassName(request.getClassName());
        studentRepository.save(student);

        int successCount = 0;

        // Lấy toàn bộ embedding hiện có trong DB
        List<EntityFaceEmbeddings> existingEmbeddings = faceEmbeddingRepository.findAll();

        for (String base64Img : request.getImages()) {
            try {

                Mat image = faceService.decodeBase64ToMat(base64Img);
                Mat face = faceService.detectFace(image);

                if (face != null) {

                    float[] embeddingVector = faceService.extractEmbedding(face);

                    if (embeddingVector != null) {

                        // ===== SO SÁNH VỚI DB =====
                        boolean duplicate = false;

                        for (EntityFaceEmbeddings item : existingEmbeddings) {

                            float[] dbEmbedding = faceService.parseEmbedding(item.getEmbedding());

                            double score = faceService.cosineSimilarity(embeddingVector, dbEmbedding);

                            if (score > 0.70) { // ngưỡng trùng khuôn mặt
                                duplicate = true;
                                break;
                            }
                        }

                        if (duplicate) {
                            studentRepository.delete(student);
                            return ResponseEntity.badRequest()
                                    .body("❌ Khuôn mặt này đã tồn tại trong hệ thống!");
                        }

                        // ===== LƯU EMBEDDING =====
                        EntityFaceEmbeddings fe = new EntityFaceEmbeddings();
                        fe.setStudentCode(request.getStudentCode());
                        fe.setEmbedding(Arrays.toString(embeddingVector));

                        faceEmbeddingRepository.save(fe);
                        successCount++;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (successCount == 0) {
            studentRepository.delete(student);
            return ResponseEntity.badRequest()
                    .body("❌ Không tìm thấy khuôn mặt rõ ràng trong ảnh gửi lên.");
        }

        return ResponseEntity.ok("✅ Đăng ký thành công! Đã lưu " + successCount + " dữ liệu khuôn mặt.");
    }
}