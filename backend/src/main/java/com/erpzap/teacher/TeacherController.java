/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.teacher;

import com.erpzap.teacher.TeacherDtos.CreateTeacherRequest;
import com.erpzap.teacher.TeacherDtos.TeacherResponse;
import com.erpzap.teacher.TeacherDtos.UpdateTeacherRequest;
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
@RequestMapping("/api/teachers")
public class TeacherController {

	private final TeacherService teacherService;

	public TeacherController(TeacherService teacherService) {
		this.teacherService = teacherService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public Page<TeacherResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
		return teacherService.list(q, pageable);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public TeacherResponse get(@PathVariable Long id) {
		return teacherService.get(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<TeacherResponse> create(@Valid @RequestBody CreateTeacherRequest request) {
		return ResponseEntity.status(CREATED).body(teacherService.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequest request) {
		return teacherService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		teacherService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
