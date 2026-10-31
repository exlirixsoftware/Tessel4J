package net.tessel.laf.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicToolBarUI;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.icons.TesselIcons;

/**
 * Tool bars with a rounded surface, a grip when floatable, and an overflow menu: buttons that no longer
 * fit move into a "more" popup. Turn the overflow off with
 * {@code toolBar.putClientProperty(Tessel.TOOLBAR_OVERFLOW, false)}.
 *
 * <p>Only buttons and separators move into the overflow menu; other components (text fields, combo
 * boxes, ...) always stay in the bar.
 */
public class TesselToolBarUI extends BasicToolBarUI {

    private static final String HIDDEN = "Tessel.toolBarHidden";

    private OverflowButton overflowButton;
    private OverflowLayout layout;

    public static ComponentUI createUI(JComponent c) {
        return new TesselToolBarUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // BasicToolBarUI forces opaque after installDefaults; the rounded surface is painted by the border.
        LookAndFeel.installProperty(c, "opaque", Boolean.FALSE);
    }

    @Override
    protected void installComponents() {
        super.installComponents();
        overflowButton = new OverflowButton();
        layout = new OverflowLayout();
        toolBar.setLayout(layout);
        toolBar.add(overflowButton);
    }

    @Override
    protected void uninstallComponents() {
        if (overflowButton != null) {
            layout.restoreAll(toolBar);
            toolBar.remove(overflowButton);
        }
        if (toolBar.getLayout() == layout) toolBar.setLayout(null);
        overflowButton = null;
        layout = null;
        super.uninstallComponents();
    }

    @Override
    protected void setBorderToRollover(Component c) {
        if (c == overflowButton) return;
        super.setBorderToRollover(c);
    }

    @Override
    protected void setBorderToNonRollover(Component c) {
        if (c == overflowButton) return;
        super.setBorderToNonRollover(c);
    }

    private void showOverflowMenu() {
        JPopupMenu menu = new JPopupMenu();
        boolean pendingSeparator = false;
        for (Component c : layout.hidden) {
            if (c instanceof JSeparator) {
                pendingSeparator = menu.getComponentCount() > 0;
            } else if (c instanceof AbstractButton) {
                if (pendingSeparator) menu.addSeparator();
                pendingSeparator = false;
                menu.add(mirror((AbstractButton) c));
            }
        }
        if (menu.getComponentCount() == 0) return;
        boolean horizontal = toolBar.getOrientation() == JToolBar.HORIZONTAL;
        int x = horizontal ? overflowButton.getWidth() - menu.getPreferredSize().width : overflowButton.getWidth();
        int y = horizontal ? overflowButton.getHeight() : 0;
        menu.show(overflowButton, x, y);
    }

    /** A menu item that behaves like the (hidden) tool bar button. */
    private static JMenuItem mirror(AbstractButton b) {
        String text = b.getText();
        if (text == null || text.isEmpty()) text = plain(b.getToolTipText());
        if (text == null || text.isEmpty()) text = b.getAccessibleContext().getAccessibleName();
        if (text == null || text.isEmpty()) text = "Item";
        JMenuItem item = b instanceof JToggleButton ? new JCheckBoxMenuItem(text, b.isSelected()) : new JMenuItem(text);
        if (b.getIcon() != null && !(item instanceof JCheckBoxMenuItem)) item.setIcon(b.getIcon());
        item.setEnabled(b.isEnabled());
        item.addActionListener(e -> b.doClick(0));
        return item;
    }

