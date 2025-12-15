package com.example.college.controller;

import com.example.college.model.AttendanceModel;
import com.example.college.repository.AttendanceRepo;
import com.example.college.service.AttendanceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    private final AttendanceRepo repo;
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceRepo repo, AttendanceService attendanceService) {
        this.repo = repo;
        this.attendanceService = attendanceService;
    }

    // -----------------------------
    // DTO
    // -----------------------------
    public static class AttendanceDTO {
        private Long employeeId;
        private Boolean absent;
        private LocalDate date;

        public AttendanceDTO() {}

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public Boolean getAbsent() { return absent; }
        public void setAbsent(Boolean absent) { this.absent = absent; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }


    // ==========================================================
    // PUT /mark  → Mark Single Attendance
    // ==========================================================
    @PutMapping("/mark")
    public ResponseEntity<?> markSingle(@RequestBody AttendanceDTO dto) {

        // Validation — consistent style
        if (dto == null || dto.getEmployeeId() == null || dto.getAbsent() == null) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "employeeI and absent required")
            );
        }

        LocalDate markDate = dto.getDate() == null ? LocalDate.now() : dto.getDate();

        AttendanceModel result =
                attendanceService.markSingleAttendance(dto.getEmployeeId(), markDate, dto.getAbsent());

        return ResponseEntity.ok(result);
    }


    // ==========================================================
    // POST /mark-batch  → Mark Batch Attendance
    // ==========================================================
    @PostMapping("/mark-batch")
    @Transactional
    public ResponseEntity<Map<String, Object>> markBatch(@RequestBody List<AttendanceDTO> payload) {

        // Validation
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "payload is empty")
            );
        }

        List<AttendanceModel> saved = new ArrayList<>();
        List<Long> invalidEntries = new ArrayList<>();

        for (AttendanceDTO dto : payload) {

            // dto missing OR invalid? → track invalid employee
            if (dto == null || dto.getEmployeeId() == null || dto.getAbsent() == null) {
                if (dto != null && dto.getEmployeeId() != null) {
                    invalidEntries.add(dto.getEmployeeId());
                }
                continue;
            }

            LocalDate markDate = dto.getDate() == null ? LocalDate.now() : dto.getDate();
            AttendanceModel rec = attendanceService.markSingleAttendance(
                    dto.getEmployeeId(), markDate, dto.getAbsent());

            saved.add(rec);
        }

        long absentCount = saved.stream().filter(AttendanceModel::getAbsent).count();
        long presentCount = saved.size() - absentCount;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("savedCount", saved.size());
        response.put("savedRecords", saved);
        response.put("presentCount", presentCount);
        response.put("absentCount", absentCount);
        response.put("invalidEntries", invalidEntries); // FIXED incorrect key name

        return ResponseEntity.ok(response);
    }
}
