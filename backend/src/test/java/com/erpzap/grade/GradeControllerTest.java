/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import com.erpzap.schoolclass.AcademicIntegrationTest;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import com.erpzap.subject.Subject;
import com.erpzap.teacher.Teacher;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GradeControllerTest extends AcademicIntegrationTest {

	private static String grade(SchoolClass c, Student s, Subject sub, String term, String score) {
		return """
				{"classId":%d,"studentId":%d,"subjectId":%d,"term":"%s","score":%s,"remarks":"ok"}
				""".formatted(c.getId(), s.getId(), sub.getId(), term, score);
	}

	@Test
	void classTeacherManagesGrades() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student student = newStudent();
		enroll(c, student);
		Subject math = newSubject();
		Subject eng = newSubject();

		long id = idOf(mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, student, math, "TERM1", "88.5")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.score").value(88.5))
				.andExpect(jsonPath("$.subjectName").value(math.getName()))
				.andExpect(jsonPath("$.studentName").value(student.getUser().getFullName()))
				.andReturn());
		mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, student, eng, "TERM1", "70")))
				.andExpect(status().isCreated());

		mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, student, math, "TERM1", "50")))
				.andExpect(status().isConflict());

		mvc.perform(get("/api/classes/{id}/grades", c.getId()).with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		mvc.perform(get("/api/classes/{id}/grades", c.getId()).param("subjectId", eng.getId().toString())
						.param("term", "term1").with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mvc.perform(put("/api/grades/{id}", id).with(as(teacher)).contentType(APPLICATION_JSON)
						.content("{\"score\":91,\"remarks\":\"better\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.score").value(91.0))
				.andExpect(jsonPath("$.remarks").value("better"));

		mvc.perform(put("/api/grades/{id}", id).with(as(newTeacher())).contentType(APPLICATION_JSON)
						.content("{\"score\":10}"))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/api/grades/{id}", id).with(as(newTeacher()))).andExpect(status().isForbidden());
		mvc.perform(delete("/api/grades/{id}", id).with(as(teacher))).andExpect(status().isNoContent());
	}

	@Test
	void validationAndEnrollmentRules() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student enrolled = newStudent();
		enroll(c, enrolled);
		Subject sub = newSubject();

		mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, enrolled, sub, "TERM1", "100.5")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.score").exists());
		mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, enrolled, sub, "TERM1", "-1")))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, newStudent(), sub, "TERM1", "60")))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/grades").with(as(newTeacher())).contentType(APPLICATION_JSON)
						.content(grade(c, enrolled, sub, "TERM1", "60")))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentSeesOwnGradesOnly() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student me = newStudent();
		Student other = newStudent();
		enroll(c, me);
		mvc.perform(post("/api/grades").with(as(teacher)).contentType(APPLICATION_JSON)
						.content(grade(c, me, newSubject(), "FINAL", "77")))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/students/{id}/grades", me.getId()).with(as(me)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].term").value("FINAL"));
		mvc.perform(get("/api/students/{id}/grades", me.getId()).with(as(other)))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/students/{id}/grades", me.getId()).with(as(teacher)))
				.andExpect(status().isOk());
		mvc.perform(get("/api/classes/{id}/grades", c.getId()).with(as(me)))
				.andExpect(status().isForbidden());
	}
}
