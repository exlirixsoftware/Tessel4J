package net.tessel.laf.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.ui.UIUtils;

/**
 * A color picker: a saturation / brightness square, a hue slider, an optional opacity slider, a hex field,
 * RGB number boxes and a row of swatches. {@code JColorChooser} uses it under the Tessel look and feel,
 * and it works on its own too.
 *
 * <pre>{@code
 * ColorPicker picker = new ColorPicker(Color.ORANGE);
 * picker.addChangeListener(e -> System.out.println(picker.getColor()));
 * }</pre>
 */
public class ColorPicker extends JPanel {

    private static final int[] SWATCHES = {
            0xEF4444, 0xF97316, 0xF59E0B, 0xEAB308, 0x84CC16, 0x22C55E, 0x14B8A6, 0x06B6D4, 0x3B82F6, 0x4F46E5,
            0x8B5CF6, 0xD946EF, 0xEC4899, 0xF43F5E, 0x78350F, 0x0F172A, 0x475569, 0x94A3B8, 0xE2E8F0, 0xFFFFFF};

    private float hue;
    private float saturation = 1f;
    private float brightness = 1f;
    private int alpha = 255;
    private boolean alphaEnabled;
    private boolean updating;
    private final EventListenerList listeners = new EventListenerList();

    private final Spectrum spectrum = new Spectrum();
    private final HueBar hueBar = new HueBar();
    private final AlphaBar alphaBar = new AlphaBar();
    private final Preview preview = new Preview();
    private final JTextField hexField = new JTextField(8);
    private final NumberBox red = channelBox();
    private final NumberBox green = channelBox();
    private final NumberBox blue = channelBox();
    private final NumberBox opacity = channelBox();
    private JPanel opacityColumn;

    public ColorPicker() {
        this(new Color(0x4F46E5));
    }

    public ColorPicker(Color color) {
        super(new GridBagLayout());
        setOpaque(false);
        build();
        setColor(color == null ? Color.WHITE : color);
        sync();
    }

    // ------------------------------------------------------------------ API

