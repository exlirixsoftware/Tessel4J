package net.tessel.gallery.pages;

import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Tessel;
import net.tessel.laf.components.Card;
import net.tessel.laf.components.ToggleSwitch;

public class SelectionPage extends Page {

    public SelectionPage() {
        super("Selection", "Check boxes, radio buttons and toggle switches.");

        JCheckBox indeterminate = new JCheckBox("Indeterminate");
        indeterminate.putClientProperty(Tessel.INDETERMINATE, Boolean.TRUE);
        JCheckBox disabled = new JCheckBox("Disabled");
        disabled.setEnabled(false);
        JCheckBox disabledChecked = new JCheckBox("Disabled checked", true);
        disabledChecked.setEnabled(false);
        JPanel checks = Layouts.stack(0, new JCheckBox("Unchecked"), new JCheckBox("Checked", true), indeterminate, disabled, disabledChecked);

        ButtonGroup shipping = new ButtonGroup();
        JRadioButton standard = new JRadioButton("Standard shipping", true);
        JRadioButton express = new JRadioButton("Express shipping");
        JRadioButton pickup = new JRadioButton("Pick up in store");
        JRadioButton unavailable = new JRadioButton("Unavailable");
        unavailable.setEnabled(false);
        for (JRadioButton r : List.of(standard, express, pickup, unavailable)) shipping.add(r);
        JPanel radios = Layouts.stack(0, standard, express, pickup, unavailable);

        ToggleSwitch onOff = new ToggleSwitch();
        onOff.setOnText("On");
        onOff.setOffText("Off");
        ToggleSwitch disabledSwitch = new ToggleSwitch("Disabled");
        disabledSwitch.setEnabled(false);
        JPanel switches = Layouts.stack(4, new ToggleSwitch("Wi-Fi", true), new ToggleSwitch("Bluetooth"), onOff, disabledSwitch);

        add(Layouts.columns(16, new Card("Check boxes", checks), new Card("Radio buttons", radios), new Card("Toggle switches", switches)));

        // Select-all pattern with a mixed state.
        JCheckBox all = new JCheckBox("Select all toppings");
        all.setFont(net.tessel.laf.Fonts.semibold(14));
        List<JCheckBox> toppings = List.of(new JCheckBox("Cheese"), new JCheckBox("Mushrooms"), new JCheckBox("Olives"));
        JPanel options = Layouts.stack(0, toppings.toArray(new JCheckBox[0]));
        options.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        boolean[] updating = {false};
        all.addActionListener(e -> {
            updating[0] = true;
            all.putClientProperty(Tessel.INDETERMINATE, null);
            for (JCheckBox t : toppings) t.setSelected(all.isSelected());
            updating[0] = false;
        });
        for (JCheckBox t : toppings) {
            t.addItemListener(e -> {
                if (updating[0]) return;
                long n = toppings.stream().filter(JCheckBox::isSelected).count();
                all.setSelected(n == toppings.size());
                all.putClientProperty(Tessel.INDETERMINATE, n > 0 && n < toppings.size() ? Boolean.TRUE : null);
                all.repaint();
            });
        }
        section("Select all pattern", Layouts.stack(0, all, options));
    }
}
