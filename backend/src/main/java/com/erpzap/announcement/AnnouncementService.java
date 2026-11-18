/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import com.erpzap.common.CurrentUser;
import com.erpzap.common.NotFoundException;
import com.erpzap.schoolclass.AccessGuard;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.schoolclass.SchoolClassRepository;
import com.erpzap.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.erpzap.announcement.Audience.ALL;
import static com.erpzap.announcement.Audience.STUDENTS;
import static com.erpzap.announcement.Audience.TEACHERS;
import static com.erpzap.user.Role.ADMIN;
import static com.erpzap.user.Role.STUDENT;
import static com.erpzap.user.Role.TEACHER;

@Service
@Transactional
public class AnnouncementService {

	private final AnnouncementRepository announcementRepository;
	private final SchoolClassRepository classRepository;
	private final UserRepository userRepository;
	private final AccessGuard accessGuard;

	public AnnouncementService(
			AnnouncementRepository announcementRepository,
			SchoolClassRepository classRepository,
			UserRepository userRepository,
			AccessGuard accessGuard
	) {
		this.announcementRepository = announcementRepository;
		this.classRepository = classRepository;
		this.userRepository = userRepository;
		this.accessGuard = accessGuard;
	}

	/**
	 * Newest first; client sort is ignored. STUDENT: ALL/STUDENTS, school-wide or for an enrolled class.
	 * TEACHER: ALL/TEACHERS plus their own. ADMIN: everything.
	 */
	@Transactional(readOnly = true)
	public Page<AnnouncementDto> list(Pageable pageable) {
		Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
		if (CurrentUser.is(STUDENT)) {
			Long studentId = accessGuard.currentStudentId().orElse(-1L);
			return announcementRepository.findStudentDtos(List.of(ALL, STUDENTS), studentId, page);
		}
		if (CurrentUser.is(TEACHER)) {
			return announcementRepository.findTeacherDtos(List.of(ALL, TEACHERS), CurrentUser.id(), page);
		}
		return announcementRepository.findAllDtos(page);
	}

	/** A TEACHER may only target a class they teach. */
	public AnnouncementDto create(AnnouncementRequest req) {
		SchoolClass c = null;
		if (req.classId() != null) {
			c = classRepository.findById(req.classId()).orElseThrow(() -> new NotFoundException("Class not found"));
			accessGuard.requireManageable(c);
		}
		Announcement a = new Announcement();
		a.setTitle(req.title().trim());
		a.setBody(req.body());
		a.setAudience(req.audience());
		a.setSchoolClass(c);
		a.setAuthor(userRepository.getReferenceById(CurrentUser.id()));
		return announcementRepository.findDtoById(announcementRepository.save(a).getId()).orElseThrow();
	}

	/** ADMIN or the author. */
	public void delete(Long id) {
		Announcement a = announcementRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Announcement not found"));
		boolean author = a.getAuthor() != null && a.getAuthor().getId().equals(CurrentUser.id());
		if (!CurrentUser.is(ADMIN) && !author) {
			throw new AccessDeniedException("Only the author or an admin can delete this announcement");
		}
		announcementRepository.delete(a);
	}
}
