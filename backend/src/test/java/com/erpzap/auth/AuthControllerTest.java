/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.auth;

import com.erpzap.IdentityTestSupport;
import com.erpzap.student.Student;
import com.erpzap.user.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IdentityTestSupport {

	@Test
	void loginReturnsTokenAndSecureRefreshCookie() throws Exception {
		User admin = createAdmin();

		MvcResult result = loginResult(admin.getUsername(), PASSWORD);

		String json = result.getResponse().getContentAsString();
		assertThat(json).contains("\"accessToken\"").contains("\"expiresIn\":900");
		assertThat(json).contains("\"username\":\"" + admin.getUsername() + "\"").contains("\"role\":\"ADMIN\"");
		assertThat(json).doesNotContain("password");
		String cookie = result.getResponse().getHeader(SET_COOKIE);
		assertThat(cookie).startsWith("refresh_token=").contains("HttpOnly").contains("SameSite=Strict")
				.contains("Path=/api/auth");
	}

	@Test
	void badPasswordAndUnknownUserGiveSameGeneric401() throws Exception {
		User admin = createAdmin();

		mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
						.content(loginJson(admin.getUsername(), "WrongPass1")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid credentials"));
		mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
						.content(loginJson("nobody_" + admin.getUsername(), "WrongPass1")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void disabledUserCannotLogin() throws Exception {
		User admin = createAdmin();
		admin.setEnabled(false);
		userRepository.save(admin);

		mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
						.content(loginJson(admin.getUsername(), PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsProfileWithStudentId() throws Exception {
		Student student = createStudent();

		mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(student.getUser())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(student.getUser().getUsername()))
				.andExpect(jsonPath("$.role").value("STUDENT"))
				.andExpect(jsonPath("$.studentId").value(student.getId()))
				.andExpect(jsonPath("$.teacherId").doesNotExist())
				.andExpect(jsonPath("$.hasPhoto").value(false));
	}

	@Test
	void meWithoutTokenIs401ProblemDetail() throws Exception {
		mvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
		mvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer not.a.jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refreshRotatesAndReuseRevokesEverything() throws Exception {
		User admin = createAdmin();
		Cookie first = refreshCookie(loginResult(admin.getUsername(), PASSWORD));

		MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();
		Cookie second = refreshCookie(refreshed);
		assertThat(second.getValue()).isNotEqualTo(first.getValue());

		// Reusing the rotated-out token is treated as theft: it fails and kills the new token too.
		mvc.perform(post("/api/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshWithoutOrWithBogusCookieIs401() throws Exception {
		mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "bogus")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesTokenAndClearsCookie() throws Exception {
		User admin = createAdmin();
		Cookie cookie = refreshCookie(loginResult(admin.getUsername(), PASSWORD));

		MvcResult result = mvc.perform(post("/api/auth/logout").cookie(cookie))
				.andExpect(status().isNoContent())
				.andReturn();

		assertThat(result.getResponse().getHeader(SET_COOKIE)).contains("Max-Age=0");
		mvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
	}

	@Test
	void changePasswordValidatesAndRevokesRefreshTokens() throws Exception {
		User admin = createAdmin();
		MvcResult login = loginResult(admin.getUsername(), PASSWORD);
		Cookie cookie = refreshCookie(login);
		String bearer = bearer(admin);

		mvc.perform(post("/api/auth/change-password").header(AUTHORIZATION, bearer).contentType(APPLICATION_JSON)
						.content(changeJson("WrongPass1", "NewPassw0rd")))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/auth/change-password").header(AUTHORIZATION, bearer).contentType(APPLICATION_JSON)
						.content(changeJson(PASSWORD, "short1")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.newPassword").exists());
		mvc.perform(post("/api/auth/change-password").header(AUTHORIZATION, bearer).contentType(APPLICATION_JSON)
						.content(changeJson(PASSWORD, "NewPassw0rd")))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
		loginResult(admin.getUsername(), "NewPassw0rd");
	}

	private static Cookie refreshCookie(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie("refresh_token");
		assertThat(cookie).isNotNull();
		return new Cookie("refresh_token", cookie.getValue());
	}

	private static String loginJson(String username, String password) {
		return """
				{"username": "%s", "password": "%s"}""".formatted(username, password);
	}

	private static String changeJson(String current, String next) {
		return """
				{"currentPassword": "%s", "newPassword": "%s"}""".formatted(current, next);
	}
}
