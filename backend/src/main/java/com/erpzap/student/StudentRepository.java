/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByUserId(Long userId);

	boolean existsByStudentNo(String studentNo);

	boolean existsByStudentNoAndIdNot(String studentNo, Long id);

	/**
	 * Case-insensitive substring search. {@code pattern} is already lower-cased, LIKE-escaped and wrapped in '%'.
	 */
	@Query(value = """
			select s from Student s join fetch s.user u
			where lower(u.fullName) like :pattern escape '\\'
			   or lower(u.username) like :pattern escape '\\'
			   or lower(s.studentNo) like :pattern escape '\\'
			""", countQuery = """
			select count(s) from Student s join s.user u
			where lower(u.fullName) like :pattern escape '\\'
			   or lower(u.username) like :pattern escape '\\'
			   or lower(s.studentNo) like :pattern escape '\\'
			""")
	Page<Student> search(@Param("pattern") String pattern, Pageable pageable);
}
