package net.tessel.gallery;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Card;
import net.tessel.laf.components.TextBlock;

/** A scrollable gallery page with a title, a description and sections. */
public class Page extends JPanel implements Scrollable {

    public Page(String title, String description) {
        super(new Layouts.StackLayout(16, 1040));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 36, 36));

        JPanel header = Layouts.stack(4, Tessel.label(title, TextStyle.TITLE));
        if (description != null) header.add(new TextBlock(description, TextStyle.SECONDARY));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(header);
    }

    /** Adds a card with a header around {@code content}. */
    public Card section(String header, Component content) {
        Card card = new Card(header, content);
        add(card);
        return card;
    }

    /** A subtitle between sections. */
    public void subtitle(String text) {
        JComponent label = Tessel.label(text, TextStyle.SUBTITLE);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(label);
    }

    /** Wraps the page in a borderless, transparent scroll pane. */
    public JScrollPane scroll() {
        JScrollPane sp = new JScrollPane(this);
        sp.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return sp;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height - 48 : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
