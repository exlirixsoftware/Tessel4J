package net.tessel.laf;

/**
 * Typography ramp. Apply to labels with {@link Tessel#textStyle(javax.swing.JComponent, TextStyle)}
 * or create a label with {@link Tessel#label(String, TextStyle)}.
 */
public enum TextStyle {
    DISPLAY(40, true, false),
    TITLE(28, true, false),
    SUBTITLE(20, true, false),
    HEADER(16, true, false),
    BODY_STRONG(14, true, false),
    BODY(14, false, false),
    SECONDARY(14, false, true),
    CAPTION(12, false, true),
    CODE(13, false, false);

    final float size;
    final boolean semibold;
    final boolean secondary;

    TextStyle(float size, boolean semibold, boolean secondary) {
        this.size = size;
        this.semibold = semibold;
        this.secondary = secondary;
    }

    public float size() {
        return size;
    }

    /** True when the style uses the secondary text color. */
    public boolean isSecondary() {
        return secondary;
    }
}
