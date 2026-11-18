/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.student;

import com.erpzap.student.StudentDtos.CreateStudentRequest;
import com.erpzap.student.StudentDtos.StudentResponse;
import com.erpzap.student.StudentDtos.UpdateStudentRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/students")
public class StudentController {

	private final StudentService studentService;

	public StudentController(StudentService studentService) {
		this.studentService = studentService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public Page<StudentResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
		return studentService.list(q, pageable);
	}

	/** Students may only read their own record; enforced in the service. */
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
	public StudentResponse get(@PathVariable Long id) {
		return studentService.get(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<StudentResponse> create(@Valid @RequestBody CreateStudentRequest request) {
		return ResponseEntity.status(CREATED).body(studentService.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
		return studentService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		studentService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
