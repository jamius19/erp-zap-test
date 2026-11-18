/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.teacher;

import com.erpzap.IdentityTestSupport;
import com.erpzap.student.Student;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherControllerTest extends IdentityTestSupport {

	@Test
	void adminCanCreateReadUpdateSearchAndDelete() throws Exception {
		String admin = bearer(createAdmin());
		String username = unique("t_");

		String created = mvc.perform(post("/api/teachers").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content(createJson(username, username + "@x.test", unique("EMP"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.fullName").value("Jane Teacher"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");
		Integer userId = JsonPath.read(created, "$.userId");

		mvc.perform(get("/api/teachers/" + id).header(AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(userId));

		mvc.perform(put("/api/teachers/" + id).header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content("""
								{"fullName": "Jane Updated", "email": null, "employeeNo": "%s", "phone": "123"}
								""".formatted(unique("EMP"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Jane Updated"))
				.andExpect(jsonPath("$.phone").value("123"));

		mvc.perform(get("/api/teachers").param("q", username.toUpperCase()).param("sort", "fullName,asc")
						.header(AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].username").value(username))
				.andExpect(jsonPath("$.page.totalElements").value(1));

		mvc.perform(delete("/api/teachers/" + id).header(AUTHORIZATION, admin))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/teachers/" + id).header(AUTHORIZATION, admin))
				.andExpect(status().isNotFound());
		assertThat(userRepository.findById(userId.longValue())).isEmpty();
	}

	@Test
	void searchTreatsLikeWildcardsLiterally() throws Exception {
		String admin = bearer(createAdmin());

		mvc.perform(get("/api/teachers").param("q", "%").header(AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void pageSizeIsCappedAndUnknownSortRejected() throws Exception {
		String admin = bearer(createAdmin());

		mvc.perform(get("/api/teachers").param("size", "1000").header(AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(100));
		mvc.perform(get("/api/teachers").param("sort", "user.passwordHash").header(AUTHORIZATION, admin))
				.andExpect(status().isBadRequest());
	}

	@Test
	void validationErrorsReturn400WithFieldMap() throws Exception {
		String admin = bearer(createAdmin());

		mvc.perform(post("/api/teachers").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content("""
								{"username": "x y", "password": "letters", "fullName": "", "email": "bad",
								 "employeeNo": ""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists())
				.andExpect(jsonPath("$.errors.fullName").exists())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.employeeNo").exists());
	}

	@Test
	void duplicateUsernameOrEmailIs409() throws Exception {
		String admin = bearer(createAdmin());
		Teacher existing = createTeacher();

		mvc.perform(post("/api/teachers").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content(createJson(existing.getUser().getUsername(), null, unique("EMP"))))
				.andExpect(status().isConflict());
		mvc.perform(post("/api/teachers").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content(createJson(unique("t_"), existing.getUser().getEmail(), unique("EMP"))))
				.andExpect(status().isConflict());
	}

	@Test
	void roleChecks() throws Exception {
		String teacher = bearer(createTeacher().getUser());
		Student student = createStudent();
		String studentBearer = bearer(student.getUser());

		mvc.perform(get("/api/teachers").header(AUTHORIZATION, teacher)).andExpect(status().isOk());
		mvc.perform(post("/api/teachers").header(AUTHORIZATION, teacher).contentType(APPLICATION_JSON)
						.content(createJson(unique("t_"), null, unique("EMP"))))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/teachers").header(AUTHORIZATION, studentBearer)).andExpect(status().isForbidden());
	}

	private static String createJson(String username, String email, String employeeNo) {
		String emailJson = email == null ? "null" : "\"" + email + "\"";
		return """
				{"username": "%s", "password": "Secret123", "fullName": "Jane Teacher", "email": %s,
				 "employeeNo": "%s", "phone": "+1 555 0100"}
				""".formatted(username, emailJson, employeeNo);
	}
}
