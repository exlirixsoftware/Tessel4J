package net.tessel.laf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicLookAndFeel;
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
        return false;
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
                "Desktop.background", background,
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
                "TextPane.background", input,
                "TextPane.foreground", text,
                "TextPane.caretForeground", text,
                "TextPane.selectionBackground", accent,
                "TextPane.selectionForeground", onAccent,
                "TextPane.inactiveForeground", textDisabled,
                "TextPane.border", textAreaBorder,

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
                "ToolBar.border", new BorderUIResource.EmptyBorderUIResource(4, 4, 4, 4),
                "ToolBar.isRollover", Boolean.TRUE,
                "ToolBar.rolloverBorder", marginBorder,
                "ToolBar.nonrolloverBorder", marginBorder,
                "ToolBar.borderColor", border,
                "SplitPane.background", background,
                "SplitPane.dividerSize", 7,
                "SplitPane.border", empty0,
                "SplitPaneDivider.border", empty0,
                "SplitPaneDivider.draggingColor", c(p.borderStrong),

                // Option pane & titled border
                "OptionPane.background", background,
                "OptionPane.foreground", text,
                "OptionPane.messageForeground", text,
                "OptionPane.border", new BorderUIResource.EmptyBorderUIResource(16, 16, 12, 16),
                "OptionPane.buttonAreaBorder", new BorderUIResource.EmptyBorderUIResource(16, 0, 0, 0),
                "OptionPane.sameSizeButtons", Boolean.TRUE,
                "OptionPane.informationIcon", TesselIcons.severity(Severity.INFORMATIONAL, 32),
                "OptionPane.warningIcon", TesselIcons.severity(Severity.WARNING, 32),
                "OptionPane.errorIcon", TesselIcons.severity(Severity.ERROR, 32),
                "OptionPane.questionIcon", TesselIcons.severity(Severity.INFORMATIONAL, 32),
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
                    prefix + ".margin", new InsetsUIResource(6, 8, 6, 8),
                    prefix + ".opaque", Boolean.FALSE,
                    prefix + ".borderPainted", Boolean.FALSE,
            });
        }
        table.put("Menu.background", background);
    }

    private static ColorUIResource c(Color color) {
        return new ColorUIResource(color);
    }
}
