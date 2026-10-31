package net.tessel.laf.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicFileChooserUI;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.icons.TesselIcons;

/**
 * A file chooser with a breadcrumb address bar, a places sidebar, back / forward / up navigation and
 * list, details and icon views.
 */
public class TesselFileChooserUI extends BasicFileChooserUI {

    private enum ViewMode { LIST, DETAILS, ICONS }

    /** A shortcut in the sidebar. */
    private static final class Place {
        final String name;
        final File file;
        final TesselIcons.Kind kind;

        Place(String name, File file, TesselIcons.Kind kind) {
            this.name = name;
            this.file = file;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Icon FOLDER_ICON = TesselIcons.of(TesselIcons.Kind.FOLDER, 16, p -> p.accent);
    private static final Icon FILE_ICON = TesselIcons.of(TesselIcons.Kind.FILE, 16, p -> p.textSecondary);
    private static final Icon DRIVE_ICON = TesselIcons.of(TesselIcons.Kind.DRIVE, 16, p -> p.textSecondary);

    private JFileChooser chooser;
    private JButton backButton;
    private JButton forwardButton;
    private JButton upButton;
    private JButton newFolderButton;
    private JButton approveButton;
    private JButton cancelButton;
    private final JToggleButton[] viewButtons = new JToggleButton[3];
    private PathBar pathBar;
    private JList<Place> places;
    private DefaultListModel<Place> placesModel;
    private JList<Object> fileList;
    private JScrollPane fileScroll;
    private DetailsHeader detailsHeader;
    private JPanel filePanel;
    private JTextField fileNameField;
    private JLabel fileNameLabel;
    private JLabel filterLabel;
    private JComboBox<FileFilter> filterCombo;
    private FilterModel filterModel;
    private JPanel buttonColumnTop;
    private JPanel buttonColumnBottom;
    private JPanel accessoryHolder;

    private ViewMode viewMode = ViewMode.LIST;
    private final List<File> history = new ArrayList<>();
    private int historyPos = -1;
    private boolean navigating;
    private boolean updatingPlaces;
    private final Map<File, long[]> meta = new HashMap<>();
    private ComponentAdapter viewportListener;

    public TesselFileChooserUI(JFileChooser c) {
        super(c);
    }

    public static ComponentUI createUI(JComponent c) {
        return new TesselFileChooserUI((JFileChooser) c);
    }

    // ------------------------------------------------------------------ strings & sizes

    private String text(String key, String fallback) {
        String s = UIManager.getString(key, chooser == null ? Locale.getDefault() : chooser.getLocale());
        return s == null || s.isEmpty() ? fallback : s;
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(820, 540);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(600, 400);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    // ------------------------------------------------------------------ installation

    @Override
    public void installComponents(JFileChooser fc) {
        chooser = fc;
        fc.setLayout(new BorderLayout(0, 12));
        fc.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        fc.add(buildHeader(), BorderLayout.NORTH);
        fc.add(buildBody(), BorderLayout.CENTER);
        fc.add(buildFooter(), BorderLayout.SOUTH);

        JComponent accessory = fc.getAccessory();
        if (accessory != null) getAccessoryPanel().add(accessory, BorderLayout.CENTER);
        updateAccessory();

        updateApproveButton();
        updateLabels();
        updateFilters();
        updateSelectionMode();
        refreshPlaces();
        setViewMode(ViewMode.LIST);
        directoryChanged();
    }

    @Override
    public void uninstallComponents(JFileChooser fc) {
        if (viewportListener != null && fileScroll != null) fileScroll.getViewport().removeComponentListener(viewportListener);
        viewportListener = null;
        fc.removeAll();
        history.clear();
        historyPos = -1;
        meta.clear();
        chooser = null;
    }

    @Override
    protected void installListeners(JFileChooser fc) {
        super.installListeners(fc);
        fileList.addListSelectionListener(createListSelectionListener(fc));
        fileList.addMouseListener(createDoubleClickListener(fc, fileList));
    }

    @Override
    protected void installDefaults(JFileChooser fc) {
        super.installDefaults(fc);
        fc.setOpaque(false);
    }

    @Override
    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return this::propertyChanged;
    }

    private void propertyChanged(PropertyChangeEvent e) {
        String name = e.getPropertyName();
        JFileChooser fc = getFileChooser();
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(name)) {
            directoryChanged();
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(name)) {
            File f = (File) e.getNewValue();
            if (f != null && ((fc.isFileSelectionEnabled() && !f.isDirectory())
                    || (f.isDirectory() && fc.isDirectorySelectionEnabled()))) {
                setFileName(fc.getName(f));
                if (!fileListContains(f)) fileList.clearSelection();
            }
        } else if (JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equals(name)) {
            File[] files = (File[]) e.getNewValue();
            if (files != null && files.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (File f : files) sb.append('"').append(fc.getName(f)).append("\" ");
                setFileName(sb.toString().trim());
            } else if (files != null && files.length == 1) {
                setFileName(fc.getName(files[0]));
            }
        } else if (JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY.equals(name)) {
            updateSelectionMode();
        } else if (JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY.equals(name)) {
            updateLabels();
        } else if (JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY.equals(name)
                || JFileChooser.APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY.equals(name)
                || JFileChooser.APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY.equals(name)
                || JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY.equals(name)) {
            updateApproveButton();
            updateLabels();
        } else if (JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY.equals(name)
                || JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(name)
                || JFileChooser.ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY.equals(name)) {
            updateFilters();
        } else if (JFileChooser.ACCESSORY_CHANGED_PROPERTY.equals(name)) {
            if (e.getOldValue() != null) getAccessoryPanel().remove((JComponent) e.getOldValue());
            if (e.getNewValue() != null) getAccessoryPanel().add((JComponent) e.getNewValue(), BorderLayout.CENTER);
            updateAccessory();
        } else if (JFileChooser.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY.equals(name)) {
            boolean shown = fc.getControlButtonsAreShown();
            buttonColumnTop.setVisible(shown);
            buttonColumnBottom.setVisible(shown);
        } else if (JFileChooser.FILE_HIDING_CHANGED_PROPERTY.equals(name)) {
            meta.clear();
        }
    }

