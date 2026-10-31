package net.tessel.laf.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.UIResource;
import net.tessel.laf.ProgressState;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.UIUtils;

/**
 * An indeterminate spinning progress indicator. A {@link ProgressState} other than {@code NORMAL} stops the
 * spinning: paused shows a resting arc in the warning color, error and success show a full ring in the danger
 * and success colors.
 */
public class ProgressRing extends JComponent {

    private boolean active = true;
    private ProgressState state = ProgressState.NORMAL;
    private float thickness = 3f;
    private double angle;
    private final Timer timer = new Timer(16, e -> {
        angle = (angle + 6.5) % 360;
        repaint();
    });

    public ProgressRing() {
        setOpaque(false);
        setPreferredSize(new Dimension(32, 32));
    }

    public ProgressRing(int size) {
        this();
        setPreferredSize(new Dimension(size, size));
        thickness = Math.max(2f, size / 11f);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        updateTimer();
        repaint();
    }

    public ProgressState getState() {
        return state;
    }

    public void setState(ProgressState state) {
        this.state = state == null ? ProgressState.NORMAL : state;
        updateTimer();
        repaint();
    }

    public float getThickness() {
        return thickness;
    }

    public void setThickness(float thickness) {
        this.thickness = thickness;
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

    @Override
    public void addNotify() {
        super.addNotify();
        updateTimer();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    private void updateTimer() {
        if (active && !state.isFrozen() && isDisplayable()) timer.start();
        else timer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!active) return;
        double size = Math.min(getWidth(), getHeight()) - thickness;
        if (size <= 0) return;
        Color color = state != ProgressState.NORMAL ? state.color(Tessel.palette())
                : getForeground() == null || getForeground() instanceof UIResource ? Tessel.palette().accent : getForeground();
        Graphics2D g2 = UIUtils.create(g);
        try {
            double x = (getWidth() - size) / 2.0;
            double y = (getHeight() - size) / 2.0;
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (state == ProgressState.ERROR || state == ProgressState.SUCCESS) {
                g2.draw(new Arc2D.Double(x, y, size, size, 0, 360, Arc2D.OPEN));
            } else if (state == ProgressState.PAUSED) {
                g2.draw(new Arc2D.Double(x, y, size, size, 90, 250, Arc2D.OPEN));
            } else {
                // A sweep that breathes between 60 and 250 degrees while rotating.
                double sweep = 155 + 95 * Math.sin(Math.toRadians(angle * 2));
                g2.draw(new Arc2D.Double(x, y, size, size, -angle * 2, sweep, Arc2D.OPEN));
            }
        } finally {
            g2.dispose();
        }
    }
}
