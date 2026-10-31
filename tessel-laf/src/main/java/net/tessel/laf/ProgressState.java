package net.tessel.laf;

import java.awt.Color;

/**
 * The state of a progress bar or progress ring: the normal accent color, paused (warning), failed (error) or
 * finished (success). Set it with {@link Tessel#progressState(javax.swing.JComponent, ProgressState)} on a
 * {@code JProgressBar}, or with {@code ProgressRing.setState}.
 *
 * <p>A paused or failed indeterminate bar stops animating; an error or success state on an indeterminate bar
 * fills it completely.
 */
public enum ProgressState {
    /** Running: the accent color. */
    NORMAL,
    /** Paused by the user or the system: the warning color, animation stopped. */
    PAUSED,
    /** Failed: the danger color, animation stopped. */
    ERROR,
    /** Finished: the success color. */
    SUCCESS;

    /** The fill color of this state in the given palette. */
    public Color color(Palette p) {
        switch (this) {
            case PAUSED: return p.warning;
            case ERROR: return p.danger;
            case SUCCESS: return p.success;
            default: return p.accent;
        }
    }

    /** True when an indeterminate indicator in this state does not animate. */
    public boolean isFrozen() {
        return this != NORMAL;
    }
}
