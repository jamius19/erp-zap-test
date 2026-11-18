/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.enrollment;

import com.erpzap.common.ConflictException;
import com.erpzap.common.NotFoundException;
import com.erpzap.schoolclass.AccessGuard;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.schoolclass.SchoolClassRepository;
import com.erpzap.student.Student;
import com.erpzap.student.StudentDtos.StudentResponse;
import com.erpzap.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnrollmentService {

	private final EnrollmentRepository enrollmentRepository;
	private final SchoolClassRepository classRepository;
	private final StudentRepository studentRepository;
	private final AccessGuard accessGuard;

	public EnrollmentService(
			EnrollmentRepository enrollmentRepository,
			SchoolClassRepository classRepository,
			StudentRepository studentRepository,
			AccessGuard accessGuard
	) {
		this.enrollmentRepository = enrollmentRepository;
		this.classRepository = classRepository;
		this.studentRepository = studentRepository;
		this.accessGuard = accessGuard;
	}

	@Transactional(readOnly = true)
	public List<StudentResponse> list(Long classId) {
		if (!classRepository.existsById(classId)) {
			throw new NotFoundException("Class not found");
		}
		return enrollmentRepository.findStudentsByClassId(classId).stream().map(StudentResponse::of).toList();
	}

	public StudentResponse add(Long classId, Long studentId) {
		SchoolClass c = accessGuard.requireManageable(classId);
		Student s = studentRepository.findById(studentId)
				.orElseThrow(() -> new NotFoundException("Student not found"));
		if (enrollmentRepository.existsBySchoolClassIdAndStudentId(classId, studentId)) {
			throw new ConflictException("Student is already enrolled in this class");
		}
		enrollmentRepository.save(new Enrollment(c, s));
		return StudentResponse.of(s);
	}

	public void remove(Long classId, Long studentId) {
		accessGuard.requireManageable(classId);
		Enrollment e = enrollmentRepository.findBySchoolClassIdAndStudentId(classId, studentId)
				.orElseThrow(() -> new NotFoundException("Student is not enrolled in this class"));
		enrollmentRepository.delete(e);
	}
}
