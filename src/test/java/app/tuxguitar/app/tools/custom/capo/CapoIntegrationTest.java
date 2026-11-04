package app.tuxguitar.app.tools.custom.capo;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.editor.undo.TGUndoableEdit;
import app.tuxguitar.editor.undo.TGUndoableManager;
import app.tuxguitar.editor.undo.impl.track.TGUndoableTrackInfo;
import app.tuxguitar.io.base.*;
import app.tuxguitar.io.tg.TGSongReaderImpl;
import app.tuxguitar.player.base.MidiSequenceHandler;
import app.tuxguitar.player.base.MidiSequenceParser;
import app.tuxguitar.song.models.*;
import org.junit.jupiter.api.Test;

class CapoIntegrationTest {
    @Test void allChangedTracksUndoAndRedoTogetherUsingNativeHistory() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2, 0, 2);
        byte[] before = host.serialize(false);
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        model.shift(1);
        model.rows.get(0).selected = false;
        model.rows.get(0).text = "7";
        assertTrue(host.apply(model).success);
        assertOffsets(host, 7, 4, 2, 4);
        TGUndoableManager undo = TGUndoableManager.getInstance(host.context);
        undo.undo(host.actionContext(null));
        assertOffsets(host, 0, 2, 0, 2);
        assertFalse(undo.canUndo());
        assertArrayEquals(before, host.serialize(false), "Undo must restore all song content");
        undo.redo(host.actionContext(null));
        assertOffsets(host, 7, 4, 2, 4);
        assertFalse(undo.canRedo());
        assertEquals("7", new CapoModel(host.song).rows.get(0).text);
    }

    @Test void nativeEditsBeforeAndAfterRemainChronological() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        TGUndoableManager undo = TGUndoableManager.getInstance(host.context);
        TGTrack track = host.song.getTrack(0);
        TGUndoableTrackInfo earlier = TGUndoableTrackInfo.startUndo(host.context, track);
        track.setName("Earlier rename");
        undo.addEdit(earlier.endUndo(track));
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        host.apply(model);
        TGUndoableTrackInfo later = TGUndoableTrackInfo.startUndo(host.context, track);
        track.setName("Later rename");
        undo.addEdit(later.endUndo(track));
        undo.undo(host.actionContext(null));
        assertEquals("Earlier rename", track.getName());
        assertOffsets(host, 1, 3);
        undo.undo(host.actionContext(null));
        assertOffsets(host, 0, 2);
        assertEquals("Earlier rename", track.getName());
        undo.undo(host.actionContext(null));
        assertEquals("Harmony", track.getName());
        for (int i = 0; i < 3; i++) undo.redo(host.actionContext(null));
        assertOffsets(host, 1, 3);
        assertEquals("Later rename", track.getName());
    }

    @Test void noopAndAbandonedSessionPreserveRedoButNewCommitReplacesIt() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        host.apply(model);
        TGUndoableManager undo = TGUndoableManager.getInstance(host.context);
        undo.undo(host.actionContext(null));
        CapoModel abandoned = new CapoModel(host.song);
        abandoned.shift(1);
        assertTrue(undo.canRedo());
        assertTrue(host.apply(new CapoModel(host.song)).success);
        assertTrue(undo.canRedo());
        CapoModel replacement = new CapoModel(host.song);
        replacement.rows.get(0).text = "3";
        host.apply(replacement);
        assertFalse(undo.canRedo());
        undo.undo(host.actionContext(null));
        assertOffsets(host, 0, 2);
    }

    @Test void staleSongOrTrackIsRejectedBeforeAnyChange() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        host.song.getTrack(1).setOffset(5);
        assertFalse(host.apply(model).success);
        assertOffsets(host, 0, 5);
        assertFalse(TGUndoableManager.getInstance(host.context).canUndo());
        host.document.setSong(new TestSongSupport(9).song);
        assertFalse(host.apply(model).success);
        assertEquals(9, host.document.getSong().getTrack(0).getOffset());
    }

    @Test void playbackPreventsApplicationAndUndoRecord() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        CapoRequest request = model.request();
        TGActionContext action = host.actionContext(request);
        CapoUndoController controller = new CapoUndoController();
        TGUndoableEdit record = controller.startUndoable(host.context, action);
        new CapoApplyAction(host.context, () -> true).execute(action);
        assertFalse(request.success);
        assertNull(controller.endUndoable(host.context, action, record));
        assertOffsets(host, 0, 2);
    }

    @Test void mutationFailureRollsBackEveryTrackAndDiscardsUndoRecord() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        TGTrack failOnce = new TGTrack(host.factory) {
            boolean armed = true;
            @Override public void setOffset(int value) {
                if (value == 3 && armed) { armed = false; throw new IllegalStateException("Injected failure"); }
                super.setOffset(value);
            }
        };
        failOnce.copyFrom(host.factory, host.song, host.song.getTrack(1));
        host.song.removeTrack(host.song.getTrack(1));
        host.song.addTrack(failOnce);
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        assertFalse(host.apply(model).success);
        assertOffsets(host, 0, 2);
        assertFalse(TGUndoableManager.getInstance(host.context).canUndo());
    }

    @Test void nativeFileRoundTripPreservesOffsetsFretsAndAlteredTuning() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        host.song.getTrack(0).getString(1).setValue(72);
        CapoModel model = new CapoModel(host.song);
        model.setAllowNegative(true);
        model.rows.get(0).text = "-3";
        model.rows.get(1).text = "24";
        host.apply(model);
        TGSongReaderHandle reader = new TGSongReaderHandle();
        reader.setFactory(host.factory);
        reader.setContext(new TGSongStreamContext());
        reader.setInputStream(new ByteArrayInputStream(host.serialize(true)));
        new TGSongReaderImpl().read(reader);
        TGSong song = reader.getSong();
        assertEquals(-3, song.getTrack(0).getOffset());
        assertEquals(24, song.getTrack(1).getOffset());
        assertEquals(72, song.getTrack(0).getString(1).getValue());
        assertEquals(3, song.getTrack(0).getMeasure(0).getBeat(0).getVoice(0).getNote(0).getValue());
        assertEquals(1, song.getTrack(0).getMeasure(0).getBeat(0).getVoice(0).getNote(0).getString());
    }

    @Test void realMidiParserReflectsCommittedOffsetsAndTheirUndo() throws Exception {
        TestSongSupport host = new TestSongSupport(0, 2);
        List<Integer> before = pitches(host);
        assertFalse(before.isEmpty());
        CapoModel model = new CapoModel(host.song);
        model.shift(1);
        assertEquals(before, pitches(host), "Staging must not affect playback");
        host.apply(model);
        List<Integer> after = pitches(host);
        assertEquals(before.size(), after.size());
        for (int i = 0; i < before.size(); i++) assertEquals(before.get(i) + 1, after.get(i));
        TGUndoableManager.getInstance(host.context).undo(host.actionContext(null));
        assertEquals(before, pitches(host));
    }

    private static void assertOffsets(TestSongSupport host, int... offsets) {
        for (int i = 0; i < offsets.length; i++) assertEquals(offsets[i], host.song.getTrack(i).getOffset(), "track " + i);
    }

    private static List<Integer> pitches(TestSongSupport host) {
        List<Integer> notes = new ArrayList<>();
        MidiSequenceHandler handler = new MidiSequenceHandler(host.song.countTracks() + 2) {
            public void addNoteOn(long t, int tr, int ch, int note, int v, int voice, boolean bend) { notes.add(note); }
            public void addNoteOff(long t, int tr, int ch, int n, int v, int voice, boolean bend) {}
            public void addPitchBend(long t, int tr, int ch, int v, int voice, boolean bend) {}
            public void addControlChange(long t, int tr, int ch, int c, int v) {}
            public void addProgramChange(long t, int tr, int ch, int i) {}
            public void addTempoInUSQ(long t, int tr, int usq) {}
            public void addTrackName(long t, int tr, String name) {}
            public void addTimeSignature(long t, int tr, TGTimeSignature ts) {}
            public void notifyFinish() {}
        };
        new MidiSequenceParser(host.song, host.document.getSongManager(), 0).parse(handler);
        return notes;
    }
}
