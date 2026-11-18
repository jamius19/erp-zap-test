/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.schoolclass;

import com.erpzap.common.ConflictException;
import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.enrollment.EnrollmentRepository;
import com.erpzap.teacher.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.erpzap.user.Role.STUDENT;
import static com.erpzap.user.Role.TEACHER;

@Service
@Transactional
public class SchoolClassService {

	private final SchoolClassRepository classRepository;
	private final TeacherRepository teacherRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final AccessGuard accessGuard;

	public SchoolClassService(
			SchoolClassRepository classRepository,
			TeacherRepository teacherRepository,
			EnrollmentRepository enrollmentRepository,
			AccessGuard accessGuard
	) {
		this.classRepository = classRepository;
		this.teacherRepository = teacherRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.accessGuard = accessGuard;
	}

	/** ADMIN: all. TEACHER: all, or only their own with mine=true. STUDENT: only enrolled classes. */
	@Transactional(readOnly = true)
	public List<SchoolClassDto> list(boolean mine) {
		if (CurrentUser.is(STUDENT)) {
			return accessGuard.currentStudentId().map(classRepository::findDtosByStudentId).orElse(List.of());
		}
		if (mine && CurrentUser.is(TEACHER)) {
			return accessGuard.currentTeacherId().map(classRepository::findDtosByTeacherId).orElse(List.of());
		}
		return classRepository.findAllDtos();
	}

	@Transactional(readOnly = true)
	public SchoolClassDto get(Long id) {
		if (CurrentUser.is(STUDENT)) {
			boolean enrolled = accessGuard.currentStudentId()
					.filter(sid -> enrollmentRepository.existsBySchoolClassIdAndStudentId(id, sid))
					.isPresent();
			if (!enrolled) {
				throw new AccessDeniedException("Not enrolled in this class");
			}
		}
		return classRepository.findDtoById(id).orElseThrow(() -> new NotFoundException("Class not found"));
	}

	public SchoolClassDto create(SchoolClassRequest req) {
		String section = req.section().trim();
		if (classRepository.existsByGradeLevelAndSectionAndAcademicYear(
				req.gradeLevel(), section, req.academicYear())) {
			throw new ConflictException("A class with this grade, section and year already exists");
		}
		SchoolClass c = new SchoolClass();
		apply(c, req);
		return classRepository.findDtoById(classRepository.save(c).getId()).orElseThrow();
	}

	public SchoolClassDto update(Long id, SchoolClassRequest req) {
		SchoolClass c = classRepository.findById(id).orElseThrow(() -> new NotFoundException("Class not found"));
		if (classRepository.existsByGradeLevelAndSectionAndAcademicYearAndIdNot(
				req.gradeLevel(), req.section().trim(), req.academicYear(), id)) {
			throw new ConflictException("A class with this grade, section and year already exists");
		}
		apply(c, req);
		classRepository.flush();
		return classRepository.findDtoById(id).orElseThrow();
	}

	public void delete(Long id) {
		SchoolClass c = classRepository.findById(id).orElseThrow(() -> new NotFoundException("Class not found"));
		classRepository.delete(c);
	}

	private void apply(SchoolClass c, SchoolClassRequest req) {
		c.setName(req.name().trim());
		c.setGradeLevel(req.gradeLevel());
		c.setSection(req.section().trim());
		c.setAcademicYear(req.academicYear());
		c.setTeacher(req.teacherId() == null
				? null
				: teacherRepository.findById(req.teacherId())
						.orElseThrow(() -> new NotFoundException("Teacher not found")));
	}
}
