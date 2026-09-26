package net.tessel.laf.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSliderUI;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/** Sliders with a 4px track, an accent fill and a round thumb. Clicking the track jumps to the point. */
public class TesselSliderUI extends BasicSliderUI {

    private boolean thumbHover;
    private java.awt.Point pressPoint = new java.awt.Point();

    /** Registered before the track listener so the press location is known when the track scrolls. */
    private final MouseAdapter pressRecorder = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            pressPoint = e.getPoint();
        }
    };

    private final MouseAdapter hoverHandler = new MouseAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            setThumbHover(thumbRect.contains(e.getPoint()));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setThumbHover(false);
        }
    };

    public TesselSliderUI(JSlider slider) {
        super(slider);
    }

    public static ComponentUI createUI(JComponent c) {
        return new TesselSliderUI((JSlider) c);
    }

    @Override
    protected void installDefaults(JSlider s) {
        super.installDefaults(s);
        LookAndFeel.installProperty(s, "opaque", Boolean.FALSE);
    }

    @Override
    protected void installListeners(JSlider s) {
        s.addMouseListener(pressRecorder);
        super.installListeners(s);
        s.addMouseListener(hoverHandler);
        s.addMouseMotionListener(hoverHandler);
    }

    @Override
    protected void uninstallListeners(JSlider s) {
        s.removeMouseListener(pressRecorder);
        s.removeMouseListener(hoverHandler);
        s.removeMouseMotionListener(hoverHandler);
        super.uninstallListeners(s);
    }

    private void setThumbHover(boolean hover) {
        if (thumbHover != hover) {
            thumbHover = hover;
            slider.repaint(thumbRect);
        }
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(20, 20);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, c);
            super.paint(g2, c);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintTrack(Graphics g) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            Rectangle t = trackRect;
            java.awt.Color fill = slider.getForeground() instanceof UIResource ? p.accent : slider.getForeground();
            if (slider.getOrientation() == JSlider.HORIZONTAL) {
                double cy = t.y + t.height / 2.0 - 2;
                g2.setColor(p.borderStrong);
                UIUtils.fillRound(g2, t.x, cy, t.width, 4, 2);
                double center = thumbRect.x + thumbRect.width / 2.0;
                boolean fromRight = slider.getInverted() ^ !slider.getComponentOrientation().isLeftToRight();
                g2.setColor(fill);
                if (fromRight) UIUtils.fillRound(g2, center, cy, t.x + t.width - center, 4, 2);
                else UIUtils.fillRound(g2, t.x, cy, center - t.x, 4, 2);
            } else {
                double cx = t.x + t.width / 2.0 - 2;
                g2.setColor(p.borderStrong);
                UIUtils.fillRound(g2, cx, t.y, 4, t.height, 2);
                double center = thumbRect.y + thumbRect.height / 2.0;
                g2.setColor(fill);
                if (slider.getInverted()) UIUtils.fillRound(g2, cx, t.y, 4, center - t.y, 2);
                else UIUtils.fillRound(g2, cx, center, 4, t.y + t.height - center, 2);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintThumb(Graphics g) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            Rectangle r = thumbRect;
            double x = r.x + (r.width - 20) / 2.0;
            double y = r.y + (r.height - 20) / 2.0;
            g2.setColor(p.dark ? p.controlBackground : p.surface);
            g2.fill(new Ellipse2D.Double(x, y, 20, 20));
            g2.setColor(p.borderStrong);
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, 19, 19));
            double d = isDragging() ? 10 : thumbHover ? 14 : 12;
            g2.setColor(isDragging() ? p.accentPressed : p.accent);
            g2.fill(new Ellipse2D.Double(x + (20 - d) / 2, y + (20 - d) / 2, d, d));
            if (UIUtils.isFocusVisible(slider)) {
                g2.setColor(p.accent);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(x - 1, y - 1, 22, 22));
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintFocus(Graphics g) {
        // The thumb draws the focus ring.
    }

    @Override
    protected void scrollDueToClickInTrack(int direction) {
        int value = slider.getOrientation() == JSlider.HORIZONTAL
                ? valueForXPosition(pressPoint.x)
                : valueForYPosition(pressPoint.y);
        slider.setValue(value);
    }
}
