package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.plaf.ComponentUI;
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
        public static ComponentUI createUI(JComponent c) {
            return new MenuUI();
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
