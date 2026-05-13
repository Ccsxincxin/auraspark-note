package com.auraspark.note.core.service.file.impl;

import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.core.entity.FileItem;
import com.auraspark.note.core.mapper.FileMapper;
import com.auraspark.note.core.service.file.FileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final List<String> ALLOWED_EXTENSIONS = List.of("txt", "md", "markdown", "pdf", "doc", "docx",
            "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "gif", "webp", "svg", "csv", "json", "xml", "yaml", "yml");

    private final FileMapper fileMapper;
    private final Path storageDir;

    public FileServiceImpl(FileMapper fileMapper,
                           @Value("${upload.dir}") String uploadDir) throws IOException {
        this.fileMapper = fileMapper;
        this.storageDir = Paths.get(uploadDir, "files").normalize();
        Files.createDirectories(this.storageDir);
    }

    @Override
    public List<FileItem> listFiles(Long userId, Long parentId) {
        return fileMapper.selectList(new LambdaQueryWrapper<FileItem>()
                .eq(FileItem::getUserId, userId)
                .eq(parentId != null, FileItem::getParentId, parentId)
                .isNull(parentId == null, FileItem::getParentId)
                .orderByDesc(FileItem::getIsFolder)
                .orderByAsc(FileItem::getName));
    }

    @Override
    @Transactional
    public FileItem createFolder(Long userId, String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "Folder name cannot be empty", "validation.folderName.empty");
        }
        if (parentId != null) {
            FileItem parent = fileMapper.selectById(parentId);
            if (parent == null || !parent.getUserId().equals(userId)) {
                throw new BusinessException(404, "Parent folder not found", "folder.parent.notFound");
            }
        }
        FileItem folder = new FileItem();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setIsFolder(true);
        folder.setParentId(parentId);
        fileMapper.insert(folder);
        return folder;
    }

    @Override
    @Transactional
    public FileItem uploadFile(Long userId, Long parentId, MultipartFile file) {
        if (parentId != null) {
            FileItem parent = fileMapper.selectById(parentId);
            if (parent == null || !parent.getUserId().equals(userId)) {
                throw new BusinessException(404, "Target folder not found", "folder.target.notFound");
            }
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BusinessException(400, "Filename cannot be empty", "validation.filename.empty");
        }

        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            ext = filename.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "Unsupported file format: " + ext, "file.format.unsupported");
        }

        String storedName = UUID.randomUUID() + "." + ext;
        try {
            Path target = storageDir.resolve(storedName);
            Files.copy(file.getInputStream(), target);

            String name = dot > 0 ? filename.substring(0, dot) : filename;
            FileItem fi = new FileItem();
            fi.setUserId(userId);
            fi.setName(name);
            fi.setIsFolder(false);
            fi.setParentId(parentId);
            fi.setFormat(ext);
            fi.setSize(file.getSize());
            fi.setUrl("/uploads/files/" + storedName);
            fileMapper.insert(fi);
            return fi;
        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new BusinessException(500, "File upload failed", "file.upload.failed");
        }
    }

    @Override
    @Transactional
    public FileItem rename(Long userId, Long id, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BusinessException(400, "Name cannot be empty", "validation.name.empty");
        }
        FileItem fi = getOwned(userId, id);
        fi.setName(newName);
        fi.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(fi);
        return fi;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        FileItem fi = getOwned(userId, id);
        if (fi.getIsFolder()) {
            deleteRecursive(id);
        } else {
            deleteFileRecord(fi);
        }
    }

    @Override
    public List<FileItem> getTree(Long userId) {
        List<FileItem> all = fileMapper.selectList(new LambdaQueryWrapper<FileItem>()
                .eq(FileItem::getUserId, userId)
                .eq(FileItem::getIsFolder, true)
                .orderByAsc(FileItem::getName));
        return buildTree(all);
    }

    @Override
    public FileItem getDetail(Long userId, Long id) {
        return getOwned(userId, id);
    }

    private void deleteRecursive(Long folderId) {
        List<FileItem> children = fileMapper.selectList(
                new LambdaQueryWrapper<FileItem>().eq(FileItem::getParentId, folderId));
        for (FileItem child : children) {
            if (child.getIsFolder()) {
                deleteRecursive(child.getId());
            } else {
                deleteFileRecord(child);
            }
        }
        fileMapper.deleteById(folderId);
    }

    private void deleteFileRecord(FileItem fi) {
        if (fi.getUrl() != null) {
            try {
                Path p = Paths.get(fi.getUrl().replace("/uploads/files/", ""));

                Path full = storageDir.resolve(p.getFileName());
                Files.deleteIfExists(full);
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", fi.getUrl());
            }
        }
        fileMapper.deleteById(fi.getId());
    }

    private FileItem getOwned(Long userId, Long id) {
        FileItem fi = fileMapper.selectById(id);
        if (fi == null || !fi.getUserId().equals(userId)) {
            throw new BusinessException(404, "File or folder not found", "file.not.found");
        }
        return fi;
    }

    private List<FileItem> buildTree(List<FileItem> all) {
        List<FileItem> roots = new ArrayList<>();
        Map<Long, List<FileItem>> childrenMap = new HashMap<>();
        for (FileItem f : all) {
            if (f.getParentId() == null) {
                roots.add(f);
            } else {
                childrenMap.computeIfAbsent(f.getParentId(), k -> new ArrayList<>()).add(f);
            }
        }
        attachChildren(roots, childrenMap);
        return roots;
    }

    private void attachChildren(List<FileItem> nodes, Map<Long, List<FileItem>> childrenMap) {
        for (FileItem node : nodes) {
            List<FileItem> children = childrenMap.get(node.getId());
            if (children != null) {
                attachChildren(children, childrenMap);
            }
        }
    }
}
