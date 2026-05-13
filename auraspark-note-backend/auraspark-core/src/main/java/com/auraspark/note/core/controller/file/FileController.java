package com.auraspark.note.core.controller.file;

import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.core.entity.FileItem;
import com.auraspark.note.core.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "File Management", description = "Nested folders and multi-format file upload management")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "List files", description = "List contents under a folder, empty parentId for root")
    @GetMapping
    public ApiResponse<List<FileItem>> listFiles(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Long parentId) {
        return ApiResponse.success(fileService.listFiles(userId, parentId));
    }

    @Operation(summary = "Create folder")
    @PostMapping("/folder")
    public ApiResponse<FileItem> createFolder(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(fileService.createFolder(userId,
                (String) body.get("name"),
                body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null));
    }

    @Operation(summary = "Upload file", description = "Supports various common formats")
    @PostMapping("/upload")
    public ApiResponse<FileItem> uploadFile(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Long parentId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileService.uploadFile(userId, parentId, file));
    }

    @Operation(summary = "Rename")
    @PutMapping("/{id}")
    public ApiResponse<FileItem> rename(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.success(fileService.rename(userId, id, body.get("name")));
    }

    @Operation(summary = "Delete", description = "Deleting a folder recursively deletes all sub-files and sub-folders")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        fileService.delete(userId, id);
        return ApiResponse.success(null, "Deleted", "common.deleted");
    }

    @Operation(summary = "Folder tree", description = "Returns tree structure of all folders for current user")
    @GetMapping("/tree")
    public ApiResponse<List<FileItem>> getTree(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(fileService.getTree(userId));
    }

    @Operation(summary = "File detail")
    @GetMapping("/{id}")
    public ApiResponse<FileItem> getDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(fileService.getDetail(userId, id));
    }
}
