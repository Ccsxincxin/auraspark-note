package com.auraspark.note.core.service.note.impl;

import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.core.entity.Note;
import com.auraspark.note.core.mapper.NoteMapper;
import com.auraspark.note.core.service.note.NoteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteServiceImpl implements NoteService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("txt", "md", "markdown");

    private final NoteMapper noteMapper;

    public NoteServiceImpl(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    @Override
    public List<Note> listNotes(Long userId, int page, int size) {
        Page<Note> p = new Page<>(page, size);
        return noteMapper.selectPage(p, new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getUpdatedAt)).getRecords();
    }

    @Override
    public Note getNote(Long id, Long userId) {
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(404, "Note not found", "note.not.found");
        }
        return note;
    }

    @Override
    @Transactional
    public Note createNote(Long userId, String title, String content, String format) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(400, "Title cannot be empty", "validation.title.empty");
        }
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content != null ? content : "");
        note.setFormat(format != null ? format : "md");
        noteMapper.insert(note);
        return note;
    }

    @Override
    @Transactional
    public Note updateNote(Long id, Long userId, String title, String content, String format) {
        Note note = getNote(id, userId);
        if (title != null) note.setTitle(title);
        if (content != null) note.setContent(content);
        if (format != null) note.setFormat(format);
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(note);
        return note;
    }

    @Override
    @Transactional
    public void deleteNote(Long id, Long userId) {
        Note note = getNote(id, userId);
        noteMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Note uploadNote(Long userId, MultipartFile file) {
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
            throw new BusinessException(400, "Unsupported file format, supports txt, md, markdown", "file.format.unsupported");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            String title = filename;
            if (dot > 0) title = filename.substring(0, dot);

            return createNote(userId, title, content, "md".equals(ext) ? "md" : "txt");
        } catch (java.io.IOException e) {
            throw new BusinessException(500, "Failed to read file", "file.read.failed");
        }
    }
}
