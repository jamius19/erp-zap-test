/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
		@NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "letters, digits, _ or - only")
		String code,
		@NotBlank @Size(max = 100) String name
) {
}
