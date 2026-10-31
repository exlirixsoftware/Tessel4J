package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;

/** Menu delegates with rounded hover highlights. */
public final class TesselMenuUIs {

    private TesselMenuUIs() {
    }

    public static class MenuItemUI extends BasicMenuItemUI {
        public static ComponentUI createUI(JComponent c) {
            return new MenuItemUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            defaultTextIconGap = MenuPainting.TEXT_ICON_GAP;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            MenuPainting.paintBackground(g, menuItem, bgColor);
        }
    }

    public static class MenuUI extends BasicMenuUI {
        private final HierarchyListener marginUpdater = e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) updateMargin();
        };

        public static ComponentUI createUI(JComponent c) {
            return new MenuUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            defaultTextIconGap = MenuPainting.TEXT_ICON_GAP;
            updateMargin();
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            menuItem.addHierarchyListener(marginUpdater);
        }

        @Override
        protected void uninstallListeners() {
            menuItem.removeHierarchyListener(marginUpdater);
            super.uninstallListeners();
        }

        /** A menu in a menu bar keeps wide side margins; a sub-menu in a popup uses the narrow ones of other items. */
        private void updateMargin() {
            Insets current = menuItem.getMargin();
            if (current != null && !(current instanceof UIResource)) return;
            Insets wanted = UIManager.getInsets(((JMenu) menuItem).isTopLevelMenu() ? "Menu.margin" : "Tessel.popupMenuMargin");
            if (wanted != null && !wanted.equals(current)) menuItem.setMargin(wanted);
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            MenuPainting.paintBackground(g, menuItem, bgColor);
        }
    }

    public static class CheckBoxMenuItemUI extends BasicCheckBoxMenuItemUI {
        public static ComponentUI createUI(JComponent c) {
            return new CheckBoxMenuItemUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            defaultTextIconGap = MenuPainting.TEXT_ICON_GAP;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            MenuPainting.paintBackground(g, menuItem, bgColor);
        }
    }

    public static class RadioButtonMenuItemUI extends BasicRadioButtonMenuItemUI {
        public static ComponentUI createUI(JComponent c) {
            return new RadioButtonMenuItemUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            defaultTextIconGap = MenuPainting.TEXT_ICON_GAP;
        }

        @Override
        protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
            MenuPainting.paintBackground(g, menuItem, bgColor);
        }
    }
}
