/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

	private final AnnouncementService announcementService;

	public AnnouncementController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public PagedModel<AnnouncementDto> list(Pageable pageable) {
		return new PagedModel<>(announcementService.list(pageable));
	}

	@PostMapping
	@ResponseStatus(CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public AnnouncementDto create(@Valid @RequestBody AnnouncementRequest req) {
		return announcementService.create(req);
	}

	/** ADMIN or the author (checked in the service). */
	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
	public void delete(@PathVariable Long id) {
		announcementService.delete(id);
	}
}
