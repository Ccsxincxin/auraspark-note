package com.auraspark.note.core.controller.auth;

import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.core.service.auth.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "File Upload", description = "Avatar upload")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Operation(summary = "Upload avatar", description = "Supports jpg/png/gif/webp, returns avatar URL")
    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        String url = uploadService.uploadAvatar(userId, file);
        return ApiResponse.success(Map.of("url", url));
    }
}
