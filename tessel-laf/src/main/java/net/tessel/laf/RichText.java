package net.tessel.laf;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.undo.UndoManager;

/**
 * Helpers for {@code JTextPane} and {@code JEditorPane}: undo and redo, hyperlinks, and character and paragraph
 * formatting. The look and feel already themes both controls (colors, HTML style sheet, context menu, Ctrl+Z / Ctrl+Y,
 * Ctrl+B / Ctrl+I / Ctrl+U); this class is for application code that wants to drive them.
 *
 * <pre>{@code
 * JTextPane pane = new JTextPane();
 * RichText.toggleBold(pane);                           // like pressing Ctrl+B
 * RichText.setTextColor(pane, Color.RED);              // the selection, or the text typed next
 *
 * JEditorPane help = new JEditorPane("text/html", "<a href='https://example.com'>Example</a>");
 * help.setEditable(false);
 * RichText.enableLinks(help);                          // hand cursor, opens links in the browser
 * }</pre>
 *
 * Formatting works on panes with a {@link StyledEditorKit}: every {@code JTextPane}, and an {@code JEditorPane}
 * showing HTML or RTF.
 */
public final class RichText {

    /** Client property: the {@link UndoManager} the look and feel keeps for an editable text pane. */
    public static final String UNDO_MANAGER = "Tessel.undoManager";
    /** Client property: a {@code Consumer<URI>} that opens links, set by {@link #enableLinks(JEditorPane, Consumer)}. */
    public static final String LINK_HANDLER = "Tessel.linkHandler";

    private static final String LINKS = "Tessel.linkSupport";

    private RichText() {
    }

    // ------------------------------------------------------------------ undo

    /** The undo manager of a text pane, or null when the component has none (not a Tessel text pane). */
    public static UndoManager undoManager(JTextComponent c) {
        Object m = c.getClientProperty(UNDO_MANAGER);
        return m instanceof UndoManager ? (UndoManager) m : null;
    }

    public static boolean canUndo(JTextComponent c) {
        UndoManager m = undoManager(c);
        return m != null && m.canUndo();
    }

    public static boolean canRedo(JTextComponent c) {
        UndoManager m = undoManager(c);
        return m != null && m.canRedo();
    }

    public static void undo(JTextComponent c) {
        UndoManager m = undoManager(c);
        if (m != null && m.canUndo()) m.undo();
    }

    public static void redo(JTextComponent c) {
        UndoManager m = undoManager(c);
        if (m != null && m.canRedo()) m.redo();
    }

    /** Forgets the undo history; call it after loading new content with {@code setText} or {@code setPage}. */
    public static void discardUndo(JTextComponent c) {
        UndoManager m = undoManager(c);
        if (m != null) m.discardAllEdits();
    }

    // ------------------------------------------------------------------ hyperlinks

    /** Makes the links of a read-only HTML pane clickable (hand cursor, tool tip, opens in the system browser). */
    public static void enableLinks(JEditorPane pane) {
        enableLinks(pane, null);
    }

    /**
     * Makes the links of an HTML pane clickable. A read-only pane reacts to a click, an editable one to Ctrl+click.
     *
     * @param handler receives the link, or null to open it with the system browser ({@code #anchors} scroll the pane)
     */
    public static void enableLinks(JEditorPane pane, Consumer<URI> handler) {
        disableLinks(pane);
        LinkSupport support = new LinkSupport(pane);
        pane.putClientProperty(LINK_HANDLER, handler);
        pane.putClientProperty(LINKS, support);
        pane.addHyperlinkListener(support);
        pane.addMouseListener(support);
    }

    public static void disableLinks(JEditorPane pane) {
        Object old = pane.getClientProperty(LINKS);
        if (old instanceof LinkSupport) {
            pane.removeHyperlinkListener((LinkSupport) old);
            pane.removeMouseListener((LinkSupport) old);
            ((LinkSupport) old).restoreCursor();
        }
        pane.putClientProperty(LINKS, null);
        pane.putClientProperty(LINK_HANDLER, null);
    }

