package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;

public class InputsPage extends Page {

    public InputsPage() {
        super("Text & Input", "Text fields with placeholders and icons, combo boxes, number and date spinners, and sliders.");

        // Text fields
        JTextField name = Tessel.placeholder(new JTextField(), "Jane Doe");
        JTextField email = Tessel.leadingIcon(Tessel.placeholder(new JTextField(), "name@example.com"), new FontIcon(Symbol.MAIL, 14));
        JPasswordField password = Tessel.leadingIcon(Tessel.placeholder(new JPasswordField(), "At least 8 characters"), new FontIcon(Symbol.LOCK, 14));
        JTextField readOnly = new JTextField("This value can't be edited");
        readOnly.setEditable(false);

        JTextArea notes = Tessel.placeholder(new JTextArea(4, 20), "Write a few lines...");
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notes);
        JTextField search = Tessel.leadingIcon(Tessel.placeholder(new JTextField(), "Search..."), new FontIcon(Symbol.SEARCH, 14));
        JTextField disabled = new JTextField("Disabled input");
        disabled.setEnabled(false);

        section("Text fields", Layouts.columns(24,
                Layouts.stack(16,
                        Layouts.field("Full name", name),
                        Layouts.field("Email", email),
                        Layouts.field("Password", password),
                        Layouts.field("Read-only", readOnly)),
                Layouts.stack(16,
                        Layouts.field("Multi-line", notesScroll),
                        search,
                        Layouts.field("Disabled", disabled))));

        // Combo boxes
        JComboBox<String> country = new JComboBox<>(new String[]{"Austria", "Germany", "Romania", "Switzerland", "United Kingdom", "United States"});
        country.setSelectedItem(null);
        Tessel.placeholder(country, "Pick a country");
        JComboBox<String> size = new JComboBox<>(new String[]{"Small", "Medium", "Large"});
        size.setSelectedIndex(1);
        JComboBox<String> editable = new JComboBox<>(new String[]{"Apple", "Banana", "Cherry"});
        editable.setEditable(true);
        editable.setSelectedItem("Custom value");
        JComboBox<String> disabledCombo = new JComboBox<>(new String[]{"Unavailable"});
        disabledCombo.setEnabled(false);
        section("Combo boxes", Layouts.columns(16,
                Layouts.field("Country", country),
                Layouts.field("Font size", size),
                Layouts.field("Editable", editable),
                Layouts.field("Disabled", disabledCombo)));

        // Spinners
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(12, 0, 100, 1));
        JSpinner price = new JSpinner(new SpinnerNumberModel(19.99, 0.0, 1000.0, 0.25));
        price.setEditor(new JSpinner.NumberEditor(price, "#,##0.00"));
        Calendar cal = Calendar.getInstance();
        JSpinner date = new JSpinner(new SpinnerDateModel(cal.getTime(), null, null, Calendar.DAY_OF_MONTH));
        date.setEditor(new JSpinner.DateEditor(date, "yyyy-MM-dd"));
        date.setValue(new Date());
        section("Numbers & dates", Layouts.columns(16,
                Layouts.field("Quantity (0-100)", quantity),
                Layouts.field("Price", price),
                Layouts.field("Due date", date)));

        // Sliders
        JSlider volume = new JSlider(0, 100, 64);
        JLabel volumeValue = Tessel.label("64%", TextStyle.SECONDARY);
        volume.addChangeListener(e -> volumeValue.setText(volume.getValue() + "%"));
        JPanel volumeHeader = Layouts.panel(new BorderLayout());
        volumeHeader.add(Tessel.label("Volume", TextStyle.BODY), BorderLayout.WEST);
        volumeHeader.add(volumeValue, BorderLayout.EAST);

        JSlider ticks = new JSlider(0, 10, 4);
        ticks.setMajorTickSpacing(1);
        ticks.setSnapToTicks(true);
        ticks.setPaintTicks(true);

        JSlider disabledSlider = new JSlider(0, 100, 30);
        disabledSlider.setEnabled(false);

        JPanel horizontal = Layouts.stack(8,
                volumeHeader, volume,
                Tessel.label("With ticks", TextStyle.BODY), ticks,
                Tessel.label("Disabled", TextStyle.BODY), disabledSlider);

        JSlider v1 = new JSlider(JSlider.VERTICAL, 0, 100, 70);
        JSlider v2 = new JSlider(JSlider.VERTICAL, 0, 100, 30);
        v1.setPreferredSize(new java.awt.Dimension(28, 170));
        v2.setPreferredSize(new java.awt.Dimension(28, 170));
        JPanel vertical = Layouts.row(v1, v2);
        vertical.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 40, 0, 16));

        JPanel sliders = Layouts.panel(new BorderLayout());
        sliders.add(horizontal, BorderLayout.CENTER);
        sliders.add(vertical, BorderLayout.EAST);
        section("Sliders", sliders);
    }
}
