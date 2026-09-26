package net.tessel.laf.components;

import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import net.tessel.laf.Fonts;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;

/**
 * Read-only, word-wrapping text with a {@link TextStyle}. Unlike a {@code JLabel} it wraps to the
 * available width without HTML. Font and color follow theme changes.
 */
public class TextBlock extends JTextArea {

    private TextStyle textStyle;

    public TextBlock(String text) {
        this(text, TextStyle.BODY);
    }

    public TextBlock(String text, TextStyle style) {
        super(text);
        this.textStyle = style;
        setEditable(false);
        setFocusable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setHighlighter(null);
        applyStyle();
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(TextStyle style) {
        this.textStyle = style;
        applyStyle();
        revalidate();
        repaint();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (textStyle != null) applyStyle();
    }

    private void applyStyle() {
        if (getFont() == null || getFont() instanceof UIResource) setFont(new FontUIResource(Fonts.forStyle(textStyle)));
        if (getForeground() == null || getForeground() instanceof UIResource) {
            setForeground(new ColorUIResource(textStyle.isSecondary() ? Tessel.palette().textSecondary : Tessel.palette().text));
        }
        setDisabledTextColor(Tessel.palette().textDisabled);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean widthChanged = width != getWidth();
        super.setBounds(x, y, width, height);
        // The preferred height depends on the width: re-layout once the real width is known.
        if (widthChanged && getLineWrap()) SwingUtilities.invokeLater(this::revalidate);
    }

    @Override
    public void scrollRectToVisible(Rectangle r) {
        // Never scroll enclosing panes because of the (invisible) caret.
    }
}
