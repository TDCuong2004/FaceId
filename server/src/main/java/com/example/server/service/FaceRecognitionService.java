package com.example.server.service;

import com.example.server.dto.AttendanceResponse;
import com.example.server.entity.EntityAttendance;
import com.example.server.entity.EntityFaceEmbeddings;
import com.example.server.repository.RepositoryAttendance;
import com.example.server.repository.RepositoryFaceEmbeddings;
import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer; // ✅ Đã thêm import quan trọng này
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class FaceRecognitionService {

    @Autowired
    private RepositoryFaceEmbeddings embeddingRepository;

    @Autowired
    private RepositoryAttendance attendanceRepository;

    private CascadeClassifier faceDetector;
    private Net recognizer; // Model AI nhận diện (SFace)

    @PostConstruct
    public void init() {
        try {
            // 1. Load HaarCascade
            File cascadeFile = Loader.extractResource("haarcascade_frontalface_default.xml", null, "cascade", ".xml");
            faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());

            // 2. Load Model ONNX (SFace)
            String modelPath = "face_recognition_sface_2021dec.onnx";
            File f = new File(modelPath);
            if (!f.exists()) {
                System.err.println("❌ KHÔNG TÌM THẤY FILE MODEL: " + modelPath);
            } else {
                recognizer = readNetFromONNX(modelPath);
                System.out.println("✅ Đã load Model nhận diện khuôn mặt: SFace");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === API ĐIỂM DANH ===
    public AttendanceResponse attendance(String base64Image) {
        Mat image = decodeBase64ToMat(base64Image);
        if (image.empty())
            return new AttendanceResponse(false, null, null, null, 0, "❌ Ảnh lỗi", false);

        Mat face = detectFace(image);
        if (face == null)
            return new AttendanceResponse(false, null, null, null, 0, "❌ Không thấy mặt", false);

        // Trích xuất đặc trưng thật
        float[] inputEmbedding = extractEmbedding(face);
        if (inputEmbedding == null)
            return new AttendanceResponse(false, null, null, null, 0, "❌ Lỗi AI", false);

        // So sánh với Database
        List<EntityFaceEmbeddings> list = embeddingRepository.findAll();
        double bestScore = 0;
        EntityFaceEmbeddings bestMatch = null;

        for (EntityFaceEmbeddings item : list) {
            float[] dbEmbedding = parseEmbedding(item.getEmbedding());
            double score = cosineSimilarity(inputEmbedding, dbEmbedding);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = item;
            }
        }

        // ✅ SỬA 1: Tăng ngưỡng lên 0.65 để tránh nhận nhầm người lạ
        double THRESHOLD = 0.65;

        if (bestScore > THRESHOLD && bestMatch != null) {

            String code = bestMatch.getStudentCode();
            String name = bestMatch.getStudent() != null ? bestMatch.getStudent().getFullName() : "Unknown";
            String clss = bestMatch.getStudent() != null ? bestMatch.getStudent().getClassName() : "Unknown";

            // ✅ SỬA 2: Logic kiểm tra đã điểm danh
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

            boolean exists = attendanceRepository.existsByStudentCodeAndCheckInTimeBetween(code, start, end);

            if (exists) {
                // Đã điểm danh rồi -> Trả về true nhưng kèm cờ alreadyCheckedIn = true
                return new AttendanceResponse(
                        true,
                        code,
                        name,
                        clss,
                        bestScore,
                        "⚠️ Hôm nay " + name + " đã điểm danh rồi!",
                        true // <--- Cờ báo hiệu màu vàng
                );
            }

            // Chưa điểm danh -> Lưu vào DB
            EntityAttendance history = new EntityAttendance();
            history.setStudentCode(code);
            history.setCheckInTime(LocalDateTime.now());
            attendanceRepository.save(history);

            return new AttendanceResponse(
                    true,
                    code,
                    name,
                    clss,
                    bestScore,
                    "✅ Điểm danh thành công",
                    false // <--- Cờ báo hiệu màu xanh (mới)
            );
        }

        // Không nhận diện được ai (Score thấp)
        return new AttendanceResponse(
                false,
                null,
                null,
                null,
                bestScore,
                "❌ Không nhận diện được (Độ giống: " + String.format("%.2f", bestScore) + ")",
                false
        );
    }

    // === CÁC HÀM HỖ TRỢ ===

    public Mat decodeBase64ToMat(String base64) {
        try {
            String data = base64.contains(",") ? base64.split(",")[1] : base64;
            byte[] bytes = Base64.getDecoder().decode(data);
            return imdecode(new Mat(new BytePointer(bytes)), IMREAD_COLOR);
        } catch (Exception e) { return new Mat(); }
    }

    public Mat detectFace(Mat image) {
        Mat gray = new Mat();
        cvtColor(image, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray); // Tăng cường độ tương phản
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces, 1.1, 5, 0, new Size(100, 100), new Size());
        if (faces.size() == 0) return null;
        return new Mat(image, faces.get(0));
    }

    // ✅ SỬA 3: Dùng FloatPointer để sửa lỗi "asFloatBuffer"
    public float[] extractEmbedding(Mat face) {
        if (recognizer == null) return null;

        // Chuẩn hóa ảnh về 112x112 float32
        Mat blob = blobFromImage(face, 1.0, new Size(112, 112), new Scalar(0, 0, 0, 0), true, false, CV_32F);
        recognizer.setInput(blob);
        Mat output = recognizer.forward();

        float[] embedding = new float[128];

        // Dùng FloatPointer để lấy dữ liệu an toàn
        FloatPointer ptr = new FloatPointer(output.ptr());
        ptr.get(embedding);

        return embedding;
    }

    public  float[] parseEmbedding(String text) {
        try {
            text = text.replace("[", "").replace("]", "");
            String[] parts = text.split(",");
            float[] res = new float[parts.length];
            for (int i = 0; i < parts.length; i++) res[i] = Float.parseFloat(parts[i].trim());
            return res;
        } catch (Exception e) {
            return new float[128]; // Trả về mảng rỗng nếu lỗi parse
        }
    }

    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}