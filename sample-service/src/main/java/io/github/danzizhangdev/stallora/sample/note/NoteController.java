package io.github.danzizhangdev.stallora.sample.note;

import io.github.danzizhangdev.stallora.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NoteController {

    private final NoteService notes;

    public NoteController(NoteService notes) {
        this.notes = notes;
    }

    public record CreateNoteRequest(@NotBlank @Size(max = 500) String text) {
    }

    @PostMapping("/sample/notes")
    public ApiResponse<NoteService.Note> create(@Valid @RequestBody CreateNoteRequest req) {
        return ApiResponse.ok(notes.create(req.text()));
    }

    @GetMapping("/sample/notes/{id}")
    public ApiResponse<NoteService.Note> get(@PathVariable long id) {
        return ApiResponse.ok(notes.get(id));
    }

    /** Lets the end-to-end script assert that the outbox round trip actually closed. */
    @GetMapping("/sample/notes/{id}/events")
    public ApiResponse<Map<String, Integer>> events(@PathVariable long id) {
        return ApiResponse.ok(Map.of("count", notes.eventCount(id)));
    }
}
