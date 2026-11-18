/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.student.Student;
import com.erpzap.student.StudentRepository;
import com.erpzap.teacher.Teacher;
import com.erpzap.teacher.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.erpzap.user.Role.ADMIN;
import static com.erpzap.user.Role.STUDENT;
import static com.erpzap.user.Role.TEACHER;

/**
 * Ownership checks shared by the academic modules. Role gates live in @PreAuthorize on the controllers.
 */
@Component
public class AccessGuard {

	private final SchoolClassRepository classRepository;
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;

	public AccessGuard(
			SchoolClassRepository classRepository,
			TeacherRepository teacherRepository,
			StudentRepository studentRepository
	) {
		this.classRepository = classRepository;
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
	}

	/** The caller's teachers.id, if the caller is a teacher. */
	public Optional<Long> currentTeacherId() {
		return CurrentUser.is(TEACHER)
				? teacherRepository.findByUserId(CurrentUser.id()).map(Teacher::getId)
				: Optional.empty();
	}

	/** The caller's students.id, if the caller is a student. */
	public Optional<Long> currentStudentId() {
		return CurrentUser.is(STUDENT)
				? studentRepository.findByUserId(CurrentUser.id()).map(Student::getId)
				: Optional.empty();
	}

	/** Loads the class (404) and requires the caller to be ADMIN or its class teacher (403). */
	public SchoolClass requireManageable(Long classId) {
		SchoolClass c = classRepository.findById(classId).orElseThrow(() -> new NotFoundException("Class not found"));
		requireManageable(c);
		return c;
	}

	public void requireManageable(SchoolClass c) {
		if (CurrentUser.is(ADMIN) || isClassTeacher(c)) {
			return;
		}
		throw new AccessDeniedException("Not the class teacher");
	}

	public boolean isClassTeacher(SchoolClass c) {
		if (c.getTeacher() == null) {
			return false;
		}
		Long teacherId = c.getTeacher().getId();
		return currentTeacherId().filter(teacherId::equals).isPresent();
	}

	/** ADMIN and TEACHER may read any student's records; a STUDENT only their own. 404 if the student is missing. */
	public void requireStudentReadable(Long studentId) {
		if (CurrentUser.is(STUDENT) && currentStudentId().filter(studentId::equals).isEmpty()) {
			throw new AccessDeniedException("Not your record");
		}
		if (!studentRepository.existsById(studentId)) {
			throw new NotFoundException("Student not found");
		}
	}
}
