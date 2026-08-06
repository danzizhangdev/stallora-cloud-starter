package io.github.danzizhangdev.stallora.sample.note;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danzizhangdev.stallora.common.error.BusinessException;
import io.github.danzizhangdev.stallora.outbox.OutboxWriter;
import io.github.danzizhangdev.stallora.sample.SampleErrorCodes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final JdbcTemplate jdbc;
    private final OutboxWriter outbox;
    private final ObjectMapper mapper;

    public NoteService(JdbcTemplate jdbc, OutboxWriter outbox, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    public record Note(long id, String text) {
    }

    /** Event payload. Serialised by Jackson, never by string concatenation - see create(). */
    record NoteCreated(long noteId, String text) {
    }

    /** The row and the event share this transaction; that is the entire point of the outbox. */
    @Transactional
    public Note create(String text) {
        Long id = jdbc.queryForObject(
                "INSERT INTO sample_note (text) VALUES (?) RETURNING id", Long.class, text);
        outbox.write("sample-note-created", String.valueOf(id), toJson(new NoteCreated(id, text)));
        return new Note(id, text);
    }

    /**
     * Hand-built JSON is a trap here: escaping only the double quote leaves backslashes,
     * newlines and control characters to produce a payload the consumer cannot parse, and
     * the failure lands in the consumer's retry loop rather than on this request.
     */
    private String toJson(NoteCreated event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise outbox payload", e);
        }
    }

    public Note get(long id) {
        var rows = jdbc.queryForList("SELECT id, text FROM sample_note WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new BusinessException(SampleErrorCodes.NOTE_NOT_FOUND);
        }
        var r = rows.get(0);
        return new Note(((Number) r.get("id")).longValue(), (String) r.get("text"));
    }

    public int eventCount(long noteId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM sample_event_log WHERE note_id = ?", Integer.class, noteId);
        return n == null ? 0 : n;
    }
}
