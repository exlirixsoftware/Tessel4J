package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.LookAndFeel;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.FontIcon;

/** Painting shared by {@link TesselButtonUI} and {@link TesselToggleButtonUI}. */
final class ButtonPainter {

    static final double RADIUS = 6;

    /** Resolved colors for the current state of a button. */
    static final class Colors {
        Color background;
        Color border;
        Color foreground;
    }

    private ButtonPainter() {
    }

    static void installDefaults(AbstractButton b) {
        LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
        LookAndFeel.installProperty(b, "rolloverEnabled", Boolean.TRUE);
        LookAndFeel.installProperty(b, "iconTextGap", 8);
    }

    static ButtonStyle styleOf(AbstractButton b) {
        Object s = b.getClientProperty(Tessel.BUTTON_STYLE);
        if (s instanceof ButtonStyle) return (ButtonStyle) s;
        if (s instanceof String) {
            try {
                return ButtonStyle.valueOf(((String) s).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (b.getParent() instanceof JToolBar) return ButtonStyle.SUBTLE;
        return ButtonStyle.STANDARD;
    }

    static Colors colors(AbstractButton b) {
        Palette p = Tessel.palette();
        ButtonModel m = b.getModel();
        boolean pressed = m.isArmed() && m.isPressed();
        boolean hover = m.isRollover() || pressed;
        boolean selected = b instanceof JToggleButton && m.isSelected();

        ButtonStyle style = styleOf(b);
        // Only filled toggle buttons switch to the accent look (a ToggleSwitch draws its own state).
        if (selected && b.isContentAreaFilled() && (style == ButtonStyle.STANDARD || style == ButtonStyle.SUBTLE || style == ButtonStyle.OUTLINE)) {
            style = ButtonStyle.ACCENT;
        }

        Color normal;
        Color hoverBg;
        Color pressedBg;
        Colors c = new Colors();
        switch (style) {
            case ACCENT:
                normal = p.accent;
                hoverBg = p.accentHover;
                pressedBg = p.accentPressed;
                c.border = p.accent;
                c.foreground = p.onAccent;
                break;
            case OUTLINE:
                normal = null;
                hoverBg = p.accentSubtle;
                pressedBg = p.accentSubtle;
                c.border = p.accent;
                c.foreground = p.accent;
                break;
            case SUBTLE:
            case ICON:
                normal = null;
                hoverBg = p.controlHover;
                pressedBg = p.controlPressed;
                c.border = null;
                c.foreground = p.text;
                break;
            case DANGER:
                normal = p.danger;
                hoverBg = p.dangerHover;
                pressedBg = p.dangerHover;
                c.border = p.danger;
                c.foreground = Color.WHITE;
                break;
            case LINK:
                normal = null;
                hoverBg = null;
                pressedBg = null;
                c.border = null;
                c.foreground = pressed ? p.accentPressed : hover ? p.accentHover : p.accent;
                break;
            default:
                normal = p.controlBackground;
                hoverBg = p.controlHover;
                pressedBg = p.controlPressed;
                c.border = p.border;
                c.foreground = p.text;
                break;
        }
        c.background = pressed ? pressedBg : hover ? hoverBg : normal;

        // An explicitly set background wins; hover and pressed shades are derived from it.
        Color userBg = b.getBackground();
        if (userBg != null && !(userBg instanceof UIResource) && style != ButtonStyle.LINK) {
            c.background = pressed ? shade(userBg, 2) : hover ? shade(userBg, 1) : userBg;
            if (style != ButtonStyle.OUTLINE) c.border = null;
        }
        Color userFg = b.getForeground();
        if (userFg != null && !(userFg instanceof UIResource)) c.foreground = userFg;
        return c;
    }

    private static Color shade(Color c, int level) {
        if (c.getAlpha() < 255) return Palette.withAlpha(c, c.getAlpha() + 28 * level);
        boolean dark = Palette.luminance(c) < 0.2;
        return Palette.mix(c, dark ? Color.WHITE : Color.BLACK, 0.07 * level);
    }

    static void paintBackground(Graphics2D g, AbstractButton b, Colors c) {
        int w = b.getWidth();
        int h = b.getHeight();
        if (b.isContentAreaFilled() && c.background != null) {
            g.setColor(c.background);
            UIUtils.fillRound(g, 0, 0, w, h, RADIUS);
        }
        if (b.isBorderPainted() && c.border != null) {
            g.setColor(c.border);
            UIUtils.strokeRound(g, 0, 0, w, h, RADIUS, 1f);
        }
        if (b.isFocusPainted() && UIUtils.isFocusVisible(b)) {
            UIUtils.paintFocusRing(g, 0, 0, w, h, RADIUS);
        }
    }

    static void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect, Colors c) {
        ButtonModel m = b.getModel();
        Icon icon = b.getIcon();
        if (icon == null) return;
        if (m.isPressed() && m.isArmed() && b.getPressedIcon() != null) icon = b.getPressedIcon();
        else if (m.isSelected() && b.getSelectedIcon() != null) icon = b.getSelectedIcon();
        else if (m.isRollover() && b.getRolloverIcon() != null) icon = b.getRolloverIcon();

        if (icon instanceof FontIcon) {
            ((FontIcon) icon).paintIcon(b, g, iconRect.x, iconRect.y, c == null ? null : c.foreground);
        } else {
            icon.paintIcon(b, g, iconRect.x, iconRect.y);
        }
    }

    static void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text, Colors c, int shift) {
        FontMetrics fm = b.getFontMetrics(b.getFont());
        g.setColor(c == null ? b.getForeground() : c.foreground);
        BasicGraphicsUtils.drawStringUnderlineCharAt(b, (Graphics2D) g, text, b.getDisplayedMnemonicIndex(),
                textRect.x + shift, textRect.y + fm.getAscent() + shift);
    }

    static Dimension preferredSize(AbstractButton b, Dimension d) {
        if (d == null) return null;
        ButtonStyle style = styleOf(b);
        if (style == ButtonStyle.LINK) return d;
        if (style == ButtonStyle.ICON && (b.getText() == null || b.getText().isEmpty())) {
            Icon icon = b.getIcon();
            int s = Math.max(32, icon == null ? 0 : Math.max(icon.getIconWidth(), icon.getIconHeight()) + 12);
            return new Dimension(s, s);
        }
        if (b.isContentAreaFilled() || b.isBorderPainted()) {
            d.height = Math.max(d.height, 32);
            d.width = Math.max(d.width, 32);
        }
        return d;
    }

    /** Wraps painting in the disabled transparency. */
    static Graphics2D begin(Graphics g, JComponent c) {
        Graphics2D g2 = UIUtils.create(g);
        UIUtils.applyEnabledAlpha(g2, c);
        return g2;
    }
}
