package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Avatar;
import net.tessel.laf.components.Badge;
import net.tessel.laf.components.Card;
import net.tessel.laf.components.Expander;
import net.tessel.laf.components.SettingsCard;
import net.tessel.laf.components.TextBlock;
import net.tessel.laf.components.ToggleSwitch;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

public class LayoutPage extends Page {

    public LayoutPage() {
        super("Layout", "Cards, settings cards, expanders, titled borders and avatars.");

        subtitle("Cards");
        Card elevated = new Card("Elevated card", new TextBlock("setElevated(true) adds a soft drop shadow.", TextStyle.SECONDARY));
        elevated.setElevated(true);
        add(Layouts.columns(16,
                new Card("Flat card", new TextBlock("A surface with a border, header and padding.", TextStyle.SECONDARY)),
                elevated,
                mediaCard()));

        subtitle("Settings cards");
        JComboBox<String> language = new JComboBox<>(new String[]{"English", "Deutsch", "Română"});
        language.setPreferredSize(new Dimension(160, 32));
        add(Layouts.stack(4,
                new SettingsCard("Notifications", "Receive alerts for mentions and replies", Symbol.RINGER, new ToggleSwitch(null, true)),
                new SettingsCard("Language", "Choose the language used across the app", Symbol.GLOBE, language),
                new SettingsCard("Account", "Sign in on another device", Symbol.CONTACT, new JButton("Manage"))));

        subtitle("Expanders & titled borders");
        add(new Expander("What is Tessel?",
                new TextBlock("Tessel is a Swing look and feel that restyles the standard controls and adds modern components on top.", TextStyle.SECONDARY), true));
        add(new Expander("How do I change the accent color?",
                new TextBlock("Call Tessel.setAccent(color), or pass it to Tessel.setup(theme, accent).", TextStyle.SECONDARY)));

        JPanel group = Layouts.columns(16, Tessel.placeholder(new JTextField(), "Street"), Tessel.placeholder(new JTextField(), "City"));
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Shipping address"), BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        add(group);

        subtitle("Avatars");
        JPanel sizes = Layouts.row(new Avatar("Ada Lovelace", 24), new Avatar("Grace Hopper", 32), new Avatar("Alan Turing", 40),
                new Avatar("Margaret Hamilton", 56), new Avatar("Linus", 72));
        JPanel person = Layouts.panel(null);
        person.setLayout(new BoxLayout(person, BoxLayout.X_AXIS));
        person.add(new Avatar("Barbara Liskov", 40));
        person.add(Box.createHorizontalStrut(12));
        person.add(Layouts.stack(0, Tessel.label("Barbara Liskov", TextStyle.BODY_STRONG), Tessel.label("Architect · Platform", TextStyle.CAPTION)));
        person.add(Box.createHorizontalStrut(16));
        person.add(new Badge("Admin", Badge.Kind.ACCENT));
        person.add(Box.createHorizontalGlue());
        add(new Card(null, Layouts.stack(0, sizes, new javax.swing.JSeparator(), person)));
    }

    private static Card mediaCard() {
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x06B6D4), getWidth(), getHeight(), Tessel.palette().accent));
                    double r = 8;
                    Path2D top = new Path2D.Double();
                    top.moveTo(0, getHeight());
                    top.lineTo(0, r);
                    top.quadTo(0, 0, r, 0);
                    top.lineTo(getWidth() - r, 0);
                    top.quadTo(getWidth(), 0, getWidth(), r);
                    top.lineTo(getWidth(), getHeight());
                    top.closePath();
                    g2.fill(top);
                } finally {
                    g2.dispose();
                }
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(100, 72));

        JPanel text = Layouts.stack(2, Tessel.label("Media card", TextStyle.BODY_STRONG), Tessel.label("setPadding(0) with custom content", TextStyle.CAPTION));
        text.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        JPanel body = Layouts.panel(new BorderLayout());
        body.add(banner, BorderLayout.NORTH);
        body.add(text, BorderLayout.CENTER);

        Card card = new Card(null, body);
        card.setPadding(0, 0, 0, 0);
        return card;
    }
}
