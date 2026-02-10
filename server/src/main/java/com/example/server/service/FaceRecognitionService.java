package com.example.server.service;

import com.example.server.dto.AttendanceResponse;
import com.example.server.entity.EntityFaceEmbeddings;
import com.example.server.repository.RepositoryFaceEmbeddings;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Base64;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class FaceRecognitionService {

    @Autowired
    private RepositoryFaceEmbeddings embeddingRepository;

    private CascadeClassifier faceDetector;

    /** ================== INIT HAAR ================== */
    public FaceRecognitionService() {
        try {
            File cascadeFile = Loader.extractResource(
                    "haarcascade_frontalface_default.xml",
                    null,
                    "cascade",
                    ".xml"
            );
            faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());

            if (faceDetector.empty()) {
                throw new RuntimeException("❌ HaarCascade load thất bại");
            }

            System.out.println("✅ HaarCascade loaded: " + cascadeFile.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("❌ Không load được HaarCascade", e);
        }
    }

    /** ================== API ĐIỂM DANH ================== */
    public AttendanceResponse attendance(String base64Image) {

        Mat image = decodeBase64ToMat(base64Image);
        if (image.empty()) {
            return new AttendanceResponse(
                    false,
                    null,
                    null,
                    null,
                    0,
                    "❌ Ảnh không hợp lệ"
            );

        }

        Mat face = detectFace(image);
        if (face == null) {
            return new AttendanceResponse(
                    false,
                    null,
                    null,
                    null,
                    0,
                    "❌ Không phát hiện khuôn mặt"
            );

        }

        double[] inputEmbedding = extractEmbedding(face);

        List<EntityFaceEmbeddings> embeddings = embeddingRepository.findAll();

        double bestScore = 0;
        EntityFaceEmbeddings bestMatch = null;

        for (EntityFaceEmbeddings e : embeddings) {
            double[] dbEmbedding = parseEmbedding(e.getEmbedding());
            double similarity = cosineSimilarity(inputEmbedding, dbEmbedding);

            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = e;
            }
        }

        if (bestScore > 0.75 && bestMatch != null) {
            return new AttendanceResponse(
                    true,
                    bestMatch.getStudentCode(),
                    bestMatch.getStudent() != null
                            ? bestMatch.getStudent().getFullName()
                            : null,
                    bestMatch.getStudent() != null
                            ? bestMatch.getStudent().getClassName()
                            : null,
                    bestScore,
                    "✅ Điểm danh thành công"
            );

        }

        return new AttendanceResponse(
                false,
                null,
                null,
                null,
                bestScore,
                "❌ Không nhận diện được khuôn mặt"
        );

    }

    /** ================== SUPPORT ================== */

    private Mat decodeBase64ToMat(String base64) {
        try {
            String data = base64.split(",")[1];
            byte[] bytes = Base64.getDecoder().decode(data);
            BytePointer bp = new BytePointer(bytes);
            return imdecode(new Mat(bp), IMREAD_COLOR);
        } catch (Exception e) {
            return new Mat();
        }
    }

    /** ✅ DETECT FACE BẰNG HAAR (ĐÚNG CHUẨN) */
    private Mat detectFace(Mat image) {
        Mat gray = new Mat();
        cvtColor(image, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(
                gray,
                faces,
                1.1,
                5,
                0,
                new Size(100, 100),
                new Size()
        );

        if (faces.size() == 0) return null;

        Rect r = faces.get(0);
        return new Mat(image, r);
    }

    /** ⚠️ GIẢ LẬP – SAU NÀY THAY FACENET / ARCFACE */
    private double[] extractEmbedding(Mat face) {
        return new double[]{0.12, 0.45, 0.78};
    }

    private double[] parseEmbedding(String embedding) {
        embedding = embedding.replace("[", "").replace("]", "");
        String[] parts = embedding.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        return result;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
