package net.tessel.laf.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.TesselIcons;

/** Tabs with an accent underline for the selected tab and a subtle hover highlight. */
public class TesselTabbedPaneUI extends BasicTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(tabPane, "opaque", Boolean.FALSE);
    }

    @Override
    protected void setRolloverTab(int index) {
        int old = getRolloverTab();
        super.setRolloverTab(index);
        if (old != index) {
            repaintTab(old);
            repaintTab(index);
        }
    }

    private void repaintTab(int index) {
        if (index >= 0 && index < tabPane.getTabCount()) {
            Rectangle r = getTabBounds(tabPane, index);
            if (r != null) tabPane.repaint(r);
        }
    }

    @Override
    protected void paintTabBackground(Graphics g, int placement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        if (tabIndex != getRolloverTab() || !tabPane.isEnabledAt(tabIndex)) return;
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(Tessel.palette().controlHover);
            UIUtils.fillRound(g2, x + 2, y + 4, w - 4, h - 10, 4);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int placement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        if (!isSelected) return;
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(Tessel.palette().accent);
            switch (placement) {
                case BOTTOM:
                    UIUtils.fillRound(g2, x + 12, y, w - 24, 3, 1.5);
                    break;
                case LEFT:
                    UIUtils.fillRound(g2, x + w - 3, y + 8, 3, h - 16, 1.5);
                    break;
                case RIGHT:
                    UIUtils.fillRound(g2, x, y + 8, 3, h - 16, 1.5);
                    break;
                default:
                    UIUtils.fillRound(g2, x + 12, y + h - 3, w - 24, 3, 1.5);
                    break;
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        if (!isSelected || !UIUtils.isFocusVisible(tabPane)) return;
        Rectangle r = rects[tabIndex];
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.paintFocusRing(g2, r.x + 2, r.y + 3, r.width - 4, r.height - 8, 5);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics, int tabIndex,
                             String title, Rectangle textRect, boolean isSelected) {
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            super.paintText(g, placement, font, metrics, tabIndex, title, textRect, isSelected);
            return;
        }
        Palette p = Tessel.palette();
        boolean enabled = tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex);
        java.awt.Color custom = tabPane.getForegroundAt(tabIndex);
        java.awt.Color color = !enabled ? p.textDisabled
                : custom != null && !(custom instanceof UIResource) ? custom
                : isSelected || tabIndex == getRolloverTab() ? p.text : p.textSecondary;
        g.setFont(font);
        g.setColor(color);
        BasicGraphicsUtils.drawStringUnderlineCharAt(tabPane, (Graphics2D) g, title,
                tabPane.getDisplayedMnemonicIndexAt(tabIndex), textRect.x, textRect.y + metrics.getAscent());
    }

    @Override
    protected int getTabLabelShiftY(int placement, int tabIndex, boolean isSelected) {
        return 0;
    }

    @Override
    protected int getTabLabelShiftX(int placement, int tabIndex, boolean isSelected) {
        return 0;
    }

    @Override
    protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets in = tabPane.getInsets();
        g.setColor(Tessel.palette().border);
        switch (placement) {
            case LEFT: {
                int x = in.left + calculateTabAreaWidth(placement, runCount, maxTabWidth);
                g.fillRect(x, in.top, 1, height - in.top - in.bottom);
                break;
            }
            case RIGHT: {
                int x = width - in.right - calculateTabAreaWidth(placement, runCount, maxTabWidth) - 1;
                g.fillRect(x, in.top, 1, height - in.top - in.bottom);
                break;
            }
            case BOTTOM: {
                int y = height - in.bottom - calculateTabAreaHeight(placement, runCount, maxTabHeight) - 1;
                g.fillRect(in.left, y, width - in.left - in.right, 1);
                break;
            }
            default: {
                int y = in.top + calculateTabAreaHeight(placement, runCount, maxTabHeight);
                g.fillRect(in.left, y, width - in.left - in.right, 1);
                break;
            }
        }
    }

    @Override
    protected JButton createScrollButton(int direction) {
        TesselIcons.Direction d = direction == SwingConstants.WEST ? TesselIcons.Direction.LEFT
                : direction == SwingConstants.EAST ? TesselIcons.Direction.RIGHT
                : direction == SwingConstants.NORTH ? TesselIcons.Direction.UP : TesselIcons.Direction.DOWN;
        JButton b = new ScrollButton(direction);
        b.setIcon(TesselIcons.chevron(d, 16));
        Tessel.style(b, net.tessel.laf.ButtonStyle.ICON);
        return b;
    }

    /** Scroll buttons must implement UIResource and SwingConstants for BasicTabbedPaneUI. */
    private static final class ScrollButton extends JButton implements UIResource {
        ScrollButton(int direction) {
            setFocusable(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(28, 28);
        }
    }
}
