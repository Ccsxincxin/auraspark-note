package com.auraspark.note.core.service.file;

import com.auraspark.note.core.entity.FileItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    List<FileItem> listFiles(Long userId, Long parentId);
    FileItem createFolder(Long userId, String name, Long parentId);
    FileItem uploadFile(Long userId, Long parentId, MultipartFile file);
    FileItem rename(Long userId, Long id, String newName);
    void delete(Long userId, Long id);
    List<FileItem> getTree(Long userId);
    FileItem getDetail(Long userId, Long id);
}

