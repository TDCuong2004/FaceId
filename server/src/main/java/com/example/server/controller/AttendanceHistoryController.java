package com.example.server.controller;

import com.example.server.entity.EntityAttendance;
import com.example.server.repository.RepositoryAttendance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class AttendanceHistoryController {

    @Autowired
    private RepositoryAttendance attendanceRepository;

    // API: GET /api/history?date=2023-10-25
    @GetMapping
    public List<EntityAttendance> getHistoryByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now(); // Nếu không gửi ngày thì lấy hôm nay
        }

        // Tạo khoảng thời gian từ 00:00:00 đến 23:59:59 của ngày đó
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return attendanceRepository.findByCheckInTimeBetween(startOfDay, endOfDay);
    }

    @GetMapping("/month")
    public List<EntityAttendance> getHistoryByMonth(
            @RequestParam int month,
            @RequestParam int year
    ) {
        // 1. Xác định tháng năm
        YearMonth yearMonth = YearMonth.of(year, month);

        // 2. Lấy ngày đầu tháng (ví dụ: 01/10/2023 00:00:00)
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();

        // 3. Lấy ngày cuối tháng (ví dụ: 31/10/2023 23:59:59)
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        // 4. Gọi Repository tìm kiếm trong khoảng này
        return attendanceRepository.findByCheckInTimeBetween(start, end);
    }
}