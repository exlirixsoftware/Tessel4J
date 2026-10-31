package net.tessel.laf.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.UIUtils;

/**
 * A button with a primary action and a drop-down menu of related actions.
 *
 * <pre>{@code
 * SplitButton save = new SplitButton("Save");
 * save.addActionListener(e -> save());                       // the main part
 * save.getMenu().add(new JMenuItem("Save as..."));           // the arrow part
 * }</pre>
 *
 * It takes a {@link ButtonStyle} like any other button. {@link DropDownButton} is the variant whose whole
 * surface opens the menu.
 */
public class SplitButton extends JButton {

    private static final int ARROW_WIDTH = 30;

    private JPopupMenu menu = new JPopupMenu();
    private Supplier<JPopupMenu> menuSupplier;
    private boolean split = true;
    private boolean overArrow;
    private boolean menuOpen;
    private final PopupMenuListener menuListener = new PopupMenuListener() {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menuOpen = true;
            repaint();
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            menuOpen = false;
            repaint();
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            menuOpen = false;
            repaint();
        }
    };

    public SplitButton() {
        this(null, null);
    }

    public SplitButton(String text) {
        this(text, null);
    }

    public SplitButton(String text, Icon icon) {
        super(text, icon);
        applyMargin();
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "tessel.openMenu");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "tessel.openMenu");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), "tessel.openMenu");
        getActionMap().put("tessel.openMenu", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showMenu();
            }
        });
        menu.addPopupMenuListener(menuListener);
    }

    // ------------------------------------------------------------------ API

    /** The menu opened by the arrow part. Fill it with menu items, or replace it with {@link #setMenu}. */
    public JPopupMenu getMenu() {
        return menu;
    }

    public void setMenu(JPopupMenu menu) {
        this.menu.removePopupMenuListener(menuListener);
        this.menu = menu == null ? new JPopupMenu() : menu;
        this.menu.addPopupMenuListener(menuListener);
    }

    /** Builds the menu each time it opens (use this for content that changes). */
    public void setMenuSupplier(Supplier<JPopupMenu> supplier) {
        this.menuSupplier = supplier;
    }

    /** Adds an item to the menu. */
    public JMenuItem addMenuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        menu.add(item);
        return item;
    }

    public JMenuItem addMenuItem(Action action) {
        return menu.add(action);
    }

    /** True (the default) when the arrow is a separate part; false when the whole button opens the menu. */
    public boolean isSplit() {
        return split;
    }

    public void setSplit(boolean split) {
        this.split = split;
        applyMargin();
        repaint();
    }

    /** Opens the menu below the button (or above it when there is no room). */
    public void showMenu() {
        JPopupMenu m = menuSupplier != null ? menuSupplier.get() : menu;
        if (m == null || m.getComponentCount() == 0 && menuSupplier == null) return;
        if (m != menu) {
            m.removePopupMenuListener(menuListener);
            m.addPopupMenuListener(menuListener);
        }
        java.awt.Dimension size = m.getPreferredSize();
        int x = 0;
        int y = getHeight() + 2;
        java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
        if (isShowing() && gc != null) {
            java.awt.Point onScreen = getLocationOnScreen();
            Rectangle screen = gc.getBounds();
            // Right-align when the menu would run off the screen, open upwards when there is no room below.
            if (onScreen.x + size.width > screen.x + screen.width) x = getWidth() - size.width;
            if (onScreen.y + y + size.height > screen.y + screen.height) y = -size.height - 2;
        }
        m.show(this, x, y);
    }

    private void applyMargin() {
        Insets base = javax.swing.UIManager.getInsets("Button.margin");
        if (base == null) base = new Insets(6, 14, 6, 14);
        setMargin(new Insets(base.top, base.left, base.bottom, base.right + ARROW_WIDTH - (split ? 0 : 6)));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (menu != null) applyMargin();
    }

    // ------------------------------------------------------------------ interaction

    private Rectangle arrowBounds() {
        return new Rectangle(getWidth() - ARROW_WIDTH, 0, ARROW_WIDTH, getHeight());
    }

    private boolean inArrow(MouseEvent e) {
        return !split || arrowBounds().contains(e.getPoint());
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        boolean leftClick = javax.swing.SwingUtilities.isLeftMouseButton(e);
        if (isEnabled() && leftClick && inArrow(e)) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                requestFocusInWindow();
                if (menuOpen) menu.setVisible(false);
                else showMenu();
                e.consume();
                return;
            }
            if (e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MouseEvent.MOUSE_CLICKED) {
                e.consume();
                return;
            }
        }
        super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_EXITED && overArrow) {
            overArrow = false;
            repaint();
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        boolean over = split && arrowBounds().contains(e.getPoint());
        if (over != overArrow) {
            overArrow = over;
            repaint();
        }
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            UIUtils.applyEnabledAlpha(g2, this);
            Rectangle arrow = arrowBounds();
            ButtonStyle style = style();
            boolean filled = style == ButtonStyle.ACCENT || style == ButtonStyle.DANGER;
            java.awt.Color fg = foreground(p, style);

            if (split) {
                // Hover highlight of the arrow part, then the divider.
                if ((overArrow || menuOpen) && isEnabled()) {
                    g2.setColor(filled ? Palette.withAlpha(java.awt.Color.WHITE, 40) : p.controlPressed);
                    java.awt.Shape old = g2.getClip();
                    g2.clipRect(arrow.x, 0, arrow.width, getHeight());
                    UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), 6);
                    g2.setClip(old);
                }
                g2.setColor(Palette.withAlpha(fg, 60));
                g2.fillRect(arrow.x, 7, 1, Math.max(0, getHeight() - 14));
            }
            TesselIcons.chevron(TesselIcons.Direction.DOWN, 16, pal -> fg)
                    .paintIcon(this, g2, arrow.x + (arrow.width - 16) / 2, (getHeight() - 16) / 2);
        } finally {
            g2.dispose();
        }
    }

    private java.awt.Color foreground(Palette p, ButtonStyle style) {
        java.awt.Color user = getForeground();
        if (user != null && !(user instanceof javax.swing.plaf.UIResource)) return user;
        switch (style) {
            case ACCENT:
                return p.onAccent;
            case DANGER:
                return java.awt.Color.WHITE;
            case OUTLINE:
            case LINK:
                return p.accent;
            default:
                return p.text;
        }
    }

    private ButtonStyle style() {
        Object s = getClientProperty(Tessel.BUTTON_STYLE);
        return s instanceof ButtonStyle ? (ButtonStyle) s : ButtonStyle.STANDARD;
    }
}
