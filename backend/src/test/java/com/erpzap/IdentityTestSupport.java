/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap;

import com.erpzap.student.Student;
import com.erpzap.student.StudentRepository;
import com.erpzap.teacher.Teacher;
import com.erpzap.teacher.TeacherRepository;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import com.erpzap.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for identity tests: create users with unique names (the DB is shared across test classes)
 * and log them in through the real endpoint.
 */
@AutoConfigureMockMvc
public abstract class IdentityTestSupport extends AbstractIntegrationTest {

	protected static final String PASSWORD = "Passw0rd!";

	private static String passwordHash;

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected TeacherRepository teacherRepository;

	@Autowired
	protected StudentRepository studentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	protected static String unique(String prefix) {
		return prefix + UUID.randomUUID().toString().substring(0, 8);
	}

	protected User createAdmin() {
		return userRepository.save(newUser(Role.ADMIN));
	}

	protected Teacher createTeacher() {
		Teacher teacher = new Teacher();
		teacher.setUser(newUser(Role.TEACHER));
		teacher.setEmployeeNo(unique("E-"));
		return teacherRepository.save(teacher);
	}

	protected Student createStudent() {
		Student student = new Student();
		student.setUser(newUser(Role.STUDENT));
		student.setStudentNo(unique("S-"));
		return studentRepository.save(student);
	}

	protected MvcResult loginResult(String username, String password) throws Exception {
		String body = """
				{"username": "%s", "password": "%s"}""".formatted(username, password);
		return mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn();
	}

	/** Returns an "Authorization" header value for the user. */
	protected String bearer(User user) throws Exception {
		String json = loginResult(user.getUsername(), PASSWORD).getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(json, "$.accessToken");
	}

	private User newUser(Role role) {
		if (passwordHash == null) {
			passwordHash = passwordEncoder.encode(PASSWORD);
		}
		User user = new User();
		user.setUsername(unique(role.name().toLowerCase() + "_"));
		user.setPasswordHash(passwordHash);
		user.setRole(role);
		user.setFullName("Test " + role.name());
		user.setEmail(user.getUsername() + "@test.local");
		return user;
	}
}
