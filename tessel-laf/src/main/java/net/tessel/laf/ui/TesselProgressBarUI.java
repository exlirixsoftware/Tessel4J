package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicProgressBarUI;
import net.tessel.laf.ProgressState;
import net.tessel.laf.Tessel;

/**
 * Thin rounded progress bars with an indeterminate sliding segment and the {@link ProgressState}s: paused
 * (warning color), error (danger) and success.
 */
public class TesselProgressBarUI extends BasicProgressBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselProgressBarUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        LookAndFeel.installProperty(progressBar, "opaque", Boolean.FALSE);
    }

    private final PropertyChangeListener stateListener = e -> {
        if (state().isFrozen()) {
            stopAnimationTimer();
        } else if (progressBar.isIndeterminate() && progressBar.isDisplayable()) {
            startAnimationTimer();
        }
        progressBar.repaint();
    };

    @Override
    protected void installListeners() {
        super.installListeners();
        progressBar.addPropertyChangeListener(Tessel.PROGRESS_STATE, stateListener);
    }

    @Override
    protected void uninstallListeners() {
        progressBar.removePropertyChangeListener(Tessel.PROGRESS_STATE, stateListener);
        super.uninstallListeners();
    }

    /** A paused, failed or finished bar does not animate. */
    @Override
    protected void startAnimationTimer() {
        if (state().isFrozen()) return;
        super.startAnimationTimer();
    }

    private ProgressState state() {
        Object s = progressBar.getClientProperty(Tessel.PROGRESS_STATE);
        return s instanceof ProgressState ? (ProgressState) s : ProgressState.NORMAL;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (!progressBar.isStringPainted()) {
            Insets in = progressBar.getInsets();
            if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) d.height = 4 + in.top + in.bottom;
            else d.width = 4 + in.left + in.right;
        }
        return d;
    }

    private Color track() {
        Color bg = progressBar.getBackground();
        return bg == null || bg instanceof UIResource ? Tessel.palette().subtle : bg;
    }

    /** The state color wins; in the normal state an explicitly set foreground is used. */
    private Color fill() {
        ProgressState state = state();
        if (state != ProgressState.NORMAL) return state.color(Tessel.palette());
        Color fg = progressBar.getForeground();
        return fg == null || fg instanceof UIResource ? Tessel.palette().accent : fg;
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Insets b = progressBar.getInsets();
        int w = progressBar.getWidth() - b.left - b.right;
        int h = progressBar.getHeight() - b.top - b.bottom;
        if (w <= 0 || h <= 0) return;
        int amount = getAmountFull(b, w, h);
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, c);
            double r = Math.min(Math.min(w, h), 4) / 2.0;
            g2.setColor(track());
            UIUtils.fillRound(g2, b.left, b.top, w, h, r);
            g2.setColor(fill());
            if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
                if (progressBar.getComponentOrientation().isLeftToRight()) UIUtils.fillRound(g2, b.left, b.top, amount, h, r);
                else UIUtils.fillRound(g2, b.left + w - amount, b.top, amount, h, r);
            } else {
                UIUtils.fillRound(g2, b.left, b.top + h - amount, w, amount, r);
            }
            if (progressBar.isStringPainted()) paintString(g2, b.left, b.top, w, h, amount, b);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Insets b = progressBar.getInsets();
        int w = progressBar.getWidth() - b.left - b.right;
        int h = progressBar.getHeight() - b.top - b.bottom;
        if (w <= 0 || h <= 0) return;
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, c);
            double r = Math.min(Math.min(w, h), 4) / 2.0;
            g2.setColor(track());
            UIUtils.fillRound(g2, b.left, b.top, w, h, r);
            ProgressState state = state();
            boolean horizontal = progressBar.getOrientation() == JProgressBar.HORIZONTAL;
            if (state == ProgressState.ERROR || state == ProgressState.SUCCESS) {
                // Nothing is in progress any more: the whole track shows the outcome.
                g2.setColor(fill());
                UIUtils.fillRound(g2, b.left, b.top, w, h, r);
            } else if (state == ProgressState.PAUSED) {
                // A stopped segment, resting in the middle of the track.
                int length = getBoxLength(horizontal ? w : h, 0);
                g2.setColor(fill());
                if (horizontal) UIUtils.fillRound(g2, b.left + (w - length) / 2.0, b.top, length, h, r);
                else UIUtils.fillRound(g2, b.left, b.top + (h - length) / 2.0, w, length, r);
            } else {
                boxRect = getBox(boxRect);
                if (boxRect != null) {
                    g2.clip(new RoundRectangle2D.Double(b.left, b.top, w, h, r * 2, r * 2));
                    g2.setColor(fill());
                    Rectangle box = boxRect;
                    UIUtils.fillRound(g2, box.x, box.y, box.width, box.height, r);
                }
            }
            if (progressBar.isStringPainted()) {
                g2.setClip(null);
                paintString(g2, b.left, b.top, w, h, 0, b);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected int getBoxLength(int availableLength, int otherDimension) {
        return (int) Math.round(availableLength * 0.3);
    }
}
