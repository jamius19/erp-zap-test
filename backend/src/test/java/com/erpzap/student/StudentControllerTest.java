/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.student;

import com.erpzap.IdentityTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentControllerTest extends IdentityTestSupport {

	@Test
	void studentCanReadOnlyOwnRecordAndCannotList() throws Exception {
		Student me = createStudent();
		Student other = createStudent();
		String bearer = bearer(me.getUser());

		mvc.perform(get("/api/students/" + me.getId()).header(AUTHORIZATION, bearer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.studentNo").value(me.getStudentNo()));
		mvc.perform(get("/api/students/" + other.getId()).header(AUTHORIZATION, bearer))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/students/999999999").header(AUTHORIZATION, bearer))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/students").header(AUTHORIZATION, bearer))
				.andExpect(status().isForbidden());
	}

	@Test
	void teacherCanListAndReadButNotWrite() throws Exception {
		Student student = createStudent();
		String teacher = bearer(createTeacher().getUser());

		mvc.perform(get("/api/students").param("q", student.getStudentNo()).header(AUTHORIZATION, teacher))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(student.getId()));
		mvc.perform(get("/api/students/" + student.getId()).header(AUTHORIZATION, teacher))
				.andExpect(status().isOk());
		mvc.perform(delete("/api/students/" + student.getId()).header(AUTHORIZATION, teacher))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCrud() throws Exception {
		String admin = bearer(createAdmin());
		String username = unique("s_");

		String created = mvc.perform(post("/api/students").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content(createJson(username, unique("S"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.dateOfBirth").value("2015-04-02"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");

		mvc.perform(put("/api/students/" + id).header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content("""
								{"fullName": "Kid Updated", "email": null, "studentNo": "%s",
								 "dateOfBirth": "2015-05-05", "guardianName": "Mom", "guardianPhone": "555"}
								""".formatted(unique("S"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Kid Updated"))
				.andExpect(jsonPath("$.guardianName").value("Mom"));

		mvc.perform(delete("/api/students/" + id).header(AUTHORIZATION, admin)).andExpect(status().isNoContent());
		mvc.perform(get("/api/students/" + id).header(AUTHORIZATION, admin)).andExpect(status().isNotFound());
	}

	@Test
	void duplicateStudentNoIs409AndInvalidIs400() throws Exception {
		String admin = bearer(createAdmin());
		Student existing = createStudent();

		mvc.perform(post("/api/students").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content(createJson(unique("s_"), existing.getStudentNo())))
				.andExpect(status().isConflict());
		mvc.perform(post("/api/students").header(AUTHORIZATION, admin).contentType(APPLICATION_JSON)
						.content("""
								{"username": "%s", "password": "12345678", "fullName": "Kid",
								 "studentNo": "X1", "dateOfBirth": "2999-01-01"}
								""".formatted(unique("s_"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists())
				.andExpect(jsonPath("$.errors.dateOfBirth").exists());
	}

	private static String createJson(String username, String studentNo) {
		return """
				{"username": "%s", "password": "Secret123", "fullName": "Kid Student", "email": "%s@x.test",
				 "studentNo": "%s", "dateOfBirth": "2015-04-02", "guardianName": "Dad", "guardianPhone": "+1 555"}
				""".formatted(username, username, studentNo);
	}
}
