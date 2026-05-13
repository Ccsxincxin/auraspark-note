package com.auraspark.note.core.controller.note;

import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.core.entity.Note;
import com.auraspark.note.core.service.note.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Notes", description = "Notes CRUD and file upload")
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(summary = "List notes", description = "Paginated query of current user's notes, sorted by update time descending")
    @GetMapping
    public ApiResponse<List<Note>> listNotes(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(noteService.listNotes(userId, page, size));
    }

    @Operation(summary = "Get note")
    @GetMapping("/{id}")
    public ApiResponse<Note> getNote(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(noteService.getNote(id, userId));
    }

    @Operation(summary = "Create note", description = "Manually write note content")
    @PostMapping
    public ApiResponse<Note> createNote(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, String> body) {
        Note note = noteService.createNote(userId,
                body.get("title"),
                body.get("content"),
                body.get("format"));
        return ApiResponse.success(note);
    }

    @Operation(summary = "Update note")
    @PutMapping("/{id}")
    public ApiResponse<Note> updateNote(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Note note = noteService.updateNote(id, userId,
                body.get("title"),
                body.get("content"),
                body.get("format"));
        return ApiResponse.success(note);
    }

    @Operation(summary = "Delete note")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNote(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        noteService.deleteNote(id, userId);
        return ApiResponse.success(null, "Note deleted", "note.deleted");
    }

    @Operation(summary = "Upload note", description = "Supports txt / md / markdown formats")
    @PostMapping("/upload")
    public ApiResponse<Note> uploadNote(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(noteService.uploadNote(userId, file));
    }
}
