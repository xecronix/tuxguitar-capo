package app.tuxguitar.app.tools.custom.capo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;

/** A dialog session. No operation on this model changes a song. */
final class CapoModel {
    static final class Row {
        final TGTrack track;
        final String name;
        final int original;
        String text;
        boolean selected = true;

        Row(TGTrack track) {
            this.track = track;
            this.name = track.getName();
            this.original = track.getOffset();
            this.text = Integer.toString(original);
        }
    }

    final TGSong song;
    final List<Row> rows;
    private boolean allowNegative;

    CapoModel(TGSong song) {
        this.song = song;
        List<Row> eligible = new ArrayList<>();
        for (int i = 0; i < song.countTracks(); i++) {
            TGTrack track = song.getTrack(i);
            if (!track.isPercussion()) {
                eligible.add(new Row(track));
                allowNegative |= track.getOffset() < 0;
            }
        }
        rows = Collections.unmodifiableList(eligible);
    }

    static Integer parse(String text) {
        if (text == null || !text.trim().matches("[+-]?[0-9]+")) return null;
        try { return Integer.valueOf(text.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    boolean allowsNegative() { return allowNegative; }
    int minimum() { return allowNegative ? TGTrack.MIN_OFFSET : 0; }

    boolean setAllowNegative(boolean allow) {
        if (!allow) {
            for (Row row : rows) {
                Integer value = parse(row.text);
                if (value != null && value < 0) return false;
            }
        }
        allowNegative = allow;
        return true;
    }

    int firstInvalidRow() {
        for (int i = 0; i < rows.size(); i++) {
            Integer value = parse(rows.get(i).text);
            if (value == null || value < minimum() || value > TGTrack.MAX_OFFSET) return i;
        }
        return -1;
    }

    boolean allSelected() {
        if (rows.isEmpty()) return false;
        for (Row row : rows) if (!row.selected) return false;
        return true;
    }

    void selectAll(boolean selected) { for (Row row : rows) row.selected = selected; }

    boolean canShift(int delta) {
        if ((delta != 1 && delta != -1) || firstInvalidRow() >= 0) return false;
        boolean selected = false;
        for (Row row : rows) {
            if (row.selected) {
                selected = true;
                int next = parse(row.text) + delta;
                if (next < minimum() || next > TGTrack.MAX_OFFSET) return false;
            }
        }
        return selected;
    }

    void shift(int delta) {
        if (!canShift(delta)) return;
        for (Row row : rows) {
            if (row.selected) row.text = Integer.toString(parse(row.text) + delta);
        }
    }

    CapoRequest request() {
        if (firstInvalidRow() >= 0) throw new IllegalStateException("Invalid offset");
        return new CapoRequest(this);
    }
}
