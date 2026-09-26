package net.tessel.gallery;

import javax.swing.SwingUtilities;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselTheme;

/** Entry point of the Tessel gallery. */
public final class TesselGallery {

    private TesselGallery() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tessel.setup(TesselTheme.SYSTEM);
            GalleryFrame frame = new GalleryFrame();
            frame.setVisible(true);
        });
    }
}
