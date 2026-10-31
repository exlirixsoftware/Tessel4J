package net.tessel.gallery.pages;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.io.File;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.ColorPicker;
import net.tessel.laf.components.TextBlock;
import net.tessel.laf.components.ToggleSwitch;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;
import net.tessel.laf.ui.UIUtils;

public class PickersPage extends Page {

    private final TextBlock fileResult = new TextBlock("Nothing chosen yet.", TextStyle.SECONDARY);
    private final TextBlock embeddedResult = new TextBlock("Select a file in the chooser above.", TextStyle.SECONDARY);

    public PickersPage() {
        super("Pickers", "JFileChooser and JColorChooser use the Tessel style automatically; ColorPicker is the same control as a plain component.");

        // File dialogs
        JButton open = Tessel.style(new JButton("Open file...", new FontIcon(Symbol.FOLDER)), ButtonStyle.ACCENT);
        open.addActionListener(e -> {
            JFileChooser fc = chooser("Open a file");
            if (fc.showOpenDialog(window()) == JFileChooser.APPROVE_OPTION) fileResult.setText("Opened: " + fc.getSelectedFile());
        });
        JButton openMany = new JButton("Open several...");
        openMany.addActionListener(e -> {
            JFileChooser fc = chooser("Open files");
            fc.setMultiSelectionEnabled(true);
            if (fc.showOpenDialog(window()) == JFileChooser.APPROVE_OPTION) {
                StringBuilder sb = new StringBuilder("Opened " + fc.getSelectedFiles().length + " file(s):");
                for (File f : fc.getSelectedFiles()) sb.append("\n  ").append(f.getName());
                fileResult.setText(sb.toString());
            }
        });
        JButton save = new JButton("Save as...", new FontIcon(Symbol.SAVE));
        save.addActionListener(e -> {
            JFileChooser fc = chooser("Save as");
            fc.setSelectedFile(new File("Untitled.txt"));
            if (fc.showSaveDialog(window()) == JFileChooser.APPROVE_OPTION) fileResult.setText("Save to: " + fc.getSelectedFile());
        });
        JButton folder = new JButton("Choose folder...");
        folder.addActionListener(e -> {
            JFileChooser fc = chooser("Choose a folder");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(window()) == JFileChooser.APPROVE_OPTION) fileResult.setText("Folder: " + fc.getSelectedFile());
        });
        section("File dialogs", Layouts.stack(12, Layouts.row(open, openMany, save, folder), fileResult));

        // Embedded chooser
        JFileChooser embedded = new JFileChooser(new File(System.getProperty("user.home")));
        embedded.setControlButtonsAreShown(false);
        embedded.setMultiSelectionEnabled(true);
        embedded.setPreferredSize(new Dimension(100, 440));
        embedded.addPropertyChangeListener(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY, e -> {
            File[] files = embedded.getSelectedFiles();
            embeddedResult.setText(files == null || files.length == 0 ? "Select a file in the chooser above."
                    : files.length == 1 ? "Selected: " + files[0] : files.length + " items selected");
        });
        JPanel embeddedHolder = Layouts.panel(new BorderLayout());
        embeddedHolder.add(embedded, BorderLayout.CENTER);
        embeddedHolder.add(embeddedResult, BorderLayout.SOUTH);
        embeddedResult.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        section("Embedded file chooser", embeddedHolder);

        // Color
        ColorPicker picker = new ColorPicker(Tessel.palette().accent);
        ColorChip chip = new ColorChip(picker.getColor());
        picker.addChangeListener(e -> chip.setColor(picker.getColor()));

        ToggleSwitch opacity = new ToggleSwitch("Opacity slider", false);
        opacity.addItemListener(e -> picker.setAlphaEnabled(opacity.isSelected()));

        JButton useAccent = Tessel.style(new JButton("Use as accent color"), ButtonStyle.ACCENT);
        useAccent.addActionListener(e -> {
            Color c = picker.getColor();
            Tessel.setAccent(new Color(c.getRGB() & 0xFFFFFF));
        });
        JButton resetAccent = new JButton("Reset accent");
        resetAccent.addActionListener(e -> Tessel.setAccent(null));
        JButton dialog = new JButton("JColorChooser dialog...", new FontIcon(Symbol.COLOR));
        dialog.addActionListener(e -> {
            Color c = JColorChooser.showDialog(window(), "Choose a color", picker.getColor());
            if (c != null) picker.setColor(c);
        });

        JPanel side = Layouts.stack(14,
                Tessel.label("Selected color", TextStyle.BODY_STRONG), chip,
                opacity,
                Layouts.row(useAccent, resetAccent),
                Layouts.row(dialog),
                new TextBlock("The picker works on its own and is also what JColorChooser shows under the Tessel look and feel.", TextStyle.CAPTION));
        JPanel colorRow = Layouts.panel(new BorderLayout(32, 0));
        colorRow.add(Layouts.row(picker), BorderLayout.WEST);
        colorRow.add(side, BorderLayout.CENTER);
        section("Color picker", colorRow);
    }

    private static Window window() {
        return javax.swing.FocusManager.getCurrentManager().getActiveWindow();
    }

    private static JFileChooser chooser(String title) {
        JFileChooser fc = new JFileChooser(new File(System.getProperty("user.home")));
        fc.setDialogTitle(title);
        FileNameExtensionFilter images = new FileNameExtensionFilter("Images (png, jpg, gif)", "png", "jpg", "jpeg", "gif");
        FileNameExtensionFilter text = new FileNameExtensionFilter("Text files (txt, md)", "txt", "md");
        fc.addChoosableFileFilter(images);
        fc.addChoosableFileFilter(text);
        return fc;
    }

    /** A rounded swatch with the hex value of the color. */
    private static final class ColorChip extends JComponent {
        private Color color;

        ColorChip(Color color) {
            setColor(color);
            setPreferredSize(new Dimension(100, 56));
        }

        void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 12, 12));
                for (int y = 0; y < h; y += 8) {
                    for (int x = 0; x < w; x += 8) {
                        g2.setColor(((x / 8 + y / 8) & 1) == 0 ? new Color(0xFFFFFF) : new Color(0xD0D0D0));
                        g2.fillRect(x, y, 8, 8);
                    }
                }
                g2.setClip(null);
                g2.setColor(color);
                UIUtils.fillRound(g2, 0, 0, w, h, 6);
                g2.setColor(p.borderStrong);
                g2.setStroke(new BasicStroke(1f));
                UIUtils.strokeRound(g2, 0, 0, w, h, 6, 1f);
                String hex = color.getAlpha() == 255
                        ? String.format(Locale.ROOT, "#%06X", color.getRGB() & 0xFFFFFF)
                        : String.format(Locale.ROOT, "#%08X", color.getRGB());
                g2.setFont(net.tessel.laf.Fonts.mono(13));
                double lum = Palette.luminance(color);
                g2.setColor(color.getAlpha() < 140 ? p.text : lum > 0.5 ? new Color(0x111418) : Color.WHITE);
                g2.drawString(hex, 14, h / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
            } finally {
                g2.dispose();
            }
        }
    }
}
