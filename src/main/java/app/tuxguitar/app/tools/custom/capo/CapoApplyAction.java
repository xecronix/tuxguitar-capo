package app.tuxguitar.app.tools.custom.capo;

import java.util.function.BooleanSupplier;
import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.song.managers.TGTrackManager;
import app.tuxguitar.util.TGContext;

final class CapoApplyAction extends TGActionBase {
    static final String NAME = "action.tools.capo.apply";
    private final BooleanSupplier playing;

    CapoApplyAction(TGContext context, BooleanSupplier playing) {
        super(context, NAME);
        this.playing = playing;
    }

    @Override protected void processAction(TGActionContext actionContext) {
        CapoRequest request = actionContext.getAttribute(CapoRequest.ATTRIBUTE);
        if (request == null) return;
        request.success = false;
        request.error = playing.getAsBoolean() ? "Stop playback before applying capo settings."
                : request.validate(TGDocumentManager.getInstance(getContext()).getSong());
        if (request.error != null) return;

        TGTrackManager manager = TGDocumentManager.getInstance(getContext()).getSongManager().getTrackManager();
        try {
            for (CapoRequest.Change change : request.changes) manager.changeOffset(change.track, change.after);
            request.success = true;
        } catch (RuntimeException e) {
            // The host Undo controller will discard the uncommitted record on failure.
            for (CapoRequest.Change change : request.changes) change.track.setOffset(change.before);
            request.error = "The capo changes could not be applied. The original offsets were restored.";
        }
    }
}