    /** The target of the link at {@code p} (component coordinates) in an HTML pane, or null. */
    public static String linkAt(JEditorPane pane, Point p) {
        Document doc = pane.getDocument();
        if (!(doc instanceof HTMLDocument)) return null;
        // Same hit test as the link handling of HTMLEditorKit.
        javax.swing.text.Position.Bias[] bias = new javax.swing.text.Position.Bias[1];
        int pos = pane.getUI().viewToModel2D(pane, new java.awt.geom.Point2D.Float(p.x, p.y), bias);
        if (bias[0] == javax.swing.text.Position.Bias.Backward && pos > 0) pos--;
        if (pos < 0 || pos >= doc.getLength()) return null;
        Element e = ((HTMLDocument) doc).getCharacterElement(pos);
        Object anchor = e.getAttributes().getAttribute(HTML.Tag.A);
        if (anchor instanceof AttributeSet) {
            Object href = ((AttributeSet) anchor).getAttribute(HTML.Attribute.HREF);
            return href == null ? null : href.toString();
        }
        return null;
    }

    /** Opens {@code href} like a click on a link would: with the registered handler, else the system browser. */
    public static void openLink(JEditorPane pane, String href) {
        if (href == null || href.isEmpty()) return;
        URI uri = resolveLink(pane, href);
        if (uri == null) return;
        Object handler = pane.getClientProperty(LINK_HANDLER);
        if (handler instanceof Consumer) {
            @SuppressWarnings("unchecked")
            Consumer<URI> consumer = (Consumer<URI>) handler;
            consumer.accept(uri);
        } else if (href.startsWith("#")) {
            pane.scrollToReference(href.substring(1));
        } else {
            browse(uri);
        }
    }

    /** Resolves a link against the base of the pane's HTML document; null when it is not a valid address. */
    public static URI resolveLink(JEditorPane pane, String href) {
        try {
            Document doc = pane.getDocument();
            URL base = doc instanceof HTMLDocument ? ((HTMLDocument) doc).getBase() : null;
            if (base != null && !href.startsWith("#")) return new URL(base, href).toURI();
            return new URI(href.replace(" ", "%20"));
        } catch (Exception e) {
            return null;
        }
    }

