package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.icons.TesselIcons;

/** Title bar of an internal frame: frame icon, title, and flat minimize / maximize / close buttons. */
public class TesselInternalFrameTitlePane extends BasicInternalFrameTitlePane {

    static final int HEIGHT = 36;
    private static final int BUTTON_W = 36;
    private static final int BUTTON_H = 26;
    private static final int ICON_SIZE = 16;

    public TesselInternalFrameTitlePane(JInternalFrame f) {
        super(f);
        setOpaque(false);
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        maxIcon = TesselIcons.of(TesselIcons.Kind.MAXIMIZE, 16);
        minIcon = TesselIcons.of(TesselIcons.Kind.RESTORE, 16);
        iconIcon = TesselIcons.of(TesselIcons.Kind.MINIMIZE, 16);
        closeIcon = TesselIcons.of(TesselIcons.Kind.CLOSE, 16);
        setFont(Fonts.regular(13));
    }

    @Override
    protected void createButtons() {
        iconButton = new CaptionButton(false);
        iconButton.addActionListener(iconifyAction);
        maxButton = new CaptionButton(false);
        maxButton.addActionListener(maximizeAction);
        closeButton = new CaptionButton(true);
        closeButton.addActionListener(closeAction);
        iconButton.getAccessibleContext().setAccessibleName(TesselStrings.get("minimize"));
        maxButton.getAccessibleContext().setAccessibleName(TesselStrings.get("maximize"));
        closeButton.getAccessibleContext().setAccessibleName(TesselStrings.get("close"));
        closeButton.setToolTipText(TesselStrings.get("close"));
        setButtonIcons();
    }

    @Override
    protected void setButtonIcons() {
        super.setButtonIcons();
        maxButton.setToolTipText(TesselStrings.get(frame.isMaximum() || frame.isIcon() ? "restore" : "maximize"));
        iconButton.setToolTipText(TesselStrings.get(frame.isIcon() ? "restore" : "minimize"));
        repaint();
    }

    @Override
    protected JMenuBar createSystemMenuBar() {
        menuBar = new FrameIconBar();
        menuBar.setBorderPainted(false);
        return menuBar;
    }

    @Override
    protected JMenu createSystemMenu() {
        JMenu menu = new JMenu("");
        menu.setOpaque(false);
        menu.setBorder(BorderFactory.createEmptyBorder());
        return menu;
    }

    @Override
    protected LayoutManager createLayout() {
        return new TitleLayout();
    }

    @Override
    protected void paintTitleBackground(Graphics g) {
        if (frame.isIcon()) return;
        Palette p = Tessel.palette();
        g.setColor(p.surface);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(p.border);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = UIUtils.create(g);
        try {
            paintTitleBackground(g2);
            String title = frame.getTitle();
            if (title == null || title.isEmpty()) return;
            Palette p = Tessel.palette();
            g2.setFont(getFont());
            g2.setColor(frame.isSelected() ? p.text : p.textTertiary);
            FontMetrics fm = g2.getFontMetrics();
            int x = menuBar.getX() + menuBar.getWidth() + 8;
            int right = getWidth() - 8;
            for (JButton b : new JButton[]{iconButton, maxButton, closeButton}) {
                if (b.getParent() == this && b.isVisible()) right = Math.min(right, b.getX() - 4);
            }
            String clipped = getTitle(title, fm, right - x);
            int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(clipped, x, baseline);
        } finally {
            g2.dispose();
        }
    }

    /** The frame icon at the start of the title bar. */
    private final class FrameIconBar extends JMenuBar implements UIResource {
        FrameIconBar() {
            setOpaque(false);
        }

        @Override
        public boolean isFocusable() {
            return false;
        }

        @Override
        public void requestFocus() {
        }

        @Override
        public void paint(Graphics g) {
            Icon icon = frame.getFrameIcon();
            if (icon == null) icon = javax.swing.UIManager.getIcon("InternalFrame.icon");
            if (icon == null) return;
            Graphics2D g2 = UIUtils.create(g);
            try {
                double s = Math.max(icon.getIconWidth(), icon.getIconHeight());
                if (s > ICON_SIZE) g2.scale(ICON_SIZE / s, ICON_SIZE / s);
                icon.paintIcon(this, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Flat caption button; the close button turns red on hover. */
    private static final class CaptionButton extends JButton implements UIResource {
        private final boolean close;

        CaptionButton(boolean close) {
            this.close = close;
            setFocusable(false);
            setRequestFocusEnabled(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setRolloverEnabled(true);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(BUTTON_W, BUTTON_H);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                boolean pressed = getModel().isArmed() && getModel().isPressed();
                boolean hover = getModel().isRollover() || pressed;
                Color fg = p.text;
                if (hover) {
                    if (close) {
                        g2.setColor(pressed ? p.dangerHover : p.danger);
                        fg = Color.WHITE;
                    } else {
                        g2.setColor(pressed ? p.controlPressed : p.controlHover);
                    }
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 4);
                }
                Icon icon = getIcon();
                if (icon instanceof TesselIcons.ShapeIcon) {
                    ((TesselIcons.ShapeIcon) icon).paintIcon(this, g2, (getWidth() - icon.getIconWidth()) / 2,
                            (getHeight() - icon.getIconHeight()) / 2, fg);
                } else if (icon != null) {
                    icon.paintIcon(this, g2, (getWidth() - icon.getIconWidth()) / 2, (getHeight() - icon.getIconHeight()) / 2);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Icon on the left, buttons on the right. */
    private final class TitleLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, java.awt.Component comp) {
        }

        @Override
        public void removeLayoutComponent(java.awt.Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container c) {
            int width = 10 + ICON_SIZE + 8 + 12;
            if (frame.isClosable()) width += BUTTON_W + 2;
            if (frame.isMaximizable()) width += BUTTON_W + 2;
            if (frame.isIconifiable()) width += BUTTON_W + 2;
            FontMetrics fm = frame.getFontMetrics(getFont());
            String title = frame.getTitle();
            int text = title == null ? 0 : fm.stringWidth(title);
            // Leave room for a few characters at least; the title is clipped when the frame is narrow.
            int wanted = frame.isIcon() ? Math.min(text, 160) : Math.min(text, 40);
            return new Dimension(width + wanted, HEIGHT);
        }

        @Override
        public Dimension minimumLayoutSize(Container c) {
            return preferredLayoutSize(c);
        }

        @Override
        public void layoutContainer(Container c) {
            int w = getWidth();
            int h = getHeight();
            menuBar.setBounds(10, (h - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
            int x = w - 6 - BUTTON_W;
            int y = (h - BUTTON_H) / 2;
            if (frame.isClosable()) {
                closeButton.setBounds(x, y, BUTTON_W, BUTTON_H);
                x -= BUTTON_W + 2;
            }
            if (frame.isMaximizable()) {
                maxButton.setBounds(x, y, BUTTON_W, BUTTON_H);
                x -= BUTTON_W + 2;
            }
            if (frame.isIconifiable()) {
                iconButton.setBounds(x, y, BUTTON_W, BUTTON_H);
            }
        }
    }
}