    private static String plain(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    // ------------------------------------------------------------------ overflow button

    private final class OverflowButton extends JButton implements UIResource {
        OverflowButton() {
            super(TesselIcons.of(TesselIcons.Kind.MORE, 16));
            Tessel.style(this, ButtonStyle.ICON);
            setToolTipText(TesselStrings.get("more"));
            getAccessibleContext().setAccessibleName(TesselStrings.get("more"));
            setVisible(false);
            addActionListener(e -> showOverflowMenu());
        }
    }

    // ------------------------------------------------------------------ layout

    /** Lays the items out like a box layout and moves the buttons that do not fit into the overflow menu. */
    private final class OverflowLayout implements LayoutManager2, UIResource {

        final List<Component> hidden = new ArrayList<>();

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            hidden.remove(comp);
        }

        private boolean horizontal(Container parent) {
            return !(parent instanceof JToolBar) || ((JToolBar) parent).getOrientation() == JToolBar.HORIZONTAL;
        }

        private boolean overflowEnabled(Container parent) {
            return parent instanceof JComponent
                    && !Boolean.FALSE.equals(((JComponent) parent).getClientProperty(Tessel.TOOLBAR_OVERFLOW));
        }

        private boolean isHidden(Component c) {
            return c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty(HIDDEN));
        }

        private void mark(Component c, boolean hide) {
            if (c instanceof JComponent) ((JComponent) c).putClientProperty(HIDDEN, hide ? Boolean.TRUE : null);
            c.setVisible(!hide);
        }

        /** Shows every item that was hidden by the overflow again. */
        void restoreAll(Container parent) {
            for (Component c : parent.getComponents()) {
                if (isHidden(c)) mark(c, false);
            }
            hidden.clear();
        }

        /** The items taking part in the layout: visible ones, and the ones hidden by the overflow. */
        private List<Component> items(Container parent) {
            List<Component> list = new ArrayList<>();
            for (Component c : parent.getComponents()) {
                if (c == overflowButton) continue;
                if (isHidden(c) && c.isVisible()) {
                    // Made visible again by the application: it is no longer ours to hide.
                    ((JComponent) c).putClientProperty(HIDDEN, null);
                }
                if (c.isVisible() || isHidden(c)) list.add(c);
            }
            return list;
        }

        private int main(Dimension d, boolean horizontal) {
            return horizontal ? d.width : d.height;
        }

        private int cross(Dimension d, boolean horizontal) {
            return horizontal ? d.height : d.width;
        }

        private boolean collapsible(Component c) {
            return c instanceof AbstractButton || c instanceof JSeparator;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            boolean horizontal = horizontal(parent);
            int main = 0;
            int cross = 0;
            for (Component c : items(parent)) {
                Dimension d = c.getPreferredSize();
                main += main(d, horizontal);
                cross = Math.max(cross, cross(d, horizontal));
            }
            return withInsets(parent, horizontal, main, cross);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            boolean horizontal = horizontal(parent);
            int main = 0;
            int cross = 0;
            boolean collapsed = false;
            for (Component c : items(parent)) {
                Dimension d = c.getPreferredSize();
                cross = Math.max(cross, cross(d, horizontal));
                if (overflowEnabled(parent) && collapsible(c)) {
                    collapsed = true;
                } else {
                    main += main(d, horizontal);
                }
            }
            if (collapsed) main += main(overflowButton.getPreferredSize(), horizontal);
            return withInsets(parent, horizontal, main, cross);
        }

        @Override
        public Dimension maximumLayoutSize(Container parent) {
            boolean horizontal = horizontal(parent);
            Dimension pref = preferredLayoutSize(parent);
            return horizontal ? new Dimension(Short.MAX_VALUE, pref.height) : new Dimension(pref.width, Short.MAX_VALUE);
        }

        private Dimension withInsets(Container parent, boolean horizontal, int main, int cross) {
            Insets in = parent.getInsets();
            return horizontal
                    ? new Dimension(main + in.left + in.right, cross + in.top + in.bottom)
                    : new Dimension(cross + in.left + in.right, main + in.top + in.bottom);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.5f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.5f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }

