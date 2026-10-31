package net.tessel.gallery.pages;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.DropDownButton;
import net.tessel.laf.components.Snackbar;
import net.tessel.laf.components.SplitButton;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;

public class ButtonsPage extends Page {

    private int count;

    public ButtonsPage() {
        super("Buttons", "Button styles, icon buttons, toggle buttons and links.");

        JButton disabled = new JButton("Disabled");
        disabled.setEnabled(false);
        JButton accentDisabled = Tessel.style(new JButton("Accent disabled"), ButtonStyle.ACCENT);
        accentDisabled.setEnabled(false);
        section("Variants", Layouts.row(
                new JButton("Standard"),
                Tessel.style(new JButton("Accent"), ButtonStyle.ACCENT),
                Tessel.style(new JButton("Outline"), ButtonStyle.OUTLINE),
                Tessel.style(new JButton("Subtle"), ButtonStyle.SUBTLE),
                Tessel.style(new JButton("Danger"), ButtonStyle.DANGER),
                disabled, accentDisabled));

        JButton settingsOnly = new JButton(new FontIcon(Symbol.SETTINGS));
        settingsOnly.setToolTipText("Icon only (standard)");
        section("With icons", Layouts.row(
                Tessel.style(new JButton("Save", new FontIcon(Symbol.SAVE)), ButtonStyle.ACCENT),
                new JButton("Share", new FontIcon(Symbol.SHARE)),
                Tessel.style(new JButton("Download", new FontIcon(Symbol.DOWNLOAD)), ButtonStyle.OUTLINE),
                Tessel.style(new JButton("Delete", new FontIcon(Symbol.DELETE)), ButtonStyle.DANGER),
                settingsOnly));

        section("Icon buttons", Layouts.row(
                iconButton(Symbol.EDIT, "Edit"),
                iconButton(Symbol.COPY, "Copy"),
                iconButton(Symbol.DELETE, "Delete"),
                iconButton(Symbol.FAVORITE_STAR, "Favorite"),
                iconButton(Symbol.MORE, "More")));

        JToggleButton bold = new JToggleButton("Bold", new FontIcon(Symbol.FONT));
        JToggleButton checked = new JToggleButton("Checked", true);

        JLabel counter = Tessel.label("0", TextStyle.SECONDARY);
        JButton repeat = new JButton("Hold to count");
        Timer repeater = new Timer(80, e -> counter.setText(String.valueOf(++count)));
        repeater.setInitialDelay(400);
        repeat.getModel().addChangeListener(e -> {
            boolean down = repeat.getModel().isArmed() && repeat.getModel().isPressed();
            if (down && !repeater.isRunning()) {
                counter.setText(String.valueOf(++count));
                repeater.start();
            } else if (!down) {
                repeater.stop();
            }
        });

        JButton link = Tessel.style(new JButton("Link button"), ButtonStyle.LINK);
        link.addActionListener(e -> Snackbar.show(this, "Link clicked", "Link buttons use the accent color.", Severity.INFORMATIONAL));

        section("Toggle, repeat & link", Layouts.stack(8,
                Layouts.row(bold, checked, repeat, counter),
                Layouts.row(link)));

        // Split and drop-down buttons
        SplitButton save = Tessel.style(new SplitButton("Save", new FontIcon(Symbol.SAVE)), ButtonStyle.ACCENT);
        save.addActionListener(e -> toast("Saved", "The primary part runs the main action."));
        save.addMenuItem("Save as...", () -> toast("Save as...", "A menu item of the split button."));
        save.addMenuItem("Save a copy", () -> toast("Save a copy", "Another menu item."));
        save.getMenu().addSeparator();
        save.addMenuItem("Save all", () -> toast("Save all", "Everything was saved."));

        SplitButton reply = new SplitButton("Reply", new FontIcon(Symbol.SEND));
        reply.addActionListener(e -> toast("Reply", "Replying to the sender."));
        reply.addMenuItem("Reply all", () -> toast("Reply all", "Replying to everyone."));
        reply.addMenuItem("Forward", () -> toast("Forward", "Forwarding the message."));

        SplitButton outline = Tessel.style(new SplitButton("Export"), ButtonStyle.OUTLINE);
        outline.addActionListener(e -> toast("Export", "Exporting as PDF."));
        outline.addMenuItem("As PDF", () -> toast("Export", "Exporting as PDF."));
        outline.addMenuItem("As HTML", () -> toast("Export", "Exporting as HTML."));

        DropDownButton sort = new DropDownButton("Sort by", new FontIcon(Symbol.SORT));
        sort.addMenuItem("Name", () -> toast("Sort by", "Name"));
        sort.addMenuItem("Date modified", () -> toast("Sort by", "Date modified"));
        sort.addMenuItem("Size", () -> toast("Sort by", "Size"));

        DropDownButton more = Tessel.style(new DropDownButton("More"), ButtonStyle.SUBTLE);
        more.addMenuItem("Rename", () -> toast("More", "Rename"));
        more.addMenuItem("Delete", () -> toast("More", "Delete"));

        SplitButton disabledSplit = new SplitButton("Disabled");
        disabledSplit.setEnabled(false);
        section("Split & drop-down buttons", Layouts.stack(8,
                Layouts.row(save, reply, outline, disabledSplit),
                Layouts.row(sort, more),
                Tessel.label("A SplitButton has a main action and a menu of alternatives. A DropDownButton opens its menu from anywhere on the button.",
                        TextStyle.CAPTION)));
    }

    private void toast(String title, String message) {
        Snackbar.show(this, title, message, Severity.INFORMATIONAL);
    }

    private static JButton iconButton(Symbol symbol, String tooltip) {
        JButton b = Tessel.style(new JButton(new FontIcon(symbol)), ButtonStyle.ICON);
        b.setToolTipText(tooltip);
        return b;
    }
}
