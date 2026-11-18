/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.subject;

import com.erpzap.common.ConflictException;
import com.erpzap.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubjectService {

	private final SubjectRepository subjectRepository;

	public SubjectService(SubjectRepository subjectRepository) {
		this.subjectRepository = subjectRepository;
	}

	@Transactional(readOnly = true)
	public List<SubjectDto> list() {
		return subjectRepository.findAllByOrderByCodeAsc().stream().map(SubjectDto::of).toList();
	}

	public SubjectDto create(SubjectRequest req) {
		String code = req.code().trim().toUpperCase();
		if (subjectRepository.existsByCode(code)) {
			throw new ConflictException("Subject code already exists");
		}
		Subject s = new Subject();
		s.setCode(code);
		s.setName(req.name().trim());
		return SubjectDto.of(subjectRepository.save(s));
	}

	public SubjectDto update(Long id, SubjectRequest req) {
		Subject s = find(id);
		String code = req.code().trim().toUpperCase();
		if (subjectRepository.existsByCodeAndIdNot(code, id)) {
			throw new ConflictException("Subject code already exists");
		}
		s.setCode(code);
		s.setName(req.name().trim());
		return SubjectDto.of(s);
	}

	public void delete(Long id) {
		subjectRepository.delete(find(id));
	}

	private Subject find(Long id) {
		return subjectRepository.findById(id).orElseThrow(() -> new NotFoundException("Subject not found"));
	}
}
