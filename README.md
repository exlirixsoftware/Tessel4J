![logo](assets/java.png)
# Tessel4J

A modern, themeable **look and feel for Java Swing**, matching the design of [Tessel.NET](../Tessel.NET).

- Light, dark and system themes, switchable at runtime
- Custom accent colors (or the Windows accent)
- Every standard Swing control restyled: rounded, flat, Fluent-inspired - including `JFileChooser`, `JColorChooser`, `JToolBar` (with an overflow menu) and `JInternalFrame` / `JDesktopPane`
- Modern components: `TesselFrame` (custom title bar), `NavigationView`, `TabView`, `ContentDialog`, `TeachingTip`, `Snackbar`, `InfoBar`, `Card`, `SettingsCard`, `Expander`, `BreadcrumbBar`, `SplitButton`, `DropDownButton`, `NumberBox`, `ColorPicker`, `RichTextEditor`, `RichTextToolBar`, `TesselDialog`, `ToggleSwitch`, `ProgressRing`, `Badge`, `Avatar`, `TextBlock`, `FontIcon`
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
| `dist/tessel-laf-1.1.0.jar` | add to your project's classpath |
| `dist/tessel-laf-1.1.0-sources.jar` | sources, for IDE navigation / Javadoc |
| `dist/tessel-gallery-1.1.0.jar` | runnable demo (keep it next to `tessel-laf-1.1.0.jar`) |

Run the gallery:

```bash
java -jar dist/tessel-gallery-1.1.0.jar
```

Rebuild from source (only a JDK 17+ is needed — no Maven or Gradle):

```bash
./build.ps1        # Windows
./build.sh         # macOS / Linux
```

The scripts use `JAVA_HOME`, or the JDK of the `java` on your `PATH`. Building also needs the JNA jars in `lib/` — see [`lib/README.md`](lib/README.md); they're a build-time dependency only and end up merged into `dist/tessel-laf-1.1.0.jar`, so consumers of that jar don't need them.

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
Tessel.placeholder(textField, "name@example.com");   // also JPasswordField, JTextArea, JTextPane, JEditorPane, JComboBox
Tessel.leadingIcon(textField, new FontIcon(Symbol.MAIL, 14));
Tessel.textStyle(label, TextStyle.TITLE);            // DISPLAY, TITLE, SUBTITLE, HEADER, BODY_STRONG, BODY, SECONDARY, CAPTION, CODE
JLabel title = Tessel.label("Settings", TextStyle.TITLE);
checkBox.putClientProperty(Tessel.INDETERMINATE, true);
```

Styled controls: JButton, JToggleButton, JCheckBox, JRadioButton, JLabel, JTextField, JPasswordField, JFormattedTextField, JTextArea, JEditorPane, JTextPane, JComboBox, JSpinner, JSlider, JProgressBar, JScrollPane/JScrollBar, JList, JTable (+ header), JTree, JTabbedPane, JMenuBar, JMenu, JMenuItem, JCheckBoxMenuItem, JRadioButtonMenuItem, JPopupMenu, JSeparator, JToolTip, JToolBar, JSplitPane, JOptionPane, JFileChooser, JColorChooser, JInternalFrame, JDesktopPane, TitledBorder.

### Tool bars, internal frames and choosers

These need no code: install the look and feel and use the standard classes.

```java
// JToolBar: rounded surface, grip when floatable, and an overflow menu - buttons that do not fit move
// into a "more" popup. Turn it off per tool bar with:
toolBar.putClientProperty(Tessel.TOOLBAR_OVERFLOW, false);

// JFileChooser: breadcrumb address bar (click it to type a path), places sidebar, back / forward / up,
// and list / details / icon views. Works in dialogs and embedded.
JFileChooser fc = new JFileChooser();
fc.showOpenDialog(frame);

// JColorChooser: shows a ColorPicker instead of the tabbed Swing panels.
Color c = JColorChooser.showDialog(frame, "Pick a color", Color.ORANGE);

