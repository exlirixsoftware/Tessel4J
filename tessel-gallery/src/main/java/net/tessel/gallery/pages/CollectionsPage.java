package net.tessel.gallery.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import net.tessel.gallery.Layouts;
import net.tessel.gallery.Page;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.components.Avatar;
import net.tessel.laf.components.Badge;
import net.tessel.laf.components.Card;
import net.tessel.laf.components.TextBlock;

public class CollectionsPage extends Page {

    /** A sample record. */
    public static final class Employee {
        final String name;
        final String role;
        final String team;
        final LocalDate start;
        final int salary;
        final boolean active;

        Employee(String name, String role, String team, LocalDate start, int salary, boolean active) {
            this.name = name;
            this.role = role;
            this.team = team;
            this.start = start;
            this.salary = salary;
            this.active = active;
        }
    }

    static final Employee[] EMPLOYEES = {
            new Employee("Ada Lovelace", "Principal Engineer", "Platform", LocalDate.of(2019, 3, 4), 182000, true),
            new Employee("Grace Hopper", "Engineering Manager", "Compilers", LocalDate.of(2017, 9, 18), 175000, true),
            new Employee("Alan Turing", "Research Scientist", "AI", LocalDate.of(2020, 1, 13), 168000, true),
            new Employee("Katherine Johnson", "Data Scientist", "Analytics", LocalDate.of(2021, 6, 1), 149000, false),
            new Employee("Linus Torvalds", "Staff Engineer", "Kernel", LocalDate.of(2018, 11, 26), 171000, true),
            new Employee("Margaret Hamilton", "Director", "Flight Software", LocalDate.of(2015, 4, 7), 198000, true),
            new Employee("Dennis Ritchie", "Senior Engineer", "Languages", LocalDate.of(2022, 2, 21), 158000, false),
            new Employee("Barbara Liskov", "Architect", "Platform", LocalDate.of(2016, 8, 15), 189000, true),
    };

    public CollectionsPage() {
        super("Collections", "Tables, lists, trees and tabs.");

        section("JTable", tableView());

        JPanel lists = Layouts.columns(16, new Card("JList", listView()), new Card("JTree", treeView()));
        add(lists);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", new TextBlock("Tabs use an accent underline for the selected tab and a subtle hover highlight.", TextStyle.SECONDARY));
        javax.swing.JProgressBar progress = new javax.swing.JProgressBar(0, 100);
        progress.setValue(60);
        tabs.addTab("Activity", Layouts.stack(12, Tessel.label("12 commits this week", TextStyle.BODY), progress));
        JPanel members = Layouts.panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (Employee e : EMPLOYEES) members.add(new Avatar(e.name, 36));
        tabs.addTab("Members", members);
        tabs.addTab("Disabled", new JLabel());
        tabs.setEnabledAt(3, false);
        tabs.setPreferredSize(new Dimension(400, 150));
        section("JTabbedPane", tabs);
    }

    private static JScrollPane tableView() {
        String[] columns = {"Name", "Role", "Team", "Start date", "Salary", "Status"};
        AbstractTableModel model = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return EMPLOYEES.length;
            }

            @Override
            public int getColumnCount() {
                return columns.length;
            }

            @Override
            public String getColumnName(int column) {
                return columns[column];
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 3 ? LocalDate.class : column == 4 ? Integer.class : column == 5 ? Boolean.class : String.class;
            }

            @Override
            public Object getValueAt(int row, int column) {
                Employee e = EMPLOYEES[row];
                switch (column) {
                    case 0: return e.name;
                    case 1: return e.role;
                    case 2: return e.team;
                    case 3: return e.start;
                    case 4: return e.salary;
                    default: return e.active;
                }
            }
        };

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());
        DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value == null ? "" : df.format((LocalDate) value));
            }
        });
        NumberFormat money = NumberFormat.getCurrencyInstance(Locale.US);
        money.setMaximumFractionDigits(0);
        DefaultTableCellRenderer salary = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value == null ? "" : money.format(value));
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(salary);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
        int[] widths = {220, 190, 130, 100, 100, 100};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(600, 320));
        return sp;
    }

    /** Avatar + name. */
    private static final class NameRenderer extends JPanel implements TableCellRenderer {
        private final Avatar avatar = new Avatar("", 28);
        private final JLabel label = new JLabel();

        NameRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            add(avatar);
            add(Box.createHorizontalStrut(10));
            add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            avatar.setDisplayName((String) value);
            label.setText((String) value);
            label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    /** "Active" / "Inactive" badge. */
    private static final class StatusRenderer extends JPanel implements TableCellRenderer {
        private final Badge badge = new Badge("");

        StatusRenderer() {
            super(new FlowLayout(FlowLayout.LEFT, 12, 8));
            add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            boolean active = Boolean.TRUE.equals(value);
            badge.setText(active ? "Active" : "Inactive");
            badge.setKind(active ? Badge.Kind.SUCCESS : Badge.Kind.NEUTRAL);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private static JScrollPane listView() {
        JList<Employee> list = new JList<>(EMPLOYEES);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setCellRenderer(new PersonRenderer());
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(300, 280));
        return sp;
    }

    /** Transparent renderer; the list delegate paints hover and selection. */
    private static final class PersonRenderer extends JPanel implements ListCellRenderer<Employee> {
        private final Avatar avatar = new Avatar("", 32);
        private final JLabel name = Tessel.label("", TextStyle.BODY);
        private final JLabel role = Tessel.label("", TextStyle.CAPTION);

        PersonRenderer() {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            JPanel text = Layouts.stack(0, name, role);
            add(avatar, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Employee> list, Employee value, int index, boolean isSelected, boolean cellHasFocus) {
            avatar.setDisplayName(value.name);
            name.setText(value.name);
            role.setText(value.role);
            return this;
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (name != null) {
                javax.swing.SwingUtilities.updateComponentTreeUI(name);
                javax.swing.SwingUtilities.updateComponentTreeUI(role);
            }
        }
    }

    private static JScrollPane treeView() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Files");
        DefaultMutableTreeNode docs = new DefaultMutableTreeNode("Documents");
        DefaultMutableTreeNode invoices = new DefaultMutableTreeNode("Invoices");
        invoices.add(new DefaultMutableTreeNode("2025.pdf"));
        invoices.add(new DefaultMutableTreeNode("2026.pdf"));
        docs.add(invoices);
        docs.add(new DefaultMutableTreeNode("Resume.docx"));
        DefaultMutableTreeNode pictures = new DefaultMutableTreeNode("Pictures");
        DefaultMutableTreeNode vacation = new DefaultMutableTreeNode("Vacation");
        vacation.add(new DefaultMutableTreeNode("beach.jpg"));
        pictures.add(vacation);
        pictures.add(new DefaultMutableTreeNode("avatar.png"));
        DefaultMutableTreeNode projects = new DefaultMutableTreeNode("Projects");
        projects.add(new DefaultMutableTreeNode("Tessel4J"));
        root.add(docs);
        root.add(pictures);
        root.add(projects);

        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        tree.setSelectionRow(1);
        JScrollPane sp = new JScrollPane(tree);
        sp.setPreferredSize(new Dimension(300, 280));
        return sp;
    }
}
