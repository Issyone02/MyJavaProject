package gui;

import controller.LibraryController;
import utils.GuiUtils;


import model.BorrowSummary;
import model.LibraryItem;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Library catalogue browser with a Reports dialog. */
public class ViewPanel extends JPanel {

    private final LibraryController controller;
    private final JTable            table;
    private final VirtualTableModel virtualModel;
    private final TableRowSorter<VirtualTableModel> sorter;

    /** Creates catalogue browser panel with controller and initializes UI components. */
    public ViewPanel(LibraryController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel  header     = new JPanel(new BorderLayout());
        JLabel  titleLabel = new JLabel("Library Catalogue");
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

        String[] cols = GuiUtils.CATALOGUE_COLUMNS;
        virtualModel = new VirtualTableModel(cols);
        table        = new JTable(virtualModel);
        sorter       = new TableRowSorter<>(virtualModel);
        table.setRowSorter(sorter);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        // Highlight rows where no copies are available
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    Object a = virtualModel.getValueAt(t.convertRowIndexToModel(row), 5);
                    int avail = (a instanceof Integer) ? (Integer) a : 1;
                    if (avail == 0) { setBackground(new Color(255,243,205)); setForeground(new Color(130,80,0)); }
                    else            { setBackground(row%2==0?Color.WHITE:new Color(248,248,248)); setForeground(Color.BLACK); }
                }
                return this;
            }
        });

        int[] widths = {65, 75, 220, 160, 55, 75, 55};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        add(header,                  BorderLayout.NORTH);
        add(new JScrollPane(table),  BorderLayout.CENTER);
        refreshTable();
    }

    /** Filters the table to show only matching items; null clears the filter. */
    public void applySearch(List<LibraryItem> matches) {
        GuiUtils.applyItemFilter(sorter, matches);
    }

    /** Refreshes the table with current catalogue data from controller. */
    public void refreshTable() {
        virtualModel.setRows(GuiUtils.buildCatalogueRows(controller));
    }

    // ── Reports Dialog ────────────────────────────────────────────────────────

    private void showReportsDialog() {
        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), "Library Reports", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(1000, 700);
        dlg.setLocationRelativeTo(this);

        BorrowSummary s = controller.getBorrowSummary();
        int[] categoryCounts = countItemsByCategoryRecursively(controller.getInventory(), 0, new int[3]);

        // --- Custom Blue Header Renderer ---
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBackground(new Color(70, 130, 180)); // Steel Blue
                setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.WHITE));
                return this;
            }
        };

        // 1. Category Distribution Table
        String[] catCols = {"Category", "Titles", "Copies", "Percentage"};
        double cpct = s.totalCopies > 0 ? 100.0 / s.totalCopies : 0;
        Object[][] catData = {
                {"Books", categoryCounts[0], s.bookCopies, String.format("%.1f%%", s.bookCopies * cpct)},
                {"Magazines", categoryCounts[1], s.magCopies, String.format("%.1f%%", s.magCopies * cpct)},
                {"Journals", categoryCounts[2], s.journalCopies, String.format("%.1f%%", s.journalCopies * cpct)},
                {"TOTAL", "-", s.totalCopies, "100%"}
        };
        JTable catTable = createReportTable(catData, catCols, headerRenderer);

        // 2. Most Borrowed Table
        String[] mostCols = {"Rank/Item Details (Title | Author | Count)"};
        Object[][] mostData = new Object[s.mostBorrowedLines.size()][1];
        for(int i=0; i<s.mostBorrowedLines.size(); i++) mostData[i][0] = s.mostBorrowedLines.get(i);
        JTable mostTable = createReportTable(mostData, mostCols, headerRenderer);

        // 3. Overdue Items Table
        String[] overdueCols = {"Overdue Records (Student | Item | Days)"};
        Object[][] overdueData = new Object[s.overdueLines.size()][1];
        for(int i=0; i<s.overdueLines.size(); i++) overdueData[i][0] = s.overdueLines.get(i);
        JTable overdueTable = createReportTable(overdueData, overdueCols, headerRenderer);

        // Right side container (Vertical stacking)
        JPanel reportContainer = new JPanel();
        reportContainer.setLayout(new BoxLayout(reportContainer, BoxLayout.Y_AXIS));
        reportContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addTableSection(reportContainer, "Category Distribution", catTable, 125);
        addTableSection(reportContainer, "Most Borrowed Items", mostTable, 180);
        addTableSection(reportContainer, "Current Overdue Loans", overdueTable, 180);

        JScrollPane rightScroll = new JScrollPane(reportContainer);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);

        // --- Left Side: Stats Grid & Chart ---
        final Color cBook  = new Color(255,183,178);
        final Color cMag   = new Color(181,234,215);
        final Color cJourn = new Color(173,203,227);

        JPanel statsGrid = new JPanel(new GridLayout(2, 3, 8, 8));
        statsGrid.add(makeCard("Total Catalogue",  s.totalCopies,   Color.WHITE));
        statsGrid.add(makeCard("Books",            s.bookCopies,    cBook));
        statsGrid.add(makeCard("Magazines",        s.magCopies,     cMag));
        statsGrid.add(makeCard("Journals",         s.journalCopies, cJourn));
        statsGrid.add(makeCard("Borrowed",         s.borrowedCount, new Color(160,210,255)));
        statsGrid.add(makeCard("Waitlist",         s.waitlistCount, new Color(255,224,130)));

        final int fBook=s.bookCopies, fMag=s.magCopies, fJourn=s.journalCopies, fTotal=s.totalCopies;
        JPanel chart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 30;
                if (size <= 0 || fTotal == 0) return;
                int x = (getWidth()-size)/2, y = (getHeight()-size)/2;
                double start = 90;
                start = drawSlice(g2, x, y, size, start, fBook,  fTotal, cBook);
                start = drawSlice(g2, x, y, size, start, fMag,   fTotal, cMag);
                drawSlice(g2, x, y, size, start, fJourn, fTotal, cJourn);
                g2.setColor(getBackground());
                int h = (int)(size * 0.6);
                g2.fillOval(x+(size-h)/2, y+(size-h)/2, h, h);
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String lbl = "Total: " + fTotal;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, x+(size-fm.stringWidth(lbl))/2, y+size/2+5);
            }
            private double drawSlice(Graphics2D g2, int x, int y, int sz, double start, int count, int total, Color c) {
                if (count == 0) return start;
                double ext = -((count / (double) total) * 360.0);
                g2.setColor(c);
                g2.fillArc(x, y, sz, sz, (int)start, (int)Math.floor(ext));
                return start + ext;
            }
        };
        chart.setPreferredSize(new Dimension(250, 250));

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        leftPanel.add(statsGrid, BorderLayout.NORTH);
        leftPanel.add(chart,     BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightScroll);
        split.setDividerLocation(350);

        // --- Bottom Bar with Export ---
        JPanel  bottom    = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton exportBtn = new JButton("Export as Text");
        exportBtn.setToolTipText("Save table data as a text report");
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File("Library_Report_" + LocalDate.now() + ".txt"));
            if (fc.showSaveDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
                    StringBuilder sb = new StringBuilder("MIVA SLCAS - LIBRARY REPORT\n");
                    sb.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");

                    sb.append("CATEGORY DISTRIBUTION\n---------------------\n");
                    sb.append(tableToText(catTable)).append("\n");

                    sb.append("MOST BORROWED\n-------------\n");
                    sb.append(tableToText(mostTable)).append("\n");

                    sb.append("OVERDUE ITEMS\n-------------\n");
                    sb.append(tableToText(overdueTable));

                    w.write(sb.toString());
                    JOptionPane.showMessageDialog(dlg, "Report exported successfully.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dlg, "Export failed: " + ex.getMessage());
                }
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dlg.dispose());

        bottom.add(exportBtn);
        bottom.add(closeBtn);

        dlg.add(split,  BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /** Helper: Formats table content into a clean string for text export. */
    private String tableToText(JTable table) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < table.getColumnCount(); i++)
            sb.append(String.format("%-25s", table.getColumnName(i)));
        sb.append("\n");
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int c = 0; c < table.getColumnCount(); c++)
                sb.append(String.format("%-25s", table.getValueAt(r, c).toString()));
            sb.append("\n");
        }
        return sb.toString();
    }

    private JTable createReportTable(Object[][] data, String[] cols, DefaultTableCellRenderer headerRen) {
        JTable t = new JTable(new javax.swing.table.DefaultTableModel(data, cols));
        t.setRowHeight(25);
        t.setEnabled(false);
        t.getTableHeader().setDefaultRenderer(headerRen);
        t.setGridColor(new Color(230, 230, 230));
        return t;
    }

    private void addTableSection(JPanel p, String title, JTable t, int height) {
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        JScrollPane s = new JScrollPane(t);
        s.setPreferredSize(new Dimension(550, height));
        s.setMaximumSize(new Dimension(Short.MAX_VALUE, height));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(s);
    }

    /** Creates a small coloured stat card showing a title and bold numeric value. */
    private JPanel makeCard(String title, int value, Color bg) {
        JPanel card = new JPanel(new BorderLayout(3, 3));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel t = new JLabel(title, JLabel.CENTER); t.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel v = new JLabel(String.valueOf(value), JLabel.CENTER); v.setFont(new Font("Segoe UI", Font.BOLD, 20));
        card.add(t, BorderLayout.NORTH); card.add(v, BorderLayout.CENTER);
        return card;
    }

    
    /** Recursively counts items by category. Returns array [books, magazines, journals]. */
    private int[] countItemsByCategoryRecursively(List<LibraryItem> items, int index, int[] counts) {
        if (index >= items.size()) return counts;
        LibraryItem item = items.get(index);
        String type = item.getType().toLowerCase();
        if (type.contains("book")) counts[0]++;
        else if (type.contains("magazine")) counts[1]++;
        else if (type.contains("journal")) counts[2]++;
        return countItemsByCategoryRecursively(items, index + 1, counts);
    }
}
