package net.tessel.gallery;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import net.tessel.gallery.pages.ButtonsPage;
import net.tessel.gallery.pages.CollectionsPage;
import net.tessel.gallery.pages.DesignPage;
import net.tessel.gallery.pages.FeedbackPage;
import net.tessel.gallery.pages.HomePage;
import net.tessel.gallery.pages.IconsPage;
import net.tessel.gallery.pages.InputsPage;
import net.tessel.gallery.pages.LayoutPage;
import net.tessel.gallery.pages.MenusPage;
import net.tessel.gallery.pages.SelectionPage;
import net.tessel.gallery.pages.SettingsPage;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselTheme;
import net.tessel.laf.components.Badge;
import net.tessel.laf.components.NavigationView;
import net.tessel.laf.components.TesselFrame;
import net.tessel.laf.icons.FontIcon;
import net.tessel.laf.icons.Symbol;

/** The gallery window: a navigation view with one page per control group. */
public class GalleryFrame extends TesselFrame {

    private final NavigationView navigation = new NavigationView("Tessel UI");
    private final JButton themeButton = new JButton();

    public GalleryFrame() {
        super("Tessel Gallery");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconSize(20);
        List<Image> icons = loadIcons();
        if (!icons.isEmpty()) setIconImages(icons);

        Badge version = new Badge("v" + Tessel.VERSION, Badge.Kind.ACCENT);
        addTitleBarComponent(version);
        Tessel.style(themeButton, ButtonStyle.ICON);
        themeButton.setToolTipText("Toggle light / dark theme");
        themeButton.addActionListener(e -> Tessel.setTheme(Tessel.isDark() ? TesselTheme.LIGHT : TesselTheme.DARK));
        updateThemeIcon();
        Tessel.addThemeListener(this::updateThemeIcon);
        addTitleBarComponent(themeButton);

        JTextField search = new JTextField();
        Tessel.placeholder(search, "Search controls");
        Tessel.leadingIcon(search, new FontIcon(Symbol.SEARCH, 14));
        search.addActionListener(e -> search(search.getText()));
        navigation.setPaneHeader(search);

        navigation.addItem("Home", Symbol.HOME, () -> new HomePage(this).scroll());
        navigation.addHeader("Controls");
        navigation.addItem("Buttons", Symbol.TOUCH_POINTER, () -> new ButtonsPage().scroll());
        navigation.addItem("Text & Input", Symbol.EDIT, () -> new InputsPage().scroll());
        navigation.addItem("Selection", Symbol.CHECKBOX_COMPOSITE, () -> new SelectionPage().scroll());
        navigation.addItem("Collections", Symbol.LIST, () -> new CollectionsPage().scroll());
        navigation.addItem("Feedback", Symbol.MESSAGE, () -> new FeedbackPage().scroll());
        navigation.addItem("Layout", Symbol.VIEW_ALL, () -> new LayoutPage().scroll());
        navigation.addItem("Menus & Tooltips", Symbol.MORE, () -> new MenusPage().scroll());
        navigation.addHeader("Design");
        navigation.addItem("Colors & Type", Symbol.COLOR, () -> new DesignPage().scroll());
        navigation.addItem("Icons", Symbol.EMOJI, IconsPage::new);
        navigation.addFooterItem("Settings", Symbol.SETTINGS, () -> new SettingsPage().scroll());
        navigation.select(0);

        setContentPane(navigation);
        setMinimumSize(new Dimension(720, 480));
        setSize(1280, 840);
        setLocationRelativeTo(null);
    }

    public NavigationView getNavigation() {
        return navigation;
    }

    /** Selects the navigation item with the given text. */
    public void navigateTo(String text) {
        for (NavigationView.Item item : navigation.getItems()) {
            if (item.getText().equalsIgnoreCase(text)) {
                navigation.select(item);
                return;
            }
        }
    }

    private void search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return;
        for (NavigationView.Item item : navigation.getItems()) {
            if (item.getText().toLowerCase(Locale.ROOT).contains(q)) {
                navigation.select(item);
                return;
            }
        }
    }

    private void updateThemeIcon() {
        themeButton.setIcon(new FontIcon(Tessel.isDark() ? Symbol.QUIET_HOURS : Symbol.BRIGHTNESS, 16));
    }

    /** Every pre-rendered icon size (16-256px, square) so Windows picks a crisp one for each context - title bar, Alt+Tab, taskbar. */
    private static List<Image> loadIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256}) {
            try (InputStream in = GalleryFrame.class.getResourceAsStream("icon-" + size + ".png")) {
                if (in != null) icons.add(ImageIO.read(in));
            } catch (IOException ignored) {
                // skip this size; the rest still give AWT something to pick from
            }
        }
        return icons;
    }
}
