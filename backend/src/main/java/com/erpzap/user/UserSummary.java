/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.user;

public record UserSummary(Long id, String username, String fullName, String email, Role role, boolean hasPhoto) {

	public static UserSummary of(User user) {
		return new UserSummary(
				user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), user.getRole(),
				user.getPhotoPath() != null
		);
	}
}
