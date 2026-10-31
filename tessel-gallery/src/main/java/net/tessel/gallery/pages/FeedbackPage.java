package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.ProgressState;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Badge;
import net.tessel.laf.components.ContentDialog;
import net.tessel.laf.components.InfoBar;
import net.tessel.laf.components.ProgressRing;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.TeachingTip;
import net.tessel.laf.components.ToggleSwitch;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;

public class FeedbackPage extends Page {

    private final JLabel dialogResult = Tessel.label("Result: -", TextStyle.SECONDARY);

    public FeedbackPage() {
        super("Feedback", "Progress indicators, info bars, badges, snackbars and dialogs.");

        // Progress
        JProgressBar determinate = new JProgressBar(0, 100);
        JSlider driver = new JSlider(0, 100, 42);
        determinate.setValue(42);
        driver.addChangeListener(e -> determinate.setValue(driver.getValue()));
        JProgressBar indeterminate = new JProgressBar();
        indeterminate.setIndeterminate(true);
        JProgressBar success = new JProgressBar(0, 100);
        success.setValue(80);
        success.setForeground(Tessel.palette().success);
        Tessel.addThemeListener(() -> success.setForeground(Tessel.palette().success));

        JPanel bars = Layouts.stack(10,
                Tessel.label("Determinate", TextStyle.BODY), determinate, driver,
                Tessel.label("Indeterminate", TextStyle.BODY), indeterminate,
                Tessel.label("Custom color", TextStyle.BODY), success);

        ProgressRing small = new ProgressRing(20);
        ProgressRing medium = new ProgressRing(32);
        ProgressRing large = new ProgressRing(48);
        ToggleSwitch active = new ToggleSwitch("Active", true);
        active.addItemListener(e -> {
            small.setActive(active.isSelected());
            medium.setActive(active.isSelected());
            large.setActive(active.isSelected());
        });
        JPanel rings = Layouts.stack(16, Layouts.row(small, medium, large), active);
        rings.setBorder(BorderFactory.createEmptyBorder(0, 48, 0, 8));

        JPanel progress = Layouts.panel(new BorderLayout());
        progress.add(bars, BorderLayout.CENTER);
        progress.add(rings, BorderLayout.EAST);
        section("Progress", progress);

        // Progress states
        section("Progress states", progressStates());

        // Info bars
        InfoBar warning = new InfoBar(Severity.WARNING, "Heads up", "Your subscription expires in 3 days.");
        warning.setAction(Tessel.style(new JButton("Renew"), ButtonStyle.OUTLINE));
        InfoBar error = new InfoBar(Severity.ERROR, "Connection lost", "We couldn't reach the server. Check your connection.");
        JButton reopen = Tessel.style(new JButton("Reopen closed info bars"), ButtonStyle.SUBTLE);
        InfoBar info = new InfoBar(Severity.INFORMATIONAL, "Update", "A new version is available.");
        InfoBar saved = new InfoBar(Severity.SUCCESS, "Saved", "Your profile has been saved.");
        reopen.addActionListener(e -> {
            for (InfoBar bar : new InfoBar[]{info, saved, warning, error}) bar.setOpen(true);
        });
        section("InfoBar", Layouts.stack(8, info, saved, warning, error, Layouts.row(reopen)));

        // Badges
        section("Badges", Layouts.stack(8,
                Layouts.row(new Badge("Neutral"), new Badge("Accent", Badge.Kind.ACCENT), new Badge("Success", Badge.Kind.SUCCESS),
                        new Badge("Warning", Badge.Kind.WARNING), new Badge("Danger", Badge.Kind.DANGER), new Badge("Info", Badge.Kind.INFO)),
                Layouts.row(new Badge("Neutral", Badge.Kind.NEUTRAL, true), new Badge("Accent", Badge.Kind.ACCENT, true),
                        new Badge("Success", Badge.Kind.SUCCESS, true), new Badge("Warning", Badge.Kind.WARNING, true),
                        new Badge("Danger", Badge.Kind.DANGER, true), new Badge("Info", Badge.Kind.INFO, true))));

        // Snackbar
        section("Snackbar", Layouts.row(
                snackButton("Info", Severity.INFORMATIONAL, "Heads up", "Snackbars disappear after a few seconds."),
                snackButton("Success", Severity.SUCCESS, "Upload complete", "3 files were uploaded to Documents."),
                snackButton("Warning", Severity.WARNING, "Low storage", "You have less than 1 GB left."),
                snackButton("Error", Severity.ERROR, "Sync failed", "Retrying in 30 seconds.")));

        // Dialogs
        JButton confirm = Tessel.style(new JButton("Confirmation dialog"), ButtonStyle.ACCENT);
        confirm.addActionListener(e -> showConfirmation());
        JButton destructive = new JButton("Destructive dialog");
        destructive.addActionListener(e -> showDestructive());
        JButton input = new JButton("Dialog with input");
        input.addActionListener(e -> showInput());
        section("ContentDialog", Layouts.stack(12, Layouts.row(confirm, destructive, input), dialogResult));

        // Teaching tips
        JButton auto = Tessel.style(new JButton("Show a tip"), ButtonStyle.ACCENT);
        TeachingTip autoTip = new TeachingTip("Save your work", "Press Ctrl+S at any time to save the current document. "
                + "The tip picks the side with the most room.");
        autoTip.setActionButton("Got it", () -> Snackbar.show(this, "Tip dismissed", null, Severity.SUCCESS));
        autoTip.setCloseButtonText("Remind me later");
        autoTip.setIcon(new FontIcon(Symbol.LIGHTBULB, 16));
        auto.addActionListener(e -> autoTip.show(auto));

        JPanel placements = Layouts.row();
        for (TeachingTip.Placement placement : new TeachingTip.Placement[]{
                TeachingTip.Placement.TOP, TeachingTip.Placement.BOTTOM, TeachingTip.Placement.LEFT, TeachingTip.Placement.RIGHT}) {
            String name = placement.name().charAt(0) + placement.name().substring(1).toLowerCase();
            JButton b = new JButton(name);
            TeachingTip tip = new TeachingTip("Placed " + name.toLowerCase(), "This tip points at its button from the " + name.toLowerCase() + ".");
            tip.setPlacement(placement);
            tip.setContentWidth(260);
            b.addActionListener(e -> tip.show(b));
            placements.add(b);
        }

        JButton flyout = new JButton("Flyout (no tail)");
        TeachingTip flyoutTip = new TeachingTip("Quick settings", "Without a tail the tip becomes a flyout with any content.");
        flyoutTip.setTailVisible(false);
        flyoutTip.setCloseButtonVisible(false);
        flyoutTip.setContentWidth(280);
        javax.swing.JCheckBox notifications = new javax.swing.JCheckBox("Notifications", true);
        ToggleSwitch dnd = new ToggleSwitch("Do not disturb", false);
        flyoutTip.setContent(Layouts.stack(4, notifications, dnd));
        flyout.addActionListener(e -> flyoutTip.show(flyout));

        section("TeachingTip", Layouts.stack(12,
                Layouts.row(auto, flyout),
                Layouts.row(Tessel.label("Fixed placement", TextStyle.BODY)),
                placements,
                Tessel.label("Press Esc or click anywhere else to dismiss a tip.", TextStyle.CAPTION)));
    }

