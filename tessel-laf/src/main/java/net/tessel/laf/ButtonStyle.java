package net.tessel.laf;

/**
 * Visual variants of buttons. Apply with {@link Tessel#style(javax.swing.AbstractButton, ButtonStyle)}
 * or the client property {@link Tessel#BUTTON_STYLE}.
 */
public enum ButtonStyle {
    /** Neutral surface with a border. */
    STANDARD,
    /** Filled with the accent color, for the primary action. */
    ACCENT,
    /** Accent-colored border and text. */
    OUTLINE,
    /** No background until hovered (ghost button). */
    SUBTLE,
    /** Red, for destructive actions. */
    DANGER,
    /** Square 32x32 icon-only button. */
    ICON,
    /** Looks like a hyperlink. */
    LINK
}
