/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/classes")
public class SchoolClassController {

	private final SchoolClassService classService;

	public SchoolClassController(SchoolClassService classService) {
		this.classService = classService;
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public List<SchoolClassDto> list(@RequestParam(defaultValue = "false") boolean mine) {
		return classService.list(mine);
	}

	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public SchoolClassDto get(@PathVariable Long id) {
		return classService.get(id);
	}

	@PostMapping
	@ResponseStatus(CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public SchoolClassDto create(@Valid @RequestBody SchoolClassRequest req) {
		return classService.create(req);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public SchoolClassDto update(@PathVariable Long id, @Valid @RequestBody SchoolClassRequest req) {
		return classService.update(id, req);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void delete(@PathVariable Long id) {
		classService.delete(id);
	}
}
