/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api")
public class GradeController {

	private final GradeService gradeService;

	public GradeController(GradeService gradeService) {
		this.gradeService = gradeService;
	}

	@GetMapping("/classes/{classId}/grades")
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public List<GradeDto> forClass(
			@PathVariable Long classId,
			@RequestParam(required = false) Long subjectId,
			@RequestParam(required = false) String term
	) {
		return gradeService.forClass(classId, subjectId, term == null ? null : term.trim().toUpperCase());
	}

	/** ADMIN, TEACHER, or the owning STUDENT (checked in the service). */
	@GetMapping("/students/{studentId}/grades")
	@PreAuthorize("isAuthenticated()")
	public List<GradeDto> forStudent(@PathVariable Long studentId) {
		return gradeService.forStudent(studentId);
	}

	/** ADMIN or the class teacher (checked in the service). */
	@PostMapping("/grades")
	@ResponseStatus(CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public GradeDto create(@Valid @RequestBody GradeCreateRequest req) {
		return gradeService.create(req);
	}

	@PutMapping("/grades/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public GradeDto update(@PathVariable Long id, @Valid @RequestBody GradeUpdateRequest req) {
		return gradeService.update(id, req);
	}

	@DeleteMapping("/grades/{id}")
	@ResponseStatus(NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public void delete(@PathVariable Long id) {
		gradeService.delete(id);
	}
}
