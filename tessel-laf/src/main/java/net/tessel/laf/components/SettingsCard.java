package net.tessel.laf.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

/** A settings row: icon, header, description and a control on the right. */
public class SettingsCard extends JPanel {

    private final JLabel iconLabel = new JLabel();
    private final JLabel headerLabel = Tessel.label("", TextStyle.BODY);
    private final JLabel descriptionLabel = Tessel.label("", TextStyle.CAPTION);
    private final JPanel trailing = new JPanel(new GridBagLayout());

    public SettingsCard(String header, String description, Symbol icon, JComponent control) {
        this(header, description, icon == null ? null : new FontIcon(icon, 20), control);
    }

    public SettingsCard(String header, String description, Icon icon, JComponent control) {
        super(new BorderLayout(16, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 16));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(headerLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(descriptionLabel);
        text.add(Box.createVerticalGlue());

        trailing.setOpaque(false);

        add(iconLabel, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(trailing, BorderLayout.EAST);

        setHeader(header);
        setDescription(description);
        setIcon(icon);
        setControl(control);
    }

    public void setHeader(String header) {
        headerLabel.setText(header);
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
        descriptionLabel.setVisible(description != null && !description.isEmpty());
    }

    public void setIcon(Icon icon) {
        iconLabel.setIcon(icon);
        iconLabel.setVisible(icon != null);
    }

    public void setControl(JComponent control) {
        trailing.removeAll();
        if (control != null) trailing.add(control);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 68);
        return d;
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            g2.setColor(p.surface);
            UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 8);
            g2.setColor(p.border);
            UIUtils.strokeRound(g2, 0, 0, getWidth(), getHeight(), 8, 1f);
        } finally {
            g2.dispose();
        }
    }
}
