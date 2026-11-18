/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.user;

import com.erpzap.common.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/users")
public class UserPhotoController {

	private final UserPhotoService photoService;

	public UserPhotoController(UserPhotoService photoService) {
		this.photoService = photoService;
	}

	@PostMapping(path = "/me/photo", consumes = MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file) {
		photoService.upload(CurrentUser.id(), file);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/photo")
	public ResponseEntity<byte[]> download(@PathVariable Long id) {
		UserPhotoService.Photo photo = photoService.download(id);
		return ResponseEntity.ok()
				.contentType(photo.contentType())
				.header("X-Content-Type-Options", "nosniff")
				.body(photo.bytes());
	}
}
