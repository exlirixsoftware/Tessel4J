package net.tessel.laf.components;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.UIUtils;

/**
 * An inline, persistent notification with a severity icon, title, message, optional action and close button.
 * Listeners registered with {@link #addActionListener(ActionListener)} receive "closed" when it is closed.
 */
public class InfoBar extends JPanel {

    private Severity severity;
    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = Tessel.label("", TextStyle.BODY_STRONG);
    private final TextBlock messageText = new TextBlock("", TextStyle.SECONDARY);
    private final JPanel actionHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final JButton closeButton = new JButton();
    private final EventListenerList actionListeners = new EventListenerList();

    private boolean elevated;
    private boolean surface;
    private float alpha = 1f;

    public InfoBar(Severity severity, String title, String message) {
        super(new BorderLayout(12, 0));
        setOpaque(false);

        iconLabel.setVerticalAlignment(JLabel.TOP);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(0f);
        messageText.setAlignmentX(0f);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(messageText);

        actionHolder.setOpaque(false);

        closeButton.setIcon(TesselIcons.close(14));
        closeButton.setToolTipText("Close");
        closeButton.setFocusable(false);
        Tessel.style(closeButton, ButtonStyle.ICON);
        closeButton.setPreferredSize(new java.awt.Dimension(28, 28));
        closeButton.addActionListener(e -> close());

        JPanel east = new JPanel(new BorderLayout(8, 0));
        east.setOpaque(false);
        east.add(actionHolder, BorderLayout.CENTER);
        JPanel closeHolder = new JPanel(new BorderLayout());
        closeHolder.setOpaque(false);
        closeHolder.add(closeButton, BorderLayout.NORTH);
        east.add(closeHolder, BorderLayout.EAST);

        add(iconLabel, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(east, BorderLayout.EAST);

        setSeverity(severity);
        setTitle(title);
        setMessage(message);
        updateBorder();
    }

    public InfoBar(String title, String message) {
        this(Severity.INFORMATIONAL, title, message);
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity == null ? Severity.INFORMATIONAL : severity;
        iconLabel.setIcon(TesselIcons.severity(this.severity, 16));
        repaint();
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
        titleLabel.setVisible(title != null && !title.isEmpty());
    }

    public String getTitle() {
        return titleLabel.getText();
    }

    public void setMessage(String message) {
        messageText.setText(message == null ? "" : message);
        messageText.setVisible(message != null && !message.isEmpty());
    }

    public String getMessage() {
        return messageText.getText();
    }

    /** A component shown on the right, e.g. a button. */
    public void setAction(JComponent action) {
        actionHolder.removeAll();
        if (action != null) actionHolder.add(action);
        revalidate();
        repaint();
    }

    public void setClosable(boolean closable) {
        closeButton.setVisible(closable);
    }

    public boolean isClosable() {
        return closeButton.isVisible();
    }

    public boolean isOpen() {
        return isVisible();
    }

    public void setOpen(boolean open) {
        if (open == isVisible()) return;
        setVisible(open);
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
        if (!open) fireClosed();
    }

    /** Closes the info bar and notifies listeners. */
    public void close() {
        setOpen(false);
    }

    /** Draws a drop shadow (used by {@link Snackbar}). */
    public void setElevated(boolean elevated) {
        this.elevated = elevated;
        updateBorder();
    }

    /** Uses the surface color instead of the severity tint (used by {@link Snackbar}). */
    public void setSurfaceBackground(boolean surface) {
        this.surface = surface;
        repaint();
    }

    /** Opacity used for fade animations. */
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        repaint();
    }

    public float getAlpha() {
        return alpha;
    }

    public void addActionListener(ActionListener l) {
        actionListeners.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.remove(ActionListener.class, l);
    }

    private void fireClosed() {
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "closed");
        for (ActionListener l : actionListeners.getListeners(ActionListener.class)) l.actionPerformed(e);
    }

    private Insets shadowInsets() {
        return elevated ? new Insets(Card.SHADOW - 2, Card.SHADOW, Card.SHADOW + 2, Card.SHADOW) : new Insets(0, 0, 0, 0);
    }

    private void updateBorder() {
        Insets s = shadowInsets();
        setBorder(BorderFactory.createEmptyBorder(12 + s.top, 16 + s.left, 12 + s.bottom, 10 + s.right));
        revalidate();
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 1f) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            super.paint(g2);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Palette p = Tessel.palette();
        Insets s = shadowInsets();
        double x = s.left;
        double y = s.top;
        double w = getWidth() - s.left - s.right;
        double h = getHeight() - s.top - s.bottom;
        Graphics2D g2 = UIUtils.create(g);
        try {
            if (elevated) UIUtils.paintShadow(g2, x, y, w, h, 8, Card.SHADOW - 1);
            Color bg = surface ? p.surface : severity.subtleColor(p);
            g2.setColor(bg);
            UIUtils.fillRound(g2, x, y, w, h, 8);
            g2.setColor(p.border);
            UIUtils.strokeRound(g2, x, y, w, h, 8, 1f);
        } finally {
            g2.dispose();
        }
    }

}
