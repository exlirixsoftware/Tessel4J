package net.tessel.laf.components;

import javax.swing.Icon;

/**
 * A button whose whole surface opens a menu, with a small arrow at the end.
 *
 * <pre>{@code
 * DropDownButton sort = new DropDownButton("Sort by");
 * sort.addMenuItem("Name", () -> sortBy(NAME));
 * sort.addMenuItem("Date", () -> sortBy(DATE));
 * }</pre>
 *
 * @see SplitButton
 */
public class DropDownButton extends SplitButton {

    public DropDownButton() {
        this(null, null);
    }

    public DropDownButton(String text) {
        this(text, null);
    }

    public DropDownButton(String text, Icon icon) {
        super(text, icon);
        setSplit(false);
    }
}
