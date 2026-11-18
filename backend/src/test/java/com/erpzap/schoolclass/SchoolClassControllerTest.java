/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import com.erpzap.student.Student;
import com.erpzap.teacher.Teacher;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SchoolClassControllerTest extends AcademicIntegrationTest {

	@Test
	void adminCanCreateReadUpdateAndDeleteClass() throws Exception {
		User admin = newUser(Role.ADMIN);
		Teacher teacher = newTeacher();
		String section = uid();
		String body = """
				{"name":"Grade 9 - %s","gradeLevel":9,"section":"%s","academicYear":"2026-2027","teacherId":%d}
				""".formatted(section, section, teacher.getId());

		long id = idOf(mvc.perform(post("/api/classes").with(as(admin)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.teacherId").value(teacher.getId()))
				.andExpect(jsonPath("$.teacherName").value(teacher.getUser().getFullName()))
				.andExpect(jsonPath("$.studentCount").value(0))
				.andReturn());

		mvc.perform(post("/api/classes").with(as(admin)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());

		enroll(classRepository.findById(id).orElseThrow(), newStudent());
		mvc.perform(get("/api/classes/{id}", id).with(as(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.studentCount").value(1));

		String update = """
				{"name":"Renamed","gradeLevel":9,"section":"%s","academicYear":"2026-2027"}
				""".formatted(section);
		mvc.perform(put("/api/classes/{id}", id).with(as(admin)).contentType(APPLICATION_JSON).content(update))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed"))
				.andExpect(jsonPath("$.teacherId").doesNotExist());

		mvc.perform(delete("/api/classes/{id}", id).with(as(admin))).andExpect(status().isNoContent());
		mvc.perform(get("/api/classes/{id}", id).with(as(admin))).andExpect(status().isNotFound());
	}

	@Test
	void invalidClassIsRejected() throws Exception {
		String body = """
				{"name":"Bad","gradeLevel":13,"section":"A","academicYear":"2026"}
				""";
		mvc.perform(post("/api/classes").with(as(newUser(Role.ADMIN))).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.gradeLevel").exists())
				.andExpect(jsonPath("$.errors.academicYear").exists());
	}

	@Test
	void teacherCannotCreateUpdateOrDeleteClasses() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		String body = """
				{"name":"X","gradeLevel":3,"section":"%s","academicYear":"2026-2027"}
				""".formatted(uid());

		mvc.perform(post("/api/classes").with(as(teacher)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(put("/api/classes/{id}", c.getId()).with(as(teacher)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/api/classes/{id}", c.getId()).with(as(teacher)))
				.andExpect(status().isForbidden());
	}

	@Test
	void unauthenticatedIsRejected() throws Exception {
		mvc.perform(get("/api/classes")).andExpect(status().isUnauthorized());
	}

	@Test
	void teacherMineFilterReturnsOnlyOwnClasses() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass mine = newClass(teacher);
		SchoolClass other = newClass(newTeacher());

		mvc.perform(get("/api/classes").param("mine", "true").with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(mine.getId()));

		mvc.perform(get("/api/classes").with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(other.getId().intValue())));
	}

	@Test
	void studentSeesOnlyEnrolledClasses() throws Exception {
		Student student = newStudent();
		SchoolClass enrolled = newClass(newTeacher());
		SchoolClass notEnrolled = newClass(newTeacher());
		enroll(enrolled, student);

		mvc.perform(get("/api/classes").with(as(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[*].id", everyItem(is(enrolled.getId().intValue()))))
				.andExpect(jsonPath("$[*].id", not(hasItem(notEnrolled.getId().intValue()))));

		mvc.perform(get("/api/classes/{id}", enrolled.getId()).with(as(student))).andExpect(status().isOk());
		mvc.perform(get("/api/classes/{id}", notEnrolled.getId()).with(as(student)))
				.andExpect(status().isForbidden());
	}
}
