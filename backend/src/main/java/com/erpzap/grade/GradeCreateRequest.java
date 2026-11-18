/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GradeCreateRequest(
		@NotNull Long classId,
		@NotNull Long studentId,
		@NotNull Long subjectId,
		@NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "letters, digits, _ or - only")
		String term,
		@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal score,
		@Size(max = 255) String remarks
) {
}
