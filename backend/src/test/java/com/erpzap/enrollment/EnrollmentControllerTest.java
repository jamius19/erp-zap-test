/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.enrollment;

import com.erpzap.schoolclass.AcademicIntegrationTest;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import com.erpzap.teacher.Teacher;
import com.erpzap.user.Role;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnrollmentControllerTest extends AcademicIntegrationTest {

	@Test
	void classTeacherCanEnrollListAndRemove() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student student = newStudent();
		String body = "{\"studentId\":%d}".formatted(student.getId());

		mvc.perform(post("/api/classes/{id}/students", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(student.getId()))
				.andExpect(jsonPath("$.username").value(student.getUser().getUsername()));

		mvc.perform(post("/api/classes/{id}/students", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/classes/{id}/students", c.getId()).with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].studentNo").value(student.getStudentNo()));

		mvc.perform(delete("/api/classes/{id}/students/{sid}", c.getId(), student.getId()).with(as(teacher)))
				.andExpect(status().isNoContent());
		mvc.perform(delete("/api/classes/{id}/students/{sid}", c.getId(), student.getId()).with(as(teacher)))
				.andExpect(status().isNotFound());
	}

	@Test
	void otherTeacherCannotEnrollOrRemove() throws Exception {
		SchoolClass c = newClass(newTeacher());
		Teacher other = newTeacher();
		Student student = newStudent();

		mvc.perform(post("/api/classes/{id}/students", c.getId()).with(as(other))
						.contentType(APPLICATION_JSON).content("{\"studentId\":%d}".formatted(student.getId())))
				.andExpect(status().isForbidden());

		enroll(c, student);
		mvc.perform(delete("/api/classes/{id}/students/{sid}", c.getId(), student.getId()).with(as(other)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanEnrollAndStudentCannotList() throws Exception {
		SchoolClass c = newClass(null);
		Student student = newStudent();

		mvc.perform(post("/api/classes/{id}/students", c.getId()).with(as(newUser(Role.ADMIN)))
						.contentType(APPLICATION_JSON).content("{\"studentId\":%d}".formatted(student.getId())))
				.andExpect(status().isCreated());
		mvc.perform(get("/api/classes/{id}/students", c.getId()).with(as(student)))
				.andExpect(status().isForbidden());
	}
}
