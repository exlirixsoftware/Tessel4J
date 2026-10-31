package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.ContentDialog;
import net.tessel.laf.components.NumberBox;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.TesselDialog;
import net.tessel.laf.components.TextBlock;
import net.tessel.laf.components.ToggleSwitch;

public class DialogsPage extends Page {

    private final JLabel result = Tessel.label("Result: -", TextStyle.SECONDARY);
    private TesselDialog palette;

    public DialogsPage() {
        super("Dialogs", "Standard Swing dialogs (JOptionPane, JFileChooser, JColorChooser, JDialog) get the Tessel title bar, "
                + "outline and - on Windows - native shadow, rounded corners and snapping. TesselDialog and ContentDialog are the "
                + "components for your own dialogs.");

        // JOptionPane: messages
        section("Messages", Layouts.row(
                button("Information", ButtonStyle.STANDARD, () -> JOptionPane.showMessageDialog(window(),
                        "Your changes were saved to Quarterly report.docx.", "Saved", JOptionPane.INFORMATION_MESSAGE)),
                button("Warning", ButtonStyle.STANDARD, () -> JOptionPane.showMessageDialog(window(),
                        "You have less than 1 GB of free space left on this drive.", "Low storage", JOptionPane.WARNING_MESSAGE)),
                button("Error", ButtonStyle.STANDARD, () -> JOptionPane.showMessageDialog(window(),
                        "We couldn't reach the server.\nCheck your connection and try again.", "Connection lost", JOptionPane.ERROR_MESSAGE))));

        // JOptionPane: questions and input
        section("Questions & input", Layouts.stack(12, Layouts.row(
                button("Confirm", ButtonStyle.ACCENT, () -> {
                    int r = JOptionPane.showConfirmDialog(window(), "Do you want to save the changes before closing?",
                            "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    result.setText("Result: " + (r == JOptionPane.YES_OPTION ? "Yes" : r == JOptionPane.NO_OPTION ? "No"
                            : r == JOptionPane.CANCEL_OPTION ? "Cancel" : "closed"));
                }),
                button("Custom buttons", ButtonStyle.STANDARD, () -> {
                    String[] options = {"Replace", "Keep both", "Skip"};
                    int r = JOptionPane.showOptionDialog(window(), "This file already exists. What do you want to do?", "File exists",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    result.setText("Result: " + (r < 0 ? "closed" : options[r]));
                }),
                button("Ask for text", ButtonStyle.STANDARD, () -> {
                    String name = JOptionPane.showInputDialog(window(), "Name of the new folder:", "Create folder", JOptionPane.PLAIN_MESSAGE);
                    result.setText("Result: " + (name == null ? "cancelled" : "“" + name + "”"));
                }),
                button("Pick from a list", ButtonStyle.STANDARD, () -> {
                    Object choice = JOptionPane.showInputDialog(window(), "Pick a color scheme:", "Scheme", JOptionPane.QUESTION_MESSAGE,
                            null, new String[]{"Light", "Dark", "System"}, "Dark");
                    result.setText("Result: " + (choice == null ? "cancelled" : choice));
                })),
                result));

        // TesselDialog
        JButton rename = button("Modal dialog", ButtonStyle.ACCENT, this::showRename);
        JButton modeless = button("Modeless palette", ButtonStyle.STANDARD, this::togglePalette);
        section("TesselDialog", Layouts.stack(12, Layouts.row(rename, modeless),
                new TextBlock("A JDialog with the same chrome: Esc and the close button call the window listeners first, "
                        + "so a windowClosing handler can ask before closing.", TextStyle.CAPTION)));

        // ContentDialog
        JButton inWindow = button("In-window dialog", ButtonStyle.STANDARD, () -> {
            ContentDialog d = new ContentDialog("Delete project?", "This permanently deletes the project and all of its files.");
            d.setPrimaryButtonText("Delete");
            d.setPrimaryDestructive(true);
            d.setCloseButtonText("Cancel");
            ContentDialog.Result r = d.showModal(this);
            result.setText("Result: " + r);
        });
        section("ContentDialog", Layouts.stack(12, Layouts.row(inWindow),
                new TextBlock("ContentDialog is drawn over the window's own content instead of in a separate window; "
                        + "use it for quick confirmations that should feel part of the app.", TextStyle.CAPTION)));

        // Chrome switch
        ToggleSwitch chrome = new ToggleSwitch("Tessel title bar on dialogs", Tessel.isDecoratedDialogs());
        chrome.addItemListener(e -> {
            Tessel.setDecoratedDialogs(chrome.isSelected());
            Snackbar.show(this, chrome.isSelected() ? "Dialogs use the Tessel title bar" : "Dialogs use the system title bar",
                    "Applies to dialogs opened from now on.", Severity.INFORMATIONAL);
        });
        section("Window chrome", Layouts.stack(12, Layouts.row(chrome),
                new TextBlock("Tessel.setDecoratedDialogs(false) keeps the operating system's title bars on JOptionPane, "
                        + "JFileChooser, JColorChooser and plain JDialogs. A TesselDialog always uses the Tessel chrome.", TextStyle.CAPTION)));
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private static JButton button(String text, ButtonStyle style, Runnable action) {
        JButton b = Tessel.style(new JButton(text), style);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void showRename() {
        TesselDialog dialog = new TesselDialog(window(), "Rename file");
        JTextField name = Tessel.placeholder(new JTextField("Quarterly report.docx", 26), "File name");
        NumberBox version = new NumberBox(1, 1, 99);
        JCheckBox keep = new JCheckBox("Keep the old name as a shortcut", true);

        JPanel form = new JPanel(new BorderLayout(0, 14));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        form.add(Layouts.stack(12, Layouts.field("New name", name), Layouts.field("Version", version), keep), BorderLayout.CENTER);

        JButton ok = Tessel.style(new JButton("Rename"), ButtonStyle.ACCENT);
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            result.setText("Result: renamed to “" + name.getText() + "” (v" + version.getIntValue() + ")");
            dialog.close();
        });
        cancel.addActionListener(e -> dialog.close());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(ok);
        buttons.add(cancel);
        form.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(form);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.showDialog();
    }

    private void togglePalette() {
        if (palette != null && palette.isShowing()) {
            palette.close();
            return;
        }
        palette = new TesselDialog(window(), "Palette", java.awt.Dialog.ModalityType.MODELESS);
        palette.setResizable(true);
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        body.add(new TextBlock("A modeless dialog: the window behind it stays usable. Resize it from the edges.", TextStyle.SECONDARY));
        palette.setContentPane(body);
        palette.setSize(320, 180);
        palette.showDialog();
    }
}
