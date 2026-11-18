/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.config;

import com.erpzap.IdentityTestSupport;
import com.erpzap.user.User;
import org.junit.jupiter.api.Test;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityHeadersTest extends IdentityTestSupport {

	@Test
	void apiResponsesCarrySecurityHeaders() throws Exception {
		User admin = createAdmin();

		mvc.perform(get("/api/auth/me").secure(true).header(AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
				.andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
				.andExpect(header().doesNotExist("X-Powered-By"));
	}

	@Test
	void unauthenticatedErrorIsProblemJsonWithHeaders() throws Exception {
		mvc.perform(get("/api/teachers"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
	}

	@Test
	void healthAndApiDocsArePublic() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void corsAllowsOnlyConfiguredOrigin() throws Exception {
		mvc.perform(options("/api/auth/login").header(ORIGIN, "http://localhost:5173")
						.header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
		mvc.perform(options("/api/auth/login").header(ORIGIN, "https://evil.example")
						.header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
