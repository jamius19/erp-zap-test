/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.student;

import com.erpzap.common.BadRequestException;
import com.erpzap.common.ConflictException;
import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.student.StudentDtos.CreateStudentRequest;
import com.erpzap.student.StudentDtos.StudentResponse;
import com.erpzap.student.StudentDtos.UpdateStudentRequest;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import com.erpzap.user.UserPhotoService;
import com.erpzap.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class StudentService {

	/** API sort property -> entity path. Anything else is rejected. */
	private static final Map<String, String> SORTS = Map.of(
			"id", "id",
			"fullName", "user.fullName",
			"username", "user.username",
			"email", "user.email",
			"studentNo", "studentNo",
			"dateOfBirth", "dateOfBirth"
	);

	private final StudentRepository studentRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserPhotoService photoService;

	public StudentService(StudentRepository studentRepository, UserRepository userRepository,
			PasswordEncoder passwordEncoder, UserPhotoService photoService) {
		this.studentRepository = studentRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.photoService = photoService;
	}

	@Transactional(readOnly = true)
	public Page<StudentResponse> list(String q, Pageable pageable) {
		return studentRepository.search(likePattern(q), mapSort(pageable)).map(StudentResponse::of);
	}

	/**
	 * A STUDENT may only read their own record. The ownership check runs before the lookup,
	 * so a student can't probe which ids exist.
	 */
	@Transactional(readOnly = true)
	public StudentResponse get(Long id) {
		if (CurrentUser.is(Role.STUDENT)) {
			Long ownId = studentRepository.findByUserId(CurrentUser.id()).map(Student::getId).orElse(null);
			if (!Objects.equals(ownId, id)) {
				throw new AccessDeniedException("Access denied");
			}
		}
		return StudentResponse.of(find(id));
	}

	@Transactional
	public StudentResponse create(CreateStudentRequest request) {
		String email = blankToNull(request.email());
		if (userRepository.existsByUsername(request.username())) {
			throw new ConflictException("Username already exists");
		}
		if (email != null && userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already exists");
		}
		if (studentRepository.existsByStudentNo(request.studentNo())) {
			throw new ConflictException("Student number already exists");
		}
		User user = new User();
		user.setUsername(request.username());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(Role.STUDENT);
		user.setFullName(request.fullName());
		user.setEmail(email);

		Student student = new Student();
		student.setUser(user);
		apply(student, request.studentNo(), request.dateOfBirth(), request.guardianName(), request.guardianPhone());
		return StudentResponse.of(studentRepository.save(student));
	}

	@Transactional
	public StudentResponse update(Long id, UpdateStudentRequest request) {
		Student student = find(id);
		String email = blankToNull(request.email());
		if (email != null && userRepository.existsByEmailAndIdNot(email, student.getUser().getId())) {
			throw new ConflictException("Email already exists");
		}
		if (studentRepository.existsByStudentNoAndIdNot(request.studentNo(), id)) {
			throw new ConflictException("Student number already exists");
		}
		student.getUser().setFullName(request.fullName());
		student.getUser().setEmail(email);
		apply(student, request.studentNo(), request.dateOfBirth(), request.guardianName(), request.guardianPhone());
		return StudentResponse.of(student);
	}

	/** Deletes the student and, through the cascade, its user account. */
	@Transactional
	public void delete(Long id) {
		Student student = find(id);
		String photoPath = student.getUser().getPhotoPath();
		studentRepository.delete(student);
		studentRepository.flush();
		photoService.deleteFile(photoPath);
	}

	private Student find(Long id) {
		return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Student not found"));
	}

	private static void apply(Student student, String studentNo, LocalDate dateOfBirth,
			String guardianName, String guardianPhone) {
		student.setStudentNo(studentNo);
		student.setDateOfBirth(dateOfBirth);
		student.setGuardianName(blankToNull(guardianName));
		student.setGuardianPhone(blankToNull(guardianPhone));
	}

	private static Pageable mapSort(Pageable pageable) {
		Sort sort = Sort.by(pageable.getSort().stream().map(order -> {
			String path = SORTS.get(order.getProperty());
			if (path == null) {
				throw new BadRequestException("Unsupported sort property");
			}
			return order.withProperty(path);
		}).toList());
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

	private static String likePattern(String q) {
		if (q == null || q.isBlank()) {
			return "%";
		}
		String escaped = q.trim().toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
