/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Stores only the SHA-256 hex of the opaque refresh token, never the token itself.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	protected RefreshToken() {
	}

	public RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public Long getId() { return id; }
	public Long getUserId() { return userId; }
	public String getTokenHash() { return tokenHash; }
	public Instant getExpiresAt() { return expiresAt; }
	public boolean isRevoked() { return revoked; }
	public Instant getCreatedAt() { return createdAt; }
}
