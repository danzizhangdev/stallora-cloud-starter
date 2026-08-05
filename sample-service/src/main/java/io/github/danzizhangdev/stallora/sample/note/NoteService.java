package io.github.danzizhangdev.stallora.sample.note;

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

    public NoteService(JdbcTemplate jdbc, OutboxWriter outbox) {
        this.jdbc = jdbc;
        this.outbox = outbox;
    }

    public record Note(long id, String text) {
    }

    /** The row and the event share this transaction; that is the entire point of the outbox. */
    @Transactional
    public Note create(String text) {
        Long id = jdbc.queryForObject(
                "INSERT INTO sample_note (text) VALUES (?) RETURNING id", Long.class, text);
        outbox.write("sample-note-created", String.valueOf(id),
                "{\"noteId\":" + id + ",\"text\":\"" + text.replace("\"", "\\\"") + "\"}");
        return new Note(id, text);
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
