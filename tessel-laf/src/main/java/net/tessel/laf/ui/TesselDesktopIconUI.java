package net.tessel.laf.ui;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;

/** The chip an internal frame shrinks to when it is minimized. */
public class TesselDesktopIconUI extends BasicDesktopIconUI {

    public static ComponentUI createUI(JComponent c) {
        return new TesselDesktopIconUI();
    }

    @Override
    protected void installDefaults() {
        LookAndFeel.installBorder(desktopIcon, "DesktopIcon.border");
        LookAndFeel.installProperty(desktopIcon, "opaque", Boolean.FALSE);
    }

    @Override
    protected void installComponents() {
        iconPane = new TesselInternalFrameTitlePane(frame);
        desktopIcon.setLayout(new BorderLayout());
        desktopIcon.add(iconPane, BorderLayout.CENTER);
    }
}
