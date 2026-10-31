package net.tessel.laf.components;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import net.tessel.laf.RichText;
import net.tessel.laf.Tessel;

/**
 * A rich text editor: a {@link RichTextToolBar} above a {@code JTextPane} in a scroll pane. Everything the
 * look and feel gives text panes is there: placeholder, context menu, undo and redo, Ctrl+B / Ctrl+I / Ctrl+U,
 * and Ctrl+click on a link in HTML content.
 *
 * <pre>{@code
 * RichTextEditor editor = new RichTextEditor();
 * editor.setPlaceholder("Write something...");
 * editor.getTextPane().setText("Hello");
 *
 * RichTextEditor notes = new RichTextEditor(RichTextEditor.Format.HTML);
 * notes.setText("<p>Hello <b>world</b></p>");
 * String html = notes.getText();
 * }</pre>
 *
 * The {@linkplain Format format} decides what {@link #getText()} and {@link #write(OutputStream)} produce.
 */
public class RichTextEditor extends JPanel {

    /** How the content is stored. */
    public enum Format {
        /** Formatted text kept in the document; {@code getText()} returns the plain text. */
        STYLED("text/plain"),
        /** HTML: {@code setText} and {@code getText} work with HTML source. */
        HTML("text/html"),
        /** Rich Text Format. Load and save with {@link #read(InputStream)} and {@link #write(OutputStream)}. */
        RTF("text/rtf");

        private final String contentType;

        Format(String contentType) {
            this.contentType = contentType;
        }
    }

    private final JTextPane textPane = new JTextPane();
    private final JScrollPane scroll = new JScrollPane(textPane);
    private final RichTextToolBar toolBar = new RichTextToolBar(textPane);
    private Format format = Format.STYLED;

    public RichTextEditor() {
        this(Format.STYLED);
    }

    public RichTextEditor(Format format) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        add(toolBar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        RichText.enableLinks(textPane);
        setFormat(format);
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public RichTextToolBar getToolBar() {
        return toolBar;
    }

    public JScrollPane getScrollPane() {
        return scroll;
    }

    public Format getFormat() {
        return format;
    }

    /** Switches the content type. The current content is discarded. */
    public void setFormat(Format format) {
        this.format = format;
        textPane.setContentType(format.contentType);
        RichText.discardUndo(textPane);
        toolBar.refresh();
    }

    /** The content as text: plain text for {@link Format#STYLED}, HTML source for {@link Format#HTML}. */
    public String getText() {
        return textPane.getText();
    }

    /** Replaces the content and clears the undo history. */
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
        RichText.discardUndo(textPane);
        toolBar.refresh();
    }

    /** Replaces the content with what the stream holds, in the current format (use this for RTF). */
    public void read(InputStream in) throws IOException {
        EditorKit kit = textPane.getEditorKit();
        textPane.setDocument(kit.createDefaultDocument());
        try {
            kit.read(in, textPane.getDocument(), 0);
        } catch (BadLocationException e) {
            throw new IOException(e);
        }
        textPane.setCaretPosition(0);
        RichText.discardUndo(textPane);
        toolBar.refresh();
    }

    /** Writes the content to the stream, in the current format (use this for RTF). */
    public void write(OutputStream out) throws IOException {
        try {
            textPane.getEditorKit().write(out, textPane.getDocument(), 0, textPane.getDocument().getLength());
        } catch (BadLocationException e) {
            throw new IOException(e);
        }
    }

    public boolean isEditable() {
        return textPane.isEditable();
    }

    /** A read-only editor disables the tool bar. */
    public void setEditable(boolean editable) {
        textPane.setEditable(editable);
        toolBar.refresh();
    }

    public boolean isToolBarVisible() {
        return toolBar.isVisible();
    }

    public void setToolBarVisible(boolean visible) {
        toolBar.setVisible(visible);
    }

    /** The hint shown while the editor is empty. */
    public void setPlaceholder(String text) {
        Tessel.placeholder(textPane, text);
    }

    @Override
    public boolean requestFocusInWindow() {
        return textPane.requestFocusInWindow();
    }
}
