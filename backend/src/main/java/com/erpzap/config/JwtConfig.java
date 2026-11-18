/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * HS256 access tokens signed with the base64 secret from {@code app.jwt.secret}, plus the BCrypt password encoder.
 */
@Configuration
public class JwtConfig {

	private final SecretKey key;

	public JwtConfig(AppProperties props) {
		byte[] bytes = Base64.getDecoder().decode(props.jwt().secret());
		if (bytes.length < 32) {
			throw new IllegalStateException("app.jwt.secret must decode to at least 32 bytes");
		}
		this.key = new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
