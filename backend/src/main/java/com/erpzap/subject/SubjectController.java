/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

	private final SubjectService subjectService;

	public SubjectController(SubjectService subjectService) {
		this.subjectService = subjectService;
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public List<SubjectDto> list() {
		return subjectService.list();
	}

	@PostMapping
	@ResponseStatus(CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public SubjectDto create(@Valid @RequestBody SubjectRequest req) {
		return subjectService.create(req);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public SubjectDto update(@PathVariable Long id, @Valid @RequestBody SubjectRequest req) {
		return subjectService.update(id, req);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void delete(@PathVariable Long id) {
		subjectService.delete(id);
	}
}