        @Override
        public void layoutContainer(Container parent) {
            boolean horizontal = horizontal(parent);
            Insets in = parent.getInsets();
            int availMain = (horizontal ? parent.getWidth() - in.left - in.right : parent.getHeight() - in.top - in.bottom);
            int availCross = (horizontal ? parent.getHeight() - in.top - in.bottom : parent.getWidth() - in.left - in.right);

            List<Component> items = items(parent);
            int total = 0;
            for (Component c : items) total += main(c.getPreferredSize(), horizontal);

            // Decide which items go into the overflow menu.
            boolean overflow = overflowEnabled(parent) && total > availMain;
            List<Component> shown = new ArrayList<>();
            hidden.clear();
            if (overflow) {
                int budget = availMain - main(overflowButton.getPreferredSize(), horizontal) - 2;
                int used = 0;
                boolean hiding = false;
                for (Component c : items) {
                    int size = main(c.getPreferredSize(), horizontal);
                    if (!collapsible(c) || (!hiding && used + size <= budget)) {
                        used += size;
                        shown.add(c);
                    } else {
                        hiding = true;
                        hidden.add(c);
                    }
                }
                // A divider at the end of the bar (or the start of the menu) is pointless.
                while (!shown.isEmpty() && shown.get(shown.size() - 1) instanceof JSeparator) {
                    hidden.add(0, shown.remove(shown.size() - 1));
                }
                if (hidden.isEmpty()) overflow = false;
            } else {
                shown.addAll(items);
            }
            for (Component c : items) {
                boolean hide = hidden.contains(c);
                if (hide != isHidden(c)) mark(c, hide);
            }

            // Distribute spare room to stretchable items (glue, text fields, ...).
            int usedMain = 0;
            for (Component c : shown) usedMain += main(c.getPreferredSize(), horizontal);
            int reserve = overflow ? main(overflowButton.getPreferredSize(), horizontal) : 0;
            int spare = Math.max(0, availMain - usedMain - reserve);
            int[] sizes = new int[shown.size()];
            int[] room = new int[shown.size()];
            int flexible = 0;
            for (int i = 0; i < sizes.length; i++) {
                Component c = shown.get(i);
                sizes[i] = main(c.getPreferredSize(), horizontal);
                int max = main(c.getMaximumSize(), horizontal);
                boolean stretch = c instanceof Box.Filler || c instanceof javax.swing.text.JTextComponent
                        || c instanceof javax.swing.JComboBox || c instanceof javax.swing.JSlider;
                room[i] = stretch ? Math.max(0, max - sizes[i]) : 0;
                if (room[i] > 0) flexible++;
            }
            while (spare > 0 && flexible > 0) {
                int share = Math.max(1, spare / flexible);
                flexible = 0;
                for (int i = 0; i < sizes.length && spare > 0; i++) {
                    if (room[i] <= 0) continue;
                    int add = Math.min(share, Math.min(room[i], spare));
                    sizes[i] += add;
                    room[i] -= add;
                    spare -= add;
                    if (room[i] > 0) flexible++;
                }
            }

            int pos = horizontal ? in.left : in.top;
            int crossStart = horizontal ? in.top : in.left;
            for (int i = 0; i < sizes.length; i++) {
                Component c = shown.get(i);
                Dimension pref = c.getPreferredSize();
                int cross = Math.min(availCross, cross(pref, horizontal));
                if (c instanceof JSeparator) cross = Math.max(cross, availCross - 8);
                int cpos = crossStart + (availCross - cross) / 2;
                if (horizontal) c.setBounds(pos, cpos, sizes[i], cross);
                else c.setBounds(cpos, pos, cross, sizes[i]);
                pos += sizes[i];
            }
            for (Component c : hidden) c.setBounds(0, 0, 0, 0);

            if (overflow) {
                Dimension d = overflowButton.getPreferredSize();
                overflowButton.setVisible(true);
                if (horizontal) {
                    overflowButton.setBounds(in.left + availMain - d.width, in.top + (availCross - d.height) / 2, d.width, d.height);
                } else {
                    overflowButton.setBounds(in.left + (availCross - d.width) / 2, in.top + availMain - d.height, d.width, d.height);
                }
            } else if (overflowButton.isVisible()) {
                overflowButton.setVisible(false);
            }
        }
    }
}
