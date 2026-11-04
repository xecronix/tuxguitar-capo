package app.tuxguitar.app.tools.custom.capo;

import java.util.ArrayList;
import java.util.List;
import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UIScrollBarPanelLayout;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UIKey;
import app.tuxguitar.ui.resource.UIRectangle;
import app.tuxguitar.ui.widget.*;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGSynchronizer;

final class CapoDialog {
    private final TGContext context;
    private final CapoModel model;
    private final List<UICheckBox> selections = new ArrayList<>();
    private final List<UITextField> fields = new ArrayList<>();
    private UIWindow window;
    private UICheckBox all;
    private UICheckBox negatives;
    private UIButton up;
    private UIButton down;
    private UIButton ok;
    private UIButton cancel;
    private UIWrapLabel status;
    private UIScrollBarPanel scroll;
    private boolean refreshing;
    private boolean busy;

    CapoDialog(TGContext context, CapoModel model) { this.context = context; this.model = model; }
    boolean isOpen() { return window != null && !window.isDisposed(); }
    void focus() { window.moveToTop(); }
    void close() { if (isOpen() && !busy) window.dispose(); }

    void show() {
        UIFactory factory = TGApplication.getInstance(context).getFactory();
        window = factory.createWindow(TGWindow.getInstance(context).getWindow(), true, true);
        window.setText("Capo Settings");
        window.setImage(TuxGuitar.getInstance().getIconManager().getAppIcon());
        UITableLayout layout = new UITableLayout(12f);
        window.setLayout(layout);
        escape(window);

        UIPanel controls = factory.createPanel(window, false);
        UITableLayout top = new UITableLayout(0f);
        controls.setLayout(top);
        layout.set(controls, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);
        all = factory.createCheckBox(controls);
        all.setText("All Tracks");
        top.set(all, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 230f, null, null);
        all.addSelectionListener(e -> {
            if (!refreshing) { model.selectAll(all.isSelected()); refresh(null); }
        });
        escape(all);
        up = button(factory, controls, "Up", () -> shift(1));
        down = button(factory, controls, "Down", () -> shift(-1));
        top.set(up, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 85f, 28f, null);
        top.set(down, 1, 3, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 85f, 28f, null);
        up.setToolTipText("Raise selected tracks by one semitone.");
        down.setToolTipText("Lower selected tracks by one semitone.");

        negatives = factory.createCheckBox(controls);
        negatives.setText("Allow Negative Fret Numbers");
        top.set(negatives, 2, 2, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false, 1, 2, null, 28f, null);
        negatives.addSelectionListener(e -> {
            if (!refreshing) {
                boolean accepted = model.setAllowNegative(negatives.isSelected());
                refresh(accepted ? null : "Change all negative values to zero or above first.");
            }
        });
        escape(negatives);

        scroll = factory.createScrollBarPanel(window, true, true, false);
        scroll.setLayout(new UIScrollBarPanelLayout(true, true, false, false, true, true));
        float listHeight = Math.min(320f, 72f + Math.max(1, model.rows.size()) * 32f);
        layout.set(scroll, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 470f, listHeight, null);
        scroll.getVScroll().setIncrement(30);
        scroll.getHScroll().setIncrement(30);
        scroll.getVScroll().addSelectionListener(e -> scroll.layout());
        scroll.getHScroll().addSelectionListener(e -> scroll.layout());
        UIPanel rows = factory.createPanel(scroll, false);
        UITableLayout rowLayout = new UITableLayout(2f);
        rows.setLayout(rowLayout);
        UILabel names = factory.createLabel(rows);
        names.setText("Track Name");
        rowLayout.set(names, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 345f, 24f, null);
        UILabel frets = factory.createLabel(rows);
        frets.setText("Fret");
        frets.setToolTipText("Playback offset in semitones; written frets and tuning stay unchanged.");
        rowLayout.set(frets, 1, 2, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 90f, null, null);
        UISeparator separator = factory.createHorizontalSeparator(rows);
        rowLayout.set(separator, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 2, null, null, null);
        for (int i = 0; i < model.rows.size(); i++) {
            final int index = i;
            CapoModel.Row row = model.rows.get(i);
            UICheckBox selected = factory.createCheckBox(rows);
            selected.setText(row.name);
            selected.addSelectionListener(e -> {
                if (!refreshing) { row.selected = selected.isSelected(); refresh(null); }
            });
            rowLayout.set(selected, i + 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, null, 28f, null);
            selections.add(selected);
            UITextField field = factory.createTextField(rows);
            field.setText(row.text);
            field.addModifyListener(e -> {
                if (!refreshing) { row.text = field.getText(); refresh(null); }
            });
            field.addFocusGainedListener(e -> reveal(index));
            selected.addFocusGainedListener(e -> reveal(index));
            rowLayout.set(field, i + 3, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 90f, 26f, null);
            fields.add(field);
            escape(selected);
            escape(field);
            wheel(selected);
            wheel(field);
        }
        if (model.rows.isEmpty()) {
            UILabel empty = factory.createLabel(rows);
            empty.setText("This song has no non-percussion tracks.");
            rowLayout.set(empty, 3, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 2, null, 35f, null);
        }
        wheel(rows);
        wheel(scroll);
        rows.computePackedSize(null, null);
        float listWidth = Math.min(760f, Math.max(470f, rows.getPackedSize().getWidth() + 32f));
        layout.set(scroll, UITableLayout.MINIMUM_PACKED_WIDTH, listWidth);

        status = factory.createWrapLabel(window);
        layout.set(status, 3, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 1, 470f, 40f, null);
        layout.set(status, UITableLayout.PACKED_WIDTH, 470f);

        UIPanel buttons = factory.createPanel(window, false);
        UITableLayout bottom = new UITableLayout(0f);
        buttons.setLayout(bottom);
        layout.set(buttons, 4, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);
        cancel = button(factory, buttons, "Cancel", this::close);
        bottom.set(cancel, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 90f, 28f, null);
        ok = button(factory, buttons, "OK", this::apply);
        bottom.set(ok, 1, 2, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 90f, 28f, null);
        ok.setDefaultButton();
        refresh(null);
        TGDialogUtil.openDialog(window, TGDialogUtil.OPEN_STYLE_PACK | TGDialogUtil.OPEN_STYLE_CENTER);
    }

