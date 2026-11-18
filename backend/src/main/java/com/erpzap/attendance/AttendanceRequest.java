/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AttendanceRequest(
		@NotNull LocalDate date,
		@NotEmpty @Size(max = 500) List<@Valid @NotNull Entry> records
) {

	public record Entry(@NotNull Long studentId, @NotNull AttendanceStatus status) {
	}
}
