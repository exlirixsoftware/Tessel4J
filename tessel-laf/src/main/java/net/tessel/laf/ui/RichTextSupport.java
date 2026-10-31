package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.net.URI;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.RichText;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;

/**
 * What the Tessel {@code JEditorPane} and {@code JTextPane} delegates add on top of a plain text component:
 * a themed HTML style sheet, undo and redo, formatting shortcuts and a context menu.
 */
final class RichTextSupport {

    /** Implemented by the delegates so the shared code can rebuild the views after a style sheet change. */
    interface ViewHost {
        void rebuildViews();
    }

    private static final String THEME_SHEET = "Tessel.themeSheet";
    private static final String UNDO_SUPPORT = "Tessel.undoSupport";
    private static final String CONTEXT_MENU_ACTION = "tessel-context-menu";

    private static final MouseAdapter MENU_MOUSE = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            popup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            popup(e);
        }

        private void popup(MouseEvent e) {
            if (e.isPopupTrigger() && e.getComponent() instanceof JEditorPane) {
                JEditorPane pane = (JEditorPane) e.getComponent();
                if (menuEnabled(pane)) showMenu(pane, e.getPoint());
            }
        }
    };

    private RichTextSupport() {
    }

    // ------------------------------------------------------------------ lifecycle

    static void installDefaults(JEditorPane c) {
        applyTheme(c);
    }

    static void installListeners(JEditorPane c) {
        TextSupport.installListeners(c);
        c.removeMouseListener(MENU_MOUSE);
        c.addMouseListener(MENU_MOUSE);
        UndoSupport undo = (UndoSupport) c.getClientProperty(UNDO_SUPPORT);
        if (undo == null) {
            undo = new UndoSupport(c);
            c.putClientProperty(UNDO_SUPPORT, undo);
            c.putClientProperty(RichText.UNDO_MANAGER, undo.manager);
        }
        undo.attach(c.getDocument());
    }

    static void uninstallListeners(JEditorPane c) {
        Object undo = c.getClientProperty(UNDO_SUPPORT);
        if (undo instanceof UndoSupport) ((UndoSupport) undo).detach();
        c.removeMouseListener(MENU_MOUSE);
        TextSupport.uninstallListeners(c);
    }

    static void installKeyboardActions(JEditorPane c) {
        InputMap keys = SwingUtilities.getUIInputMap(c, JComponent.WHEN_FOCUSED);
        ActionMap actions = SwingUtilities.getUIActionMap(c);
        if (keys == null || actions == null) return;
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl), "tessel-undo");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ctrl), "tessel-redo");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ctrl | java.awt.event.InputEvent.SHIFT_DOWN_MASK), "tessel-redo");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, ctrl), "tessel-bold");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, ctrl), "tessel-italic");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, ctrl), "tessel-underline");
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), CONTEXT_MENU_ACTION);
        keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, java.awt.event.InputEvent.SHIFT_DOWN_MASK), CONTEXT_MENU_ACTION);
        actions.put("tessel-undo", new EditAction(RichText::undo));
        actions.put("tessel-redo", new EditAction(RichText::redo));
        actions.put("tessel-bold", new EditAction(RichText::toggleBold));
        actions.put("tessel-italic", new EditAction(RichText::toggleItalic));
        actions.put("tessel-underline", new EditAction(RichText::toggleUnderline));
        actions.put(CONTEXT_MENU_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JEditorPane pane = (JEditorPane) e.getSource();
                if (!menuEnabled(pane)) return;
                Point at = new Point(8, pane.getHeight() / 2);
                try {
                    Rectangle2D r = pane.modelToView2D(pane.getCaretPosition());
                    if (r != null) at = new Point((int) r.getX(), (int) r.getMaxY());
                } catch (BadLocationException ex) {
                    // keep the default position
                }
                showMenu(pane, at);
            }
        });
    }

    /** An action on an editable pane that does nothing otherwise (so the shortcut is still consumed). */
    private static final class EditAction extends AbstractAction {
        private final java.util.function.Consumer<JEditorPane> body;

        EditAction(java.util.function.Consumer<JEditorPane> body) {
            this.body = body;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JEditorPane) {
                JEditorPane pane = (JEditorPane) e.getSource();
                if (pane.isEditable() && pane.isEnabled()) body.accept(pane);
            }
        }
    }

    static void propertyChange(PropertyChangeEvent e, JEditorPane c, ViewHost host) {
        String name = e.getPropertyName();
        if ("document".equals(name)) {
            Object undo = c.getClientProperty(UNDO_SUPPORT);
            if (undo instanceof UndoSupport) ((UndoSupport) undo).attach(c.getDocument());
            applyTheme(c); // the views are rebuilt right after this callback
        } else if ("font".equals(name) || "foreground".equals(name)) {
            applyTheme(c);
            host.rebuildViews();
        }
        TextSupport.propertyChange(e, c);
    }

    // ------------------------------------------------------------------ theme

    /**
     * Applies the palette and the pane's font to an HTML document. The rules live in a style sheet linked to the
     * document's own, so they are not written out with the HTML and rules from the content's own {@code <style>}
     * still win. Other styled documents need nothing: {@code BasicTextPaneUI} keeps their default style in sync.
     */
    static void applyTheme(JEditorPane c) {
        Document doc = c.getDocument();
        if (!(doc instanceof HTMLDocument)) return;
        StyleSheet sheet = ((HTMLDocument) doc).getStyleSheet();
        Object old = doc.getProperty(THEME_SHEET);
        if (old instanceof StyleSheet) sheet.removeStyleSheet((StyleSheet) old);
        StyleSheet theme = new StyleSheet();
        theme.addRule(styleSheet(c));
        sheet.addStyleSheet(theme);
        doc.putProperty(THEME_SHEET, theme);
    }

    /** CSS for an HTML document that matches the theme: text and link colors, headings, code, quotes and tables. */
    static String styleSheet(JEditorPane c) {
        Palette p = Tessel.palette();
        Font font = c.getFont() != null ? c.getFont() : Fonts.regular(14);
        Color fg = c.getForeground() != null ? c.getForeground() : p.text;
        int size = font.getSize();
        String family = font.getFamily();
        Font mono = Fonts.mono(size);
        Font heading = Fonts.semibold(size);
        String headingWeight = heading.isBold() ? "bold" : "normal";
        StringBuilder css = new StringBuilder(1024);
        css.append("body { font-family: ").append(family).append("; font-size: ").append(size)
                .append("pt; color: ").append(hex(fg)).append("; margin: 0; }\n");
        css.append("p { margin-top: 0; margin-bottom: ").append(Math.round(size * 0.6f)).append("; }\n");
        css.append("li { margin-bottom: 2; }\n");
        css.append("a { color: ").append(hex(p.dark ? Palette.mix(p.accent, Color.WHITE, 0.3) : p.accent)).append("; text-decoration: none; }\n");
        double[] scale = {1.9, 1.55, 1.3, 1.15, 1.0, 0.9};
        for (int i = 0; i < scale.length; i++) {
            css.append('h').append(i + 1).append(" { font-family: ").append(heading.getFamily())
                    .append("; font-weight: ").append(headingWeight)
                    .append("; font-size: ").append(Math.round(size * scale[i]))
                    .append("pt; color: ").append(hex(fg))
                    .append("; margin-top: ").append(Math.round(size * (i < 2 ? 1.0 : 0.8)))
                    .append("; margin-bottom: ").append(Math.round(size * 0.4)).append("; }\n");
        }
        css.append("code, tt, kbd, samp { font-family: ").append(mono.getFamily()).append("; font-size: ")
                .append(Math.max(8, size - 1)).append("pt; background-color: ").append(hex(p.subtle)).append("; }\n");
        css.append("pre { font-family: ").append(mono.getFamily()).append("; font-size: ")
                .append(Math.max(8, size - 1)).append("pt; background-color: ").append(hex(p.subtle))
                .append("; padding: 8; margin-top: 4; margin-bottom: 8; }\n");
        css.append("blockquote { margin-left: 8; margin-right: 0; padding-left: 12; color: ").append(hex(p.textSecondary))
                .append("; border-left: 3px solid ").append(hex(p.accent)).append("; }\n");
        css.append("table { border-color: ").append(hex(p.borderStrong)).append("; }\n");
        css.append("th { background-color: ").append(hex(p.subtle)).append("; }\n");
        css.append("td, th { padding: 4; }\n");
        css.append("hr { color: ").append(hex(p.border)).append("; }\n");
        return css.toString();
    }

    private static String hex(Color c) {
        return String.format("#%06x", c.getRGB() & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ context menu

    private static boolean menuEnabled(JEditorPane c) {
        return c.isEnabled() && c.getComponentPopupMenu() == null
                && !Boolean.FALSE.equals(c.getClientProperty(Tessel.CONTEXT_MENU));
    }

    static void showMenu(JEditorPane c, Point at) {
        if (!c.hasFocus()) c.requestFocusInWindow();
        JPopupMenu menu = new JPopupMenu();
        boolean editable = c.isEditable();
        boolean selection = c.getSelectionStart() != c.getSelectionEnd();

        String href = RichText.linkAt(c, at);
        if (href != null) {
            menu.add(item("openLink", Symbol.LINK, null, true, () -> RichText.openLink(c, href)));
            menu.add(item("copyLink", Symbol.COPY, null, true, () -> copyText(c, linkText(c, href))));
            menu.addSeparator();
        }
        if (editable) {
            menu.add(item("undo", Symbol.UNDO, KeyEvent.VK_Z, RichText.canUndo(c), () -> RichText.undo(c)));
            menu.add(item("redo", Symbol.REDO, KeyEvent.VK_Y, RichText.canRedo(c), () -> RichText.redo(c)));
            menu.addSeparator();
            menu.add(item("cut", Symbol.CUT, KeyEvent.VK_X, selection, c::cut));
        }
        menu.add(item("copy", Symbol.COPY, KeyEvent.VK_C, selection, c::copy));
        if (editable) {
            menu.add(item("paste", Symbol.PASTE, KeyEvent.VK_V, true, c::paste));
            menu.add(item("delete", Symbol.DELETE, null, selection, () -> c.replaceSelection("")));
        }
        menu.addSeparator();
        menu.add(item("selectAll", null, KeyEvent.VK_A, c.getDocument().getLength() > 0, c::selectAll));
        menu.show(c, at.x, at.y);
    }

    private static String linkText(JEditorPane c, String href) {
        URI uri = RichText.resolveLink(c, href);
        return uri != null ? uri.toString() : href;
    }

    private static void copyText(JEditorPane c, String text) {
        c.getToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
    }

    private static JMenuItem item(String key, Symbol icon, Integer keyCode, boolean enabled, Runnable action) {
        JMenuItem item = new JMenuItem(TesselStrings.get(key));
        if (icon != null) item.setIcon(new FontIcon(icon, 16));
        if (keyCode != null) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }
        item.setEnabled(enabled);
        item.addActionListener(e -> action.run());
        return item;
    }

    // ------------------------------------------------------------------ undo

    private static final class UndoSupport implements UndoableEditListener {
        final JEditorPane pane;
        final UndoManager manager = new TypingUndoManager();
        private Document doc;
        private Document lastDoc;

        UndoSupport(JEditorPane pane) {
            this.pane = pane;
            manager.setLimit(200);
        }

        void attach(Document d) {
            if (doc != null) doc.removeUndoableEditListener(this);
            doc = d;
            if (d == null) return;
            // A new document starts with a clean history; reinstalling the UI (theme change) keeps it.
            if (d != lastDoc) {
                manager.discardAllEdits();
                lastDoc = d;
            }
            d.addUndoableEditListener(this);
        }

        void detach() {
            if (doc != null) doc.removeUndoableEditListener(this);
            doc = null;
        }

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            if (pane.isEditable()) manager.addEdit(e.getEdit());
        }
    }

    /** An undo manager that treats a run of typed (or deleted) characters as one step, like a text editor. */
    private static final class TypingUndoManager extends UndoManager {
        private static final long PAUSE = 1000;

        private CompoundEdit run;
        private int kind;
        private int lastOffset;
        private long lastTime;

        @Override
        public synchronized boolean addEdit(UndoableEdit e) {
            int k = kindOf(e);
            int offset = k == 0 ? 0 : ((DocumentEvent) e).getOffset();
            long now = System.currentTimeMillis();
            if (k != 0 && run != null && k == kind && now - lastTime < PAUSE && continues(k, offset)) {
                lastOffset = offset;
                lastTime = now;
                return super.addEdit(e); // the open compound edit at the end of the list absorbs it
            }
            endRun();
            if (k == 0) return super.addEdit(e);
            run = new CompoundEdit();
            run.addEdit(e);
            kind = k;
            lastOffset = offset;
            lastTime = now;
            return super.addEdit(run);
        }

        private boolean continues(int k, int offset) {
            if (k == 1) return offset == lastOffset + 1;
            return offset == lastOffset || offset == lastOffset - 1;
        }

        private static int kindOf(UndoableEdit e) {
            if (!(e instanceof AbstractDocument.DefaultDocumentEvent)) return 0;
            DocumentEvent de = (DocumentEvent) e;
            if (de.getLength() != 1) return 0;
            if (de.getType() == DocumentEvent.EventType.INSERT) return 1;
            if (de.getType() == DocumentEvent.EventType.REMOVE) return 2;
            return 0;
        }

        private void endRun() {
            if (run != null) {
                run.end();
                run = null;
            }
        }

        @Override
        public synchronized boolean canUndo() {
            endRun();
            return super.canUndo();
        }

        @Override
        public synchronized boolean canRedo() {
            endRun();
            return super.canRedo();
        }

        @Override
        public synchronized void undo() {
            endRun();
            super.undo();
        }

        @Override
        public synchronized void redo() {
            endRun();
            super.redo();
        }

        @Override
        public synchronized void discardAllEdits() {
            run = null;
            super.discardAllEdits();
        }
    }
}