    // ------------------------------------------------------------------ building

    private JPanel buildHeader() {
        JPanel header = transparent(new BorderLayout(12, 0));

        backButton = iconButton(TesselIcons.Kind.ARROW_LEFT, TesselStrings.get("back"));
        forwardButton = iconButton(TesselIcons.Kind.ARROW_RIGHT, TesselStrings.get("forward"));
        upButton = iconButton(TesselIcons.Kind.ARROW_UP, TesselStrings.get("upOneLevel"));
        backButton.addActionListener(e -> navigate(-1));
        forwardButton.addActionListener(e -> navigate(1));
        upButton.addActionListener(e -> getChangeToParentDirectoryAction().actionPerformed(e));
        JPanel nav = transparent(null);
        nav.setLayout(new BoxLayout(nav, BoxLayout.X_AXIS));
        nav.add(backButton);
        nav.add(javax.swing.Box.createHorizontalStrut(2));
        nav.add(forwardButton);
        nav.add(javax.swing.Box.createHorizontalStrut(2));
        nav.add(upButton);
        header.add(nav, BorderLayout.WEST);

        pathBar = new PathBar();
        header.add(pathBar, BorderLayout.CENTER);

        newFolderButton = iconButton(TesselIcons.Kind.NEW_FOLDER, TesselStrings.get("newFolder"));
        newFolderButton.addActionListener(e -> getNewFolderAction().actionPerformed(e));
        JPanel tools = transparent(null);
        tools.setLayout(new BoxLayout(tools, BoxLayout.X_AXIS));
        tools.add(newFolderButton);
        tools.add(javax.swing.Box.createHorizontalStrut(8));
        ButtonGroup group = new ButtonGroup();
        TesselIcons.Kind[] kinds = {TesselIcons.Kind.VIEW_LIST, TesselIcons.Kind.VIEW_DETAILS, TesselIcons.Kind.VIEW_TILES};
        String[] tips = {TesselStrings.get("viewList"),
                TesselStrings.get("viewDetails"), TesselStrings.get("viewIcons")};
        ViewMode[] modes = ViewMode.values();
        for (int i = 0; i < viewButtons.length; i++) {
            JToggleButton b = new JToggleButton(TesselIcons.of(kinds[i], 16));
            Tessel.style(b, ButtonStyle.ICON);
            b.setToolTipText(tips[i]);
            b.setFocusable(true);
            ViewMode mode = modes[i];
            b.addActionListener(e -> setViewMode(mode));
            group.add(b);
            viewButtons[i] = b;
            tools.add(b);
        }
        header.add(tools, BorderLayout.EAST);
        return header;
    }

