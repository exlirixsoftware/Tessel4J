package net.tessel.laf.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;
import javax.swing.plaf.UIResource;
import net.tessel.laf.Fonts;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.UIUtils;

/** A round picture of a person, falling back to initials on a color derived from the name. */
public class Avatar extends JComponent {

    private static final Color[] PALETTE = {
            new Color(0x4F46E5), new Color(0x0891B2), new Color(0x059669), new Color(0xD97706),
            new Color(0xDB2777), new Color(0x7C3AED), new Color(0xDC2626), new Color(0x2563EB),
    };

    private String displayName;
    private Image image;

    public Avatar(String displayName) {
        this(displayName, 40);
    }

    public Avatar(String displayName, int size) {
        this.displayName = displayName;
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
        setToolTipText(displayName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        setToolTipText(displayName);
        repaint();
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
        repaint();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Initials: first letter of the first and last word. */
    public String getInitials() {
        if (displayName == null || displayName.isBlank()) return "";
        String[] parts = displayName.trim().split("\\s+");
        String s = parts.length == 1 ? parts[0].substring(0, 1)
                : parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    /** The background color: explicit background, or one derived from the name (same as Tessel.NET). */
    public Color getFillColor() {
        Color bg = getBackground();
        if (bg != null && !(bg instanceof UIResource) && isBackgroundSet()) return bg;
        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) return Tessel.palette().accent;
        int hash = 0;
        for (int i = 0; i < name.length(); i++) hash = hash * 31 + name.charAt(i);
        return PALETTE[(hash & Integer.MAX_VALUE) % PALETTE.length];
    }

    @Override
    protected void paintComponent(Graphics g) {
        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        double x = (getWidth() - size) / 2.0;
        double y = (getHeight() - size) / 2.0;
        Graphics2D g2 = UIUtils.create(g);
        try {
            Ellipse2D circle = new Ellipse2D.Double(x, y, size, size);
            if (image != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.clip(circle);
                int iw = image.getWidth(null);
                int ih = image.getHeight(null);
                double scale = Math.max(size / (double) iw, size / (double) ih);
                int dw = (int) Math.round(iw * scale);
                int dh = (int) Math.round(ih * scale);
                g2.drawImage(image, (int) (x + (size - dw) / 2.0), (int) (y + (size - dh) / 2.0), dw, dh, null);
                return;
            }
            g2.setColor(getFillColor());
            g2.fill(circle);
            String initials = getInitials();
            if (!initials.isEmpty()) {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Fonts.semibold(Math.max(8f, size * 0.38f)));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                float tx = (float) (x + (size - fm.stringWidth(initials)) / 2.0);
                float ty = (float) (y + (size - fm.getAscent() - fm.getDescent()) / 2.0 + fm.getAscent());
                g2.drawString(initials, tx, ty);
            }
        } finally {
            g2.dispose();
        }
    }
}
