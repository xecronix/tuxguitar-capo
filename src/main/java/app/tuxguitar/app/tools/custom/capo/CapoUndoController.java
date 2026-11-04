package app.tuxguitar.app.tools.custom.capo;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.undo.TGUndoableActionController;
import app.tuxguitar.editor.undo.TGUndoableEdit;
import app.tuxguitar.editor.undo.impl.TGUndoableEditComposite;
import app.tuxguitar.editor.undo.impl.track.TGUndoableTrackInfo;
import app.tuxguitar.util.TGContext;

/** Uses only host Undo records, which remain usable even if the plugin is disabled. */
final class CapoUndoController implements TGUndoableActionController {
    @Override public TGUndoableEdit startUndoable(TGContext context, TGActionContext actionContext) {
        CapoRequest request = actionContext.getAttribute(CapoRequest.ATTRIBUTE);
        if (request == null || request.changes.isEmpty()
                || request.validate(TGDocumentManager.getInstance(context).getSong()) != null) return null;
        TGUndoableEditComposite group = new TGUndoableEditComposite();
        for (CapoRequest.Change change : request.changes) {
            group.addEdit(TGUndoableTrackInfo.startUndo(context, change.track));
        }
        return group;
    }

    @Override public TGUndoableEdit endUndoable(TGContext context, TGActionContext actionContext, TGUndoableEdit edit) {
        CapoRequest request = actionContext.getAttribute(CapoRequest.ATTRIBUTE);
        if (request == null || !request.success || edit == null) return null;
        TGUndoableEditComposite group = (TGUndoableEditComposite) edit;
        for (int i = 0; i < request.changes.size(); i++) {
            ((TGUndoableTrackInfo) group.getEdits().get(i)).endUndo(request.changes.get(i).track);
        }
        return group;
    }
}
