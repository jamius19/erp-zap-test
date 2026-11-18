/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import com.erpzap.user.Role;
import com.erpzap.user.UserSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record LoginRequest(
			@NotBlank @Size(max = 50) String username,
			@NotBlank @Size(max = 200) String password
	) {
	}

	public record LoginResponse(String accessToken, long expiresIn, UserSummary user) {
	}

	public record ChangePasswordRequest(
			@NotBlank @Size(max = 200) String currentPassword,
			@StrongPassword String newPassword
	) {
	}

	public record MeResponse(
			Long id, String username, String fullName, String email, Role role, boolean hasPhoto,
			Long teacherId, Long studentId
	) {
	}
}
