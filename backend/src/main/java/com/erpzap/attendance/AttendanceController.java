/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api")
public class AttendanceController {

	private final AttendanceService attendanceService;

	public AttendanceController(AttendanceService attendanceService) {
		this.attendanceService = attendanceService;
	}

	@GetMapping("/classes/{classId}/attendance")
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public List<AttendanceRecordDto> forClass(
			@PathVariable Long classId,
			@RequestParam @DateTimeFormat(iso = DATE) LocalDate date
	) {
		return attendanceService.forClass(classId, date);
	}

	/** ADMIN or the class teacher (checked in the service). */
	@PutMapping("/classes/{classId}/attendance")
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public List<AttendanceRecordDto> save(@PathVariable Long classId, @Valid @RequestBody AttendanceRequest req) {
		return attendanceService.save(classId, req);
	}

	/** ADMIN, TEACHER, or the owning STUDENT (checked in the service). */
	@GetMapping("/students/{studentId}/attendance")
	@PreAuthorize("isAuthenticated()")
	public List<AttendanceRecordDto> forStudent(
			@PathVariable Long studentId,
			@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate to
	) {
		return attendanceService.forStudent(studentId, from, to);
	}
}
