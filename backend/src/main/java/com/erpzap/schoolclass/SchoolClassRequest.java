/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SchoolClassRequest(
		@NotBlank @Size(max = 50) String name,
		@NotNull @Min(1) @Max(12) Integer gradeLevel,
		@NotBlank @Size(max = 10) String section,
		@NotBlank @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "must look like 2026-2027") String academicYear,
		Long teacherId
) {
}
