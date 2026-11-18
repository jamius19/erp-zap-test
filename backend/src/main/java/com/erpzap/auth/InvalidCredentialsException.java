/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

/**
 * Login or refresh failed. Always carries the same generic message so callers learn nothing about why.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid credentials");
	}
}