    /** The selected color; has an alpha channel only when {@link #isAlphaEnabled()}. */
    public Color getColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        return alphaEnabled ? new Color((alpha << 24) | rgb, true) : new Color(rgb);
    }

    public void setColor(Color color) {
        if (color == null) return;
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int newAlpha = alphaEnabled ? color.getAlpha() : 255;
        int newRgb = color.getRGB() & 0xFFFFFF;
        if (newRgb == (Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF) && newAlpha == alpha) return;
        if (hsb[2] == 0f) {
            brightness = 0f;
        } else if (hsb[1] == 0f) {
            saturation = 0f;
            brightness = hsb[2];
        } else {
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
        }
        alpha = newAlpha;
        sync();
        fireChange();
    }

    public boolean isAlphaEnabled() {
        return alphaEnabled;
    }

    /** Shows the opacity slider and field. */
    public void setAlphaEnabled(boolean enabled) {
        if (alphaEnabled == enabled) return;
        alphaEnabled = enabled;
        if (!enabled) alpha = 255;
        alphaBar.setVisible(enabled);
        opacityColumn.setVisible(enabled);
        revalidate();
        sync();
        fireChange();
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(ChangeListener.class, l);
    }

    private void fireChange() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners.getListeners(ChangeListener.class)) l.stateChanged(e);
    }

    // ------------------------------------------------------------------ building

    private static NumberBox channelBox() {
        NumberBox box = new NumberBox(0, 0, 255);
        box.setSpinButtonPlacement(NumberBox.SpinButtonPlacement.HIDDEN);
        box.setFractionDigits(0, 0);
        box.getEditor().setColumns(3);
        return box;
    }

    private void build() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 4;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 10, 0);
        add(spectrum, c);

        c.gridy++;
        c.weighty = 0;
        add(hueBar, c);
        c.gridy++;
        add(alphaBar, c);
        alphaBar.setVisible(false);

        // Preview and hex
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 10, 12);
        add(preview, c);

        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 10, 0);
        JPanel hexRow = new JPanel(new java.awt.BorderLayout(8, 0));
        hexRow.setOpaque(false);
        hexRow.add(Tessel.label("Hex", TextStyle.CAPTION), java.awt.BorderLayout.WEST);
        hexRow.add(hexField, java.awt.BorderLayout.CENTER);
        add(hexRow, c);
        hexField.addActionListener(e -> applyHex());
        hexField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                applyHex();
            }
        });

        // Channels
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 4;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 10, 0);
        JPanel channels = new JPanel(new GridLayout(1, 0, 8, 0));
        channels.setOpaque(false);
        channels.add(labelled("R", red));
        channels.add(labelled("G", green));
        channels.add(labelled("B", blue));
        opacityColumn = labelled("A", opacity);
        channels.add(opacityColumn);
        opacityColumn.setVisible(false);
        add(channels, c);
        for (NumberBox box : new NumberBox[]{red, green, blue}) box.addChangeListener(e -> fromChannels());
        opacity.setValue(255);
        opacity.addChangeListener(e -> {
            if (updating) return;
            alpha = opacity.getIntValue();
            sync();
            fireChange();
        });

        // Swatches
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel swatches = new JPanel(new GridLayout(2, 10, 4, 4));
        swatches.setOpaque(false);
        for (int rgb : SWATCHES) swatches.add(new Swatch(new Color(rgb)));
        add(swatches, c);
    }

    private JPanel labelled(String text, NumberBox box) {
        JPanel p = new JPanel(new java.awt.BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = Tessel.label(text, TextStyle.CAPTION);
        l.setLabelFor(box.getEditor());
        p.add(l, java.awt.BorderLayout.NORTH);
        p.add(box, java.awt.BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------ state

    private void fromChannels() {
        if (updating) return;
        setColor(new Color(red.getIntValue(), green.getIntValue(), blue.getIntValue(), alpha));
    }

    private void applyHex() {
        if (updating) return;
        String t = hexField.getText().trim();
        if (t.startsWith("#")) t = t.substring(1);
        try {
            if (t.length() == 3) {
                t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
            }
            if (t.length() == 6) {
                setColor(new Color(Integer.parseInt(t, 16)));
            } else if (t.length() == 8) {
                long argb = Long.parseLong(t, 16);
                int a = (int) ((argb >> 24) & 0xFF);
                if (alphaEnabled) alpha = a;
                setColor(new Color((int) (argb & 0xFFFFFF)));
            }
        } catch (NumberFormatException ignored) {
            // restored below
        }
        sync();
    }

    /** Pushes the state into the fields and repaints the custom parts. */
    private void sync() {
        updating = true;
        try {
            int rgb = Color.HSBtoRGB(hue, saturation, brightness);
            red.setValue((rgb >> 16) & 0xFF);
            green.setValue((rgb >> 8) & 0xFF);
            blue.setValue(rgb & 0xFF);
            opacity.setValue(alpha);
            String hex = String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
            if (alphaEnabled) hex = String.format(Locale.ROOT, "#%02X%06X", alpha, rgb & 0xFFFFFF);
            hexField.setText(hex);
        } finally {
            updating = false;
        }
        spectrum.repaint();
        hueBar.repaint();
        alphaBar.repaint();
        preview.repaint();
    }

    private void setHsv(float h, float s, float v) {
        hue = clamp01(h);
        saturation = clamp01(s);
        brightness = clamp01(v);
        sync();
        fireChange();
    }

    private void setAlpha(int a) {
        alpha = Math.max(0, Math.min(255, a));
        sync();
        fireChange();
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // ------------------------------------------------------------------ painted parts

    private static void paintThumb(Graphics2D g, double cx, double cy, Color fill) {
        double r = 8;
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(cx - r - 1, cy - r, 2 * r + 2, 2 * r + 2));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(fill);
        g.fill(new Ellipse2D.Double(cx - r + 2.5, cy - r + 2.5, 2 * r - 5, 2 * r - 5));
    }

    private static void paintChecker(Graphics2D g, Shape clip, int w, int h) {
        java.awt.Shape old = g.getClip();
        g.clip(clip);
        for (int y = 0; y < h; y += 6) {
            for (int x = 0; x < w; x += 6) {
                g.setColor(((x / 6 + y / 6) & 1) == 0 ? new Color(0xFFFFFF) : new Color(0xCCCCCC));
                g.fillRect(x, y, 6, 6);
            }
        }
        g.setClip(old);
    }

    /** Base for the custom-painted, mouse- and keyboard-driven parts. */
    private abstract class Track extends JComponent {
        Track() {
            setFocusable(true);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    drag(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    drag(e.getX(), e.getY());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    float step = e.isShiftDown() ? 0.1f : 0.01f;
                    int dx = e.getKeyCode() == KeyEvent.VK_RIGHT ? 1 : e.getKeyCode() == KeyEvent.VK_LEFT ? -1 : 0;
                    int dy = e.getKeyCode() == KeyEvent.VK_UP ? 1 : e.getKeyCode() == KeyEvent.VK_DOWN ? -1 : 0;
                    if (dx != 0 || dy != 0) {
                        nudge(dx * step, dy * step);
                        e.consume();
                    }
                }
            });
        }

        abstract void drag(int x, int y);

        abstract void nudge(float dx, float dy);

        void paintFocus(Graphics2D g, double radius) {
            if (UIUtils.isFocusVisible(this)) UIUtils.paintFocusRing(g, 0, 0, getWidth(), getHeight(), radius);
        }
    }

    private final class Spectrum extends Track {
        private BufferedImage image;
        private float imageHue = -1;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 180);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(160, 100);
        }

        @Override
        void drag(int x, int y) {
            setHsv(hue, (float) x / Math.max(1, getWidth() - 1), 1f - (float) y / Math.max(1, getHeight() - 1));
        }

        @Override
        void nudge(float dx, float dy) {
            setHsv(hue, saturation + dx, brightness + dy);
        }

        private void ensureImage() {
            int w = getWidth();
            int h = getHeight();
            if (image != null && image.getWidth() == w && image.getHeight() == h && imageHue == hue) return;
            image = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
            int[] row = new int[image.getWidth()];
            for (int y = 0; y < image.getHeight(); y++) {
                float v = 1f - (float) y / Math.max(1, h - 1);
                for (int x = 0; x < row.length; x++) {
                    row[x] = Color.HSBtoRGB(hue, (float) x / Math.max(1, w - 1), v);
                }
                image.setRGB(0, y, row.length, 1, row, 0, row.length);
            }
            imageHue = hue;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                ensureImage();
                int w = getWidth();
                int h = getHeight();
                Shape shape = new RoundRectangle2D.Double(0, 0, w, h, 12, 12);
                g2.setClip(shape);
                g2.drawImage(image, 0, 0, null);
                g2.setClip(null);
                g2.setColor(p.border);
                UIUtils.strokeRound(g2, 0, 0, w, h, 6, 1f);
                paintThumb(g2, saturation * (w - 1), (1 - brightness) * (h - 1), new Color(Color.HSBtoRGB(hue, saturation, brightness)));
                paintFocus(g2, 6);
            } finally {
                g2.dispose();
            }
        }
    }

    private final class HueBar extends Track {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 20);
        }

        @Override
        void drag(int x, int y) {
            setHsv((float) (x - 8) / Math.max(1, getWidth() - 16), saturation, brightness);
        }

        @Override
        void nudge(float dx, float dy) {
            setHsv(hue + dx, saturation, brightness);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                float[] stops = {0f, 1f / 6, 2f / 6, 3f / 6, 4f / 6, 5f / 6, 1f};
                Color[] colors = new Color[stops.length];
                for (int i = 0; i < stops.length; i++) colors[i] = new Color(Color.HSBtoRGB(stops[i], 1f, 1f));
                double ty = (h - 10) / 2.0;
                g2.setPaint(new LinearGradientPaint(8, 0, w - 8, 0, stops, colors));
                UIUtils.fillRound(g2, 0, ty, w, 10, 5);
                g2.setColor(p.border);
                UIUtils.strokeRound(g2, 0, ty, w, 10, 5, 1f);
                paintThumb(g2, 8 + hue * (w - 16), h / 2.0, new Color(Color.HSBtoRGB(hue, 1f, 1f)));
                paintFocus(g2, 6);
            } finally {
                g2.dispose();
            }
        }
    }

    private final class AlphaBar extends Track {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 20);
        }

        @Override
        void drag(int x, int y) {
            setAlpha(Math.round(255f * (x - 8) / Math.max(1, getWidth() - 16)));
        }

        @Override
        void nudge(float dx, float dy) {
            setAlpha(alpha + Math.round(dx * 255));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                double ty = (h - 10) / 2.0;
                Shape track = new RoundRectangle2D.Double(0, ty, w, 10, 10, 10);
                paintChecker(g2, track, w, h);
                int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
                Shape old = g2.getClip();
                g2.clip(track);
                g2.setPaint(new GradientPaint(8, 0, new Color(rgb, true), w - 8, 0, new Color(0xFF000000 | rgb, true)));
                g2.fillRect(0, 0, w, h);
                g2.setClip(old);
                g2.setColor(p.border);
                UIUtils.strokeRound(g2, 0, ty, w, 10, 5, 1f);
                paintThumb(g2, 8 + alpha / 255.0 * (w - 16), h / 2.0, new Color((alpha << 24) | rgb, true));
                paintFocus(g2, 6);
            } finally {
                g2.dispose();
            }
        }
    }

    private final class Preview extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(40, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                paintChecker(g2, new RoundRectangle2D.Double(0, 0, w, h, 12, 12), w, h);
                g2.setColor(getColor());
                UIUtils.fillRound(g2, 0, 0, w, h, 6);
                g2.setColor(p.borderStrong);
                UIUtils.strokeRound(g2, 0, 0, w, h, 6, 1f);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A clickable color tile. */
    private final class Swatch extends JComponent {
        private final Color color;
        private boolean hover;

        Swatch(Color color) {
            this.color = color;
            setFocusable(true);
            setToolTipText(String.format(Locale.ROOT, "#%06X", color.getRGB() & 0xFFFFFF));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    choose();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) choose();
                }
            });
        }

        private void choose() {
            if (alphaEnabled) setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            else setColor(color);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(26, 26);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                g2.setColor(color);
                UIUtils.fillRound(g2, 0, 0, w, h, 5);
                boolean selected = (getColor().getRGB() & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF);
                g2.setColor(selected ? p.text : hover ? p.borderStrong : p.border);
                UIUtils.strokeRound(g2, 0, 0, w, h, 5, selected ? 2f : 1f);
                if (UIUtils.isFocusVisible(this)) UIUtils.paintFocusRing(g2, -2, -2, w + 4, h + 4, 7);
            } finally {
                g2.dispose();
            }
        }
    }
}
