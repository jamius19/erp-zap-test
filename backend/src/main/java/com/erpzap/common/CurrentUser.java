/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.common;

import com.erpzap.user.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the authenticated principal from the JWT in the security context.
 * Access token claims: sub = user id, "username", "role" (ADMIN | TEACHER | STUDENT).
 */
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Long id() {
		return Long.valueOf(jwt().getSubject());
	}

	public static Role role() {
		return Role.valueOf(jwt().getClaimAsString("role"));
	}

	public static boolean is(Role role) {
		return role() == role;
	}

	private static Jwt jwt() {
		return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}
}
