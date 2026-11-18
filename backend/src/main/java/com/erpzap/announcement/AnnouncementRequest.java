/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
		@NotBlank @Size(max = 150) String title,
		@NotBlank @Size(max = 5000) String body,
		@NotNull Audience audience,
		Long classId
) {
}