    private UIButton button(UIFactory factory, UIContainer parent, String text, Runnable action) {
        UIButton button = factory.createButton(parent);
        button.setText(text);
        button.addSelectionListener(e -> { if (!busy) action.run(); });
        escape(button);
        return button;
    }

    private void escape(UIControl control) {
        control.addKeyPressedListener(e -> { if (e.getKeyCombination().contains(UIKey.ESC)) close(); });
    }

    private void wheel(UIControl control) {
        control.addMouseWheelListener(e -> {
            scroll.getVScroll().setValue(Math.max(0, scroll.getVScroll().getValue() - e.getValue() * 30));
            scroll.layout();
        });
    }

    private void reveal(int index) {
        UIRectangle bounds = fields.get(index).getBounds();
        int value = scroll.getVScroll().getValue();
        int bottom = Math.round(bounds.getY() + bounds.getHeight());
        int height = Math.round(scroll.getChildArea().getHeight());
        if (bounds.getY() < value) scroll.getVScroll().setValue(Math.round(bounds.getY()));
        else if (bottom > value + height) scroll.getVScroll().setValue(bottom - height);
        scroll.layout();
    }

    private void shift(int delta) {
        model.shift(delta);
        refreshing = true;
        for (int i = 0; i < fields.size(); i++) fields.get(i).setText(model.rows.get(i).text);
        refreshing = false;
        refresh(null);
    }

    private void refresh(String message) {
        refreshing = true;
        all.setSelected(model.allSelected());
        all.setEnabled(!busy && !model.rows.isEmpty());
        negatives.setSelected(model.allowsNegative());
        negatives.setEnabled(!busy && !model.rows.isEmpty());
        up.setEnabled(!busy && model.canShift(1));
        down.setEnabled(!busy && model.canShift(-1));
        for (int i = 0; i < fields.size(); i++) {
            selections.get(i).setSelected(model.rows.get(i).selected);
            selections.get(i).setEnabled(!busy);
            fields.get(i).setEnabled(!busy);
        }
        ok.setEnabled(!busy);
        cancel.setEnabled(!busy);
        int invalid = model.firstInvalidRow();
        status.setText(message != null ? message : invalid >= 0
                ? "Track " + (invalid + 1) + " (" + model.rows.get(invalid).name + "): enter a whole number from " + model.minimum() + " to " + TGTrack.MAX_OFFSET + "."
                : "Each step shifts playback by one semitone. Changes apply when you click OK.");
        refreshing = false;
    }

    private void apply() {
        int invalid = model.firstInvalidRow();
        if (invalid >= 0) { refresh(null); reveal(invalid); fields.get(invalid).setFocus(); return; }
        CapoRequest request = model.request();
        if (request.changes.isEmpty()) { close(); return; }
        busy = true;
        refresh("Applying capo settings...");
        TGActionProcessor processor = new TGActionProcessor(context, CapoApplyAction.NAME);
        processor.setAttribute(CapoRequest.ATTRIBUTE, request);
        processor.setOnFinish(() -> finish(request.success, request.error));
        processor.setErrorHandler(error -> finish(false, "The host could not complete the operation. Reopen the dialog to check the song."));
        processor.process();
    }

    private void finish(boolean success, String error) {
        TGSynchronizer.getInstance(context).executeLater(() -> {
            busy = false;
            if (!isOpen()) return;
            if (success) close();
            else refresh(error != null ? error : "The changes were not applied. Try again after stopping playback.");
        });
    }
}
