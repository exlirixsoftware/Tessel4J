package net.tessel.gallery.pages;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.RichText;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.RichTextEditor;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.TextBlock;
import net.tessel.laf.components.ToggleSwitch;

public class RichTextPage extends Page {

    private static final String SAMPLE_HTML = "<h1>Release notes</h1>"
            + "<p>Tessel4J themes <b>JTextPane</b> and <b>JEditorPane</b> too: this HTML follows the light and dark "
            + "theme and the accent color, with <a href=\"https://github.com\">links</a>, <code>inline code</code> and "
            + "<i>emphasis</i>.</p>"
            + "<h2>Highlights</h2><ul><li>Placeholder text</li><li>Undo and redo</li><li>Context menu</li></ul>"
            + "<blockquote>Switch the theme on the Settings page and this page changes with it.</blockquote>"
            + "<pre>JTextPane pane = new JTextPane();\nRichText.toggleBold(pane);</pre>"
            + "<table border=\"1\" cellpadding=\"4\"><tr><th>Shortcut</th><th>Action</th></tr>"
            + "<tr><td>Ctrl+B</td><td>Bold</td></tr><tr><td>Ctrl+I</td><td>Italic</td></tr>"
            + "<tr><td>Ctrl+U</td><td>Underline</td></tr></table>"
            + "<p>Links open through a handler of your choice: <a href=\"https://example.com/docs\">example.com/docs</a>.</p>";

    public RichTextPage() {
        super("Rich Text", "JTextPane and JEditorPane: a formatting editor, HTML with clickable links, and live HTML source.");

        // Rich text editor
        RichTextEditor editor = new RichTextEditor();
        editor.setPlaceholder("Start typing, then use the tool bar or Ctrl+B / Ctrl+I / Ctrl+U...");
        fill(editor.getTextPane());
        editor.setPreferredSize(new Dimension(600, 320));

        ToggleSwitch editable = new ToggleSwitch("Editable", true);
        editable.addItemListener(e -> editor.setEditable(editable.isSelected()));
        ToggleSwitch toolBar = new ToggleSwitch("Tool bar", true);
        toolBar.addItemListener(e -> editor.setToolBarVisible(toolBar.isSelected()));
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> editor.setText(""));
        section("Rich text editor", Layouts.stack(12, editor, Layouts.row(editable, toolBar, clear)));

        // HTML viewer
        JEditorPane viewer = new JEditorPane("text/html", SAMPLE_HTML);
        viewer.setEditable(false);
        JLabel last = Tessel.label("Click a link.", TextStyle.SECONDARY);
        RichText.enableLinks(viewer, uri -> {
            last.setText("Last link: " + uri);
            Snackbar.show(this, "Link clicked", uri.toString(), Severity.INFORMATIONAL);
        });
        JScrollPane viewerScroll = new JScrollPane(viewer);
        viewerScroll.setPreferredSize(new Dimension(600, 320));
        section("HTML viewer", Layouts.stack(8, viewerScroll, last,
                new TextBlock("Right-click for the context menu; over a link it offers Open link and Copy link address.",
                        TextStyle.CAPTION)));

        // Live HTML source
        JEditorPane source = Tessel.placeholder(new JEditorPane("text/plain", ""), "Write some HTML...");
        source.setText("<h2>Live preview</h2>\n<p>Edit this <b>HTML</b> and the preview updates as you type.</p>\n"
                + "<ul>\n  <li>Undo and redo with Ctrl+Z / Ctrl+Y</li>\n  <li>Copy and paste from the context menu</li>\n</ul>");
        RichText.discardUndo(source);
        JEditorPane preview = new JEditorPane("text/html", source.getText());
        preview.setEditable(false);
        source.getDocument().addDocumentListener(new DocumentListener() {
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

            private void update() {
                preview.setText(source.getText());
                preview.setCaretPosition(0);
            }
        });
        JPanel both = Layouts.panel(new GridLayout(1, 2, 16, 0));
        both.add(Layouts.field("HTML source", new JScrollPane(source)));
        both.add(Layouts.field("Preview", new JScrollPane(preview)));
        both.setPreferredSize(new Dimension(600, 240));
        section("Live preview", both);

        // States
        JTextPane empty = Tessel.placeholder(new JTextPane(), "A text pane with a placeholder");
        JTextPane readOnly = new JTextPane();
        readOnly.setText("This text pane is read-only. You can still select and copy.");
        readOnly.setEditable(false);
        JTextPane disabled = new JTextPane();
        disabled.setText("This text pane is disabled.");
        disabled.setEnabled(false);
        section("States", Layouts.columns(16,
                Layouts.field("Placeholder", small(empty)),
                Layouts.field("Read-only", small(readOnly)),
                Layouts.field("Disabled", small(disabled))));
    }

    private static JScrollPane small(JEditorPane pane) {
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(180, 110));
        return scroll;
    }

    /** Some formatted text so the editor does not start empty. */
    private static void fill(JTextPane pane) {
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        SimpleAttributeSet title = new SimpleAttributeSet();
        StyleConstants.setFontSize(title, 24);
        StyleConstants.setBold(title, true);
        SimpleAttributeSet bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        SimpleAttributeSet italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        SimpleAttributeSet color = new SimpleAttributeSet();
        StyleConstants.setForeground(color, new Color(0xDC2626));
        SimpleAttributeSet highlight = new SimpleAttributeSet();
        StyleConstants.setBackground(highlight, new Color(0xFEF08A));
        StyleConstants.setForeground(highlight, new Color(0x1B1F24));
        try {
            doc.insertString(doc.getLength(), "Meeting notes\n", title);
            doc.insertString(doc.getLength(), "Select some text and try ", normal);
            doc.insertString(doc.getLength(), "bold", bold);
            doc.insertString(doc.getLength(), ", ", normal);
            doc.insertString(doc.getLength(), "italic", italic);
            doc.insertString(doc.getLength(), ", ", normal);
            doc.insertString(doc.getLength(), "colors", color);
            doc.insertString(doc.getLength(), " and ", normal);
            doc.insertString(doc.getLength(), "highlights", highlight);
            doc.insertString(doc.getLength(), ".\nAlignment applies to whole paragraphs.", normal);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, 1, center, false);
        pane.setCaretPosition(0);
        RichText.discardUndo(pane);
    }
}
