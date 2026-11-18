/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Binds the {@code app.*} block of application.yml.
 */
@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, RefreshCookie refreshCookie, Cors cors, String uploadDir, Seed seed) {

	public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
	}

	public record RefreshCookie(String name, boolean secure) {
	}

	public record Cors(List<String> allowedOrigins) {
	}

	public record Seed(boolean enabled) {
	}
}
