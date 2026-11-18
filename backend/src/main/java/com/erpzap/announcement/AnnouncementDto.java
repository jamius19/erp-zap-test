/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import java.time.Instant;

/** `body` is plain text; clients must render it as text, never as HTML. */
public record AnnouncementDto(
		Long id,
		String title,
		String body,
		Audience audience,
		Long classId,
		String className,
		Long authorId,
		String authorName,
		Instant createdAt
) {
}
