/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.grade;

import com.erpzap.common.BadRequestException;
import com.erpzap.common.ConflictException;
import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.enrollment.EnrollmentRepository;
import com.erpzap.schoolclass.AccessGuard;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.schoolclass.SchoolClassRepository;
import com.erpzap.student.StudentRepository;
import com.erpzap.subject.Subject;
import com.erpzap.subject.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GradeService {

	private final GradeRepository gradeRepository;
	private final SchoolClassRepository classRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final StudentRepository studentRepository;
	private final SubjectRepository subjectRepository;
	private final AccessGuard accessGuard;

	public GradeService(
			GradeRepository gradeRepository,
			SchoolClassRepository classRepository,
			EnrollmentRepository enrollmentRepository,
			StudentRepository studentRepository,
			SubjectRepository subjectRepository,
			AccessGuard accessGuard
	) {
		this.gradeRepository = gradeRepository;
		this.classRepository = classRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.studentRepository = studentRepository;
		this.subjectRepository = subjectRepository;
		this.accessGuard = accessGuard;
	}

	@Transactional(readOnly = true)
	public List<GradeDto> forClass(Long classId, Long subjectId, String term) {
		if (!classRepository.existsById(classId)) {
			throw new NotFoundException("Class not found");
		}
		return gradeRepository.findDtosByClass(classId, subjectId, term);
	}

	@Transactional(readOnly = true)
	public List<GradeDto> forStudent(Long studentId) {
		accessGuard.requireStudentReadable(studentId);
		return gradeRepository.findDtosByStudent(studentId);
	}

	public GradeDto create(GradeCreateRequest req) {
		SchoolClass c = accessGuard.requireManageable(req.classId());
		Subject subject = subjectRepository.findById(req.subjectId())
				.orElseThrow(() -> new NotFoundException("Subject not found"));
		if (!enrollmentRepository.existsBySchoolClassIdAndStudentId(c.getId(), req.studentId())) {
			throw new BadRequestException("Student is not enrolled in this class");
		}
		String term = req.term().trim().toUpperCase();
		if (gradeRepository.existsBySchoolClassIdAndStudentIdAndSubjectIdAndTerm(
				c.getId(), req.studentId(), subject.getId(), term)) {
			throw new ConflictException("A grade for this student, subject and term already exists");
		}
		Grade g = new Grade();
		g.setSchoolClass(c);
		g.setStudent(studentRepository.getReferenceById(req.studentId()));
		g.setSubject(subject);
		g.setTerm(term);
		g.setScore(req.score());
		g.setRemarks(req.remarks());
		g.setGradedBy(CurrentUser.id());
		return gradeRepository.findDtoById(gradeRepository.save(g).getId()).orElseThrow();
	}

	public GradeDto update(Long id, GradeUpdateRequest req) {
		Grade g = find(id);
		accessGuard.requireManageable(g.getSchoolClass());
		g.setScore(req.score());
		g.setRemarks(req.remarks());
		g.setGradedBy(CurrentUser.id());
		gradeRepository.flush();
		return gradeRepository.findDtoById(id).orElseThrow();
	}

	public void delete(Long id) {
		Grade g = find(id);
		accessGuard.requireManageable(g.getSchoolClass());
		gradeRepository.delete(g);
	}

	private Grade find(Long id) {
		return gradeRepository.findById(id).orElseThrow(() -> new NotFoundException("Grade not found"));
	}
}
