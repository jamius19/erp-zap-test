/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Atomically revokes one token. Returns 0 if it was already revoked (e.g. a concurrent refresh won the race).
	 */
	@Modifying
	@Query("update RefreshToken t set t.revoked = true where t.id = :id and t.revoked = false")
	int revoke(@Param("id") Long id);

	@Modifying
	@Query("update RefreshToken t set t.revoked = true where t.userId = :userId and t.revoked = false")
	int revokeAllForUser(@Param("userId") Long userId);
}