    private static JPanel progressStates() {
        String[] names = {"Normal", "Paused", "Error", "Success"};
        ProgressState[] states = ProgressState.values();
        JPanel rows = Layouts.stack(10);
        for (int i = 0; i < states.length; i++) {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(states[i] == ProgressState.SUCCESS ? 100 : 60);
            Tessel.progressState(bar, states[i]);
            JProgressBar busy = new JProgressBar();
            busy.setIndeterminate(true);
            Tessel.progressState(busy, states[i]);
            rows.add(Layouts.columns(24, Layouts.stack(4, Tessel.label(names[i], TextStyle.BODY), bar),
                    Layouts.stack(4, Tessel.label(names[i] + " (indeterminate)", TextStyle.BODY), busy)));
        }

        // One bar and one ring driven by a selector.
        JComboBox<String> chooser = new JComboBox<>(names);
        JProgressBar live = new JProgressBar(0, 100);
        live.setValue(45);
        JProgressBar liveBusy = new JProgressBar();
        liveBusy.setIndeterminate(true);
        ProgressRing ring = new ProgressRing(32);
        chooser.addActionListener(e -> {
            ProgressState st = states[chooser.getSelectedIndex()];
            Tessel.progressState(live, st);
            Tessel.progressState(liveBusy, st);
            ring.setState(st);
        });
        JPanel ringRow = Layouts.row(new ProgressRing(32), ring);
        ProgressRing paused = new ProgressRing(32);
        paused.setState(ProgressState.PAUSED);
        ProgressRing error = new ProgressRing(32);
        error.setState(ProgressState.ERROR);
        ProgressRing done = new ProgressRing(32);
        done.setState(ProgressState.SUCCESS);
        JPanel rings = Layouts.row(new ProgressRing(32), paused, error, done);

        rows.add(Tessel.label("Rings: normal, paused, error, success", TextStyle.BODY));
        rows.add(rings);
        rows.add(Layouts.field("Try a state on live indicators", chooser));
        rows.add(live);
        rows.add(liveBusy);
        rows.add(ringRow);
        rows.add(Tessel.label("Tessel.progressState(bar, ProgressState.PAUSED) / ProgressRing.setState(...). Paused and error stop an "
                + "indeterminate animation; error and success fill an indeterminate bar.", TextStyle.CAPTION));
        return rows;
    }

