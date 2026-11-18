/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.user;

import com.erpzap.IdentityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPhotoControllerTest extends IdentityTestSupport {

	private static final byte[] PNG = {
			(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
	};

	@Test
	void uploadPngThenDownloadIt() throws Exception {
		User user = createAdmin();
		String bearer = bearer(user);

		mvc.perform(multipart("/api/users/me/photo")
						.file(new MockMultipartFile("file", "../../evil.png", "image/png", PNG))
						.header(AUTHORIZATION, bearer))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/users/" + user.getId() + "/photo").header(AUTHORIZATION, bearer))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(content().bytes(PNG))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"));
		mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer))
				.andExpect(jsonPath("$.hasPhoto").value(true));
	}

	@Test
	void rejectsNonImageEvenWithImageContentType() throws Exception {
		String bearer = bearer(createAdmin());
		byte[] html = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

		mvc.perform(multipart("/api/users/me/photo")
						.file(new MockMultipartFile("file", "x.png", "image/png", html))
						.header(AUTHORIZATION, bearer))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingPhotoIs404AndUploadNeedsAuth() throws Exception {
		User user = createAdmin();

		mvc.perform(get("/api/users/" + user.getId() + "/photo").header(AUTHORIZATION, bearer(user)))
				.andExpect(status().isNotFound());
		mvc.perform(multipart("/api/users/me/photo").file(new MockMultipartFile("file", "a.png", "image/png", PNG)))
				.andExpect(status().isUnauthorized());
	}
}
