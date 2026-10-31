package net.tessel.laf.components;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import net.tessel.laf.ui.TesselRootPaneUI;

/**
 * A dialog window with the Tessel title bar, outline and (on Windows) native shadow, rounded corners,
 * dragging and resizing - whatever the application's {@link net.tessel.laf.Tessel#setDecoratedDialogs(boolean)}
 * setting is.
 *
 * <pre>{@code
 * TesselDialog dialog = new TesselDialog(frame, "Rename file");
 * dialog.setContentPane(form);
 * dialog.showDialog();                       // packs, centers on the owner and shows (blocks when modal)
 * }</pre>
 *
 * Esc closes the dialog (the same way the close button does) unless {@link #setCloseOnEscape(boolean)} says
 * otherwise. The default close operation is {@code DISPOSE_ON_CLOSE}; the title bar's close button calls
 * the window listeners first, so a {@code windowClosing} handler can veto it by setting
 * {@code DO_NOTHING_ON_CLOSE}.
 */
public class TesselDialog extends JDialog {

    private static final String CLOSE_KEY = "Tessel.closeDialog";

    private boolean closeOnEscape = true;

    public TesselDialog() {
        this((Window) null, "");
    }

    public TesselDialog(Frame owner, String title) {
        this((Window) owner, title);
    }

    public TesselDialog(Window owner, String title) {
        this(owner, title, ModalityType.APPLICATION_MODAL);
    }

    public TesselDialog(Window owner, String title, Dialog.ModalityType modality) {
        super(owner, title, modality);
        if (!isUndecorated()) {
            // The application turned look-and-feel decorations off: this dialog still wants them.
            setUndecorated(true);
            getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        installEscape();
    }

    private void installEscape() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), CLOSE_KEY);
        root.getActionMap().put(CLOSE_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (closeOnEscape) TesselRootPaneUI.requestClose(TesselDialog.this);
            }
        });
    }

    public boolean isCloseOnEscape() {
        return closeOnEscape;
    }

    public void setCloseOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
    }

    /** Closes the dialog like its close button does (window listeners first, then the close operation). */
    public void close() {
        TesselRootPaneUI.requestClose(this);
    }

    /**
     * Packs the dialog (unless a size was set), centers it on its owner and shows it. A modal dialog blocks
     * here until it is closed.
     */
    public void showDialog() {
        if (!isDisplayable() && getWidth() <= 1) pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }
}
