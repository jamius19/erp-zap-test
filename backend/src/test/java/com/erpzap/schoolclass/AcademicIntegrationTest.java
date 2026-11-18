/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import com.erpzap.AbstractIntegrationTest;
import com.erpzap.enrollment.Enrollment;
import com.erpzap.enrollment.EnrollmentRepository;
import com.erpzap.student.Student;
import com.erpzap.student.StudentRepository;
import com.erpzap.subject.Subject;
import com.erpzap.subject.SubjectRepository;
import com.erpzap.teacher.Teacher;
import com.erpzap.teacher.TeacherRepository;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import com.erpzap.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Shared setup for the academic module tests. The database is shared across test classes,
 * so every helper creates uniquely named rows and tests only assert on their own data.
 */
@AutoConfigureMockMvc
public abstract class AcademicIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected TeacherRepository teacherRepository;

	@Autowired
	protected StudentRepository studentRepository;

	@Autowired
	protected SchoolClassRepository classRepository;

	@Autowired
	protected EnrollmentRepository enrollmentRepository;

	@Autowired
	protected SubjectRepository subjectRepository;

	protected static String uid() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	protected User newUser(Role role) {
		return userRepository.save(buildUser(role));
	}

	/** Unsaved user; Teacher and Student cascade the save. */
	private static User buildUser(Role role) {
		String id = uid();
		User u = new User();
		u.setUsername(role.name().toLowerCase() + "_" + id);
		u.setPasswordHash("not-a-real-hash");
		u.setRole(role);
		u.setFullName(role.name() + " " + id);
		return u;
	}

	protected Teacher newTeacher() {
		Teacher t = new Teacher();
		t.setUser(buildUser(Role.TEACHER));
		t.setEmployeeNo("E-" + uid());
		return teacherRepository.save(t);
	}

	protected Student newStudent() {
		Student s = new Student();
		s.setUser(buildUser(Role.STUDENT));
		s.setStudentNo("S-" + uid());
		return studentRepository.save(s);
	}

	protected SchoolClass newClass(Teacher teacher) {
		SchoolClass c = new SchoolClass();
		c.setName("Class " + uid());
		c.setGradeLevel(7);
		c.setSection(uid());
		c.setAcademicYear("2026-2027");
		c.setTeacher(teacher);
		return classRepository.save(c);
	}

	protected Subject newSubject() {
		Subject s = new Subject();
		s.setCode("S" + uid());
		s.setName("Subject");
		return subjectRepository.save(s);
	}

	protected void enroll(SchoolClass c, Student s) {
		enrollmentRepository.save(new Enrollment(c, s));
	}

	protected static RequestPostProcessor as(User u) {
		return jwt()
				.jwt(j -> j.subject(String.valueOf(u.getId()))
						.claim("username", u.getUsername())
						.claim("role", u.getRole().name()))
				.authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
	}

	protected static RequestPostProcessor as(Teacher t) {
		return as(t.getUser());
	}

	protected static RequestPostProcessor as(Student s) {
		return as(s.getUser());
	}

	protected static long idOf(MvcResult result) throws Exception {
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}
}