// JInternalFrame / JDesktopPane: rounded, shadowed windows with a Fluent title bar and a chip for
// minimized frames.
```

`JColorChooser` keeps its color and selection model in sync with the picker; chooser panels and the preview panel are not used under Tessel. The picker has an opacity slider, so `getColor()` can return a color with an alpha channel; call `chooser.putClientProperty(Tessel.COLOR_CHOOSER_ALPHA, false)` to hide the slider and get opaque colors only.

### Text panes and editor panes

`JTextPane` and `JEditorPane` get the same themed look as the other text controls, plus a few things of their own. None of it needs code, except where noted.

- **HTML follows the theme.** The style sheet of an HTML document is generated from the palette: text, headings, links (accent), `code`, `pre`, block quotes and tables. It is linked to the document rather than added to it, so `getText()` returns your HTML without the theme, and a `<style>` in your content still wins. It updates when the theme, the accent, or the pane's font changes.
- **Undo and redo** (Ctrl+Z, Ctrl+Y / Ctrl+Shift+Z) for editable panes. A run of typed or deleted characters is one step.
- **Ctrl+B / Ctrl+I / Ctrl+U** toggle bold, italic and underline on panes with a styled kit (`JTextPane`, HTML, RTF).
- **Context menu** with Undo, Redo, Cut, Copy, Paste, Delete and Select all, localized through `TesselStrings`; over an HTML link it adds Open link and Copy link address. It also opens with the context-menu key or Shift+F10. A pane that has its own `setComponentPopupMenu(...)` keeps it; turn the built-in one off with `pane.putClientProperty(Tessel.CONTEXT_MENU, false)`.
- **Placeholder** with `Tessel.placeholder(pane, "Write something...")`. Put the pane in a `JScrollPane` for the rounded outline.

`RichText` is the API for the rest:

```java
JTextPane pane = new JTextPane();
RichText.toggleBold(pane);                       // the selection, or the text typed next
RichText.setFontFamily(pane, "Georgia");
RichText.setFontSize(pane, 18);
RichText.setTextColor(pane, Color.RED);          // null goes back to the pane's color
RichText.setHighlight(pane, Color.YELLOW);
RichText.setAlignment(pane, StyleConstants.ALIGN_CENTER);
RichText.clearFormatting(pane);
RichText.isBold(pane);                           // also isItalic, isUnderline, fontFamily, fontSize, alignment, ...

RichText.undo(pane);                             // also redo, canUndo, canRedo
RichText.discardUndo(pane);                      // call after loading content with setText or setPage

JEditorPane help = new JEditorPane("text/html", "<a href=\"https://example.com\">Example</a>");
help.setEditable(false);
RichText.enableLinks(help);                      // hand cursor, tool tip, opens in the browser
RichText.enableLinks(help, uri -> open(uri));    // or handle the link yourself
```

An editable pane opens links with Ctrl+click. `#anchors` scroll the pane when no handler is given.

`RichTextEditor` is a ready-made editor: a `RichTextToolBar` (undo, redo, font, size, bold, italic, underline, strikethrough, text and highlight color, alignment, clear formatting) above a `JTextPane` in a scroll pane. The tool bar follows the caret and is disabled while the pane is read-only; use `new RichTextToolBar(pane)` on its own to format any text pane.

```java
RichTextEditor editor = new RichTextEditor();                        // formatted text, getText() is plain text
RichTextEditor notes = new RichTextEditor(RichTextEditor.Format.HTML);
notes.setPlaceholder("Write something...");
notes.setText("<p>Hello <b>world</b></p>");
String html = notes.getText();

RichTextEditor doc = new RichTextEditor(RichTextEditor.Format.RTF);  // RTF is an 8-bit format:
doc.read(inputStream);                                               // use streams, not setText/getText
doc.write(outputStream);
```

### Dialogs

Every dialog Swing creates gets the Tessel window chrome - a themed title bar with icon, title and close button, a thin outline, and on Windows the native drop shadow, rounded corners, dragging, edge resizing and snapping. This covers `JOptionPane` (message, confirm, input, option dialogs), `JFileChooser`, `JColorChooser` and plain `JDialog`s. It works through Swing's own "look and feel decorated" mechanism, which `Tessel.setup` switches on:

```java
JOptionPane.showMessageDialog(frame, "Saved.", "Done", JOptionPane.INFORMATION_MESSAGE);   // Tessel chrome, no code needed
Tessel.setDecoratedDialogs(false);                      // keep the operating system's title bars instead
```

Option panes are styled like `ContentDialog`: the icon (including a "?" for questions) and message on the surface, a tinted footer with right-aligned buttons, and the default button in the accent style.

For your own dialogs use `TesselDialog`, which is decorated even when `setDecoratedDialogs(false)` is in effect:

```java
TesselDialog dialog = new TesselDialog(frame, "Rename file");   // modal by default; a modality can be passed
dialog.setContentPane(form);
dialog.getRootPane().setDefaultButton(renameButton);
dialog.showDialog();                                            // packs, centers on the owner, shows
```

Esc and the title bar's close button run the window listeners first (a `windowClosing` handler can veto by using `DO_NOTHING_ON_CLOSE`) and then apply the close operation; `dialog.close()` does the same from code. `ContentDialog` remains the choice for confirmations drawn over the window's own content.

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

### SplitButton and DropDownButton

```java
SplitButton save = Tessel.style(new SplitButton("Save", new FontIcon(Symbol.SAVE)), ButtonStyle.ACCENT);
save.addActionListener(e -> save());                        // the main part
save.addMenuItem("Save as...", () -> saveAs());             // the arrow part; getMenu() is a plain JPopupMenu
save.setMenuSupplier(() -> buildRecentMenu());              // optional: build the menu each time it opens

DropDownButton sort = new DropDownButton("Sort by");         // the whole button opens the menu
sort.addMenuItem("Name", () -> sortBy(NAME));
```

Both take every `ButtonStyle`; Alt+Down / F4 / Down open the menu from the keyboard.

