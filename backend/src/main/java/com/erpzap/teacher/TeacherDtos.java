/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.teacher;

import com.erpzap.auth.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TeacherDtos {

	private TeacherDtos() {
	}

	public record TeacherResponse(
			Long id, Long userId, String username, String fullName, String email, String employeeNo, String phone
	) {
		public static TeacherResponse of(Teacher t) {
			return new TeacherResponse(
					t.getId(), t.getUser().getId(), t.getUser().getUsername(), t.getUser().getFullName(),
					t.getUser().getEmail(), t.getEmployeeNo(), t.getPhone()
			);
		}
	}

	public record CreateTeacherRequest(
			@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
			@StrongPassword String password,
			@NotBlank @Size(max = 100) String fullName,
			@Email @Size(max = 150) String email,
			@NotBlank @Size(max = 30) String employeeNo,
			@Size(max = 30) @Pattern(regexp = "^[0-9+()\\- ]*$") String phone
	) {
	}

	public record UpdateTeacherRequest(
			@NotBlank @Size(max = 100) String fullName,
			@Email @Size(max = 150) String email,
			@NotBlank @Size(max = 30) String employeeNo,
			@Size(max = 30) @Pattern(regexp = "^[0-9+()\\- ]*$") String phone
	) {
	}
}
