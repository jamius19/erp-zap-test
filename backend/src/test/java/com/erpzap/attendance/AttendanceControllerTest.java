/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.attendance;

import com.erpzap.schoolclass.AcademicIntegrationTest;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import com.erpzap.teacher.Teacher;
import com.erpzap.user.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttendanceControllerTest extends AcademicIntegrationTest {

	private static String rollCall(LocalDate date, Student s, String status) {
		return """
				{"date":"%s","records":[{"studentId":%d,"status":"%s"}]}
				""".formatted(date, s.getId(), status);
	}

	@Test
	void classTeacherUpsertsRollCall() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student a = newStudent();
		Student b = newStudent();
		enroll(c, a);
		enroll(c, b);
		LocalDate today = LocalDate.now();
		String body = """
				{"date":"%s","records":[{"studentId":%d,"status":"PRESENT"},{"studentId":%d,"status":"ABSENT"}]}
				""".formatted(today, a.getId(), b.getId());

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		// Second call updates instead of inserting.
		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(rollCall(today, b, "LATE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mvc.perform(get("/api/classes/{id}/attendance", c.getId()).param("date", today.toString())
						.with(as(newTeacher())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.studentId == %d)].status".formatted(b.getId())).value("LATE"))
				.andExpect(jsonPath("$[0].className").value(c.getName()))
				.andExpect(jsonPath("$[0].date").value(today.toString()));
	}

	@Test
	void rejectsNonEnrolledStudentFutureDateAndOtherTeacher() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student enrolled = newStudent();
		enroll(c, enrolled);
		LocalDate today = LocalDate.now();

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(rollCall(today, newStudent(), "PRESENT")))
				.andExpect(status().isBadRequest());

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(rollCall(today.plusDays(1), enrolled, "PRESENT")))
				.andExpect(status().isBadRequest());

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(rollCall(today, enrolled, "SLEEPING")))
				.andExpect(status().isBadRequest());

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(newTeacher()))
						.contentType(APPLICATION_JSON).content(rollCall(today, enrolled, "PRESENT")))
				.andExpect(status().isForbidden());

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(enrolled))
						.contentType(APPLICATION_JSON).content(rollCall(today, enrolled, "PRESENT")))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentSeesOwnAttendanceOnly() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass c = newClass(teacher);
		Student me = newStudent();
		Student other = newStudent();
		enroll(c, me);
		LocalDate yesterday = LocalDate.now().minusDays(1);
		LocalDate today = LocalDate.now();

		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(newUser(Role.ADMIN)))
						.contentType(APPLICATION_JSON).content(rollCall(yesterday, me, "ABSENT")))
				.andExpect(status().isOk());
		mvc.perform(put("/api/classes/{id}/attendance", c.getId()).with(as(teacher))
						.contentType(APPLICATION_JSON).content(rollCall(today, me, "PRESENT")))
				.andExpect(status().isOk());

		mvc.perform(get("/api/students/{id}/attendance", me.getId()).with(as(me)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].date").value(today.toString()))
				.andExpect(jsonPath("$[1].status").value("ABSENT"));

		mvc.perform(get("/api/students/{id}/attendance", me.getId()).param("from", today.toString())
						.with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mvc.perform(get("/api/students/{id}/attendance", me.getId()).with(as(other)))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/classes/{id}/attendance", c.getId()).param("date", today.toString()).with(as(me)))
				.andExpect(status().isForbidden());
	}
}
