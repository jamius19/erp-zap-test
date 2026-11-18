/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
package com.erpzap.user;

import com.erpzap.common.BadRequestException;
import com.erpzap.common.NotFoundException;
import com.erpzap.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.http.MediaType.IMAGE_PNG;

/**
 * Profile photos. The file type is decided by magic bytes, never by the client's filename or Content-Type.
 * Files get random UUID names inside {@code app.upload-dir}; only that name is stored in users.photo_path.
 */
@Service
public class UserPhotoService {

	public record Photo(byte[] bytes, MediaType contentType) {
	}

	private static final long MAX_BYTES = 2L * 1024 * 1024;
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

	private final UserRepository userRepository;
	private final Path uploadDir;

	public UserPhotoService(UserRepository userRepository, AppProperties props) {
		this.userRepository = userRepository;
		this.uploadDir = Path.of(props.uploadDir()).toAbsolutePath().normalize();
	}

	@Transactional
	public void upload(Long userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("File is required");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new BadRequestException("File must be at most 2MB");
		}
		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		String extension;
		if (startsWith(bytes, PNG_MAGIC)) {
			extension = ".png";
		} else if (startsWith(bytes, JPEG_MAGIC)) {
			extension = ".jpg";
		} else {
			throw new BadRequestException("Only PNG or JPEG images are allowed");
		}

		User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
		Path target = resolve(UUID.randomUUID() + extension);
		try {
			Files.createDirectories(uploadDir);
			Files.write(target, bytes);
			if (user.getPhotoPath() != null) {
				Files.deleteIfExists(resolve(user.getPhotoPath()));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		user.setPhotoPath(target.getFileName().toString());
	}

	@Transactional(readOnly = true)
	public Photo download(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Photo not found"));
		if (user.getPhotoPath() == null) {
			throw new NotFoundException("Photo not found");
		}
		Path path = resolve(user.getPhotoPath());
		if (!Files.isRegularFile(path)) {
			throw new NotFoundException("Photo not found");
		}
		try {
			MediaType type = path.getFileName().toString().endsWith(".png") ? IMAGE_PNG : IMAGE_JPEG;
			return new Photo(Files.readAllBytes(path), type);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Removes the stored file for a user that is being deleted. */
	public void deleteFile(String photoPath) {
		if (photoPath == null) {
			return;
		}
		try {
			Files.deleteIfExists(resolve(photoPath));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Resolves a stored file name, refusing anything that escapes the upload directory. */
	private Path resolve(String fileName) {
		Path path = uploadDir.resolve(fileName).normalize();
		if (!path.getParent().equals(uploadDir)) {
			throw new BadRequestException("Invalid file path");
		}
		return path;
	}

	private static boolean startsWith(byte[] bytes, byte[] prefix) {
		return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
	}
}
