/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import java.time.LocalDate;

public record AttendanceRecordDto(
		Long id,
		Long classId,
		String className,
		Long studentId,
		String studentName,
		LocalDate date,
		AttendanceStatus status
) {
}
