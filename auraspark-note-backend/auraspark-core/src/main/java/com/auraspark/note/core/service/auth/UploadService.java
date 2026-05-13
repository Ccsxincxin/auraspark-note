package com.auraspark.note.core.service.auth;

import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.core.entity.UserProfile;
import com.auraspark.note.core.mapper.UserProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UploadService {

    private final Path uploadDir;
    private final UserProfileMapper profileMapper;

    public UploadService(@Value("${upload.dir}") String uploadDir,
                         UserProfileMapper profileMapper) throws IOException {
        this.uploadDir = Paths.get(uploadDir, "avatars").normalize();
        this.profileMapper = profileMapper;
        Files.createDirectories(this.uploadDir);
    }

    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty", "file.empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "Only image files are supported", "file.imageOnly");
        }

        String ext = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new BusinessException(400, "Unsupported image format, supports jpg/png/gif/webp", "file.image.unsupportedFormat");
        };

        String filename = UUID.randomUUID().toString() + ext;
        try {
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target);

            String avatarUrl = "/uploads/avatars/" + filename;
            UserProfile profile = profileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
            if (profile != null) {
                profile.setAvatar(avatarUrl);
                profileMapper.updateById(profile);
            }
            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException(500, "File upload failed", "file.upload.failed");
        }
    }
}
