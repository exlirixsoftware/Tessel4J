package net.tessel.laf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.text.DefaultEditorKit;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.TesselBorders;
import net.tessel.laf.ui.TesselInputBorder;
import net.tessel.laf.ui.TesselListUI;
import net.tessel.laf.ui.UIUtils;

/**
 * The Tessel look and feel. Prefer {@link Tessel#setup(TesselTheme)}, which also enables runtime theme switching;
 * {@code UIManager.setLookAndFeel(new TesselLookAndFeel())} works as well.
 */
public class TesselLookAndFeel extends BasicLookAndFeel {

    private final Palette palette;

    /** Uses the current palette (light by default). */
    public TesselLookAndFeel() {
        this(Tessel.palette());
    }

    public TesselLookAndFeel(Palette palette) {
        this.palette = palette;
    }

    public Palette getPalette() {
        return palette;
    }

    @Override
    public String getName() {
        return "Tessel";
    }

    @Override
    public String getID() {
        return "Tessel";
    }

    @Override
    public String getDescription() {
        return "Tessel - a modern light and dark look and feel";
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    @Override
    public boolean getSupportsWindowDecorations() {
        // Lets decorated JDialogs (JOptionPane, file and color choosers, ...) use Tessel's title bar.
        return true;
    }

    @Override
    public void initialize() {
        super.initialize();
        Tessel.onInstalled(this);
        UIUtils.installFocusTracker();
    }

    @Override
    public void uninitialize() {
        UIUtils.uninstallFocusTracker();
        super.uninitialize();
    }

    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return icon == null ? null : new TesselIcons.AlphaIcon(icon, UIUtils.DISABLED_ALPHA);
    }

