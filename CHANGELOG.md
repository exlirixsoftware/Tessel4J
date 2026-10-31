# Changelog

All notable changes to Tessel4J are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/): new components and features raise the minor version, and changes
that break existing code would raise the major version.

| Version | Date       | Summary |
| ------- | ---------- | ------- |
| [1.1.0](#110---2026-10-07) | 2026-10-07 | File and color choosers, tool bar overflow, internal frames, dialogs with Tessel window chrome, and new components. |
| [1.0.0](#100---2026-09-26) | 2026-09-26 | First release: themeable look and feel, every standard control, and the core Tessel components. |

---

## [1.1.0] - 2026-10-07

Everything below is additive unless it is listed under **Changed**. Code written against 1.0.0 keeps compiling and
running; the visible differences are described under **Changed** and **Upgrading from 1.0.0**.

### Added

#### Standard Swing controls that now have a Tessel look

These need no code changes: install the look and feel and use the usual Swing classes.

- **`JFileChooser`** (`TesselFileChooserUI`)
  - Breadcrumb address bar. Click a segment to jump to it, click the empty part to type a path, and a `...` button
    lists the folders that do not fit.
  - Places sidebar: home, desktop, documents, downloads, pictures (when they exist) and every drive.
  - Back, forward, up and new-folder buttons, with a navigation history.
  - List, details (name, date modified, type, size) and icon views. Hidden files are dimmed.
  - Works in dialogs (`showOpenDialog`, `showSaveDialog`, `showDialog`) and embedded in a window, honours
    multi-selection, file filters, directories-only mode, accessory components and `setControlButtonsAreShown`.
  - The name field and the approve button follow the dialog type, so the existing `JFileChooser` logic (typing a
    folder name opens it, wildcards, overwrite checks) is unchanged.
- **`JColorChooser`** (`TesselColorChooserUI`) hosts a single `ColorPicker` instead of the tabbed Swing panels.
  `getColor`/`setColor`, the selection model and its change listeners keep working, including replacing the
  selection model. Chooser panels and the preview panel are not used.
  - The picker includes an **opacity (transparency) slider** and an alpha field, so the chooser can return colors with
    an alpha channel; opaque colors compare equal to plain `Color`s as before. Hide it with
    `chooser.putClientProperty(Tessel.COLOR_CHOOSER_ALPHA, false)`.
- **`JTextPane` and `JEditorPane`** (`TesselTextPaneUI`, `TesselEditorPaneUI`) were drawn by the Basic delegates; they
  now match the other text controls.
  - Placeholder text (`Tessel.placeholder`), hover and focus outline in a scroll pane, themed selection and caret.
  - **HTML follows the theme.** A generated style sheet sets the text color and font, headings, links (accent), `code`,
    `pre`, block quotes and tables, and is re-applied when the theme, the accent or the pane's font changes. It is
    linked to the `HTMLDocument` instead of added to it, so `getText()` does not include it and a `<style>` in the
    content still takes precedence.
  - Undo and redo (Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z) for editable panes; a run of typed or deleted characters counts as
    one step. The history survives a theme change.
  - Ctrl+B, Ctrl+I and Ctrl+U on panes with a styled editor kit.
  - Context menu (Undo, Redo, Cut, Copy, Paste, Delete, Select all; Open link and Copy link address over an HTML
    link), also on the context-menu key and Shift+F10. A pane with its own `componentPopupMenu` keeps it, and
    `Tessel.CONTEXT_MENU` set to `false` turns the menu off.
- **`JToolBar`** (`TesselToolBarUI`, `TesselToolBarSeparatorUI`)
  - Rounded surface with an outline (`TesselBorders.ToolBarBorder`) and a dotted grip when the bar is floatable;
    floating and docking still work, in both orientations.
  - Thin divider lines for `addSeparator()`.
  - **Overflow menu:** buttons and separators that do not fit are hidden and offered in a "more" popup. Toggle
    buttons become check items. Turn it off with `toolBar.putClientProperty(Tessel.TOOLBAR_OVERFLOW, false)`.
    Text fields, combo boxes and other non-button components are never moved. The overflow button is the last child
    of the bar and is marked as a `UIResource`.
  - Stretchable items (`Box.createHorizontalGlue()`, text fields, combo boxes, sliders) share the spare room.
- **`JInternalFrame` and `JDesktopPane`** (`TesselInternalFrameUI`, `TesselInternalFrameTitlePane`,
  `TesselDesktopIconUI`)
  - Rounded frames with a soft shadow, a stronger outline while selected (accent for a minimized chip), and an invisible resize zone.
  - Fluent-style 36 px title bar: frame icon, title, and flat minimize / maximize / close buttons (the close button
    turns red on hover).
  - Minimized frames shrink to a compact chip with restore, maximize and close buttons.

#### New components (`net.tessel.laf.components`)

- **`BreadcrumbBar`**: a path of clickable items separated by chevrons, the last one being the current location (bold,
  not clickable). Items are any objects (`setItemTextProvider`, `setItemIconProvider`); `setTruncateOnClick(true)`
  removes the items after the clicked one; `addItemClickListener` reports the index and item. Too narrow a bar
  collapses its first items into a "..." menu; fully keyboard operable (Left / Right / Home / End, Enter / Space,
  Down for the menu).
- **`SplitButton`**: a button with a main action and a drop-down menu of alternatives. The menu is a plain
  `JPopupMenu` (`getMenu()`, `setMenu(...)`, `addMenuItem(...)`) or is built on demand with `setMenuSupplier(...)`.
  Supports every `ButtonStyle`, hover feedback on the arrow part, and opens from the keyboard with Down, Alt+Down or
  F4. The menu flips upwards or to the left when it would leave the screen.
- **`DropDownButton`**: a `SplitButton` whose whole surface opens the menu.
- **`NumberBox`**: number input with
  - `INLINE`, `COMPACT` or `HIDDEN` step buttons (hold a button to repeat);
  - stepping with Up/Down (`setSmallChange`), Page Up/Down (`setLargeChange`) and the mouse wheel;
  - minimum and maximum (values are clamped), optional wrap-around, prefix, suffix and fraction digits;
  - arithmetic expressions (`+ - * / % ^` and parentheses; `setAcceptsExpression(false)` turns this off);
  - locale-aware number parsing that understands both `1.5` and `1,5`;
  - an optional empty value (`setNullable`) with placeholder text;
  - a red flash and a restored value for text that cannot be evaluated, and a `value` property change event.
- **Progress states** for `JProgressBar` and `ProgressRing` (`ProgressState`: `NORMAL`, `PAUSED`, `ERROR`,
  `SUCCESS`; set with `Tessel.progressState(bar, state)` or `ProgressRing.setState`). The fill uses the accent,
  warning, danger or success color; paused and failed indeterminate indicators stop animating (a paused bar rests as a
  segment in the middle, error and success fill the whole track, rings show a resting arc or a full ring).
- **`TabView`**: closable, reorderable tabs.
  - Add (`setAddTabHandler`), close, middle-click to close, drag to reorder, tooltips, per-tab icons and
    `closable` flags, and free-form `Tab.setData(...)`.
  - Tabs shrink to fit and then scroll with arrow buttons or the mouse wheel; the selected tab is kept in view.
  - `TabViewListener` reports selection changes, closing (return `false` to veto), closed and moved tabs.
  - Keyboard: Left/Right/Home/End/Delete on the tab strip, Ctrl+W and Ctrl+F4 to close, Ctrl+PageUp/PageDown to
    switch (and Ctrl+Tab while the strip has focus).
- **`TeachingTip`**: a popover with a tail that points at a control.
  - Title, subtitle, icon, any content component, an accent action button, a plain close button and a corner "x".
  - Placement `AUTO` (picks the side with the most room), `TOP`, `BOTTOM`, `LEFT` or `RIGHT`; stays inside the window.
  - Follows its target when the window is resized, scrolled or moved, and closes when the target is hidden.
  - Light dismiss (Esc or a click elsewhere, on by default) and a `CloseReason` passed to close listeners.
  - `setTailVisible(false)` turns it into a plain flyout.
- **`RichTextEditor`**: a `RichTextToolBar` above a `JTextPane` in a scroll pane. `Format.STYLED` (default),
  `Format.HTML` and `Format.RTF`; `setText`/`getText` for the first two, `read`/`write` streams for all (RTF is an
  8-bit format). Also `setEditable`, `setToolBarVisible` and `setPlaceholder`.
- **`RichTextToolBar`**: undo and redo, font family and size, bold, italic, underline, strikethrough, text color and
  highlight (split buttons with swatches, "More colors..." and "No color"), four alignments and clear formatting.
  Follows the caret, is disabled for read-only panes, and works with any `JTextPane` or styled `JEditorPane`.
- **`ColorPicker`**: saturation / brightness square, hue slider, optional opacity slider, hex field, RGB(A) number
  boxes and a swatch row. Keyboard operable (arrow keys, Shift for bigger steps) and usable on its own.

#### Library additions

- **`TesselIcons.of(Kind, size)`**: 27 vector glyphs that do not depend on an icon font (plus, minus, more, four
  arrows, new folder, three view modes, drive, computer, home, desktop, documents, downloads, pictures, folder, file,
  minimize, maximize, restore, close, window, search, eyedropper). Unlike `FontIcon` they also render on macOS and
  Linux. They take the foreground color of the button they are painted on (including accent and danger buttons) or an
  explicit palette color.
- **`TesselStrings`**: the texts of the new controls (file chooser labels, window buttons, "More", ...) in English,
  German, French, Spanish and Romanian, following the default locale. Replace any text, or add a language, with
  `UIManager.put("Tessel.<key>", "...")`. Standard dialog buttons (Open, Cancel, Save) keep using the JDK's own
  translations.
- **`RichText`**: API for text panes. Formatting of the selection or the text typed next (`toggleBold`, `toggleItalic`,
  `toggleUnderline`, `toggleStrikeThrough`, `setFontFamily`, `setFontSize`, `setTextColor`, `setHighlight`,
  `setAlignment`, `clearFormatting`) with the matching queries (`isBold`, `fontFamily`, `characterColor`, `alignment`,
  ...; HTML tags such as `<b>` are recognized); undo (`undo`, `redo`, `canUndo`, `canRedo`, `discardUndo`,
  `undoManager`); and links (`enableLinks`, `disableLinks`, `linkAt`, `openLink`, `resolveLink`).
- **`Tessel.CONTEXT_MENU`**: client property that turns the text pane context menu off.
- `TesselStrings`: Undo, Redo, Cut, Copy, Paste, Delete, Select all, link and formatting texts in all five languages.
- Look and feel defaults `EditorPane.margin` and `TextPane.margin`, and the UI classes `EditorPaneUI` and `TextPaneUI`.
- **`Tessel.PROGRESS_STATE`**: client property holding the `ProgressState` of a `JProgressBar`.
- **`Tessel.TOOLBAR_OVERFLOW`**: client property that switches the tool bar overflow off.
- **`Tessel.COLOR_CHOOSER_ALPHA`**: client property that hides the `JColorChooser` opacity slider.
- **`TesselBorders.ToolBarBorder`** and **`TesselBorders.InternalFrameBorder`**.
- Look and feel defaults: `ToolBar.border`, `ToolBar.separatorSize`, `ToolBar.dockingBackground`,
  `ToolBar.dockingForeground`, `ToolBar.floatingBackground`, `ToolBar.floatingForeground`, `InternalFrame.border`,
  `InternalFrame.icon`, `InternalFrame.titleFont`, the four `InternalFrame.*TitleBackground/Foreground` colors,
  `DesktopIcon.border`, `FileChooser.usesSingleFilePane` and `FileChooser.readOnly`; and the UI classes
  `ToolBarUI`, `ToolBarSeparatorUI`, `InternalFrameUI`, `DesktopIconUI`, `FileChooserUI` and `ColorChooserUI`.

#### Gallery

- New **Tabs & Windows** page: a `TabView` of editors (closing a tab with text asks for confirmation through a
  `ContentDialog`) and a desktop pane with internal frames you can add, minimize, restore and close.
- New **Pickers** page: file dialogs (open, open several, save, choose folder), an embedded file chooser, the
  `ColorPicker` with an opacity switch, a button that applies the picked color as the app accent, and a
  `JColorChooser` dialog.
- New **Rich Text** page: a `RichTextEditor`, an HTML viewer with clickable links (through a handler) and its context
  menu, live HTML source next to its preview, and text panes in their placeholder, read-only and disabled states.
- New sections: split and drop-down buttons (Buttons), `NumberBox` variants (Text & Input), a tool bar with an
  adjustable width to try the overflow menu (Menus & Tooltips), and teaching tips in every placement plus a flyout
  (Feedback), progress states (Feedback) and a breadcrumb bar (Layout).

#### Dialogs
- **Dialogs with Tessel window chrome.** `JOptionPane`, `JFileChooser`, `JColorChooser` and plain `JDialog`s are
  decorated by the new `TesselRootPaneUI`: a themed 36 px title bar (window icon, title, close button), a thin
  outline, and Windows-native drop shadow, rounded corners, dragging, edge resizing and snapping through the existing
  `NativeWindowEffects`; on other systems dragging and resizing are done in Java. Decorated `JFrame`s additionally get
  minimize and maximize buttons. Root panes without a decoration style (such as `TesselFrame`'s) are untouched.
  - Works through Swing's `JDialog.setDefaultLookAndFeelDecorated`, which `Tessel.setup` now enables;
    `TesselLookAndFeel.getSupportsWindowDecorations()` returns `true`.
  - `Tessel.setDecoratedDialogs(boolean)` / `Tessel.isDecoratedDialogs()` turn the chrome off (system title bars)
    and on again for dialogs created afterwards.
  - `TesselRootPaneUI.requestClose(Window)`: closes a window like its title bar button (window listeners first, then
    the close operation).
- **`TesselOptionPaneUI`**: option panes in the style of `ContentDialog` - surface-colored message area, tinted footer
  with a divider and right-aligned buttons, default button in the accent style. New defaults:
  `OptionPane.messageAreaBorder`, `OptionPane.setButtonMargin`, `OptionPane.buttonPadding`,
  `OptionPane.buttonOrientation`, `OptionPane.messageAnchor`, `OptionPane.minimumSize`.
- **`TesselDialog`**: a `JDialog` that is always decorated, closes with Esc (`setCloseOnEscape`), offers `close()` and
  `showDialog()` (pack, center on the owner, show) and defaults to `DISPOSE_ON_CLOSE`.
- `TesselIcons.question(size)`: the "?" icon used by question option panes (`OptionPane.questionIcon`).
- Gallery: new **Dialogs** page with message, confirm, custom-button, text-input and list-input option panes, a modal
  `TesselDialog` form, a modeless one, a `ContentDialog`, and a switch for `setDecoratedDialogs`.

### Changed

- `ToolBar.border` is now a rounded surface with an outline instead of a plain 4 px empty border. Set
  `toolBar.setBorderPainted(false)` for the previous flat look; floatable bars also reserve room for the grip.
- **Tighter menus.** The icon (or text) of a popup menu item now starts about 25 px from the menu's edge instead of
  about 50 px: the Basic delegate's empty check-icon column is gone for menus without check or radio items
  (`MenuItem.checkIcon` / `Menu.checkIcon` are empty icons), popup items use a narrower left margin (`MenuItem.margin`,
  new `Tessel.popupMenuMargin` for sub-menus), and menus with check or radio items use a smaller check column
  (`*.checkIconOffset`, `*.afterCheckIconGap`). Menu bar entries keep their margins (`Menu.margin`).
- `Desktop.background` uses the `subtle` palette color (it was `background`) so internal frames stand out.
- The README documents the new controls, and its list of styled controls now includes `JFileChooser`,
  `JColorChooser`, `JInternalFrame` and `JDesktopPane`.
- `OptionPane.background` is now the `surface` color and `OptionPane.border` is empty (the padding moved into the
  message area), so option panes no longer have the grey page background.
- `NativeWindowEffects` only adds the resize and maximize styles to a `Dialog` when it is resizable (before, every
  dialog got them).
- Dialogs opened by the application now look different by default (Tessel title bar instead of the system one). Call
  `Tessel.setDecoratedDialogs(false)` before creating them to keep the previous behavior.

### Fixed

- `TesselListUI` drew the accent indicator bar on the selected item of wrapped (`HORIZONTAL_WRAP` /
  `VERTICAL_WRAP`) lists, where it looks wrong; it is now only drawn in vertical lists.
- `ButtonPainter` painted icons that are not `FontIcon`s with the component's raw foreground, so a vector icon on an
  accent or danger button was dark on a colored background. Vector glyphs from `TesselIcons.of(...)` now use the
  button's resolved foreground in every state and style.
- **White border and native title bar flashing when a dialog opens or closes.**
  - A light band, several pixels wide, flashed over the left, right and bottom edges of a `TesselFrame` for a frame
    whenever its activation changed (a modal dialog opening or closing, or another window being clicked). This was
    present since 1.0.0. `NativeWindowEffects` now handles the activation change without letting Windows draw its
    frame, and repaints the whole window afterwards.
  - The Windows title bar flashed for a frame or more while a decorated dialog faded out on close: the native hook
    was removed as soon as the dialog was disposed, and the frame styles that native snapping needs let Windows draw
    its own frame. The hook now stays until the window is destroyed, and the caption style is dropped as the window
    is hidden.
  - Decorated dialogs now use `NativeWindowEffects.installVisualOnly`: only the DWM shadow, rounded corners and
    dark / light setting, with no frame styles and no window procedure hook. Dialogs are dragged by their title bar
    and resized from their edges in Java (dialogs neither snap nor maximize). `TesselFrame` keeps full native chrome.

### Upgrading from 1.0.0

1. Replace `tessel-laf-1.0.0.jar` with the new jar; the API of 1.0.0 is unchanged, so no code changes are needed.
2. If you rely on the old flat tool bar, call `setBorderPainted(false)` on it.
3. If you embed a `JColorChooser` and used its chooser or preview panels, note that Tessel shows a `ColorPicker`
   and ignores `setChooserPanels`/`setPreviewPanel`.
4. If your application shows `JInternalFrame`s, give their content a transparent or `Palette` colored background:
   the frame body is painted by the frame itself.

### Known limitations

- `JFileChooser` has no column sorting, rename or search box, and uses vector icons only (folder, file,
  drive) instead of the system's file icons.
- The tool bar overflow only moves buttons and separators.
- `TabView` content that is an opaque `JPanel` paints the default panel color over the tab surface; make it
  non-opaque or put it in a `Card`.
- The text pane tool bar has no lists, links or images, and `HTMLEditorKit` writes formatting as `<b>`/`<font>` tags
  rather than CSS. In an HTML document, a selection that starts at position 0 also formats the invisible head.
- `TeachingTip` is drawn inside the window's layered pane, so it cannot extend beyond the window.
- Tested on Windows 11 with JDK 26 only; macOS and Linux are expected to work but were not exercised. It will be added in the next version.

---

## [1.0.0] - 2026-09-26

First release. Tessel4J is a themeable look and feel for Java Swing that matches the design of Tessel.NET.
It requires Java 17+, has no runtime dependencies for consumers (the jar bundles JNA for the Windows window
chrome), and is built with the JDK tools only (`build.ps1` / `build.sh`).

### Added

#### Theming (`net.tessel.laf`)

- `Tessel.setup(...)` installs the look and feel (`TesselLookAndFeel` can also be installed through
  `UIManager.setLookAndFeel`); `Tessel.setTheme(...)` and `Tessel.setAccent(...)` update every open window at
  runtime; `Tessel.addThemeListener(...)` reports changes.
- Light, dark and system themes (`TesselTheme`), the system theme being read on Windows, macOS and GNOME.
- Custom accent colors, or the Windows accent through `Tessel.useSystemAccent()`; hover, pressed, subtle and
  on-accent shades are derived automatically.
- `Palette`: every color of the theme (background, surface, text levels, borders, control states, accent and status
  colors, shadow), also published as `UIManager` keys `Tessel.<name>`. `Palette.mix`, `withAlpha` and `luminance`
  help custom painting.
- `TextStyle` (`DISPLAY`, `TITLE`, `SUBTITLE`, `HEADER`, `BODY_STRONG`, `BODY`, `SECONDARY`, `CAPTION`, `CODE`),
  `ButtonStyle` (`STANDARD`, `ACCENT`, `OUTLINE`, `SUBTLE`, `DANGER`, `ICON`, `LINK`) and `Severity`
  (`INFORMATIONAL`, `SUCCESS`, `WARNING`, `ERROR`).
- `Fonts`: Segoe UI and Segoe Fluent Icons / Segoe MDL2 Assets on Windows, with sensible fallbacks elsewhere.
- Helpers and client properties: `Tessel.style(button, style)`, `Tessel.textStyle(...)`, `Tessel.label(...)`,
  `Tessel.placeholder(...)` (text fields, password fields, text areas, combo boxes), `Tessel.leadingIcon(...)` and the
  check box `Tessel.INDETERMINATE` state.

#### Restyled standard controls (`net.tessel.laf.ui`)

Rounded, flat, Fluent-inspired delegates with hover, pressed, focus-ring (keyboard focus only) and disabled states:

- Buttons: `JButton`, `JToggleButton`, `JCheckBox` (with an indeterminate state), `JRadioButton`.
- Text: `JLabel`, `JTextField`, `JPasswordField`, `JFormattedTextField`, `JTextArea`, with placeholder text and a
  leading icon; `JEditorPane` and `JTextPane` through defaults.
- Selection and ranges: `JComboBox`, `JSpinner` (number-box style with minus and plus buttons), `JSlider`,
  `JProgressBar` (determinate and indeterminate).
- Collections: `JList` (rounded hover and selection), `JTable` and its header, `JTree`.
- Containers: `JScrollPane` and `JScrollBar`, `JTabbedPane` (accent underline), `JSplitPane`, `JSeparator`.
- Menus: `JMenuBar`, `JMenu`, `JMenuItem`, `JCheckBoxMenuItem`, `JRadioButtonMenuItem`, `JPopupMenu` and popup
  separators, with accelerators and sub-menus.
- Through defaults: `JToolTip`, `JToolBar`, `JOptionPane` (with severity icons), `TitledBorder`, `JPanel`,
  `JViewport`, `JRootPane`.
- Disabled icons are drawn translucent through the look and feel's `getDisabledIcon`.

#### Components (`net.tessel.laf.components`)

- `TesselFrame`: a frame with a custom, themed title bar and room for extra title bar components.
  - On Windows it hooks the native window procedure for DWM drop shadow, Windows 11 rounded corners, native
    dragging, edge resizing, Aero Snap and the Snap Layouts flyout on the maximize button.
  - On macOS and Linux (or when the hook cannot install) it falls back to pure-Java dragging, resizing and
    maximizing.
- `NavigationView`: a side pane with items, section headers, footer items, an optional pane header (such as a
  search box), lazily created and cached pages, a collapsible compact pane and selection listeners.
- `ContentDialog`: a modal dialog drawn over the window content, with primary, secondary and close buttons, a
  destructive style, veto handlers, `showModal` and `showAsync`.
- `Snackbar`: transient notifications stacked in the corner of a window, paused while hovered.
- `InfoBar`: an inline notification with a severity icon, title, message, optional action and close button.
- `Card` (optionally elevated, with a header) and `SettingsCard` (icon, title, description and a trailing control).
- `Expander`: a collapsible section.
- `ToggleSwitch`, `ProgressRing`, `Badge`, `Avatar` (initials or image) and `TextBlock` (wrapping text with a
  `TextStyle`).
- `FontIcon` and `Symbol` (a set of common Segoe Fluent glyphs), plus the look and feel's own vector icons for check
  marks, chevrons, status icons, folders and files, which work without an icon font.

#### Gallery (`tessel-gallery`)

- A demo application with a navigation view that shows every control: Home, Buttons, Text & Input, Selection,
  Collections, Feedback, Layout, Menus & Tooltips, Colors & Type, Icons and Settings (theme and accent switching).

#### Build

- `build.ps1` and `build.sh` compile the library and the gallery with the JDK only, bundle JNA into
  `tessel-laf-<version>.jar`, and write `tessel-laf-<version>-sources.jar` and `tessel-gallery-<version>.jar` to
  `dist/`.

[1.1.0]: #110---2026-10-07
[1.0.0]: #100---2026-09-26
