/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.teacher;

import com.erpzap.common.BadRequestException;
import com.erpzap.common.ConflictException;
import com.erpzap.common.NotFoundException;
import com.erpzap.teacher.TeacherDtos.CreateTeacherRequest;
import com.erpzap.teacher.TeacherDtos.TeacherResponse;
import com.erpzap.teacher.TeacherDtos.UpdateTeacherRequest;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import com.erpzap.user.UserPhotoService;
import com.erpzap.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
public class TeacherService {

	/** API sort property -> entity path. Anything else is rejected. */
	private static final Map<String, String> SORTS = Map.of(
			"id", "id",
			"fullName", "user.fullName",
			"username", "user.username",
			"email", "user.email",
			"employeeNo", "employeeNo"
	);

	private final TeacherRepository teacherRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserPhotoService photoService;

	public TeacherService(TeacherRepository teacherRepository, UserRepository userRepository,
			PasswordEncoder passwordEncoder, UserPhotoService photoService) {
		this.teacherRepository = teacherRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.photoService = photoService;
	}

	@Transactional(readOnly = true)
	public Page<TeacherResponse> list(String q, Pageable pageable) {
		return teacherRepository.search(likePattern(q), mapSort(pageable)).map(TeacherResponse::of);
	}

	@Transactional(readOnly = true)
	public TeacherResponse get(Long id) {
		return TeacherResponse.of(find(id));
	}

	@Transactional
	public TeacherResponse create(CreateTeacherRequest request) {
		String email = blankToNull(request.email());
		if (userRepository.existsByUsername(request.username())) {
			throw new ConflictException("Username already exists");
		}
		if (email != null && userRepository.existsByEmail(email)) {
			throw new ConflictException("Email already exists");
		}
		if (teacherRepository.existsByEmployeeNo(request.employeeNo())) {
			throw new ConflictException("Employee number already exists");
		}
		User user = new User();
		user.setUsername(request.username());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(Role.TEACHER);
		user.setFullName(request.fullName());
		user.setEmail(email);

		Teacher teacher = new Teacher();
		teacher.setUser(user);
		teacher.setEmployeeNo(request.employeeNo());
		teacher.setPhone(blankToNull(request.phone()));
		return TeacherResponse.of(teacherRepository.save(teacher));
	}

	@Transactional
	public TeacherResponse update(Long id, UpdateTeacherRequest request) {
		Teacher teacher = find(id);
		String email = blankToNull(request.email());
		if (email != null && userRepository.existsByEmailAndIdNot(email, teacher.getUser().getId())) {
			throw new ConflictException("Email already exists");
		}
		if (teacherRepository.existsByEmployeeNoAndIdNot(request.employeeNo(), id)) {
			throw new ConflictException("Employee number already exists");
		}
		teacher.getUser().setFullName(request.fullName());
		teacher.getUser().setEmail(email);
		teacher.setEmployeeNo(request.employeeNo());
		teacher.setPhone(blankToNull(request.phone()));
		return TeacherResponse.of(teacher);
	}

	/** Deletes the teacher and, through the cascade, its user account. */
	@Transactional
	public void delete(Long id) {
		Teacher teacher = find(id);
		String photoPath = teacher.getUser().getPhotoPath();
		teacherRepository.delete(teacher);
		teacherRepository.flush();
		photoService.deleteFile(photoPath);
	}

	private Teacher find(Long id) {
		return teacherRepository.findById(id).orElseThrow(() -> new NotFoundException("Teacher not found"));
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
