package gui;

import controller.LibraryManager;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Library catalogue browser with a Reports button.
 * The Reports button opens a dialog containing dashboard-style analytics
 * (stat cards, donut chart, most borrowed, overdue) with text export.
 */
public class ViewPanel extends JPanel {
    private final LibraryManager manager;
    private final JTable table;
    private final VirtualTableModel virtualModel;
    private final TableRowSorter<VirtualTableModel> sorter;

    public ViewPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header with Reports button
        JPanel header = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Library Catalogue");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton reportsBtn = new JButton("Reports");
        reportsBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reportsBtn.setBackground(new Color(70, 130, 180));
        reportsBtn.setForeground(Color.WHITE);
        reportsBtn.setOpaque(true);
        reportsBtn.setBorderPainted(false);
        reportsBtn.setToolTipText("Generate and view library reports");
        reportsBtn.addActionListener(e -> showReportsDialog());

        header.add(titleLabel, BorderLayout.WEST);
        header.add(reportsBtn, BorderLayout.EAST);

        // Table
        String[] cols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        virtualModel = new VirtualTableModel(cols);
        table = new JTable(virtualModel);
        sorter = new TableRowSorter<>(virtualModel);
        table.setRowSorter(sorter);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    int modelRow = t.convertRowIndexToModel(row);
                    Object availObj = virtualModel.getValueAt(modelRow, 5);
                    int avail = (availObj instanceof Integer) ? (Integer) availObj : 1;
                    if (avail == 0) {
                        setBackground(new Color(255, 243, 205));
                        setForeground(new Color(130, 80, 0));
                    } else {
                        setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                        setForeground(Color.BLACK);
                    }
                }
                return this;
            }
        });

        int[] widths = {65, 75, 220, 160, 55, 75, 55};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshTable();
    }

    // Called by global search — filters table to show only matching items
    public void applySearch(java.util.List<LibraryItem> matches) {
        if (matches == null) { if (sorter != null) sorter.setRowFilter(null); return; }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (LibraryItem m : matches) ids.add(m.getId());
        sorter.setRowFilter(new RowFilter<VirtualTableModel, Integer>() {
            @Override public boolean include(Entry<? extends VirtualTableModel, ? extends Integer> entry) {
                return ids.contains((String) entry.getValue(0));
            }
        });
    }

    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (LibraryItem i : manager.getInventory()) {
            rows.add(new Object[]{
                i.getId(), i.getType(), i.getTitle(), i.getAuthor(),
                i.getYear(), i.getAvailableCopies(), i.getTotalCopies()
            });
        }
        virtualModel.setRows(rows);
    }

    // ── Reports Dialog ───────────────────────────────────────────────────────

    private void showReportsDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Library Reports", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(900, 650);
        dlg.setLocationRelativeTo(this);

        // Compute all stats (uses recursive count for category distribution — req. 6)
        List<LibraryItem> inventory = manager.getInventory();
        List<UserAccount> students = manager.getStudents();
        int totalTitles = countItemsRecursively(inventory, 0);
        int bookCount = 0, magCount = 0, journalCount = 0;
        int bookCopies = 0, magCopies = 0, journalCopies = 0;
        int borrowedCount = 0;
        for (LibraryItem item : inventory) {
            String t = item.getType().toLowerCase();
            if (t.contains("book"))          { bookCount++;    bookCopies    += item.getTotalCopies(); }
            else if (t.contains("magazine")) { magCount++;     magCopies     += item.getTotalCopies(); }
            else if (t.contains("journal"))  { journalCount++; journalCopies += item.getTotalCopies(); }
        }
        int totalCopies = bookCopies + magCopies + journalCopies;
        for (UserAccount s : students) borrowedCount += s.getCurrentLoans().size();
        int waitlistCount = manager.getWaitlist().size();

        // Build the report text
        StringBuilder report = new StringBuilder();
        report.append("MIVA SLCAS - LIBRARY REPORT\n");
        report.append("Generated: ").append(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("==================================================\n\n");

        report.append("CATEGORY DISTRIBUTION\n");
        double cpct = totalCopies > 0 ? 100.0 / totalCopies : 0;
        report.append(String.format("  %-12s %d books, %d copies (%.1f%%)\n",     "Books:",      bookCount,    bookCopies,    bookCopies    * cpct));
        report.append(String.format("  %-12s %d magazines, %d copies (%.1f%%)\n", "Magazines:",  magCount,     magCopies,     magCopies     * cpct));
        report.append(String.format("  %-12s %d journals, %d copies (%.1f%%)\n",  "Journals:",   journalCount, journalCopies, journalCopies * cpct));
        report.append(String.format("  %-12s %d copies\n",                        "Total Catalogue:", totalCopies));
        report.append(String.format("  %-12s %d\n",                               "Borrowed:",   borrowedCount));
        report.append(String.format("  %-12s %d\n",                               "Waitlist:",   waitlistCount));
        report.append("\n");

        // Most borrowed
        report.append("MOST BORROWED ITEMS\n");
        Map<String, Integer> borrowCounts = new HashMap<>();
        Map<String, LibraryItem> itemById = new HashMap<>();
        for (UserAccount s : students) {
            for (BorrowRecord r : s.getCurrentLoans()) {
                if (r.getItem() != null) { borrowCounts.merge(r.getItem().getId(), 1, Integer::sum); itemById.put(r.getItem().getId(), r.getItem()); }
            }
            for (BorrowRecord r : s.getHistory()) {
                if (r.getItem() != null) { borrowCounts.merge(r.getItem().getId(), 1, Integer::sum); itemById.put(r.getItem().getId(), r.getItem()); }
            }
        }
        if (borrowCounts.isEmpty()) {
            report.append("  No borrow history yet.\n");
        } else {
            borrowCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .forEach(e -> {
                    LibraryItem it = itemById.get(e.getKey());
                    report.append(String.format("  %dx  %s by %s [%s]\n", e.getValue(), it.getTitle(), it.getAuthor(), it.getType()));
                });
        }
        report.append("\n");

        // Overdue
        report.append("OVERDUE ITEMS\n");
        boolean hasOverdue = false;
        for (UserAccount s : students) {
            for (BorrowRecord r : s.getCurrentLoans()) {
                if (r.isOverdue()) {
                    hasOverdue = true;
                    report.append(String.format("  %s (%s) — %s — %d day(s) overdue\n",
                        s.getName(), s.getStudentId(), r.getItem().getTitle(), r.getDaysOverdue()));
                }
            }
        }
        if (!hasOverdue) report.append("  No overdue items.\n");

        // Left: stat cards + chart panel, Right: report text
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        // Stat cards
        JPanel statsGrid = new JPanel(new GridLayout(2, 3, 8, 8));
        final Color cBook = new Color(255, 183, 178);
        final Color cMag  = new Color(181, 234, 215);
        final Color cJourn = new Color(173, 203, 227);
        statsGrid.add(makeCard("Total Catalogue", totalCopies, Color.WHITE));
        statsGrid.add(makeCard("Books", bookCopies, cBook));
        statsGrid.add(makeCard("Magazines", magCopies, cMag));
        statsGrid.add(makeCard("Journals", journalCopies, cJourn));
        statsGrid.add(makeCard("Borrowed", borrowedCount, new Color(160, 210, 255)));
        statsGrid.add(makeCard("Waitlist", waitlistCount, new Color(255, 224, 130)));

        // Donut chart — proportions based on copy counts
        final int fBook = bookCopies, fMag = magCopies, fJourn = journalCopies, fTotal = totalCopies;
        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 30;
                if (size <= 0 || fTotal == 0) return;
                int x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;

                double start = 90;
                start = drawSlice(g2, x, y, size, start, fBook, fTotal, cBook);
                start = drawSlice(g2, x, y, size, start, fMag, fTotal, cMag);
                drawSlice(g2, x, y, size, start, fJourn, fTotal, cJourn);

                g2.setColor(getBackground());
                int h = (int)(size * 0.6);
                g2.fillOval(x + (size-h)/2, y + (size-h)/2, h, h);
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String label = "Total: " + fTotal;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, x + (size - fm.stringWidth(label))/2, y + size/2 + 5);
            }
            private double drawSlice(Graphics2D g2, int x, int y, int s, double start, int count, int total, Color c) {
                if (count == 0) return start;
                double ext = -((count / (double) total) * 360.0);
                g2.setColor(c);
                g2.fillArc(x, y, s, s, (int)start, (int)Math.floor(ext));
                return start + ext;
            }
        };
        chart.setPreferredSize(new Dimension(250, 250));

        leftPanel.add(statsGrid, BorderLayout.NORTH);
        leftPanel.add(chart, BorderLayout.CENTER);

        // Right: scrollable report text
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane textScroll = new JScrollPane(textArea);
        textScroll.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, textScroll);
        split.setDividerLocation(320);

        // Bottom: export + close
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton exportBtn = new JButton("Export as Text");
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File("Library_Report_" + LocalDate.now() + ".txt"));
            if (fc.showSaveDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
                    w.write(report.toString());
                    JOptionPane.showMessageDialog(dlg, "Report exported.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dlg, "Export failed: " + ex.getMessage());
                }
            }
        });
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dlg.dispose());
        bottom.add(exportBtn);
        bottom.add(closeBtn);

        dlg.add(split, BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private JPanel makeCard(String title, int value, Color bg) {
        JPanel card = new JPanel(new BorderLayout(3, 3));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel t = new JLabel(title, JLabel.CENTER);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel v = new JLabel(String.valueOf(value), JLabel.CENTER);
        v.setFont(new Font("Segoe UI", Font.BOLD, 20));
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    // Recursive count: base case = end of list → 0, otherwise 1 + recurse on rest
    private int countItemsRecursively(List<LibraryItem> items, int index) {
        if (index >= items.size()) return 0;
        return 1 + countItemsRecursively(items, index + 1);
    }
}
