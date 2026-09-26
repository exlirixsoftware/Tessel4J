package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Badge;
import net.tessel.laf.components.ContentDialog;
import net.tessel.laf.components.InfoBar;
import net.tessel.laf.components.ProgressRing;
import net.tessel.laf.components.Snackbar;
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
