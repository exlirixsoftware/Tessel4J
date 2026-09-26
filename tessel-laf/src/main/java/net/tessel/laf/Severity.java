package net.tessel.laf;

import java.awt.Color;

/** Severity of an info bar, snackbar or status icon. */
public enum Severity {
    INFORMATIONAL,
    SUCCESS,
    WARNING,
    ERROR;

    /** The strong color of this severity in the given palette. */
    public Color color(Palette p) {
        switch (this) {
            case SUCCESS: return p.success;
            case WARNING: return p.warning;
            case ERROR: return p.danger;
            default: return p.info;
        }
    }

    /** The subtle (background) color of this severity in the given palette. */
    public Color subtleColor(Palette p) {
        switch (this) {
            case SUCCESS: return p.successSubtle;
            case WARNING: return p.warningSubtle;
            case ERROR: return p.dangerSubtle;
            default: return p.infoSubtle;
        }
    }
}
