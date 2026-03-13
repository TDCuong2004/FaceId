package com.example.server.controller;
import com.example.server.repository.RepositoryStudents;
import com.example.server.entity.EntityStudents;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/students")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ControllerStudents {
    private final RepositoryStudents repositorystudents;

    @GetMapping
    public List<EntityStudents> getall(){
        return repositorystudents.findAll();
    }
    @PostMapping
    public ResponseEntity<?> create(@RequestBody EntityStudents student) {

        if (repositorystudents.existsByStudentCode(student.getStudentCode())) {
            return ResponseEntity
                    .badRequest()
                    .body("Mã sinh viên đã tồn tại");
        }

        return ResponseEntity.ok(
                repositorystudents.save(student)
        );
    }
    @DeleteMapping("/{studentCode}")
    public ResponseEntity<?> delete(
            @PathVariable String studentCode
    ) {
        return repositorystudents.findByStudentCode(studentCode)
                .map(student -> {
                    repositorystudents.delete(student);
                    return ResponseEntity.ok("Đã xóa sinh viên");
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
