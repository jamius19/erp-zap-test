/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.dashboard;

import com.erpzap.attendance.Attendance;
import com.erpzap.attendance.AttendanceRepository;
import com.erpzap.attendance.AttendanceStatus;
import com.erpzap.grade.Grade;
import com.erpzap.grade.GradeRepository;
import com.erpzap.schoolclass.AcademicIntegrationTest;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import com.erpzap.subject.Subject;
import com.erpzap.teacher.Teacher;
import com.erpzap.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest extends AcademicIntegrationTest {

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private GradeRepository gradeRepository;

	@Test
	void adminSeesCounts() throws Exception {
		newTeacher();
		newSubject();
		mvc.perform(get("/api/dashboard").with(as(newUser(Role.ADMIN))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teachers", greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.students").isNumber())
				.andExpect(jsonPath("$.classes").isNumber())
				.andExpect(jsonPath("$.subjects", greaterThanOrEqualTo(1)));
	}

	@Test
	void teacherSeesOwnClassesAndStudents() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass a = newClass(teacher);
		SchoolClass b = newClass(teacher);
		Student shared = newStudent();
		enroll(a, shared);
		enroll(b, shared);
		enroll(b, newStudent());
		enroll(newClass(newTeacher()), newStudent());

		mvc.perform(get("/api/dashboard").with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.myClasses").value(2))
				.andExpect(jsonPath("$.myStudents").value(2));
	}

	@Test
	void studentSeesRateAndAverage() throws Exception {
		Student fresh = newStudent();
		mvc.perform(get("/api/dashboard").with(as(fresh)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.myClasses").value(0))
				.andExpect(jsonPath("$.attendanceRate").value(nullValue()))
				.andExpect(jsonPath("$.averageScore").value(nullValue()));

		Student student = newStudent();
		SchoolClass c = newClass(newTeacher());
		enroll(c, student);
		mark(c, student, LocalDate.now().minusDays(1), AttendanceStatus.PRESENT);
		mark(c, student, LocalDate.now().minusDays(2), AttendanceStatus.LATE);
		mark(c, student, LocalDate.now().minusDays(3), AttendanceStatus.ABSENT);
		mark(c, student, LocalDate.now().minusDays(4), AttendanceStatus.EXCUSED);
		grade(c, student, newSubject(), "80");
		grade(c, student, newSubject(), "91");

		mvc.perform(get("/api/dashboard").with(as(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.myClasses").value(1))
				.andExpect(jsonPath("$.attendanceRate").value(50.0))
				.andExpect(jsonPath("$.averageScore").value(85.5));
	}

	private void mark(SchoolClass c, Student s, LocalDate date, AttendanceStatus status) {
		Attendance a = new Attendance();
		a.setSchoolClass(c);
		a.setStudent(s);
		a.setDate(date);
		a.setStatus(status);
		attendanceRepository.save(a);
	}

	private void grade(SchoolClass c, Student s, Subject sub, String score) {
		Grade g = new Grade();
		g.setSchoolClass(c);
		g.setStudent(s);
		g.setSubject(sub);
		g.setTerm("TERM1");
		g.setScore(new BigDecimal(score));
		gradeRepository.save(g);
	}
}
