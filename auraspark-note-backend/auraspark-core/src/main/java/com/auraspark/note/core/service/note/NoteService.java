package com.auraspark.note.core.service.note;

import com.auraspark.note.core.entity.Note;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NoteService {
    List<Note> listNotes(Long userId, int page, int size);
    Note getNote(Long id, Long userId);
    Note createNote(Long userId, String title, String content, String format);
    Note updateNote(Long id, Long userId, String title, String content, String format);
    void deleteNote(Long id, Long userId);
    Note uploadNote(Long userId, MultipartFile file);
}

