/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	/** Shape depends on the caller's role: AdminStats, TeacherStats or StudentStats. */
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public Object get() {
		return dashboardService.forCurrentUser();
	}
}
