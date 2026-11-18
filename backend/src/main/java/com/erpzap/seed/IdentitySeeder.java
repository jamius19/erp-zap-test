/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.seed;

import com.erpzap.student.Student;
import com.erpzap.student.StudentRepository;
import com.erpzap.teacher.Teacher;
import com.erpzap.teacher.TeacherRepository;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import com.erpzap.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Seeds admin, teacher1..3 and student1..12 when {@code app.seed.enabled=true} and there are no users yet.
 * AcademicSeeder (order 2) looks these users up by username.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class IdentitySeeder implements ApplicationRunner {

	private static final String[] TEACHER_NAMES = {"Ayesha Rahman", "Daniel Brooks", "Mei Tanaka"};
	private static final String[] STUDENT_NAMES = {
			"Liam Carter", "Sofia Nguyen", "Omar Haddad", "Emma Wilson", "Noah Patel", "Ava Kim",
			"Lucas Silva", "Mia Johnson", "Ethan Chen", "Zara Ahmed", "Leo Martin", "Chloe Davis"
	};

	private final UserRepository userRepository;
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder;

	public IdentitySeeder(UserRepository userRepository, TeacherRepository teacherRepository,
			StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}
		userRepository.save(user("admin", "Admin@1234", Role.ADMIN, "School Admin"));

		String teacherHash = passwordEncoder.encode("Teacher@1234");
		for (int i = 1; i <= TEACHER_NAMES.length; i++) {
			Teacher teacher = new Teacher();
			teacher.setUser(userWithHash("teacher" + i, teacherHash, Role.TEACHER, TEACHER_NAMES[i - 1]));
			teacher.setEmployeeNo("EMP-%03d".formatted(i));
			teacher.setPhone("+1 555 010%d".formatted(i));
			teacherRepository.save(teacher);
		}

		String studentHash = passwordEncoder.encode("Student@1234");
		for (int i = 1; i <= STUDENT_NAMES.length; i++) {
			Student student = new Student();
			student.setUser(userWithHash("student" + i, studentHash, Role.STUDENT, STUDENT_NAMES[i - 1]));
			student.setStudentNo("STU-%04d".formatted(i));
			// Students 1-8 are in grade 5 (born 2016), 9-12 in grade 6 (born 2015).
			student.setDateOfBirth(LocalDate.of(i <= 8 ? 2016 : 2015, (i % 12) + 1, 10 + i));
			student.setGuardianName("Guardian of " + STUDENT_NAMES[i - 1]);
			student.setGuardianPhone("+1 555 02%02d".formatted(i));
			studentRepository.save(student);
		}
	}

	private User user(String username, String password, Role role, String fullName) {
		return userWithHash(username, passwordEncoder.encode(password), role, fullName);
	}

	private static User userWithHash(String username, String hash, Role role, String fullName) {
		User user = new User();
		user.setUsername(username);
		user.setPasswordHash(hash);
		user.setRole(role);
		user.setFullName(fullName);
		user.setEmail(username + "@school.test");
		return user;
	}
}