    private JButton snackButton(String text, Severity severity, String title, String message) {
        JButton b = new JButton(text);
        b.addActionListener(e -> Snackbar.show(this, title, message, severity));
        return b;
    }

    private void showConfirmation() {
        ContentDialog d = new ContentDialog("Save changes?",
                "You have unsaved changes to “Quarterly report.docx”. Do you want to save them before closing?");
        d.setPrimaryButtonText("Save");
        d.setSecondaryButtonText("Don't save");
        d.setCloseButtonText("Cancel");
        ContentDialog.Result result = d.showModal(this);
        dialogResult.setText("Result: " + result);
    }

    private void showDestructive() {
        ContentDialog d = new ContentDialog("Delete project?",
                "This permanently deletes the project and all of its files. This action cannot be undone.");
        d.setPrimaryButtonText("Delete");
        d.setCloseButtonText("Cancel");
        d.setPrimaryDestructive(true);
        ContentDialog.Result result = d.showModal(this);
        dialogResult.setText("Result: " + result);
        if (result == ContentDialog.Result.PRIMARY) Snackbar.show(this, "Project deleted", null, Severity.SUCCESS);
    }

    private void showInput() {
        JTextField name = Tessel.leadingIcon(Tessel.placeholder(new JTextField(), "New folder"), new FontIcon(Symbol.FOLDER, 14));
        ContentDialog d = new ContentDialog("Create folder", Layouts.field("Folder name", name));
        d.setPrimaryButtonText("Create");
        d.setCloseButtonText("Cancel");
        d.setPrimaryButtonEnabled(false);
        name.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                d.setPrimaryButtonEnabled(!name.getText().trim().isEmpty());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        name.addActionListener(e -> {
            if (d.isPrimaryButtonEnabled()) d.hide(ContentDialog.Result.PRIMARY);
        });
        ContentDialog.Result result = d.showModal(this);
        dialogResult.setText(result == ContentDialog.Result.PRIMARY ? "Result: created “" + name.getText().trim() + "”" : "Result: cancelled");
    }
}
