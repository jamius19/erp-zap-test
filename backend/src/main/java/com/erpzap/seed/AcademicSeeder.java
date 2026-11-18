/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.seed;

import com.erpzap.announcement.Announcement;
import com.erpzap.announcement.AnnouncementRepository;
import com.erpzap.announcement.Audience;
import com.erpzap.attendance.Attendance;
import com.erpzap.attendance.AttendanceRepository;
import com.erpzap.attendance.AttendanceStatus;
import com.erpzap.enrollment.Enrollment;
import com.erpzap.enrollment.EnrollmentRepository;
import com.erpzap.grade.Grade;
import com.erpzap.grade.GradeRepository;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.schoolclass.SchoolClassRepository;
import com.erpzap.student.Student;
import com.erpzap.student.StudentRepository;
import com.erpzap.subject.Subject;
import com.erpzap.subject.SubjectRepository;
import com.erpzap.teacher.Teacher;
import com.erpzap.teacher.TeacherRepository;
import com.erpzap.user.User;
import com.erpzap.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seeds subjects, classes, enrollments, a week of attendance, TERM1 grades and two announcements.
 * Runs after IdentitySeeder (@Order(1)) and only when the subjects table is empty.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class AcademicSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AcademicSeeder.class);
	private static final String YEAR = "2026-2027";

	private final UserRepository userRepository;
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final SubjectRepository subjectRepository;
	private final SchoolClassRepository classRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final AttendanceRepository attendanceRepository;
	private final GradeRepository gradeRepository;
	private final AnnouncementRepository announcementRepository;

	public AcademicSeeder(
			UserRepository userRepository,
			TeacherRepository teacherRepository,
			StudentRepository studentRepository,
			SubjectRepository subjectRepository,
			SchoolClassRepository classRepository,
			EnrollmentRepository enrollmentRepository,
			AttendanceRepository attendanceRepository,
			GradeRepository gradeRepository,
			AnnouncementRepository announcementRepository
	) {
		this.userRepository = userRepository;
		this.teacherRepository = teacherRepository;
		this.studentRepository = studentRepository;
		this.subjectRepository = subjectRepository;
		this.classRepository = classRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.attendanceRepository = attendanceRepository;
		this.gradeRepository = gradeRepository;
		this.announcementRepository = announcementRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (subjectRepository.count() > 0) {
			return;
		}
		Optional<User> admin = userRepository.findByUsername("admin");
		List<Teacher> teachers = new ArrayList<>();
		for (int i = 1; i <= 3; i++) {
			teacher("teacher" + i).ifPresent(teachers::add);
		}
		List<Student> students = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			student("student" + i).ifPresent(students::add);
		}
		if (admin.isEmpty() || teachers.size() != 3 || students.size() != 12) {
			log.warn("Academic seed skipped: identity seed users (admin, teacher1..3, student1..12) not found");
			return;
		}

		List<Subject> subjects = List.of(
				subject("MATH", "Mathematics"),
				subject("ENG", "English"),
				subject("SCI", "Science"),
				subject("HIST", "History")
		);
		List<SchoolClass> classes = List.of(
				schoolClass("Grade 5 - A", 5, "A", teachers.get(0)),
				schoolClass("Grade 5 - B", 5, "B", teachers.get(1)),
				schoolClass("Grade 6 - A", 6, "A", teachers.get(2))
		);

		List<LocalDate> schoolDays = lastWeekdays(5);
		for (int i = 0; i < students.size(); i++) {
			Student student = students.get(i);
			SchoolClass c = classes.get(i / 4);
			Long markedBy = teachers.get(i / 4).getUser().getId();
			enrollmentRepository.save(new Enrollment(c, student));

			for (int d = 0; d < schoolDays.size(); d++) {
				Attendance a = new Attendance();
				a.setSchoolClass(c);
				a.setStudent(student);
				a.setDate(schoolDays.get(d));
				a.setStatus(statusFor(i, d));
				a.setMarkedBy(markedBy);
				attendanceRepository.save(a);
			}

			for (int j = 0; j < subjects.size(); j++) {
				Grade g = new Grade();
				g.setSchoolClass(c);
				g.setStudent(student);
				g.setSubject(subjects.get(j));
				g.setTerm("TERM1");
				g.setScore(BigDecimal.valueOf(55 + (i * 13 + j * 7) % 45));
				g.setRemarks(null);
				g.setGradedBy(markedBy);
				gradeRepository.save(g);
			}
		}

		Instant now = Instant.now();
		announcement(
				"Welcome to the 2026-2027 academic year",
				"Classes start on Monday. Please check your timetable and bring your ID card.",
				Audience.ALL, null, admin.get(), now.minus(2, ChronoUnit.DAYS)
		);
		announcement(
				"Math quiz on Friday",
				"Grade 5 - A: there will be a short quiz on fractions this Friday.",
				Audience.STUDENTS, classes.get(0), teachers.get(0).getUser(), now.minus(1, ChronoUnit.DAYS)
		);
		log.info("Academic seed data created");
	}

	private Optional<Teacher> teacher(String username) {
		return userRepository.findByUsername(username).flatMap(u -> teacherRepository.findByUserId(u.getId()));
	}

	private Optional<Student> student(String username) {
		return userRepository.findByUsername(username).flatMap(u -> studentRepository.findByUserId(u.getId()));
	}

	private Subject subject(String code, String name) {
		Subject s = new Subject();
		s.setCode(code);
		s.setName(name);
		return subjectRepository.save(s);
	}

	private SchoolClass schoolClass(String name, int gradeLevel, String section, Teacher teacher) {
		SchoolClass c = new SchoolClass();
		c.setName(name);
		c.setGradeLevel(gradeLevel);
		c.setSection(section);
		c.setAcademicYear(YEAR);
		c.setTeacher(teacher);
		return classRepository.save(c);
	}

	private void announcement(String title, String body, Audience audience, SchoolClass c, User author, Instant at) {
		Announcement a = new Announcement();
		a.setTitle(title);
		a.setBody(body);
		a.setAudience(audience);
		a.setSchoolClass(c);
		a.setAuthor(author);
		a.setCreatedAt(at);
		announcementRepository.save(a);
	}

	/** The last {@code count} weekdays before today, oldest first. */
	private static List<LocalDate> lastWeekdays(int count) {
		List<LocalDate> days = new ArrayList<>();
		LocalDate d = LocalDate.now().minusDays(1);
		while (days.size() < count) {
			if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
				days.addFirst(d);
			}
			d = d.minusDays(1);
		}
		return days;
	}

	/** Deterministic mix: mostly PRESENT with some ABSENT, LATE and EXCUSED. */
	private static AttendanceStatus statusFor(int student, int day) {
		int n = (student * 3 + day * 5) % 17;
		if (n == 0) {
			return AttendanceStatus.ABSENT;
		}
		if (n == 1) {
			return AttendanceStatus.LATE;
		}
		if (n == 2) {
			return AttendanceStatus.EXCUSED;
		}
		return AttendanceStatus.PRESENT;
	}
}
