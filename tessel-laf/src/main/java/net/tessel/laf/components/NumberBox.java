package net.tessel.laf.components;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.UIResource;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.TesselInputBorder;
import net.tessel.laf.ui.UIUtils;

/**
 * A number input with step buttons, arrow-key / mouse-wheel stepping, range validation and optional
 * arithmetic expressions ("12 * 3 + 4").
 *
 * <pre>{@code
 * NumberBox quantity = new NumberBox(1, 0, 99);
 * quantity.setSuffix(" pcs");
 * quantity.addChangeListener(e -> System.out.println(quantity.getValue()));
 * }</pre>
 *
 * The value is committed on Enter, when the box loses focus and when it is stepped. Text that cannot be
 * evaluated flashes the outline red and restores the previous value.
 */
public class NumberBox extends JPanel {

    public enum SpinButtonPlacement {
        /** "-" and "+" buttons to the right of the text. */
        INLINE,
        /** A stacked up / down pair to the right of the text. */
        COMPACT,
        /** No buttons; the arrow keys and the mouse wheel still step the value. */
        HIDDEN
    }

    public static final String VALUE_PROPERTY = "value";

    private final JTextField field = new JTextField();
    private final StepButton minus = new StepButton(StepButton.MINUS);
    private final StepButton plus = new StepButton(StepButton.PLUS);
    private final StepButton up = new StepButton(StepButton.UP);
    private final StepButton down = new StepButton(StepButton.DOWN);
    private final EventListenerList listeners = new EventListenerList();
    private final Timer invalidTimer = new Timer(900, e -> setInvalid(false));

    private double value;
    private double minimum = Double.NEGATIVE_INFINITY;
    private double maximum = Double.POSITIVE_INFINITY;
    private double smallChange = 1;
    private double largeChange = 10;
    private boolean wrap;
    private boolean acceptsExpression = true;
    private boolean nullable;
    private boolean invalid;
    private String prefix = "";
    private String suffix = "";
    private int minFraction;
    private int maxFraction = 3;
    private SpinButtonPlacement placement = SpinButtonPlacement.INLINE;

    public NumberBox() {
        this(0);
    }

