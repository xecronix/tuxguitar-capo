package app.tuxguitar.app.tools.custom.capo;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionAdapterManager;
import app.tuxguitar.app.action.listener.cache.controller.TGUpdateSongController;
import app.tuxguitar.app.document.TGDocumentListManager;
import app.tuxguitar.app.tools.custom.TGToolItemPlugin;
import app.tuxguitar.app.util.TGMessageDialogUtil;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.plugin.TGPluginException;

public final class CapoPlugin extends TGToolItemPlugin {
    private static final String ACTIVE_PLUGIN = CapoPlugin.class.getName() + ".active";
    private boolean connected;
    private CapoDialog dialog;

    @Override public String getModuleId() { return "tuxguitar-capo"; }
    @Override protected String getItemName() { return "capo.settings"; }

    @Override public void connect(TGContext context) throws TGPluginException {
        // Some desktop launchers expose the same service through more than one URL.
        if (connected || context.hasAttribute(ACTIVE_PLUGIN)) return;
        TGActionAdapterManager adapters = TGActionAdapterManager.getInstance(context);
        TGActionManager.getInstance(context).mapAction(CapoApplyAction.NAME,
                new CapoApplyAction(context, () -> TuxGuitar.getInstance().getPlayer().isRunning()));
        adapters.getLockableActionListener().addActionId(CapoApplyAction.NAME);
        adapters.getUndoableActionListener().getControllers().set(CapoApplyAction.NAME, new CapoUndoController());
        adapters.getUpdatableActionListener().getControllers().set(CapoApplyAction.NAME, new TGUpdateSongController() {
            @Override public void update(TGContext host, TGActionContext action) {
                CapoRequest request = action.getAttribute(CapoRequest.ATTRIBUTE);
                if (request != null && request.success && !request.changes.isEmpty()) {
                    TGDocumentListManager.getInstance(host).findCurrentDocument().setUnsaved(true);
                    super.update(host, action);
                }
            }
        });
        try {
            super.connect(context);
            connected = true;
            context.setAttribute(ACTIVE_PLUGIN, this);
        } catch (TGPluginException e) {
            removeApplyAction(context);
            throw e;
        }
    }

    @Override protected void doAction(TGContext context) {
        if (dialog != null && dialog.isOpen()) { dialog.focus(); return; }
        if (TuxGuitar.getInstance().getPlayer().isRunning()) {
            TGMessageDialogUtil.infoMessage(context, "Capo Settings", "Stop playback before opening Capo Settings.");
            return;
        }
        dialog = new CapoDialog(context, new CapoModel(TGDocumentManager.getInstance(context).getSong()));
        dialog.show();
    }

    @Override public void disconnect(TGContext context) throws TGPluginException {
        if (!connected) return;
        if (dialog != null && dialog.isOpen()) dialog.close();
        super.disconnect(context);
        removeApplyAction(context);
        // The superclass leaves these registrations behind; release our entries too.
        TGActionAdapterManager adapters = TGActionAdapterManager.getInstance(context);
        adapters.getKeyBindingActionIds().removeActionId(getItemName());
        adapters.getSyncThreadInterceptor().removeActionId(getItemName());
        connected = false;
        if (context.getAttribute(ACTIVE_PLUGIN) == this) context.removeAttribute(ACTIVE_PLUGIN);
    }

    private void removeApplyAction(TGContext context) {
        TGActionAdapterManager adapters = TGActionAdapterManager.getInstance(context);
        TGActionManager.getInstance(context).unmapAction(CapoApplyAction.NAME);
        adapters.getLockableActionListener().removeActionId(CapoApplyAction.NAME);
        adapters.getUndoableActionListener().getControllers().set(CapoApplyAction.NAME, null);
        adapters.getUpdatableActionListener().getControllers().set(CapoApplyAction.NAME, null);
    }
}
