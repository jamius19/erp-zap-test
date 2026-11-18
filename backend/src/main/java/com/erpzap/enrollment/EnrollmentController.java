/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.enrollment;

import com.erpzap.student.StudentDtos.StudentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/classes/{classId}/students")
public class EnrollmentController {

	private final EnrollmentService enrollmentService;

	public EnrollmentController(EnrollmentService enrollmentService) {
		this.enrollmentService = enrollmentService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public List<StudentResponse> list(@PathVariable Long classId) {
		return enrollmentService.list(classId);
	}

	/** ADMIN or the class teacher (checked in the service). */
	@PostMapping
	@ResponseStatus(CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public StudentResponse add(@PathVariable Long classId, @Valid @RequestBody EnrollmentRequest req) {
		return enrollmentService.add(classId, req.studentId());
	}

	@DeleteMapping("/{studentId}")
	@ResponseStatus(NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public void remove(@PathVariable Long classId, @PathVariable Long studentId) {
		enrollmentService.remove(classId, studentId);
	}
}