    // ------------------------------------------------------------------ defaults

    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        String p = "net.tessel.laf.ui.";
        table.putDefaults(new Object[]{
                "ButtonUI", p + "TesselButtonUI",
                "ToggleButtonUI", p + "TesselToggleButtonUI",
                "CheckBoxUI", p + "TesselCheckBoxUI",
                "RadioButtonUI", p + "TesselRadioButtonUI",
                "LabelUI", p + "TesselLabelUI",
                "TextFieldUI", p + "TesselTextFieldUI",
                "PasswordFieldUI", p + "TesselPasswordFieldUI",
                "FormattedTextFieldUI", p + "TesselFormattedTextFieldUI",
                "TextAreaUI", p + "TesselTextAreaUI",
                "EditorPaneUI", p + "TesselEditorPaneUI",
                "TextPaneUI", p + "TesselTextPaneUI",
                "ComboBoxUI", p + "TesselComboBoxUI",
                "ListUI", p + "TesselListUI",
                "ScrollBarUI", p + "TesselScrollBarUI",
                "ScrollPaneUI", p + "TesselScrollPaneUI",
                "SliderUI", p + "TesselSliderUI",
                "ProgressBarUI", p + "TesselProgressBarUI",
                "TabbedPaneUI", p + "TesselTabbedPaneUI",
                "TableUI", p + "TesselTableUI",
                "TableHeaderUI", p + "TesselTableHeaderUI",
                "TreeUI", p + "TesselTreeUI",
                "SpinnerUI", p + "TesselSpinnerUI",
                "SplitPaneUI", p + "TesselSplitPaneUI",
                "SeparatorUI", p + "TesselSeparatorUI",
                "PopupMenuSeparatorUI", p + "TesselSeparatorUI",
                "MenuItemUI", p + "TesselMenuUIs$MenuItemUI",
                "MenuUI", p + "TesselMenuUIs$MenuUI",
                "CheckBoxMenuItemUI", p + "TesselMenuUIs$CheckBoxMenuItemUI",
                "RadioButtonMenuItemUI", p + "TesselMenuUIs$RadioButtonMenuItemUI",
                "ToolBarUI", p + "TesselToolBarUI",
                "ToolBarSeparatorUI", p + "TesselToolBarSeparatorUI",
                "InternalFrameUI", p + "TesselInternalFrameUI",
                "DesktopIconUI", p + "TesselDesktopIconUI",
                "FileChooserUI", p + "TesselFileChooserUI",
                "ColorChooserUI", p + "TesselColorChooserUI",
                "RootPaneUI", p + "TesselRootPaneUI",
                "OptionPaneUI", p + "TesselOptionPaneUI",
        });
        table.put("ClassLoader", TesselLookAndFeel.class.getClassLoader());
    }

    @Override
    protected void initSystemColorDefaults(UIDefaults table) {
        super.initSystemColorDefaults(table);
        Palette p = palette;
        table.putDefaults(new Object[]{
                "desktop", c(p.background),
                "activeCaption", c(p.surface),
                "activeCaptionText", c(p.text),
                "activeCaptionBorder", c(p.border),
                "inactiveCaption", c(p.background),
                "inactiveCaptionText", c(p.textSecondary),
                "inactiveCaptionBorder", c(p.border),
                "window", c(p.surface),
                "windowBorder", c(p.border),
                "windowText", c(p.text),
                "menu", c(p.surface),
                "menuText", c(p.text),
                "text", c(p.inputBackground),
                "textText", c(p.text),
                "textHighlight", c(p.accent),
                "textHighlightText", c(p.onAccent),
                "textInactiveText", c(p.textDisabled),
                "control", c(p.background),
                "controlText", c(p.text),
                "controlHighlight", c(p.surface),
                "controlLtHighlight", c(p.surface),
                "controlShadow", c(p.border),
                "controlDkShadow", c(p.borderStrong),
                "scrollbar", c(p.background),
                "info", c(p.surface),
                "infoText", c(p.text),
        });
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        // Localized texts of the file chooser ("File name:", "Details", ...) live in Metal's bundle.
        table.addResourceBundle("com.sun.swing.internal.plaf.metal.resources.metal");
        Palette p = palette;

        FontUIResource font = new FontUIResource(Fonts.regular(14));
        FontUIResource small = new FontUIResource(Fonts.regular(12));
        FontUIResource smallSemibold = new FontUIResource(Fonts.semibold(12));
        List<Object> fontKeys = new ArrayList<>();
        for (Object key : table.keySet()) {
            if (key instanceof String && ((String) key).endsWith(".font")) fontKeys.add(key);
        }
        for (Object key : fontKeys) table.put(key, font);

        ColorUIResource background = c(p.background);
        ColorUIResource surface = c(p.surface);
        ColorUIResource subtle = c(p.subtle);
        ColorUIResource border = c(p.border);
        ColorUIResource text = c(p.text);
        ColorUIResource textSecondary = c(p.textSecondary);
        ColorUIResource textTertiary = c(p.textTertiary);
        ColorUIResource textDisabled = c(p.textDisabled);
        ColorUIResource control = c(p.controlBackground);
        ColorUIResource controlHover = c(p.controlHover);
        ColorUIResource controlPressed = c(p.controlPressed);
        ColorUIResource input = c(p.inputBackground);
        ColorUIResource accent = c(p.accent);
        ColorUIResource onAccent = c(p.onAccent);
        ColorUIResource accentSubtle = c(p.accentSubtle);
        ColorUIResource transparent = new ColorUIResource(new Color(0, 0, 0, 0));

        Object marginBorder = new BasicBorders.MarginBorder();
        Object empty0 = new BorderUIResource.EmptyBorderUIResource(0, 0, 0, 0);
        Object cellBorder = new BorderUIResource.EmptyBorderUIResource(0, 12, 0, 12);
        Object listCellBorder = new BorderUIResource.EmptyBorderUIResource(8, 12, 8, 12);
        Object textAreaBorder = new BorderUIResource.EmptyBorderUIResource(6, 10, 6, 10);
        Object scrollPaneBorder = new TesselBorders.ScrollPaneBorder();

        UIDefaults.ActiveValue listRenderer = t -> new TesselListUI.TesselListCellRenderer();

        Object[] defaults = {
                // Palette, for application code
                "Tessel.background", background,
                "Tessel.surface", surface,
                "Tessel.surfaceAlt", c(p.surfaceAlt),
                "Tessel.subtle", subtle,
                "Tessel.border", border,
                "Tessel.borderStrong", c(p.borderStrong),
                "Tessel.text", text,
                "Tessel.textSecondary", textSecondary,
                "Tessel.textTertiary", textTertiary,
                "Tessel.textDisabled", textDisabled,
                "Tessel.controlBackground", control,
                "Tessel.controlHover", controlHover,
                "Tessel.controlPressed", controlPressed,
                "Tessel.inputBackground", input,
                "Tessel.accent", accent,
                "Tessel.accentHover", c(p.accentHover),
                "Tessel.accentPressed", c(p.accentPressed),
                "Tessel.accentSubtle", accentSubtle,
                "Tessel.onAccent", onAccent,
                "Tessel.success", c(p.success),
                "Tessel.warning", c(p.warning),
                "Tessel.danger", c(p.danger),
                "Tessel.info", c(p.info),

                // General
                "Panel.background", background,
                "Panel.foreground", text,
                "Label.background", background,
                "Label.foreground", text,
                "Label.disabledForeground", textDisabled,
                "Viewport.background", surface,
                "Viewport.foreground", text,
                "RootPane.background", background,
                "Desktop.background", subtle,
                "ColorChooser.background", background,
                "ColorChooser.foreground", text,

                // Buttons
                "Button.background", control,
                "Button.foreground", text,
                "Button.border", marginBorder,
                "Button.margin", new InsetsUIResource(6, 14, 6, 14),
                "Button.rollover", Boolean.TRUE,
                "Button.textShiftOffset", 0,
                "Button.disabledText", textDisabled,
                "Button.select", controlPressed,
                "Button.focus", accent,
                "Button.defaultButtonFollowsFocus", Boolean.FALSE,
                "ToggleButton.background", control,
                "ToggleButton.foreground", text,
                "ToggleButton.border", marginBorder,
                "ToggleButton.margin", new InsetsUIResource(6, 14, 6, 14),
                "ToggleButton.rollover", Boolean.TRUE,
                "ToggleButton.textShiftOffset", 0,
                "ToggleButton.disabledText", textDisabled,
                "ToggleButton.select", accent,
                "ToggleButton.focus", accent,
                "CheckBox.background", background,
                "CheckBox.foreground", text,
                "CheckBox.icon", new TesselIcons.CheckBoxIcon(),
                "CheckBox.border", marginBorder,
                "CheckBox.margin", new InsetsUIResource(4, 2, 4, 2),
                "CheckBox.rollover", Boolean.TRUE,
                "CheckBox.disabledText", textDisabled,
                "CheckBox.focus", accent,
                "RadioButton.background", background,
                "RadioButton.foreground", text,
                "RadioButton.icon", new TesselIcons.RadioIcon(),
                "RadioButton.border", marginBorder,
                "RadioButton.margin", new InsetsUIResource(4, 2, 4, 2),
                "RadioButton.rollover", Boolean.TRUE,
                "RadioButton.disabledText", textDisabled,
                "RadioButton.focus", accent,

                // Text
                "TextField.background", input,
                "TextField.foreground", text,
                "TextField.caretForeground", text,
                "TextField.selectionBackground", accent,
                "TextField.selectionForeground", onAccent,
                "TextField.inactiveForeground", textDisabled,
                "TextField.inactiveBackground", input,
                "TextField.disabledBackground", input,
                "TextField.border", new TesselInputBorder(6, 10, 6, 10),
                "TextField.margin", new InsetsUIResource(0, 0, 0, 0),
                "PasswordField.background", input,
                "PasswordField.foreground", text,
                "PasswordField.caretForeground", text,
                "PasswordField.selectionBackground", accent,
                "PasswordField.selectionForeground", onAccent,
                "PasswordField.inactiveForeground", textDisabled,
                "PasswordField.inactiveBackground", input,
                "PasswordField.border", new TesselInputBorder(6, 10, 6, 10),
                "PasswordField.margin", new InsetsUIResource(0, 0, 0, 0),
                "PasswordField.echoChar", '•',
                "FormattedTextField.background", input,
                "FormattedTextField.foreground", text,
                "FormattedTextField.caretForeground", text,
                "FormattedTextField.selectionBackground", accent,
                "FormattedTextField.selectionForeground", onAccent,
                "FormattedTextField.inactiveForeground", textDisabled,
                "FormattedTextField.inactiveBackground", input,
                "FormattedTextField.border", new TesselInputBorder(6, 10, 6, 10),
                "FormattedTextField.margin", new InsetsUIResource(0, 0, 0, 0),
                "TextArea.background", input,
                "TextArea.foreground", text,
                "TextArea.caretForeground", text,
                "TextArea.selectionBackground", accent,
                "TextArea.selectionForeground", onAccent,
                "TextArea.inactiveForeground", textDisabled,
                "TextArea.border", textAreaBorder,
                "TextArea.margin", new InsetsUIResource(0, 0, 0, 0),
                "EditorPane.background", input,
                "EditorPane.foreground", text,
                "EditorPane.caretForeground", text,
                "EditorPane.selectionBackground", accent,
                "EditorPane.selectionForeground", onAccent,
                "EditorPane.inactiveForeground", textDisabled,
                "EditorPane.border", textAreaBorder,
                "EditorPane.margin", new InsetsUIResource(0, 0, 0, 0),
                "TextPane.background", input,
                "TextPane.foreground", text,
                "TextPane.caretForeground", text,
                "TextPane.selectionBackground", accent,
                "TextPane.selectionForeground", onAccent,
                "TextPane.inactiveForeground", textDisabled,
                "TextPane.border", textAreaBorder,
                "TextPane.margin", new InsetsUIResource(0, 0, 0, 0),

                // Combo box
                "ComboBox.background", control,
                "ComboBox.foreground", text,
                "ComboBox.buttonBackground", control,
                "ComboBox.selectionBackground", controlHover,
                "ComboBox.selectionForeground", text,
                "ComboBox.disabledBackground", control,
                "ComboBox.disabledForeground", textDisabled,
                "ComboBox.border", new TesselInputBorder(1, 12, 1, 1),
                "ComboBox.padding", new InsetsUIResource(5, 0, 5, 0),
                "ComboBox.squareButton", Boolean.FALSE,
                "ComboBox.isEnterSelectablePopup", Boolean.TRUE,

                // List
                "List.background", surface,
                "List.foreground", text,
                "List.selectionBackground", subtle,
                "List.selectionForeground", text,
                "List.cellNoFocusBorder", listCellBorder,
                "List.focusCellHighlightBorder", listCellBorder,
                "List.focusSelectedCellHighlightBorder", listCellBorder,
                "List.cellRenderer", listRenderer,
                "List.dropLineColor", accent,

                // Table
                "Table.background", surface,
                "Table.foreground", text,
                "Table.selectionBackground", accentSubtle,
                "Table.selectionForeground", text,
                "Table.gridColor", border,
                "Table.focusCellHighlightBorder", cellBorder,
                "Table.focusSelectedCellHighlightBorder", cellBorder,
                "Table.cellNoFocusBorder", cellBorder,
                "Table.scrollPaneBorder", scrollPaneBorder,
                "Table.ascendingSortIcon", TesselIcons.chevron(TesselIcons.Direction.UP, 12),
                "Table.descendingSortIcon", TesselIcons.chevron(TesselIcons.Direction.DOWN, 12),
                "Table.dropLineColor", accent,
                "Table.dropLineShortColor", accent,
                "TableHeader.background", surface,
                "TableHeader.foreground", textSecondary,
                "TableHeader.font", smallSemibold,
                "TableHeader.cellBorder", new TesselBorders.TableHeaderBorder(),

                // Tree
                "Tree.background", surface,
                "Tree.foreground", text,
                "Tree.textForeground", text,
                "Tree.textBackground", surface,
                "Tree.selectionBackground", subtle,
                "Tree.selectionForeground", text,
                "Tree.selectionBorderColor", transparent,
                "Tree.rendererFillBackground", Boolean.FALSE,
                "Tree.drawsFocusBorderAroundIcon", Boolean.FALSE,
                "Tree.paintLines", Boolean.FALSE,
                "Tree.leftChildIndent", 8,
                "Tree.rightChildIndent", 14,
                "Tree.expandedIcon", TesselIcons.chevron(TesselIcons.Direction.DOWN, 16),
                "Tree.collapsedIcon", TesselIcons.chevron(TesselIcons.Direction.RIGHT, 16),
                "Tree.openIcon", new TesselIcons.FolderIcon(),
                "Tree.closedIcon", new TesselIcons.FolderIcon(),
                "Tree.leafIcon", new TesselIcons.FileIcon(),
                "Tree.hash", border,
                "Tree.editorBorder", new BorderUIResource(new TesselInputBorder(2, 4, 2, 4)),

                // Scrolling
                "ScrollPane.background", background,
                "ScrollPane.foreground", text,
                "ScrollPane.border", scrollPaneBorder,
                "ScrollBar.width", 12,
                "ScrollBar.background", background,
                "ScrollBar.track", background,
                "ScrollBar.thumb", textTertiary,
                "ScrollBar.minimumThumbSize", new DimensionUIResource(12, 32),

                // Tabs
                "TabbedPane.background", background,
                "TabbedPane.foreground", text,
                "TabbedPane.tabInsets", new InsetsUIResource(10, 14, 10, 14),
                "TabbedPane.selectedTabPadInsets", new InsetsUIResource(0, 0, 0, 0),
                "TabbedPane.tabAreaInsets", new InsetsUIResource(0, 0, 0, 0),
                "TabbedPane.contentBorderInsets", new InsetsUIResource(17, 0, 0, 0),
                "TabbedPane.tabRunOverlay", 0,
                "TabbedPane.textIconGap", 8,
                "TabbedPane.contentOpaque", Boolean.FALSE,
                "TabbedPane.tabsOpaque", Boolean.FALSE,
                "TabbedPane.tabsOverlapBorder", Boolean.FALSE,
                "TabbedPane.selected", background,
                "TabbedPane.focus", accent,
                "TabbedPane.light", border,
                "TabbedPane.highlight", border,
                "TabbedPane.shadow", border,
                "TabbedPane.darkShadow", border,
                "TabbedPane.contentAreaColor", background,

                // Slider & progress
                "Slider.background", background,
                "Slider.foreground", accent,
                "Slider.tickColor", textTertiary,
                "Slider.focus", accent,
                "Slider.focusInsets", new InsetsUIResource(2, 2, 2, 2),
                "ProgressBar.background", subtle,
                "ProgressBar.foreground", accent,
                "ProgressBar.selectionBackground", text,
                "ProgressBar.selectionForeground", onAccent,
                "ProgressBar.border", empty0,
                "ProgressBar.cycleTime", 1400,
                "ProgressBar.repaintInterval", 20,
                "ProgressBar.horizontalSize", new DimensionUIResource(146, 4),
                "ProgressBar.verticalSize", new DimensionUIResource(4, 146),

                // Spinner
                "Spinner.background", input,
                "Spinner.foreground", text,
                "Spinner.border", new TesselInputBorder(1, 10, 1, 3),
                "Spinner.editorBorderPainted", Boolean.FALSE,
                "Spinner.editorAlignment", SwingConstants.LEADING,

                // Menus
                "MenuBar.background", background,
                "MenuBar.foreground", text,
                "MenuBar.border", new BorderUIResource.EmptyBorderUIResource(2, 2, 2, 2),
                "PopupMenu.background", surface,
                "PopupMenu.foreground", text,
                "PopupMenu.border", new TesselBorders.PopupBorder(4, 4, 4, 4),
                "PopupMenuSeparator.foreground", border,
                "Menu.arrowIcon", TesselIcons.chevron(TesselIcons.Direction.RIGHT, 16),
                "Menu.menuPopupOffsetY", 3,
                "Menu.submenuPopupOffsetX", 2,
                "Menu.submenuPopupOffsetY", -5,
                "CheckBoxMenuItem.checkIcon", new TesselIcons.MenuCheckIcon(),
                "RadioButtonMenuItem.checkIcon", new TesselIcons.MenuRadioIcon(),
                "Separator.foreground", border,
                "Separator.background", border,

                // Tooltip
                "ToolTip.background", surface,
                "ToolTip.foreground", text,
                "ToolTip.border", new TesselBorders.PopupBorder(6, 10, 6, 10),
                "ToolTip.font", small,

                // Tool bar & split pane
                "ToolBar.background", background,
                "ToolBar.foreground", text,
                "ToolBar.border", new TesselBorders.ToolBarBorder(),
                "ToolBar.isRollover", Boolean.TRUE,
                "ToolBar.rolloverBorder", marginBorder,
                "ToolBar.nonrolloverBorder", marginBorder,
                "ToolBar.borderColor", border,
                "ToolBar.separatorSize", new DimensionUIResource(9, 24),
                "ToolBar.dockingBackground", surface,
                "ToolBar.dockingForeground", accent,
                "ToolBar.floatingBackground", surface,
                "ToolBar.floatingForeground", c(p.borderStrong),
                "SplitPane.background", background,
                "SplitPane.dividerSize", 7,
                "SplitPane.border", empty0,
                "SplitPaneDivider.border", empty0,
                "SplitPaneDivider.draggingColor", c(p.borderStrong),

                // Option pane & titled border
                "OptionPane.background", surface,
                "OptionPane.foreground", text,
                "OptionPane.messageForeground", text,
                "OptionPane.border", empty0,
                "OptionPane.messageAreaBorder", new BorderUIResource.EmptyBorderUIResource(24, 24, 24, 24),
                "OptionPane.buttonAreaBorder", empty0,
                "OptionPane.sameSizeButtons", Boolean.TRUE,
                "OptionPane.setButtonMargin", Boolean.FALSE,
                "OptionPane.buttonPadding", 8,
                "OptionPane.buttonOrientation", SwingConstants.RIGHT,
                "OptionPane.messageAnchor", java.awt.GridBagConstraints.WEST,
                "OptionPane.minimumSize", new DimensionUIResource(340, 120),
                "OptionPane.informationIcon", TesselIcons.severity(Severity.INFORMATIONAL, 32),
                "OptionPane.warningIcon", TesselIcons.severity(Severity.WARNING, 32),
                "OptionPane.errorIcon", TesselIcons.severity(Severity.ERROR, 32),
                "OptionPane.questionIcon", TesselIcons.question(32),
                // Internal frames, file chooser
                "InternalFrame.border", new TesselBorders.InternalFrameBorder(),
                "DesktopIcon.border", new TesselBorders.InternalFrameBorder(),
                "InternalFrame.icon", TesselIcons.of(TesselIcons.Kind.WINDOW, 16, pal -> pal.textSecondary),
                "InternalFrame.titleFont", new FontUIResource(Fonts.regular(13)),
                "InternalFrame.activeTitleBackground", surface,
                "InternalFrame.activeTitleForeground", text,
                "InternalFrame.inactiveTitleBackground", surface,
                "InternalFrame.inactiveTitleForeground", textTertiary,
                "FileChooser.usesSingleFilePane", Boolean.TRUE,
                "FileChooser.readOnly", Boolean.FALSE,
                "TitledBorder.border", new TesselBorders.RoundedLineBorder(8),
                "TitledBorder.titleColor", text,
                "TitledBorder.font", new FontUIResource(Fonts.semibold(14)),
        };
        table.putDefaults(defaults);

        // Accelerators and small print use the small font.
        for (String prefix : new String[]{"MenuItem", "Menu", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
            table.putDefaults(new Object[]{
                    prefix + ".background", surface,
                    prefix + ".foreground", text,
                    prefix + ".selectionBackground", controlHover,
                    prefix + ".selectionForeground", text,
                    prefix + ".disabledForeground", textDisabled,
                    prefix + ".acceleratorForeground", textTertiary,
                    prefix + ".acceleratorSelectionForeground", textTertiary,
                    prefix + ".acceleratorFont", small,
                    // Items in a popup need little room on the left: the icon (or text) already keeps a gap.
                    prefix + ".margin", new InsetsUIResource(6, 2, 6, 8),
                    // Room around the check mark column of menus that have check or radio items.
                    prefix + ".checkIconOffset", 4,
                    prefix + ".afterCheckIconGap", 8,
                    prefix + ".opaque", Boolean.FALSE,
                    prefix + ".borderPainted", Boolean.FALSE,
            });
        }
        table.put("Menu.background", background);
        // Plain items and sub-menus have no check icon of their own, but the Basic delegate would still reserve
        // an empty column for one (the column reappears by itself when a menu has check or radio items).
        table.put("MenuItem.checkIcon", new TesselIcons.EmptyIcon(0, 0));
        table.put("Menu.checkIcon", new TesselIcons.EmptyIcon(0, 0));
        // Top-level menus (in a menu bar) keep the wider side margins; TesselMenuUIs.MenuUI switches between them.
        table.put("Menu.margin", new InsetsUIResource(6, 8, 6, 8));
        table.put("Tessel.popupMenuMargin", new InsetsUIResource(6, 2, 6, 8));
        installTextKeyBindings(table);
    }

    /**
     * Key bindings of the text controls (arrow keys, Backspace, Delete, Home/End, clipboard and select-all
     * shortcuts, Enter and Tab in multi-line text). {@code BasicLookAndFeel} does not define them: Metal and
     * the other look and feels bring their own, and without them these keys do nothing in a text component
     * (typing still works).
     */
    private static void installTextKeyBindings(UIDefaults table) {
        Object[] field = textBindings(false, false);
        Object[] password = textBindings(false, true);
        Object[] multiline = textBindings(true, false);
        table.put("TextField.focusInputMap", new UIDefaults.LazyInputMap(field));
        // A formatted text field (and so the spinner editors) also reverts on Esc and steps with Up / Down.
        List<Object> formatted = new ArrayList<>(java.util.Arrays.asList(field));
        formatted.addAll(java.util.Arrays.asList("ESCAPE", "reset-field-edit",
                "UP", "increment", "KP_UP", "increment", "DOWN", "decrement", "KP_DOWN", "decrement"));
        table.put("FormattedTextField.focusInputMap", new UIDefaults.LazyInputMap(formatted.toArray()));
        table.put("PasswordField.focusInputMap", new UIDefaults.LazyInputMap(password));
        table.put("TextArea.focusInputMap", new UIDefaults.LazyInputMap(multiline));
        table.put("TextPane.focusInputMap", new UIDefaults.LazyInputMap(multiline));
        table.put("EditorPane.focusInputMap", new UIDefaults.LazyInputMap(multiline));
    }

    private static Object[] textBindings(boolean multiline, boolean password) {
        String copy = password ? DefaultEditorKit.beepAction : DefaultEditorKit.copyAction;
        String cut = password ? DefaultEditorKit.beepAction : DefaultEditorKit.cutAction;
        String wordLeft = password ? DefaultEditorKit.beginLineAction : DefaultEditorKit.previousWordAction;
        String wordRight = password ? DefaultEditorKit.endLineAction : DefaultEditorKit.nextWordAction;
        String selWordLeft = password ? DefaultEditorKit.selectionBeginLineAction : DefaultEditorKit.selectionPreviousWordAction;
        String selWordRight = password ? DefaultEditorKit.selectionEndLineAction : DefaultEditorKit.selectionNextWordAction;
        String deleteWordBack = password ? DefaultEditorKit.beepAction : DefaultEditorKit.deletePrevWordAction;
        String deleteWordNext = password ? DefaultEditorKit.beepAction : DefaultEditorKit.deleteNextWordAction;
        List<Object> b = new ArrayList<>(java.util.Arrays.asList(
                "ctrl C", copy, "ctrl V", DefaultEditorKit.pasteAction, "ctrl X", cut,
                "COPY", copy, "PASTE", DefaultEditorKit.pasteAction, "CUT", cut,
                "control INSERT", copy, "shift INSERT", DefaultEditorKit.pasteAction, "shift DELETE", cut,
                "RIGHT", DefaultEditorKit.forwardAction, "KP_RIGHT", DefaultEditorKit.forwardAction,
                "LEFT", DefaultEditorKit.backwardAction, "KP_LEFT", DefaultEditorKit.backwardAction,
                "shift RIGHT", DefaultEditorKit.selectionForwardAction, "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                "shift LEFT", DefaultEditorKit.selectionBackwardAction, "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                "ctrl LEFT", wordLeft, "ctrl KP_LEFT", wordLeft, "ctrl RIGHT", wordRight, "ctrl KP_RIGHT", wordRight,
                "ctrl shift LEFT", selWordLeft, "ctrl shift KP_LEFT", selWordLeft,
                "ctrl shift RIGHT", selWordRight, "ctrl shift KP_RIGHT", selWordRight,
                "ctrl A", DefaultEditorKit.selectAllAction,
                "HOME", DefaultEditorKit.beginLineAction, "END", DefaultEditorKit.endLineAction,
                "shift HOME", DefaultEditorKit.selectionBeginLineAction, "shift END", DefaultEditorKit.selectionEndLineAction,
                "BACK_SPACE", DefaultEditorKit.deletePrevCharAction, "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                "ctrl H", DefaultEditorKit.deletePrevCharAction,
                "DELETE", DefaultEditorKit.deleteNextCharAction,
                "ctrl BACK_SPACE", deleteWordBack, "ctrl DELETE", deleteWordNext,
                "ctrl BACK_SLASH", "unselect",
                "control shift O", "toggle-componentOrientation"));
        if (multiline) {
            b.addAll(java.util.Arrays.asList(
                    "UP", DefaultEditorKit.upAction, "KP_UP", DefaultEditorKit.upAction,
                    "DOWN", DefaultEditorKit.downAction, "KP_DOWN", DefaultEditorKit.downAction,
                    "shift UP", DefaultEditorKit.selectionUpAction, "shift KP_UP", DefaultEditorKit.selectionUpAction,
                    "shift DOWN", DefaultEditorKit.selectionDownAction, "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
                    "PAGE_UP", DefaultEditorKit.pageUpAction, "PAGE_DOWN", DefaultEditorKit.pageDownAction,
                    "shift PAGE_UP", "selection-page-up", "shift PAGE_DOWN", "selection-page-down",
                    "ctrl shift PAGE_UP", "selection-page-left", "ctrl shift PAGE_DOWN", "selection-page-right",
                    "ctrl HOME", DefaultEditorKit.beginAction, "ctrl END", DefaultEditorKit.endAction,
                    "ctrl shift HOME", DefaultEditorKit.selectionBeginAction, "ctrl shift END", DefaultEditorKit.selectionEndAction,
                    "ENTER", DefaultEditorKit.insertBreakAction, "TAB", DefaultEditorKit.insertTabAction,
                    "ctrl T", "next-link-action", "ctrl shift T", "previous-link-action"));
        } else {
            b.addAll(java.util.Arrays.asList("ENTER", JTextField.notifyAction));
        }
        return b.toArray();
    }

    private static ColorUIResource c(Color color) {
        return new ColorUIResource(color);
    }
}