### NumberBox

```java
NumberBox quantity = new NumberBox(1, 0, 99);                // value, minimum, maximum
quantity.setSuffix(" pcs");
quantity.setSpinButtonPlacement(NumberBox.SpinButtonPlacement.COMPACT);   // INLINE (default), COMPACT, HIDDEN
quantity.addChangeListener(e -> use(quantity.getValue()));
```

The value is committed on Enter, on focus loss and when stepped (arrow keys, Page Up / Down, mouse wheel, buttons). It accepts expressions such as `12 * 3 + 4` (`setAcceptsExpression(false)` turns that off), clamps to the range, can wrap around (`setWrapEnabled`), can be empty (`setNullable`), and flashes red and restores the old value for text it cannot evaluate. `setPrefix`, `setSuffix` and `setFractionDigits` control the display.

### TabView

```java
TabView tabs = new TabView();
tabs.addTab("Readme.md", new FontIcon(Symbol.DOCUMENT, 16), new JScrollPane(new JTextArea()));
tabs.setAddTabHandler(() -> tabs.addTab("Untitled", new JScrollPane(new JTextArea())));   // shows the + button
tabs.addTabViewListener(new TabView.TabViewListener() {
    @Override public boolean tabClosing(TabView view, TabView.Tab tab) { return confirmClose(tab); }   // false vetoes
});
```

Drag tabs to reorder (`setReorderable`), middle-click or Ctrl+W to close, Ctrl+PageUp / Ctrl+PageDown to switch. Tabs shrink to fit and then scroll with arrow buttons or the mouse wheel. Give the tab content a transparent background (or put it in a `Card`) so it blends with the tab surface.

### TeachingTip

```java
TeachingTip tip = new TeachingTip("Save your work", "Press Ctrl+S at any time.");
tip.setActionButton("Got it", () -> markTipSeen());
tip.setPlacement(TeachingTip.Placement.AUTO);                // or TOP / BOTTOM / LEFT / RIGHT
tip.show(saveButton);
```

The tip is drawn inside the window, follows its target when the window is resized or scrolled, and closes on Esc or on a click elsewhere (`setLightDismiss(false)` keeps it open). `setTailVisible(false)` turns it into a flyout and `setContent(component)` puts any component inside it.

### BreadcrumbBar

```java
BreadcrumbBar bar = new BreadcrumbBar("Home", "Documents", "Reports");
bar.setTruncateOnClick(true);                                          // clicking an item makes it the last one
bar.setItemIconProvider(o -> "Home".equals(o) ? new FontIcon(Symbol.HOME, 14) : null);
bar.addItemClickListener((b, index, item) -> open(index));
```

Items are any objects (`setItemTextProvider` names them). The last item is the current location: bold and not clickable. When the bar is too narrow the first items collapse into a "..." button that lists them in a menu. Keyboard: Left / Right / Home / End move between items, Enter or Space activate, Down opens the "..." menu.

### Progress states

```java
Tessel.progressState(progressBar, ProgressState.PAUSED);    // NORMAL (default), PAUSED, ERROR, SUCCESS
ring.setState(ProgressState.ERROR);                          // ProgressRing takes the same states
```

The bar is drawn in the accent, warning, danger or success color. A paused or failed indeterminate bar stops animating (paused leaves a resting segment in the middle; error and success fill the whole track), and a `ProgressRing` stops spinning and shows a resting arc (paused) or a full ring (error, success). An explicitly set `setForeground` is only used in the normal state.

### ColorPicker

```java
ColorPicker picker = new ColorPicker(Color.ORANGE);
picker.setAlphaEnabled(true);                                // adds the opacity slider and field
picker.addChangeListener(e -> use(picker.getColor()));
```

## Notes

- **Fonts:** Segoe UI and Segoe Fluent Icons (Windows 11) / Segoe MDL2 Assets (Windows 10). On other systems a similar UI font is used, and `FontIcon` glyphs stay blank; the controls' own icons (check marks, chevrons, status icons…) are vector-drawn and work everywhere.
- **TesselFrame** stays undecorated at the Swing level (it always paints its own title bar), but on Windows it hooks the native window procedure to get real DWM chrome: drop shadow, Windows 11 rounded corners, dragging/edge-resizing/maximizing/Win+Arrow snap and drag-to-edge Aero Snap all handled natively, plus a Snap Layouts flyout on hovering the maximize button. On macOS/Linux (or if the native hook can't install) it falls back to the previous pure-Java dragging/resizing/maximizing.
- **Texts:** the file chooser, internal frames, tool bar overflow and teaching tips show their own labels through `TesselStrings` (English, German, French, Spanish, Romanian; follows the default locale). Replace any of them with `UIManager.put("Tessel.fileName", "...")`. Standard dialog buttons (Open, Cancel, Save) come from the JDK's own translations.
- **System theme** is read when the theme is applied; call `Tessel.setTheme(TesselTheme.SYSTEM)` again to pick up a change.
