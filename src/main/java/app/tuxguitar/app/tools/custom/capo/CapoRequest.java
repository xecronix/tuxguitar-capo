package app.tuxguitar.app.tools.custom.capo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;

/** Immutable values crossing from the UI to the host editing thread. */
final class CapoRequest {
    static final String ATTRIBUTE = CapoRequest.class.getName();

    static final class Change {
        final TGTrack track;
        final int before;
        final int after;
        Change(CapoModel.Row row) {
            track = row.track;
            before = row.original;
            after = CapoModel.parse(row.text);
        }
    }

    final TGSong song;
    final List<Change> observed;
    final List<Change> changes;
    volatile boolean success;
    volatile String error;

    CapoRequest(CapoModel model) {
        song = model.song;
        List<Change> all = new ArrayList<>();
        List<Change> changed = new ArrayList<>();
        for (CapoModel.Row row : model.rows) {
            Change change = new Change(row);
            all.add(change);
            if (change.before != change.after) changed.add(change);
        }
        observed = Collections.unmodifiableList(all);
        changes = Collections.unmodifiableList(changed);
    }

    String validate(TGSong current) {
        if (current != song) return "The active song changed. Cancel and reopen Capo Settings.";
        for (Change change : observed) {
            boolean found = false;
            for (int i = 0; i < song.countTracks(); i++) found |= song.getTrack(i) == change.track;
            if (!found || change.track.isPercussion() || change.track.getOffset() != change.before) {
                return "A track changed while this dialog was open. Cancel and reopen Capo Settings.";
            }
            if (change.after < TGTrack.MIN_OFFSET || change.after > TGTrack.MAX_OFFSET) {
                return "An offset is outside TuxGuitar's supported range.";
            }
        }
        return null;
    }
}
