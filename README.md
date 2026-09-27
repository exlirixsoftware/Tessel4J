# Tessel4J

A modern, themeable **look and feel for Java Swing**, matching the design of [Tessel.NET](../Tessel.NET).

- Light, dark and system themes, switchable at runtime
- Custom accent colors (or the Windows accent)
- Every standard Swing control restyled: rounded, flat, Fluent-inspired
- Modern components: `TesselFrame` (custom title bar), `NavigationView`, `ContentDialog`, `Snackbar`, `InfoBar`, `Card`, `SettingsCard`, `Expander`, `ToggleSwitch`, `ProgressRing`, `Badge`, `Avatar`, `TextBlock`, `FontIcon`
- On Windows, `TesselFrame` gets real native chrome: DWM drop shadow, Windows 11 rounded corners, and native drag/edge-resize/Aero Snap/Snap Layouts hit-testing - while still painting its own themed title bar
- Pure Java 17+, **zero runtime dependencies** for consumers (the jar bundles what it needs for the Windows native chrome above)

```
Tessel4J
├── tessel-laf/       the look and feel (library)
├── tessel-gallery/   demo app showing every control
├── dist/             built jars
├── build.ps1         build script (Windows PowerShell)
└── build.sh          build script (macOS / Linux / Git Bash)
```

## Jars

| File | Use |
| --- | --- |
| `dist/tessel-laf-1.0.0.jar` | add to your project's classpath |
| `dist/tessel-laf-1.0.0-sources.jar` | sources, for IDE navigation / Javadoc |
| `dist/tessel-gallery-1.0.0.jar` | runnable demo (keep it next to `tessel-laf-1.0.0.jar`) |

Run the gallery:

```bash
java -jar dist/tessel-gallery-1.0.0.jar
```

Rebuild from source (only a JDK 17+ is needed — no Maven or Gradle):

```bash
./build.ps1        # Windows
./build.sh         # macOS / Linux
```

The scripts use `JAVA_HOME`, or the JDK of the `java` on your `PATH`. Building also needs the JNA jars in `lib/` — see [`lib/README.md`](lib/README.md); they're a build-time dependency only and end up merged into `dist/tessel-laf-1.0.0.jar`, so consumers of that jar don't need them.

## Getting started

```java
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselTheme;

public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        Tessel.setup(TesselTheme.SYSTEM);          // or LIGHT / DARK
        new MyFrame().setVisible(true);
    });
}
```

`UIManager.setLookAndFeel(new TesselLookAndFeel())` works too, but `Tessel.setup` also enables runtime theme switching.

For the custom title bar, extend `TesselFrame` instead of `JFrame`:

```java
public class MyFrame extends TesselFrame {
    public MyFrame() {
        super("My App");
        setIconImage(logo);
        setIconSize(20);                         // title bar icon size (default 16)
        addTitleBarComponent(new JButton("…"));  // components on the right of the title bar
        setContentPane(new NavigationView("My App"));
    }
}
```

## Theming

```java
Tessel.setTheme(TesselTheme.DARK);        // updates every open window
Tessel.setAccent(new Color(0x0078D4));    // null = built-in indigo
Tessel.useSystemAccent();                 // Windows accent color
Tessel.addThemeListener(() -> { ... });

Palette p = Tessel.palette();             // the active colors
p.accent; p.surface; p.textSecondary; p.danger; ...
```

Palette colors are also available as `UIManager` keys: `Tessel.accent`, `Tessel.surface`, `Tessel.text`, …

Custom-painted code should read colors from `Tessel.palette()` while painting, so it follows theme changes.

## Styling standard controls

```java
Tessel.style(button, ButtonStyle.ACCENT);            // STANDARD, ACCENT, OUTLINE, SUBTLE, DANGER, ICON, LINK
Tessel.placeholder(textField, "name@example.com");   // also JPasswordField, JTextArea, JComboBox
Tessel.leadingIcon(textField, new FontIcon(Symbol.MAIL, 14));
Tessel.textStyle(label, TextStyle.TITLE);            // DISPLAY, TITLE, SUBTITLE, HEADER, BODY_STRONG, BODY, SECONDARY, CAPTION, CODE
JLabel title = Tessel.label("Settings", TextStyle.TITLE);
checkBox.putClientProperty(Tessel.INDETERMINATE, true);
```

Styled controls: JButton, JToggleButton, JCheckBox, JRadioButton, JLabel, JTextField, JPasswordField, JFormattedTextField, JTextArea, JEditorPane, JTextPane, JComboBox, JSpinner, JSlider, JProgressBar, JScrollPane/JScrollBar, JList, JTable (+ header), JTree, JTabbedPane, JMenuBar, JMenu, JMenuItem, JCheckBoxMenuItem, JRadioButtonMenuItem, JPopupMenu, JSeparator, JToolTip, JToolBar, JSplitPane, JOptionPane, TitledBorder.

## Components

```java
NavigationView nav = new NavigationView("My App");
nav.setPaneHeader(searchField);
nav.addItem("Home", Symbol.HOME, HomePage::new);         // pages are created lazily and cached
nav.addHeader("Section");
nav.addFooterItem("Settings", Symbol.SETTINGS, SettingsPage::new);
nav.select(0);

new Card("Profile", content);                             // setElevated(true) for a shadow
new SettingsCard("Notifications", "Alerts for mentions", Symbol.RINGER, new ToggleSwitch(null, true));
new Expander("Details", content, true);
new InfoBar(Severity.WARNING, "Heads up", "Your subscription expires soon.");
new Badge("New", Badge.Kind.SUCCESS);
new Avatar("Ada Lovelace", 40);
new ProgressRing(32);
new TextBlock("Wrapping text…", TextStyle.SECONDARY);
new JLabel(new FontIcon(Symbol.SAVE, 16));

Snackbar.show(frame, "Saved", "Your changes were saved.", Severity.SUCCESS);

ContentDialog dialog = new ContentDialog("Delete file?", "This can't be undone.");
dialog.setPrimaryButtonText("Delete");
dialog.setCloseButtonText("Cancel");
dialog.setPrimaryDestructive(true);
if (dialog.showModal(frame) == ContentDialog.Result.PRIMARY) { ... }   // or showAsync(frame)
```

## Notes

- **Fonts:** Segoe UI and Segoe Fluent Icons (Windows 11) / Segoe MDL2 Assets (Windows 10). On other systems a similar UI font is used, and `FontIcon` glyphs stay blank; the controls' own icons (check marks, chevrons, status icons…) are vector-drawn and work everywhere.
- **TesselFrame** stays undecorated at the Swing level (it always paints its own title bar), but on Windows it hooks the native window procedure to get real DWM chrome: drop shadow, Windows 11 rounded corners, dragging/edge-resizing/maximizing/Win+Arrow snap and drag-to-edge Aero Snap all handled natively, plus a Snap Layouts flyout on hovering the maximize button. On macOS/Linux (or if the native hook can't install) it falls back to the previous pure-Java dragging/resizing/maximizing.
- **System theme** is read when the theme is applied; call `Tessel.setTheme(TesselTheme.SYSTEM)` again to pick up a change.