    public NumberBox(double value) {
        super(null);
        setOpaque(false);
        setBorder(new TesselInputBorder(1, 10, 1, 3));
        setLayout(new NumberLayout());
        invalidTimer.setRepeats(false);

        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setColumns(8);
        field.addActionListener(e -> commit());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commit();
            }
        });
        bindKey(KeyEvent.VK_UP, 0, +1, false);
        bindKey(KeyEvent.VK_DOWN, 0, -1, false);
        bindKey(KeyEvent.VK_PAGE_UP, 0, +1, true);
        bindKey(KeyEvent.VK_PAGE_DOWN, 0, -1, true);
        field.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "tessel.revert");
        field.getActionMap().put("tessel.revert", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                updateText();
                field.selectAll();
            }
        });
        field.addMouseWheelListener(e -> {
            if (!field.isEnabled() || !UIUtils.hasFocusWithin(NumberBox.this)) return;
            step(-e.getWheelRotation(), false);
        });
        UIUtils.installHover(field);
        UIUtils.installHover(this);

        add(field);
        add(minus);
        add(plus);
        add(up);
        add(down);
        applyPlacement();
        this.value = clamp(value);
        updateText();
    }

    public NumberBox(double value, double minimum, double maximum) {
        this(0);
        setMinimum(minimum);
        setMaximum(maximum);
        setValue(value);
    }

    private void bindKey(int key, int modifiers, int direction, boolean large) {
        String name = "tessel.step." + key;
        field.getInputMap().put(KeyStroke.getKeyStroke(key, modifiers), name);
        field.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                step(direction, large);
            }
        });
    }

    // ------------------------------------------------------------------ value

    /** The current value, or NaN when the box is empty (see {@link #setNullable(boolean)}). */
    public double getValue() {
        return value;
    }

    public int getIntValue() {
        return (int) Math.round(value);
    }

    public boolean isEmpty() {
        return Double.isNaN(value);
    }

    /** Sets the value, clamped to the range. NaN empties a nullable box. */
    public void setValue(double newValue) {
        double v = Double.isNaN(newValue) ? (nullable ? Double.NaN : value) : clamp(newValue);
        boolean changed = Double.compare(v, value) != 0;
        double old = value;
        value = v;
        updateText();
        if (changed) {
            firePropertyChange(VALUE_PROPERTY, old, v);
            fireChange();
        }
    }

    private double clamp(double v) {
        if (Double.isNaN(v)) return v;
        return Math.max(minimum, Math.min(maximum, v));
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
        setValue(value);
        updateButtons();
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
        setValue(value);
        updateButtons();
    }

    /** The step of the buttons, the arrow keys and the mouse wheel. */
    public double getSmallChange() {
        return smallChange;
    }

    public void setSmallChange(double smallChange) {
        this.smallChange = smallChange;
    }

    /** The step of Page Up / Page Down. */
    public double getLargeChange() {
        return largeChange;
    }

    public void setLargeChange(double largeChange) {
        this.largeChange = largeChange;
    }

    public boolean isWrapEnabled() {
        return wrap;
    }

    /** When true, stepping past one end of the range continues at the other end. */
    public void setWrapEnabled(boolean wrap) {
        this.wrap = wrap;
        updateButtons();
    }

    public boolean isAcceptsExpression() {
        return acceptsExpression;
    }

    /** Allows typing arithmetic such as {@code 3 * (4 + 1)}. */
    public void setAcceptsExpression(boolean acceptsExpression) {
        this.acceptsExpression = acceptsExpression;
    }

    public boolean isNullable() {
        return nullable;
    }

    /** When true an empty text is a valid value (NaN) instead of being restored. */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
        updateText();
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix == null ? "" : suffix;
        updateText();
    }

    /** Number of fraction digits shown: at least {@code min}, at most {@code max}. */
    public void setFractionDigits(int min, int max) {
        this.minFraction = Math.max(0, min);
        this.maxFraction = Math.max(this.minFraction, max);
        updateText();
    }

    public SpinButtonPlacement getSpinButtonPlacement() {
        return placement;
    }

    public void setSpinButtonPlacement(SpinButtonPlacement placement) {
        this.placement = placement == null ? SpinButtonPlacement.INLINE : placement;
        applyPlacement();
    }

    public String getPlaceholder() {
        Object p = field.getClientProperty(Tessel.PLACEHOLDER);
        return p instanceof String ? (String) p : null;
    }

    /** Hint text shown when the box is empty. */
    public void setPlaceholder(String text) {
        Tessel.placeholder(field, text);
    }

    /** The editor, e.g. to set its font or accessible name. */
    public JTextField getEditor() {
        return field;
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(ChangeListener.class, l);
    }

    private void fireChange() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener l : listeners.getListeners(ChangeListener.class)) l.stateChanged(event);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        updateButtons();
        repaint();
    }

    @Override
    public void requestFocus() {
        field.requestFocus();
    }

    @Override
    public boolean requestFocusInWindow() {
        return field.requestFocusInWindow();
    }

    // ------------------------------------------------------------------ stepping & committing

    private void step(int direction, boolean large) {
        commit();
        if (isEmpty()) {
            setValue(Double.isInfinite(minimum) ? 0 : minimum);
            return;
        }
        double delta = (large ? largeChange : smallChange) * direction;
        double next = value + delta;
        if (wrap && !Double.isInfinite(minimum) && !Double.isInfinite(maximum)) {
            if (next > maximum) next = minimum;
            else if (next < minimum) next = maximum;
        }
        // Avoid 0.30000000000000004 after repeated 0.1 steps.
        next = Math.round(next * 1e9) / 1e9;
        setValue(next);
        field.selectAll();
    }

    private void commit() {
        String text = field.getText();
        String plain = strip(text);
        if (plain.isEmpty()) {
            if (nullable) setValue(Double.NaN);
            else updateText();
            return;
        }
        double parsed;
        try {
            parsed = acceptsExpression ? new Parser(normalize(plain)).parse() : Double.parseDouble(normalize(plain));
        } catch (RuntimeException ex) {
            setInvalid(true);
            updateText();
            return;
        }
        if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
            setInvalid(true);
            updateText();
            return;
        }
        setValue(parsed);
        updateText();
    }

    private String strip(String text) {
        String s = text.trim();
        if (!prefix.isEmpty() && s.startsWith(prefix.trim())) s = s.substring(prefix.trim().length());
        if (!suffix.isEmpty() && s.endsWith(suffix.trim())) s = s.substring(0, s.length() - suffix.trim().length());
        return s.trim();
    }

    private static final Pattern NUMBER = Pattern.compile("[0-9][0-9.,]*|[.,][0-9]+");

    /** Rewrites every number of the text with '.' as the decimal separator and no grouping separators. */
    private static String normalize(String s) {
        String t = s.replace(" ", "").replace(" ", "");
        Matcher m = NUMBER.matcher(t);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(normalizeNumber(m.group())));
        m.appendTail(sb);
        return sb.toString();
    }

    private static String normalizeNumber(String token) {
        int lastDot = token.lastIndexOf('.');
        int lastComma = token.lastIndexOf(',');
        if (lastDot < 0 && lastComma < 0) return token;
        char decimal;
        if (lastDot >= 0 && lastComma >= 0) {
            decimal = lastDot > lastComma ? '.' : ',';
        } else {
            char sep = lastDot >= 0 ? '.' : ',';
            int first = token.indexOf(sep);
            int count = 0;
            for (int i = 0; i < token.length(); i++) {
                if (token.charAt(i) == sep) count++;
            }
            char grouping = DecimalFormatSymbols.getInstance(Locale.getDefault()).getGroupingSeparator();
            boolean groupingLike = count > 1 || (sep == grouping && first > 0 && token.length() - first - 1 == 3);
            decimal = groupingLike ? 0 : sep;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c)) out.append(c);
            else if (c == decimal) out.append('.');
        }
        return out.toString();
    }

    private void updateText() {
        if (Double.isNaN(value)) {
            field.setText("");
        } else {
            DecimalFormat f = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.getDefault()));
            f.setMinimumFractionDigits(minFraction);
            f.setMaximumFractionDigits(maxFraction);
            field.setText(prefix + f.format(value) + suffix);
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean enabled = isEnabled();
        boolean canDown = enabled && (wrap || isEmpty() || value > minimum);
        boolean canUp = enabled && (wrap || isEmpty() || value < maximum);
        minus.setEnabled(canDown);
        down.setEnabled(canDown);
        plus.setEnabled(canUp);
        up.setEnabled(canUp);
    }

    private void applyPlacement() {
        minus.setVisible(placement == SpinButtonPlacement.INLINE);
        plus.setVisible(placement == SpinButtonPlacement.INLINE);
        up.setVisible(placement == SpinButtonPlacement.COMPACT);
        down.setVisible(placement == SpinButtonPlacement.COMPACT);
        revalidate();
        repaint();
    }

    private void setInvalid(boolean invalid) {
        this.invalid = invalid;
        if (invalid) invalidTimer.restart();
        repaint();
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, this);
            g2.setColor(Tessel.palette().inputBackground);
            UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), TesselInputBorder.RADIUS);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (invalid) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(Tessel.palette().danger);
                UIUtils.strokeRound(g2, 0, 0, getWidth(), getHeight(), TesselInputBorder.RADIUS, 2f);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ layout

    private final class NumberLayout implements LayoutManager {
        private int buttonsWidth() {
            switch (placement) {
                case INLINE:
                    return 2 * 28;
                case COMPACT:
                    return 24;
                default:
                    return 0;
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            Dimension f = field.getPreferredSize();
            return new Dimension(in.left + f.width + 6 + buttonsWidth() + in.right, Math.max(32, in.top + Math.max(f.height, 28) + in.bottom));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            return new Dimension(in.left + 40 + buttonsWidth() + in.right, 32);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int h = parent.getHeight() - in.top - in.bottom;
            int right = parent.getWidth() - in.right;
            switch (placement) {
                case INLINE: {
                    int by = in.top + (h - 26) / 2;
                    plus.setBounds(right - 28, by, 28, 26);
                    minus.setBounds(right - 56, by, 28, 26);
                    right -= 56 + 4;
                    break;
                }
                case COMPACT: {
                    int bh = Math.min(14, h / 2);
                    int by = in.top + (h - 2 * bh) / 2;
                    up.setBounds(right - 22, by, 22, bh);
                    down.setBounds(right - 22, by + bh, 22, bh);
                    right -= 22 + 4;
                    break;
                }
                default:
                    break;
            }
            field.setBounds(in.left, in.top, Math.max(0, right - in.left), h);
        }
    }

    /** A flat step button; holding it down repeats. */
    private final class StepButton extends JButton implements UIResource {
        static final int MINUS = 0;
        static final int PLUS = 1;
        static final int UP = 2;
        static final int DOWN = 3;

        private final int kind;
        private final Timer repeat;

        StepButton(int kind) {
            this.kind = kind;
            setFocusable(false);
            setRequestFocusEnabled(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setRolloverEnabled(true);
            int direction = (kind == PLUS || kind == UP) ? +1 : -1;
            repeat = new Timer(70, e -> {
                if (isEnabled()) step(direction, false);
            });
            repeat.setInitialDelay(400);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled() || !javax.swing.SwingUtilities.isLeftMouseButton(e)) return;
                    step(direction, false);
                    repeat.restart();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    repeat.stop();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    repeat.stop();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                UIUtils.applyEnabledAlpha(g2, this);
                boolean pressed = getModel().isArmed() && getModel().isPressed();
                boolean hover = getModel().isRollover() && isEnabled();
                if (pressed || hover) {
                    g2.setColor(pressed ? p.controlPressed : p.controlHover);
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 4);
                }
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0;
                g2.setColor(hover ? p.text : p.textSecondary);
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (kind == MINUS || kind == PLUS) {
                    g2.draw(new Line2D.Double(cx - 5, cy, cx + 5, cy));
                    if (kind == PLUS) g2.draw(new Line2D.Double(cx, cy - 5, cx, cy + 5));
                } else {
                    Path2D path = new Path2D.Double();
                    double dir = kind == UP ? -1 : 1;
                    path.moveTo(cx - 3.5, cy - dir * 1.5);
                    path.lineTo(cx, cy + dir * 1.5);
                    path.lineTo(cx + 3.5, cy - dir * 1.5);
                    g2.draw(path);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ expression parser

    /** Recursive-descent evaluation of + - * / % ^ and parentheses. */
    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        double parse() {
            double v = expression();
            if (pos != s.length()) throw new IllegalArgumentException("Unexpected '" + s.charAt(pos) + "'");
            return v;
        }

        private double expression() {
            double v = term();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '+') {
                    pos++;
                    v += term();
                } else if (c == '-') {
                    pos++;
                    v -= term();
                } else {
                    break;
                }
            }
            return v;
        }

        private double term() {
            double v = power();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '*' || c == 'x' || c == '×') {
                    pos++;
                    v *= power();
                } else if (c == '/' || c == '÷') {
                    pos++;
                    v /= power();
                } else if (c == '%') {
                    pos++;
                    v %= power();
                } else {
                    break;
                }
            }
            return v;
        }

        private double power() {
            double base = unary();
            if (pos < s.length() && s.charAt(pos) == '^') {
                pos++;
                return Math.pow(base, power());
            }
            return base;
        }

        private double unary() {
            if (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '-') {
                    pos++;
                    return -unary();
                }
                if (c == '+') {
                    pos++;
                    return unary();
                }
            }
            return primary();
        }

        private double primary() {
            if (pos >= s.length()) throw new IllegalArgumentException("Unexpected end");
            if (s.charAt(pos) == '(') {
                pos++;
                double v = expression();
                if (pos >= s.length() || s.charAt(pos) != ')') throw new IllegalArgumentException("Missing )");
                pos++;
                return v;
            }
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E') && pos > start) {
                int save = pos;
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                int digits = pos;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                if (digits == pos) pos = save;
            }
            if (start == pos) throw new IllegalArgumentException("Number expected");
            return Double.parseDouble(s.substring(start, pos));
        }
    }
}
