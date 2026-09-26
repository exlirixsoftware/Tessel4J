package net.tessel.laf.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.UIUtils;

/** A collapsible section with a header row and a chevron. */
public class Expander extends JPanel {

    private final Header header = new Header();
    private final JPanel body = new JPanel(new BorderLayout());
    private String title;
    private boolean expanded;
    private double chevronAngle;
    private final Timer rotation = new Timer(15, e -> stepRotation());

    public Expander(String title, Component content) {
        this(title, content, false);
    }

    public Expander(String title, Component content, boolean expanded) {
        super(new BorderLayout());
        this.title = title;
        this.expanded = expanded;
        this.chevronAngle = expanded ? 180 : 0;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        if (content != null) body.add(content, BorderLayout.CENTER);
        body.setVisible(expanded);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        body.setVisible(expanded);
        rotation.start();
        revalidate();
        repaint();
        firePropertyChange("expanded", !expanded, expanded);
    }

    public void setTitle(String title) {
        this.title = title;
        header.repaint();
    }

    public String getTitle() {
        return title;
    }

    public void setContent(Component content) {
        body.removeAll();
        if (content != null) body.add(content, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void stepRotation() {
        double target = expanded ? 180 : 0;
        chevronAngle += Math.signum(target - chevronAngle) * 20;
        if (Math.abs(target - chevronAngle) < 20) {
            chevronAngle = target;
            rotation.stop();
        }
        header.repaint();
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
            if (expanded) g2.fillRect(1, header.getHeight() + 1, getWidth() - 2, 1);
        } finally {
            g2.dispose();
        }
    }

    private final class Header extends JComponent {
        private boolean hover;
        private final Icon chevron = TesselIcons.chevron(TesselIcons.Direction.DOWN, 16, pal -> pal.text);

        Header() {
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setExpanded(!expanded);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggle");
            getActionMap().put("toggle", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    setExpanded(!expanded);
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Fonts.semibold(14));
                g2.setColor(p.text);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(title == null ? "" : title, 16, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());

                int bx = getWidth() - 12 - 28;
                int by = (getHeight() - 28) / 2;
                if (hover) {
                    g2.setColor(p.controlHover);
                    UIUtils.fillRound(g2, bx, by, 28, 28, 4);
                }
                AffineTransform old = g2.getTransform();
                g2.rotate(Math.toRadians(chevronAngle), bx + 14, by + 14);
                chevron.paintIcon(this, g2, bx + 6, by + 6);
                g2.setTransform(old);

                if (UIUtils.isFocusVisible(this)) UIUtils.paintFocusRing(g2, 1, 1, getWidth() - 2, getHeight() - 2, 7);
            } finally {
                g2.dispose();
            }
        }
    }
}
