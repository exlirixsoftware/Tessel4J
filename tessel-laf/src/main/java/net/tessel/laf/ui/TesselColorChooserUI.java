package net.tessel.laf.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorChooserUI;
import javax.swing.plaf.ComponentUI;
import net.tessel.laf.Tessel;
import net.tessel.laf.components.ColorPicker;

/**
 * A {@code JColorChooser} that shows a single {@link ColorPicker} (with its opacity slider; turn that off with
 * {@code chooser.putClientProperty(Tessel.COLOR_CHOOSER_ALPHA, false)}) instead of the tabbed Swing panels.
 * The chooser's color, selection model and listeners keep working; chooser panels and the preview
 * panel are not used.
 */
public class TesselColorChooserUI extends ColorChooserUI {

    private JColorChooser chooser;
    private ColorPicker picker;
    private ChangeListener pickerListener;
    private ChangeListener modelListener;
    private java.beans.PropertyChangeListener modelSwapListener;
    private java.beans.PropertyChangeListener alphaListener;
    private boolean updating;

    public static ComponentUI createUI(JComponent c) {
        return new TesselColorChooserUI();
    }

    @Override
    public void installUI(JComponent c) {
        chooser = (JColorChooser) c;
        LookAndFeel.installColorsAndFont(chooser, "ColorChooser.background", "ColorChooser.foreground", "ColorChooser.font");
        LookAndFeel.installProperty(chooser, "opaque", Boolean.FALSE);

        picker = new ColorPicker();
        picker.setAlphaEnabled(alphaWanted());
        picker.setColor(chooser.getColor());
        picker.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        chooser.setLayout(new BorderLayout());
        chooser.add(picker, BorderLayout.CENTER);

        pickerListener = e -> {
            if (updating) return;
            updating = true;
            try {
                chooser.setColor(picker.getColor());
            } finally {
                updating = false;
            }
        };
        picker.addChangeListener(pickerListener);

        modelListener = e -> {
            if (updating) return;
            ColorSelectionModel model = chooser.getSelectionModel();
            Color color = model.getSelectedColor();
            if (color == null) return;
            updating = true;
            try {
                picker.setColor(color);
            } finally {
                updating = false;
            }
        };
        chooser.getSelectionModel().addChangeListener(modelListener);
        modelSwapListener = e -> {
            // A new selection model was set on the chooser: follow it.
            ((ColorSelectionModel) e.getOldValue()).removeChangeListener(modelListener);
            ColorSelectionModel model = (ColorSelectionModel) e.getNewValue();
            model.addChangeListener(modelListener);
            modelListener.stateChanged(null);
        };
        chooser.addPropertyChangeListener(JColorChooser.SELECTION_MODEL_PROPERTY, modelSwapListener);
        alphaListener = e -> {
            picker.setAlphaEnabled(alphaWanted());
            chooser.revalidate();
        };
        chooser.addPropertyChangeListener(Tessel.COLOR_CHOOSER_ALPHA, alphaListener);
    }

    /** The opacity slider is shown unless the chooser has the client property set to false. */
    private boolean alphaWanted() {
        return !Boolean.FALSE.equals(chooser.getClientProperty(Tessel.COLOR_CHOOSER_ALPHA));
    }

    @Override
    public void uninstallUI(JComponent c) {
        chooser.removePropertyChangeListener(JColorChooser.SELECTION_MODEL_PROPERTY, modelSwapListener);
        chooser.removePropertyChangeListener(Tessel.COLOR_CHOOSER_ALPHA, alphaListener);
        chooser.getSelectionModel().removeChangeListener(modelListener);
        picker.removeChangeListener(pickerListener);
        chooser.remove(picker);
        picker = null;
        pickerListener = null;
        modelListener = null;
        modelSwapListener = null;
        alphaListener = null;
        chooser = null;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = picker == null ? new Dimension(320, 440) : picker.getPreferredSize();
        return new Dimension(d.width + 8, d.height + 8);
    }
}
