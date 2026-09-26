package net.tessel.laf.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.UIUtils;

/** An animated on/off switch. Optional {@link #setOnText(String) on}/{@link #setOffText(String) off} labels. */
public class ToggleSwitch extends JToggleButton {

    private float progress;
    private String onText;
    private String offText;
    private final Timer animation = new Timer(15, e -> step());

    public ToggleSwitch() {
        this(null, false);
    }

    public ToggleSwitch(String text) {
        this(text, false);
    }

    public ToggleSwitch(String text, boolean selected) {
        super(text, selected);
        progress = selected ? 1f : 0f;
        setIcon(new SwitchIcon());
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setIconTextGap(12);
        setHorizontalAlignment(SwingConstants.LEADING);
        addItemListener(e -> {
            updateText();
            animation.start();
        });
    }

    public void setOnText(String onText) {
        this.onText = onText;
        updateText();
    }

    public void setOffText(String offText) {
        this.offText = offText;
        updateText();
    }

    private void updateText() {
        String t = isSelected() ? onText : offText;
        if (t != null) setText(t);
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        if (!isShowing()) {
            progress = b ? 1f : 0f;
            animation.stop();
        }
    }

    private void step() {
        float target = isSelected() ? 1f : 0f;
        float delta = 1f / 9f;
        progress = target > progress ? Math.min(target, progress + delta) : Math.max(target, progress - delta);
        if (progress == target) animation.stop();
        repaint();
    }

    private final class SwitchIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Palette p = Tessel.palette();
            boolean hover = getModel().isRollover() && isEnabled();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.translate(x, y);
                float t = progress;
                Color offFill = hover ? p.controlHover : p.inputBackground;
                Color onFill = hover ? p.accentHover : p.accent;
                g2.setColor(t >= 0.5f ? onFill : offFill);
                UIUtils.fillRound(g2, 0, 0, 40, 20, 10);
                g2.setColor(t >= 0.5f ? onFill : p.textSecondary);
                UIUtils.strokeRound(g2, 0, 0, 40, 20, 10, 1.5f);

                double knob = 12;
                double kx = 4 + t * (40 - 8 - knob);
                g2.setColor(Palette.mix(p.textSecondary, p.onAccent, t));
                g2.fill(new Ellipse2D.Double(kx, 4, knob, knob));

                if (UIUtils.isFocusVisible(ToggleSwitch.this)) UIUtils.paintFocusRing(g2, -3, -3, 46, 26, 13);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 40;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }
    }
}
