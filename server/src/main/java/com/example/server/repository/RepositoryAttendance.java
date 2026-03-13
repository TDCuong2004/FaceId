package com.example.server.repository;

import com.example.server.entity.EntityAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.server.entity.*;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RepositoryAttendance extends JpaRepository<EntityAttendance, Long> {

    // Tìm kiếm trong khoảng thời gian (từ đầu ngày đến cuối ngày)
    List<EntityAttendance> findByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);
    //Kiểm tra xem sinh viên đã điểm danh trong khoảng thời gian này chưa
    boolean existsByStudentCodeAndCheckInTimeBetween(String studentCode, LocalDateTime start, LocalDateTime end);
}