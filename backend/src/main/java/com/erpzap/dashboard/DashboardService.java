/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.dashboard;

import com.erpzap.attendance.AttendanceRepository;
import com.erpzap.common.CurrentUser;
import com.erpzap.enrollment.EnrollmentRepository;
import com.erpzap.grade.GradeRepository;
import com.erpzap.schoolclass.AccessGuard;
import com.erpzap.schoolclass.SchoolClassRepository;
import com.erpzap.student.StudentRepository;
import com.erpzap.subject.SubjectRepository;
import com.erpzap.teacher.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.erpzap.attendance.AttendanceStatus.LATE;
import static com.erpzap.attendance.AttendanceStatus.PRESENT;
import static com.erpzap.user.Role.STUDENT;
import static com.erpzap.user.Role.TEACHER;

@Service
@Transactional(readOnly = true)
public class DashboardService {

	public record AdminStats(long teachers, long students, long classes, long subjects) {
	}

	public record TeacherStats(long myClasses, long myStudents) {
	}

	/** attendanceRate is 0-100 (PRESENT and LATE count as attended); both are null when there is no data. */
	public record StudentStats(long myClasses, BigDecimal attendanceRate, BigDecimal averageScore) {
	}

	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final SchoolClassRepository classRepository;
	private final SubjectRepository subjectRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final AttendanceRepository attendanceRepository;
	private final GradeRepository gradeRepository;
	private final AccessGuard accessGuard;

	public DashboardService(
			TeacherRepository teacherRepository,
			StudentRepository studentRepository,
			SchoolClassRepository classRepository,
			SubjectRepository subjectRepository,
			EnrollmentRepository enrollmentRepository,
			AttendanceRepository attendanceRepository,
			GradeRepository gradeRepository,
			AccessGuard accessGuard
	) {
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
		this.classRepository = classRepository;
		this.subjectRepository = subjectRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.attendanceRepository = attendanceRepository;
		this.gradeRepository = gradeRepository;
		this.accessGuard = accessGuard;
	}

	public Object forCurrentUser() {
		if (CurrentUser.is(TEACHER)) {
			Long teacherId = accessGuard.currentTeacherId().orElse(-1L);
			return new TeacherStats(
					classRepository.countByTeacherId(teacherId),
					enrollmentRepository.countStudentsByTeacherId(teacherId)
			);
		}
		if (CurrentUser.is(STUDENT)) {
			Long studentId = accessGuard.currentStudentId().orElse(-1L);
			long total = attendanceRepository.countByStudentId(studentId);
			BigDecimal rate = null;
			if (total > 0) {
				long attended = attendanceRepository.countByStudentIdAndStatusIn(studentId, List.of(PRESENT, LATE));
				rate = BigDecimal.valueOf(attended * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
			}
			Double avg = gradeRepository.averageScoreByStudentId(studentId);
			return new StudentStats(
					enrollmentRepository.countByStudentId(studentId),
					rate,
					avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
			);
		}
		return new AdminStats(
				teacherRepository.count(),
				studentRepository.count(),
				classRepository.count(),
				subjectRepository.count()
		);
	}
}