    private JButton iconButton(TesselIcons.Kind kind, String tooltip) {
        JButton b = new JButton(TesselIcons.of(kind, 16));
        Tessel.style(b, ButtonStyle.ICON);
        b.setToolTipText(tooltip);
        b.getAccessibleContext().setAccessibleName(tooltip);
        return b;
    }

    private JPanel buildBody() {
        JPanel body = transparent(new BorderLayout(12, 0));

        // Sidebar
        placesModel = new DefaultListModel<>();
        places = new JList<>(placesModel);
        places.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        places.setCellRenderer(new PlaceRenderer());
        places.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingPlaces) return;
            Place p = places.getSelectedValue();
            if (p != null && p.file.isDirectory()) chooser.setCurrentDirectory(p.file);
        });
        JScrollPane placesScroll = new JScrollPane(places);
        placesScroll.setPreferredSize(new Dimension(176, 100));
        body.add(placesScroll, BorderLayout.WEST);

        // Files
        fileList = new JList<>(getModel());
        fileList.setCellRenderer(new FileCell());
        fileList.setFixedCellHeight(36);
        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "tessel.open");
        fileList.getActionMap().put("tessel.open", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openSelection(e);
            }
        });
        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "tessel.up");
        fileList.getActionMap().put("tessel.up", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                getChangeToParentDirectoryAction().actionPerformed(e);
            }
        });

        fileScroll = new JScrollPane(fileList);
        detailsHeader = new DetailsHeader();
        filePanel = transparent(new BorderLayout());
        filePanel.add(fileScroll, BorderLayout.CENTER);
        body.add(filePanel, BorderLayout.CENTER);
        viewportListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateCellWidth();
            }
        };
        fileScroll.getViewport().addComponentListener(viewportListener);

        accessoryHolder = transparent(new BorderLayout());
        accessoryHolder.add(getAccessoryPanel(), BorderLayout.CENTER);
        accessoryHolder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        body.add(accessoryHolder, BorderLayout.EAST);
        return body;
    }

    private JPanel buildFooter() {
        JPanel footer = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 8, 12);
        c.anchor = GridBagConstraints.WEST;

        fileNameLabel = new JLabel();
        filterLabel = new JLabel(TesselStrings.get("filesOfType"));
        fileNameField = new JTextField();
        fileNameField.addActionListener(e -> getApproveSelectionAction().actionPerformed(e));

        filterModel = new FilterModel();
        filterCombo = new JComboBox<>(filterModel);
        filterCombo.setRenderer(new FilterRenderer());

        approveButton = Tessel.style(new JButton(), ButtonStyle.ACCENT);
        approveButton.addActionListener(getApproveSelectionAction());
        cancelButton = new JButton(text("FileChooser.cancelButtonText", "Cancel"));
        cancelButton.addActionListener(getCancelSelectionAction());
        approveButton.setPreferredSize(new Dimension(104, 32));
        cancelButton.setPreferredSize(new Dimension(104, 32));

        buttonColumnTop = transparent(new BorderLayout());
        buttonColumnTop.add(approveButton);
        buttonColumnBottom = transparent(new BorderLayout());
        buttonColumnBottom.add(cancelButton);

        c.gridx = 0;
        c.gridy = 0;
        footer.add(fileNameLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        footer.add(fileNameField, c);
        c.gridx = 2;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 8, 0);
        footer.add(buttonColumnTop, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 12);
        footer.add(filterLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        footer.add(filterCombo, c);
        c.gridx = 2;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        footer.add(buttonColumnBottom, c);
        return footer;
    }

    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    // ------------------------------------------------------------------ state updates

    private void updateApproveButton() {
        JFileChooser fc = getFileChooser();
        String t = getApproveButtonText(fc);
        approveButton.setText(t == null ? "OK" : t);
        approveButton.setToolTipText(getApproveButtonToolTipText(fc));
        int mnemonic = getApproveButtonMnemonic(fc);
        if (mnemonic > 0) approveButton.setMnemonic(mnemonic);
    }

    private void updateLabels() {
        JFileChooser fc = getFileChooser();
        boolean foldersOnly = fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled();
        fileNameLabel.setText(foldersOnly ? TesselStrings.get("folderName")
                : TesselStrings.get("fileName"));
        fileNameLabel.setLabelFor(fileNameField);
        filterLabel.setLabelFor(filterCombo);
    }

    private void updateFilters() {
        JFileChooser fc = getFileChooser();
        filterModel.refresh();
        FileFilter[] filters = fc.getChoosableFileFilters();
        boolean show = filters.length > 1 || (filters.length == 1 && filters[0] != fc.getAcceptAllFileFilter());
        filterLabel.setVisible(show);
        filterCombo.setVisible(show);
    }

    private void updateAccessory() {
        boolean has = getAccessoryPanel().getComponentCount() > 0;
        accessoryHolder.setVisible(has);
        accessoryHolder.setBorder(has ? BorderFactory.createEmptyBorder(0, 0, 0, 0) : null);
        getFileChooser().revalidate();
    }

    private void updateSelectionMode() {
        fileList.setSelectionMode(getFileChooser().isMultiSelectionEnabled()
                ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public String getFileName() {
        return fileNameField == null ? null : fileNameField.getText();
    }

    @Override
    public void setFileName(String filename) {
        if (fileNameField != null) fileNameField.setText(filename == null ? "" : filename);
    }

    @Override
    public String getDirectoryName() {
        return pathBar == null || pathBar.dir == null ? null : pathBar.dir.getPath();
    }

    @Override
    public void setDirectoryName(String dirname) {
        // The breadcrumb bar follows the chooser's current directory.
    }

    @Override
    public void rescanCurrentDirectory(JFileChooser fc) {
        meta.clear();
        getModel().validateFileCache();
    }

    @Override
    public void ensureFileIsVisible(JFileChooser fc, File f) {
        int index = f == null ? -1 : getModel().indexOf(f);
        if (index >= 0) fileList.ensureIndexIsVisible(index);
    }

    @Override
    protected JButton getApproveButton(JFileChooser fc) {
        return approveButton;
    }

    private FileView tesselFileView;

    @Override
    public FileView getFileView(JFileChooser fc) {
        if (tesselFileView == null) tesselFileView = new TesselFileView();
        return tesselFileView;
    }

    private boolean fileListContains(File f) {
        return getModel().contains(f);
    }

    private void openSelection(java.awt.event.ActionEvent e) {
        File f = (File) fileList.getSelectedValue();
        if (f != null && getFileChooser().isTraversable(f)) {
            fileList.clearSelection();
            getFileChooser().setCurrentDirectory(f);
        } else {
            getApproveSelectionAction().actionPerformed(e);
        }
    }

    // ------------------------------------------------------------------ navigation

    private void directoryChanged() {
        JFileChooser fc = getFileChooser();
        File dir = fc.getCurrentDirectory();
        meta.clear();
        if (dir == null) return;
        pathBar.setDirectory(dir);
        FileSystemView fsv = fc.getFileSystemView();
        upButton.setEnabled(fsv.getParentDirectory(dir) != null);
        if (!navigating && (historyPos < 0 || !dir.equals(history.get(historyPos)))) {
            while (history.size() > historyPos + 1) history.remove(history.size() - 1);
            history.add(dir);
            historyPos = history.size() - 1;
        }
        navigating = false;
        backButton.setEnabled(historyPos > 0);
        forwardButton.setEnabled(historyPos >= 0 && historyPos < history.size() - 1);
        selectPlaceFor(dir);
        if (fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled() && fsv.isFileSystem(dir)) {
            setFileName(dir.getPath());
        }
    }

    private void navigate(int delta) {
        int target = historyPos + delta;
        if (target < 0 || target >= history.size()) return;
        historyPos = target;
        navigating = true;
        getFileChooser().setCurrentDirectory(history.get(target));
    }

    private void refreshPlaces() {
        placesModel.clear();
        FileSystemView fsv = chooser.getFileSystemView();
        File home = fsv.getHomeDirectory();
        if (home == null) home = new File(System.getProperty("user.home", "."));
        File userHome = new File(System.getProperty("user.home", home.getPath()));
        addPlace(TesselStrings.get("placeHome"), userHome, TesselIcons.Kind.HOME);
        addPlace(TesselStrings.get("placeDesktop"), new File(userHome, "Desktop"), TesselIcons.Kind.DESKTOP);
        addPlace(TesselStrings.get("placeDocuments"), new File(userHome, "Documents"), TesselIcons.Kind.DOCUMENTS);
        addPlace(TesselStrings.get("placeDownloads"), new File(userHome, "Downloads"), TesselIcons.Kind.DOWNLOADS);
        addPlace(TesselStrings.get("placePictures"), new File(userHome, "Pictures"), TesselIcons.Kind.PICTURES);
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                if (root.getTotalSpace() <= 0) continue;
                String name = fsv.getSystemDisplayName(root);
                if (name == null || name.isEmpty()) name = root.getPath();
                placesModel.addElement(new Place(name, root, TesselIcons.Kind.DRIVE));
            }
        }
    }

    private void addPlace(String name, File file, TesselIcons.Kind kind) {
        if (file.isDirectory()) placesModel.addElement(new Place(name, file, kind));
    }

    private void selectPlaceFor(File dir) {
        updatingPlaces = true;
        try {
            places.clearSelection();
            for (int i = 0; i < placesModel.size(); i++) {
                if (placesModel.get(i).file.equals(dir)) {
                    places.setSelectedIndex(i);
                    break;
                }
            }
        } finally {
            updatingPlaces = false;
        }
    }

    // ------------------------------------------------------------------ views

    private void setViewMode(ViewMode mode) {
        viewMode = mode;
        viewButtons[mode.ordinal()].setSelected(true);
        filePanel.remove(detailsHeader);
        switch (mode) {
            case ICONS:
                fileList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                fileList.setVisibleRowCount(-1);
                fileList.setFixedCellHeight(92);
                break;
            case DETAILS:
                fileList.setLayoutOrientation(JList.VERTICAL);
                fileList.setFixedCellHeight(36);
                filePanel.add(detailsHeader, BorderLayout.NORTH);
                break;
            default:
                fileList.setLayoutOrientation(JList.VERTICAL);
                fileList.setFixedCellHeight(36);
                break;
        }
        updateCellWidth();
        filePanel.revalidate();
        filePanel.repaint();
    }

    private void updateCellWidth() {
        if (fileScroll == null) return;
        if (viewMode == ViewMode.ICONS) {
            fileList.setFixedCellWidth(104);
        } else {
            int w = fileScroll.getViewport().getWidth() - 8;
            fileList.setFixedCellWidth(Math.max(viewMode == ViewMode.DETAILS ? 520 : 200, w));
        }
        fileList.revalidate();
        detailsHeader.repaint();
    }

    /** Column geometry of the details view: {nameX, dateX, typeX, sizeX, sizeWidth}. */
    private static int[] columns(int width) {
        int sizeW = 84;
        int typeW = 120;
        int dateW = 150;
        int pad = 12;
        int nameW = Math.max(160, width - pad * 2 - sizeW - typeW - dateW);
        int dateX = pad + nameW;
        int typeX = dateX + dateW;
        int sizeX = typeX + typeW;
        return new int[]{pad, dateX, typeX, sizeX, sizeW};
    }

    private long[] metaOf(File f) {
        long[] m = meta.get(f);
        if (m == null) {
            m = new long[]{f.lastModified(), f.isDirectory() ? -1 : f.length()};
            meta.put(f, m);
        }
        return m;
    }

    private static String sizeText(long bytes) {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double v = bytes;
        int i = -1;
        do {
            v /= 1024;
            i++;
        } while (v >= 1024 && i < units.length - 1);
        return new DecimalFormat(v < 10 ? "0.#" : "0").format(v) + " " + units[i];
    }

    private static String typeText(File f) {
        if (f.isDirectory()) return TesselStrings.get("typeFolder");
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) return TesselStrings.format("typeFileOf", name.substring(dot + 1).toUpperCase(Locale.ROOT));
        return TesselStrings.get("typeFile");
    }

    // ------------------------------------------------------------------ file view

    private final class TesselFileView extends FileView {
        @Override
        public String getName(File f) {
            if (f == null) return null;
            FileSystemView fsv = getFileChooser().getFileSystemView();
            if (fsv.isFileSystemRoot(f)) {
                String n = fsv.getSystemDisplayName(f);
                if (n != null && !n.isEmpty()) return n;
            }
            String n = f.getName();
            return n.isEmpty() ? f.getPath() : n;
        }

        @Override
        public String getTypeDescription(File f) {
            return f == null ? null : typeText(f);
        }

        @Override
        public Icon getIcon(File f) {
            if (f == null) return null;
            if (getFileChooser().getFileSystemView().isDrive(f)) return DRIVE_ICON;
            return f.isDirectory() ? FOLDER_ICON : FILE_ICON;
        }

        @Override
        public Boolean isTraversable(File f) {
            return f == null ? null : Boolean.valueOf(f.isDirectory());
        }
    }

    // ------------------------------------------------------------------ renderers

    /** One row (or tile) of the file list. */
    private final class FileCell extends JComponent implements ListCellRenderer<Object> {
        private File file;
        private String name = "";
        private Icon icon;
        private boolean selected;
        private boolean hidden;
        private int listWidth;

        FileCell() {
            setOpaque(false);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
            file = (File) value;
            JFileChooser fc = getFileChooser();
            name = fc.getName(file);
            if (name == null) name = file.getName();
            icon = fc.getIcon(file);
            selected = isSelected;
            hidden = file.isHidden();
            listWidth = list.getFixedCellWidth() > 0 ? list.getFixedCellWidth() : list.getWidth();
            setFont(list.getFont());
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            if (viewMode == ViewMode.ICONS) return new Dimension(104, 92);
            return new Dimension(Math.max(200, listWidth), 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (hidden) g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.6f));
                int w = getWidth();
                int h = getHeight();
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(p.text);
                if (viewMode == ViewMode.ICONS) {
                    Icon big = enlarge(icon, 32);
                    if (big != null) paintIcon(g2, big, (w - 32) / 2, 10);
                    String clipped = clip(name, fm, w - 12);
                    g2.drawString(clipped, (w - fm.stringWidth(clipped)) / 2, 10 + 32 + 10 + fm.getAscent());
                    return;
                }
                int[] col = columns(w);
                int textX = col[0];
                if (icon != null) {
                    paintIcon(g2, icon, col[0] + 4, (h - icon.getIconHeight()) / 2);
                    textX = col[0] + 4 + icon.getIconWidth() + 10;
                }
                int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
                int nameRight = viewMode == ViewMode.DETAILS ? col[1] - 8 : w - 12;
                g2.drawString(clip(name, fm, nameRight - textX), textX, baseline);
                if (viewMode == ViewMode.DETAILS) {
                    g2.setFont(Fonts.regular(13));
                    fm = g2.getFontMetrics();
                    g2.setColor(p.textSecondary);
                    int by = (h - fm.getHeight()) / 2 + fm.getAscent();
                    long[] m = metaOf(file);
                    String date = m[0] > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(m[0])) : "";
                    g2.drawString(clip(date, fm, col[2] - col[1] - 8), col[1], by);
                    g2.drawString(clip(typeText(file), fm, col[3] - col[2] - 8), col[2], by);
                    String size = sizeText(m[1]);
                    g2.drawString(size, col[3] + col[4] - fm.stringWidth(size), by);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintIcon(Graphics2D g2, Icon i, int x, int y) {
            i.paintIcon(this, g2, x, y);
        }

        private Icon enlarge(Icon i, int size) {
            if (i instanceof TesselIcons.ShapeIcon) {
                TesselIcons.ShapeIcon s = (TesselIcons.ShapeIcon) i;
                return s.hasOwnColor() ? TesselIcons.of(s.getKind(), size, kindColor(s.getKind())) : TesselIcons.of(s.getKind(), size);
            }
            return i;
        }

        private java.util.function.Function<Palette, Color> kindColor(TesselIcons.Kind kind) {
            return kind == TesselIcons.Kind.FOLDER ? pal -> pal.accent : pal -> pal.textSecondary;
        }
    }

    private static String clip(String s, FontMetrics fm, int width) {
        if (s == null) return "";
        if (width <= 0) return "";
        if (fm.stringWidth(s) <= width) return s;
        String ellipsis = "…";
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end) + ellipsis) > width) end--;
        return s.substring(0, end) + ellipsis;
    }

    /** Sidebar entry. */
    private static final class PlaceRenderer extends TesselListUI.TesselListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Place p = (Place) value;
            setText(p.name);
            setIcon(TesselIcons.of(p.kind, 16, pal -> pal.accent));
            setIconTextGap(12);
            setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 8));
            return this;
        }
    }

    private final class FilterRenderer extends TesselListUI.TesselListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(value instanceof FileFilter ? ((FileFilter) value).getDescription() : "");
            return this;
        }
    }

    private final class FilterModel extends AbstractListModel<FileFilter> implements ComboBoxModel<FileFilter> {
        @Override
        public int getSize() {
            return getFileChooser().getChoosableFileFilters().length;
        }

        @Override
        public FileFilter getElementAt(int index) {
            return getFileChooser().getChoosableFileFilters()[index];
        }

        @Override
        public void setSelectedItem(Object item) {
            if (item instanceof FileFilter) getFileChooser().setFileFilter((FileFilter) item);
        }

        @Override
        public Object getSelectedItem() {
            return getFileChooser().getFileFilter();
        }

        void refresh() {
            fireContentsChanged(this, -1, -1);
        }
    }

    // ------------------------------------------------------------------ details header

    private final class DetailsHeader extends JComponent {
        DetailsHeader() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Fonts.semibold(12));
                g2.setColor(p.textSecondary);
                FontMetrics fm = g2.getFontMetrics();
                int[] col = columns(fileList.getFixedCellWidth() > 0 ? fileList.getFixedCellWidth() : getWidth() - 8);
                int baseline = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                int offset = 8; // scroll pane border + list border
                g2.drawString(TesselStrings.get("columnName"), offset + col[0] + 4 + 16 + 10 - 1, baseline);
                g2.drawString(TesselStrings.get("columnDate"), offset + col[1], baseline);
                g2.drawString(TesselStrings.get("columnType"), offset + col[2], baseline);
                String size = TesselStrings.get("columnSize");
                g2.drawString(size, offset + col[3] + col[4] - fm.stringWidth(size), baseline);
                g2.setColor(p.border);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ breadcrumb bar

    /** Clickable path segments; clicking the empty part switches to a text field for typing a path. */
    private final class PathBar extends JPanel {
        private final CardLayout cards = new CardLayout();
        private final JPanel crumbs = new JPanel();
        private final JTextField editor = new JTextField();
        private final List<File> chain = new ArrayList<>();
        File dir;

        PathBar() {
            super(null);
            setLayout(cards);
            setOpaque(false);
            setBorder(new TesselInputBorder(2, 4, 2, 4));
            crumbs.setOpaque(false);
            crumbs.setLayout(new BoxLayout(crumbs, BoxLayout.X_AXIS));
            crumbs.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    startEdit();
                }
            });
            crumbs.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR));
            editor.setOpaque(false);
            editor.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            editor.addActionListener(e -> commitEdit());
            editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "tessel.cancelEdit");
            editor.getActionMap().put("tessel.cancelEdit", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    stopEdit();
                }
            });
            editor.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    stopEdit();
                }
            });
            add(crumbs, "crumbs");
            add(editor, "edit");
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    rebuild();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(100, Math.max(d.height, 32));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(Tessel.palette().inputBackground);
                UIUtils.fillRound(g2, 0, 0, getWidth(), getHeight(), TesselInputBorder.RADIUS);
            } finally {
                g2.dispose();
            }
        }

        void setDirectory(File newDir) {
            dir = newDir;
            chain.clear();
            FileSystemView fsv = chooser.getFileSystemView();
            File f = newDir;
            for (int guard = 0; f != null && guard < 64; guard++) {
                chain.add(f);
                if (fsv.isFileSystemRoot(f)) break;
                f = fsv.getParentDirectory(f);
            }
            Collections.reverse(chain);
            stopEdit();
            rebuild();
        }

        private String nameOf(File f) {
            FileSystemView fsv = chooser.getFileSystemView();
            if (fsv.isFileSystemRoot(f)) {
                String n = fsv.getSystemDisplayName(f);
                return n == null || n.isEmpty() ? f.getPath() : n;
            }
            String n = f.getName();
            return n.isEmpty() ? f.getPath() : n;
        }

        private void rebuild() {
            crumbs.removeAll();
            if (chain.isEmpty()) {
                crumbs.revalidate();
                crumbs.repaint();
                return;
            }
            Insets in = getInsets();
            int available = getWidth() > 0 ? getWidth() - in.left - in.right - 24 : Integer.MAX_VALUE;
            List<Segment> segments = new ArrayList<>();
            for (File f : chain) segments.add(new Segment(f));
            int chevron = 16;
            int used = 0;
            int first = segments.size();
            while (first > 0) {
                int w = segments.get(first - 1).getPreferredSize().width + chevron;
                boolean mustFit = first == segments.size();
                if (!mustFit && used + w + 36 > available) break;
                used += w;
                first--;
            }
            if (first > 0) {
                List<File> hiddenParents = new ArrayList<>(chain.subList(0, first));
                JButton more = segmentButton("…");
                more.addActionListener(e -> {
                    JPopupMenu menu = new JPopupMenu();
                    for (int i = hiddenParents.size() - 1; i >= 0; i--) {
                        File target = hiddenParents.get(i);
                        JMenuItem item = new JMenuItem(nameOf(target));
                        item.addActionListener(ev -> chooser.setCurrentDirectory(target));
                        menu.add(item);
                    }
                    menu.show(more, 0, more.getHeight());
                });
                crumbs.add(more);
                crumbs.add(chevronLabel());
            }
            for (int i = first; i < segments.size(); i++) {
                crumbs.add(segments.get(i));
                if (i < segments.size() - 1) crumbs.add(chevronLabel());
            }
            crumbs.add(javax.swing.Box.createHorizontalGlue());
            crumbs.revalidate();
            crumbs.repaint();
        }

        private JLabel chevronLabel() {
            JLabel l = new JLabel(TesselIcons.chevron(TesselIcons.Direction.RIGHT, 14));
            l.setAlignmentY(0.5f);
            l.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
            return l;
        }

        private JButton segmentButton(String text) {
            JButton b = new JButton(text) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    return new Dimension(d.width, 28);
                }

                @Override
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            Tessel.style(b, ButtonStyle.SUBTLE);
            b.setMargin(new Insets(2, 8, 2, 8));
            b.setAlignmentY(0.5f);
            return b;
        }

        private final class Segment extends JButton {
            Segment(File f) {
                super(nameOf(f));
                Tessel.style(this, ButtonStyle.SUBTLE);
                setMargin(new Insets(2, 8, 2, 8));
                setAlignmentY(0.5f);
                addActionListener(e -> chooser.setCurrentDirectory(f));
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, 28);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        }

        private void startEdit() {
            if (dir == null) return;
            editor.setText(dir.getPath());
            cards.show(this, "edit");
            editor.requestFocusInWindow();
            editor.selectAll();
        }

        private void stopEdit() {
            cards.show(this, "crumbs");
        }

        private void commitEdit() {
            File f = chooser.getFileSystemView().createFileObject(editor.getText().trim());
            if (f.isDirectory()) {
                stopEdit();
                chooser.setCurrentDirectory(f);
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }
}
