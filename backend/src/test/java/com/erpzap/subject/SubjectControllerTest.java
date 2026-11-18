/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

import com.erpzap.schoolclass.AcademicIntegrationTest;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubjectControllerTest extends AcademicIntegrationTest {

	@Test
	void adminCrudAndOthersReadOnly() throws Exception {
		User admin = newUser(Role.ADMIN);
		String code = "C" + uid().toUpperCase();
		String body = """
				{"code":"%s","name":"Art"}
				""".formatted(code);

		long id = idOf(mvc.perform(post("/api/subjects").with(as(admin)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(code))
				.andReturn());
		mvc.perform(post("/api/subjects").with(as(admin)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/subjects").with(as(newStudent())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].code", hasItem(code)));
		mvc.perform(post("/api/subjects").with(as(newTeacher())).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());

		mvc.perform(put("/api/subjects/{id}", id).with(as(admin)).contentType(APPLICATION_JSON)
						.content("{\"code\":\"%s\",\"name\":\"Fine Art\"}".formatted(code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Fine Art"));
		mvc.perform(delete("/api/subjects/{id}", id).with(as(admin))).andExpect(status().isNoContent());
		mvc.perform(delete("/api/subjects/{id}", id).with(as(admin))).andExpect(status().isNotFound());
	}
}
