/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.announcement;

import com.erpzap.schoolclass.AcademicIntegrationTest;
import com.erpzap.schoolclass.SchoolClass;
import com.erpzap.student.Student;
import com.erpzap.teacher.Teacher;
import com.erpzap.user.Role;
import com.erpzap.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnnouncementControllerTest extends AcademicIntegrationTest {

	private long create(RequestPostProcessor who, String title, String audience, Long classId) throws Exception {
		String body = """
				{"title":"%s","body":"<b>plain</b> text","audience":"%s","classId":%s}
				""".formatted(title, audience, classId);
		return idOf(mvc.perform(post("/api/announcements").with(who).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn());
	}

	@Test
	void visibilityDependsOnRole() throws Exception {
		User admin = newUser(Role.ADMIN);
		Teacher teacher = newTeacher();
		Teacher author = newTeacher();
		SchoolClass myClass = newClass(author);
		SchoolClass otherClass = newClass(newTeacher());
		Student student = newStudent();
		enroll(myClass, student);

		String p = uid();
		String all = p + "-all";
		String teachers = p + "-teachers";
		String students = p + "-students";
		String myClassOnly = p + "-myclass";
		String otherClassOnly = p + "-otherclass";
		String authorsOwn = p + "-own";
		create(as(admin), all, "ALL", null);
		create(as(admin), teachers, "TEACHERS", null);
		create(as(admin), students, "STUDENTS", null);
		create(as(author), myClassOnly, "STUDENTS", myClass.getId());
		create(as(admin), otherClassOnly, "STUDENTS", otherClass.getId());
		create(as(author), authorsOwn, "STUDENTS", null);

		mvc.perform(get("/api/announcements").param("size", "100").with(as(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(100))
				.andExpect(jsonPath("$.content[*].title",
						hasItems(all, teachers, students, myClassOnly, otherClassOnly, authorsOwn)));

		mvc.perform(get("/api/announcements").param("size", "100").with(as(student)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].title", hasItems(all, students, myClassOnly, authorsOwn)))
				.andExpect(jsonPath("$.content[*].title", not(hasItem(teachers))))
				.andExpect(jsonPath("$.content[*].title", not(hasItem(otherClassOnly))));

		mvc.perform(get("/api/announcements").param("size", "100").with(as(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].title", hasItems(all, teachers)))
				.andExpect(jsonPath("$.content[*].title", not(hasItem(students))))
				.andExpect(jsonPath("$.content[*].title", not(hasItem(authorsOwn))));

		mvc.perform(get("/api/announcements").param("size", "100").with(as(author)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].title", hasItems(all, teachers, myClassOnly, authorsOwn)));
	}

	@Test
	void teacherCanOnlyTargetOwnClass() throws Exception {
		Teacher teacher = newTeacher();
		SchoolClass other = newClass(newTeacher());
		String body = """
				{"title":"x","body":"y","audience":"STUDENTS","classId":%d}
				""".formatted(other.getId());

		mvc.perform(post("/api/announcements").with(as(teacher)).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/announcements").with(as(newStudent())).contentType(APPLICATION_JSON)
						.content("{\"title\":\"x\",\"body\":\"y\",\"audience\":\"ALL\"}"))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/announcements").with(as(teacher)).contentType(APPLICATION_JSON)
						.content("{\"title\":\"\",\"body\":\"y\",\"audience\":\"ALL\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void onlyAuthorOrAdminCanDelete() throws Exception {
		Teacher author = newTeacher();
		long first = create(as(author), "del-" + uid(), "ALL", null);
		long second = create(as(author), "del-" + uid(), "ALL", null);

		mvc.perform(delete("/api/announcements/{id}", first).with(as(newTeacher())))
				.andExpect(status().isForbidden());
		mvc.perform(delete("/api/announcements/{id}", first).with(as(author))).andExpect(status().isNoContent());
		mvc.perform(delete("/api/announcements/{id}", second).with(as(newUser(Role.ADMIN))))
				.andExpect(status().isNoContent());
		mvc.perform(delete("/api/announcements/{id}", second).with(as(author))).andExpect(status().isNotFound());
	}
}
