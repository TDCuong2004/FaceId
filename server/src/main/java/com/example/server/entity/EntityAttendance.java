package com.example.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_checkin")
@Data
public class EntityAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    // Join để lấy tên sinh viên hiển thị cho đẹp
    @ManyToOne(fetch = FetchType.EAGER) // EAGER để đảm bảo nó luôn tải thông tin sinh viên kèm theo
    @JoinColumn(
            name = "student_code",
            referencedColumnName = "student_code", // 👈 QUAN TRỌNG: Chỉ định nối vào cột student_code chứ không phải ID
            insertable = false,
            updatable = false
    )
    private EntityStudents student;
}