    private static void browse(URI uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception e) {
            // nothing sensible to do: no browser, or the link was refused
        }
    }

    private static final class LinkSupport extends MouseAdapter implements HyperlinkListener {

        private final JEditorPane pane;
        private Cursor saved;

        LinkSupport(JEditorPane pane) {
            this.pane = pane;
        }

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                if (saved == null) saved = pane.getCursor();
                pane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                pane.setToolTipText(e.getDescription());
            } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                restoreCursor();
                pane.setToolTipText(null);
            } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openLink(pane, e.getDescription());
            }
        }

        void restoreCursor() {
            if (saved != null) {
                pane.setCursor(saved);
                saved = null;
            }
        }

        // Editable panes don't fire hyperlink events, so Ctrl+click is handled here.
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!pane.isEditable() || !e.isControlDown() || e.getButton() != MouseEvent.BUTTON1) return;
            openLink(pane, linkAt(pane, e.getPoint()));
        }
    }

    // ------------------------------------------------------------------ formatting

    /** The character attributes at the caret, or of the first selected character. */
    public static AttributeSet characterAttributes(JEditorPane pane) {
        Document doc = pane.getDocument();
        if (!(doc instanceof StyledDocument)) return SimpleAttributeSet.EMPTY;
        int start = pane.getSelectionStart();
        if (start != pane.getSelectionEnd()) return ((StyledDocument) doc).getCharacterElement(start).getAttributes();
        if (pane.getEditorKit() instanceof StyledEditorKit) {
            return ((StyledEditorKit) pane.getEditorKit()).getInputAttributes();
        }
        return ((StyledDocument) doc).getCharacterElement(Math.max(0, start - 1)).getAttributes();
    }

    public static boolean isBold(JEditorPane pane) {
        return flag(characterAttributes(pane), StyleConstants.Bold, HTML.Tag.B, HTML.Tag.STRONG);
    }

    public static boolean isItalic(JEditorPane pane) {
        return flag(characterAttributes(pane), StyleConstants.Italic, HTML.Tag.I, HTML.Tag.EM, HTML.Tag.CITE);
    }

    public static boolean isUnderline(JEditorPane pane) {
        return flag(characterAttributes(pane), StyleConstants.Underline, HTML.Tag.U);
    }

    public static boolean isStrikeThrough(JEditorPane pane) {
        return flag(characterAttributes(pane), StyleConstants.StrikeThrough, HTML.Tag.STRIKE, HTML.Tag.S);
    }

    /** A style set explicitly wins; HTML content marks the same formats with tags such as {@code <b>}. */
    private static boolean flag(AttributeSet a, Object styleKey, HTML.Tag... tags) {
        Object explicit = a.getAttribute(styleKey);
        if (explicit instanceof Boolean) return (Boolean) explicit;
        for (HTML.Tag tag : tags) {
            if (a.isDefined(tag)) return true;
        }
        return false;
    }

    /** The text color (or, with {@code background}, the highlight) at the caret, or null when there is none. */
    public static Color characterColor(JEditorPane pane, boolean background) {
        AttributeSet a = characterAttributes(pane);
        Object key = background ? StyleConstants.Background : StyleConstants.Foreground;
        if (!a.isDefined(key)) return null;
        return background ? StyleConstants.getBackground(a) : StyleConstants.getForeground(a);
    }

    /** The font family at the caret, or null when the pane uses its own font there. */
    public static String fontFamily(JEditorPane pane) {
        AttributeSet a = characterAttributes(pane);
        return a.isDefined(StyleConstants.FontFamily) ? StyleConstants.getFontFamily(a) : null;
    }

    /** The font size at the caret, or 0 when the pane uses its own font there. */
    public static int fontSize(JEditorPane pane) {
        AttributeSet a = characterAttributes(pane);
        return a.isDefined(StyleConstants.FontSize) ? StyleConstants.getFontSize(a) : 0;
    }

    /** The paragraph alignment at the caret ({@link StyleConstants#ALIGN_LEFT}, ...). */
    public static int alignment(JEditorPane pane) {
        Document doc = pane.getDocument();
        if (!(doc instanceof StyledDocument)) return StyleConstants.ALIGN_LEFT;
        return StyleConstants.getAlignment(((StyledDocument) doc).getParagraphElement(pane.getSelectionStart()).getAttributes());
    }

    public static void toggleBold(JEditorPane pane) {
        apply(pane, StyleConstants.Bold, !isBold(pane));
    }

    public static void toggleItalic(JEditorPane pane) {
        apply(pane, StyleConstants.Italic, !isItalic(pane));
    }

    public static void toggleUnderline(JEditorPane pane) {
        apply(pane, StyleConstants.Underline, !isUnderline(pane));
    }

    public static void toggleStrikeThrough(JEditorPane pane) {
        apply(pane, StyleConstants.StrikeThrough, !isStrikeThrough(pane));
    }

    public static void setFontFamily(JEditorPane pane, String family) {
        if (family == null) removeCharacterAttribute(pane, StyleConstants.FontFamily);
        else apply(pane, StyleConstants.FontFamily, family);
    }

    /** @param size in points; 0 or less goes back to the font of the pane */
    public static void setFontSize(JEditorPane pane, int size) {
        if (size <= 0) removeCharacterAttribute(pane, StyleConstants.FontSize);
        else apply(pane, StyleConstants.FontSize, size);
    }

    /** Colors the selection (or the text typed next); null goes back to the pane's text color. */
    public static void setTextColor(JEditorPane pane, Color color) {
        if (color == null) removeCharacterAttribute(pane, StyleConstants.Foreground);
        else apply(pane, StyleConstants.Foreground, color);
    }

    /** Highlights the selection (or the text typed next); null removes the highlight. */
    public static void setHighlight(JEditorPane pane, Color color) {
        if (color == null) removeCharacterAttribute(pane, StyleConstants.Background);
        else apply(pane, StyleConstants.Background, color);
    }

    /** @param alignment {@link StyleConstants#ALIGN_LEFT}, {@code ALIGN_CENTER}, {@code ALIGN_RIGHT} or {@code ALIGN_JUSTIFIED} */
    public static void setAlignment(JEditorPane pane, int alignment) {
        if (!(pane.getDocument() instanceof StyledDocument)) return;
        StyledDocument doc = (StyledDocument) pane.getDocument();
        int p0 = pane.getSelectionStart();
        int p1 = pane.getSelectionEnd();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrs, alignment);
        doc.setParagraphAttributes(p0, Math.max(0, p1 - p0), attrs, false);
    }

    /** Removes every character format (bold, color, font, ...) from the selection and from the text typed next. */
    public static void clearFormatting(JEditorPane pane) {
        if (!(pane.getDocument() instanceof StyledDocument)) return;
        StyledDocument doc = (StyledDocument) pane.getDocument();
        int p0 = pane.getSelectionStart();
        int p1 = pane.getSelectionEnd();
        if (p0 != p1) doc.setCharacterAttributes(p0, p1 - p0, SimpleAttributeSet.EMPTY, true);
        MutableAttributeSet input = inputAttributes(pane);
        if (input != null) input.removeAttributes(input.copyAttributes());
    }

    private static void apply(JEditorPane pane, Object key, Object value) {
        if (!(pane.getDocument() instanceof StyledDocument)) return;
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(key, value);
        int p0 = pane.getSelectionStart();
        int p1 = pane.getSelectionEnd();
        if (p0 != p1) ((StyledDocument) pane.getDocument()).setCharacterAttributes(p0, p1 - p0, attrs, false);
        MutableAttributeSet input = inputAttributes(pane);
        if (input != null) input.addAttributes(attrs);
    }

    /** Removes one character attribute from the selection and from the attributes of the text typed next. */
    private static void removeCharacterAttribute(JEditorPane pane, Object key) {
        if (!(pane.getDocument() instanceof StyledDocument)) return;
        StyledDocument doc = (StyledDocument) pane.getDocument();
        int p0 = pane.getSelectionStart();
        int p1 = pane.getSelectionEnd();
        if (p0 != p1) {
            // Collect the runs first: changing attributes while walking the elements would invalidate them.
            List<int[]> runs = new ArrayList<>();
            List<AttributeSet> sets = new ArrayList<>();
            for (int pos = p0; pos < p1; ) {
                Element run = doc.getCharacterElement(pos);
                int end = Math.min(run.getEndOffset(), p1);
                if (run.getAttributes().isDefined(key)) {
                    runs.add(new int[]{pos, end});
                    sets.add(run.getAttributes().copyAttributes());
                }
                pos = Math.max(end, pos + 1);
            }
            for (int i = 0; i < runs.size(); i++) {
                MutableAttributeSet without = new SimpleAttributeSet(sets.get(i));
                without.removeAttribute(key);
                doc.setCharacterAttributes(runs.get(i)[0], runs.get(i)[1] - runs.get(i)[0], without, true);
            }
        }
        MutableAttributeSet input = inputAttributes(pane);
        if (input != null) input.removeAttribute(key);
    }

    private static MutableAttributeSet inputAttributes(JEditorPane pane) {
        return pane.getEditorKit() instanceof StyledEditorKit ? ((StyledEditorKit) pane.getEditorKit()).getInputAttributes() : null;
    }
}
