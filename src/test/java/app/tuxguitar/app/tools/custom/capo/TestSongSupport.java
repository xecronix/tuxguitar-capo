package app.tuxguitar.app.tools.custom.capo;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.track.TGSetTrackInfoAction;
import app.tuxguitar.editor.undo.TGUndoableEdit;
import app.tuxguitar.editor.undo.TGUndoableManager;
import app.tuxguitar.io.base.TGSongWriterHandle;
import app.tuxguitar.io.tg.TGSongWriterImpl;
import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.*;
import app.tuxguitar.util.TGContext;

final class TestSongSupport {
    final TGContext context = new TGContext();
    final TGDocumentManager document = TGDocumentManager.getInstance(context);
    final TGSong song = document.getSong();
    final TGFactory factory = document.getSongManager().getFactory();

    TestSongSupport(int... offsets) {
        document.setSong(song);
        TGTrack template = song.getTrack(0);
        TGNote note = factory.newNote();
        note.setValue(3);
        note.setString(1);
        template.getMeasure(0).getBeat(0).getVoice(0).addNote(note);
        template = template.clone(factory, song);
        while (song.countTracks() > 0) song.removeTrack(song.getTrack(0));
        String[] names = {"Harmony", "Melody", "Bass", "Second Guitar"};
        for (int i = 0; i < offsets.length; i++) {
            TGTrack track = template.clone(factory, song);
            track.setNumber(i + 1);
            track.setName(i < names.length ? names[i] : "Track " + (i + 1));
            track.setOffset(offsets[i]);
            song.addTrack(track);
        }
        TGActionManager.getInstance(context).mapAction(TGSetTrackInfoAction.NAME, new TGSetTrackInfoAction(context));
    }

    void percussion() {
        TGChannel channel = factory.newChannel();
        channel.setChannelId(9);
        channel.setBank(TGChannel.DEFAULT_PERCUSSION_BANK);
        song.addChannel(channel);
        TGTrack drums = song.getTrack(0).clone(factory, song);
        drums.setChannelId(9);
        drums.setNumber(song.countTracks() + 1);
        drums.setName("Drums (excluded from Capo Settings)");
        drums.setOffset(0);
        song.addTrack(drums);
    }

    TGActionContext actionContext(CapoRequest request) {
        TGActionContext action = new TGActionContext() {};
        action.setAttribute(TGSongManager.class.getName(), document.getSongManager());
        action.setAttribute(CapoRequest.ATTRIBUTE, request);
        return action;
    }

    CapoRequest apply(CapoModel model) {
        CapoRequest request = model.request();
        TGActionContext action = actionContext(request);
        CapoUndoController controller = new CapoUndoController();
        TGUndoableEdit edit = controller.startUndoable(context, action);
        new CapoApplyAction(context, () -> false).execute(action);
        if (edit != null) {
            edit = controller.endUndoable(context, action, edit);
            if (edit != null) TGUndoableManager.getInstance(context).addEdit(edit);
        }
        return request;
    }

    byte[] serialize(boolean compressed) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TGSongWriterHandle handle = new TGSongWriterHandle();
        handle.setFactory(factory);
        handle.setSong(song);
        handle.setOutputStream(bytes);
        if (compressed) new TGSongWriterImpl().write(handle);
        else new TGSongWriterImpl().writeContent(handle);
        return bytes.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paths.get(args[0]);
        Files.createDirectories(folder);
        TestSongSupport basic = new TestSongSupport(0, 2, 0, 2);
        basic.song.setName("Capo plugin smoke test");
        basic.percussion();
        Files.write(folder.resolve("capo-demo.tg"), basic.serialize(true));
        TestSongSupport many = new TestSongSupport(new int[30]);
        many.song.setName("Capo scrolling test");
        many.song.getTrack(12).setName("A long track name that must remain reachable using horizontal scrolling");
        Files.write(folder.resolve("capo-many-tracks.tg"), many.serialize(true));
        TestSongSupport empty = new TestSongSupport(0);
        empty.percussion();
        empty.song.removeTrack(empty.song.getTrack(0));
        empty.song.getTrack(0).setNumber(1);
        Files.write(folder.resolve("capo-percussion-only.tg"), empty.serialize(true));
        TestSongSupport negative = new TestSongSupport(-2, 2);
        Files.write(folder.resolve("capo-negative.tg"), negative.serialize(true));
    }
}
