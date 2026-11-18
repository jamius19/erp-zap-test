/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.student;

import com.erpzap.auth.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class StudentDtos {

	private StudentDtos() {
	}

	public record StudentResponse(
			Long id, Long userId, String username, String fullName, String email, String studentNo,
			LocalDate dateOfBirth, String guardianName, String guardianPhone
	) {
		public static StudentResponse of(Student s) {
			return new StudentResponse(
					s.getId(), s.getUser().getId(), s.getUser().getUsername(), s.getUser().getFullName(),
					s.getUser().getEmail(), s.getStudentNo(), s.getDateOfBirth(), s.getGuardianName(),
					s.getGuardianPhone()
			);
		}
	}

	public record CreateStudentRequest(
			@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
			@StrongPassword String password,
			@NotBlank @Size(max = 100) String fullName,
			@Email @Size(max = 150) String email,
			@NotBlank @Size(max = 30) String studentNo,
			@Past LocalDate dateOfBirth,
			@Size(max = 100) String guardianName,
			@Size(max = 30) @Pattern(regexp = "^[0-9+()\\- ]*$") String guardianPhone
	) {
	}

	public record UpdateStudentRequest(
			@NotBlank @Size(max = 100) String fullName,
			@Email @Size(max = 150) String email,
			@NotBlank @Size(max = 30) String studentNo,
			@Past LocalDate dateOfBirth,
			@Size(max = 100) String guardianName,
			@Size(max = 30) @Pattern(regexp = "^[0-9+()\\- ]*$") String guardianPhone
	) {
	}
}
