package gui;

import controller.LibraryController;
import utils.GuiUtils;

import controller.BorrowController;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** Borrow, return, and waitlist panel — three CardLayout cards. */
public class BorrowPanel extends JPanel {

    private static final String CARD_CATALOGUE = "CATALOGUE";
    private static final String CARD_LOANS     = "BORROWED";
    private static final String CARD_WAITLIST  = "WAITLIST";

    private final LibraryController controller;
    private final BorrowController  borrowController;
    private final int               currentUserId;

    private final JTable            catalogueTable;
    private final VirtualTableModel catalogueModel;
    private       TableRowSorter<VirtualTableModel> sorter;

    private final JTable            loansTable;
    private final VirtualTableModel loansModel;
    private       TableRowSorter<VirtualTableModel> loansSorter;

    private final JTable            waitlistTable;
    private final VirtualTableModel waitlistModel;

    private final CardLayout cardLayout;
    private final JPanel     cardPanel;

    public BorrowPanel(LibraryController controller, int userId) {
        this.controller       = controller;
        this.borrowController = new BorrowController(controller);
        this.currentUserId    = userId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ── Header ────────────────────────────────────────────────────────────
        JPanel  header      = new JPanel(new BorderLayout());
        JLabel  titleLabel  = new JLabel("Catalogue");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JPanel  viewToggle  = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton catBtn      = new JButton("Catalogue");
        JButton loansBtn    = new JButton("Active Borrowed Items");
        JButton waitlistBtn = new JButton("Waitlist");
        catBtn.setToolTipText("Browse catalogue to borrow / return");
        loansBtn.setToolTipText("View all active borrowed items");
        waitlistBtn.setToolTipText("View and manage the reservation waitlist");
        viewToggle.add(catBtn); viewToggle.add(loansBtn); viewToggle.add(waitlistBtn);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(viewToggle, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        // ── Card 1: Catalogue ─────────────────────────────────────────────────
        String[] catCols = GuiUtils.CATALOGUE_COLUMNS;
        catalogueModel = new VirtualTableModel(catCols);
        catalogueTable = new JTable(catalogueModel);
        sorter         = new TableRowSorter<>(catalogueModel);
        catalogueTable.setRowSorter(sorter);
        catalogueTable.setRowHeight(26);

        // Rows with no available copies are highlighted amber
        catalogueTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    int avail = (Integer) catalogueModel.getValueAt(
                            t.convertRowIndexToModel(row), 5);
                    if (avail == 0) { setBackground(new Color(255,243,205)); setForeground(new Color(130,80,0)); }
                    else            { setBackground(row%2==0?Color.WHITE:new Color(248,248,248)); setForeground(Color.BLACK); }
                }
                return this;
            }
        });

        JPanel  catActionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        JButton borrowBtn    = new JButton("Borrow Selected");
        JButton returnBtn    = new JButton("Return Selected");
        borrowBtn.setToolTipText("Borrow the selected item for a student (Alt+B)");
        returnBtn.setToolTipText("Return the selected item from a student (Alt+R)");
        borrowBtn.setMnemonic(KeyEvent.VK_B);
        returnBtn.setMnemonic(KeyEvent.VK_R);
        borrowBtn.setPreferredSize(new Dimension(180, 36));
        returnBtn.setPreferredSize(new Dimension(180, 36));
        catActionBar.add(borrowBtn); catActionBar.add(returnBtn);

        JPanel catCard = new JPanel(new BorderLayout(5, 5));
        catCard.add(new JScrollPane(catalogueTable), BorderLayout.CENTER);
        catCard.add(catActionBar, BorderLayout.SOUTH);

        // ── Card 2: Active Loans ──────────────────────────────────────────────
        String[] loanCols = {"Student Name", "Student ID", "Item ID", "Item Title",
                "Type", "Borrow Date", "Due Date", "Status"};
        loansModel = new VirtualTableModel(loanCols);
        loansTable = new JTable(loansModel);
        loansTable.setRowHeight(26);
        loansSorter = new TableRowSorter<>(loansModel);
        loansTable.setRowSorter(loansSorter);
        loansSorter.setSortKeys(List.of(new RowSorter.SortKey(5, SortOrder.ASCENDING)));

        // Colour overdue rows red, deleted-item rows grey, on-time rows green
        loansTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    String status = (String) loansModel.getValueAt(
                            t.convertRowIndexToModel(row), 7);
                    boolean overdue = status != null && status.startsWith("OVERDUE");
                    boolean deleted = status != null && status.startsWith("Item removed");
                    if      (deleted) { setBackground(new Color(230,230,230)); setForeground(new Color(100,100,100)); }
                    else if (overdue) { setBackground(new Color(255,220,220)); setForeground(new Color(150,0,0)); }
                    else              { setBackground(row%2==0?Color.WHITE:new Color(240,255,240)); setForeground(new Color(0,100,0)); }
                }
                return this;
            }
        });

        JPanel  loansActionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        JButton loansReturnBtn = new JButton("Return Selected");
        loansReturnBtn.setToolTipText("Return the selected loan");
        loansReturnBtn.setPreferredSize(new Dimension(180, 36));
        loansActionBar.add(loansReturnBtn);
        loansReturnBtn.addActionListener(e -> handleReturnFromLoans());

        JPanel loansCard = new JPanel(new BorderLayout(5, 5));
        loansCard.add(new JScrollPane(loansTable), BorderLayout.CENTER);
        loansCard.add(loansActionBar, BorderLayout.SOUTH);

        // ── Card 3: Waitlist ──────────────────────────────────────────────────
        String[] waitCols = {"No.", "Student Name", "Student ID", "Requested Item"};
        waitlistModel = new VirtualTableModel(waitCols);
        waitlistTable = new JTable(waitlistModel);
        waitlistTable.setRowHeight(26);
        waitlistTable.setAutoCreateRowSorter(true);

        waitlistTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) { setBackground(row%2==0?Color.WHITE:new Color(245,240,255)); setForeground(new Color(80,50,120)); }
                return this;
            }
        });

        JPanel  waitActionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton moveUpBtn     = new JButton("\u2191 Up");
        JButton moveDownBtn   = new JButton("\u2193 Down");
        JButton fulfillBtn    = new JButton("Fulfill");
        JButton removeWaitBtn = new JButton("Remove");
        fulfillBtn.setBackground(new Color(70,160,70)); fulfillBtn.setForeground(Color.WHITE);
        fulfillBtn.setOpaque(true); fulfillBtn.setBorderPainted(false);
        fulfillBtn.setToolTipText("Issue the requested item to the selected student if copies are available");

        moveUpBtn.addActionListener(e -> {
            int idx = waitlistTable.getSelectedRow();
            if (idx > 0) { controller.moveWaitlistEntry(String.valueOf(currentUserId), idx, true); refreshWaitlistTable(); }
        });
        moveDownBtn.addActionListener(e -> {
            int idx = waitlistTable.getSelectedRow();
            if (idx != -1 && idx < controller.getWaitlist().size()-1) {
                controller.moveWaitlistEntry(String.valueOf(currentUserId), idx, false); refreshWaitlistTable();
            }
        });
        fulfillBtn.addActionListener(e    -> handleFulfillWaitlistEntry());
        removeWaitBtn.addActionListener(e -> {
            int idx = waitlistTable.getSelectedRow();
            if (idx != -1) { controller.removeWaitlistEntry(String.valueOf(currentUserId), idx); refreshWaitlistTable(); }
        });

        waitActionBar.add(moveUpBtn); waitActionBar.add(moveDownBtn);
        waitActionBar.add(fulfillBtn); waitActionBar.add(removeWaitBtn);

        JPanel waitCard = new JPanel(new BorderLayout(5, 5));
        waitCard.add(new JScrollPane(waitlistTable), BorderLayout.CENTER);
        waitCard.add(waitActionBar, BorderLayout.SOUTH);

        cardPanel.add(catCard,    CARD_CATALOGUE);
        cardPanel.add(loansCard,  CARD_LOANS);
        cardPanel.add(waitCard,   CARD_WAITLIST);
        add(cardPanel, BorderLayout.CENTER);

        catBtn.addActionListener(e -> {
            titleLabel.setText("Catalogue");
            cardLayout.show(cardPanel, CARD_CATALOGUE);
        });
        loansBtn.addActionListener(e -> {
            titleLabel.setText("Active Loans");
            refreshLoansTable();
            cardLayout.show(cardPanel, CARD_LOANS);
        });
        waitlistBtn.addActionListener(e -> {
            titleLabel.setText("Waitlist");
            refreshWaitlistTable();
            cardLayout.show(cardPanel, CARD_WAITLIST);
        });
        borrowBtn.addActionListener(e -> handleBorrowAction());
        returnBtn.addActionListener(e -> handleCatalogueReturnAction());

        refreshTable();
    }

    // ── Borrow ────────────────────────────────────────────────────────────────

    private void handleBorrowAction() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an item."); return; }
        String itemId = (String) catalogueModel.getValueAt(catalogueTable.convertRowIndexToModel(row), 0);
        String sId    = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (sId == null || sId.isBlank()) return;

        BorrowController.BorrowStatus status =
                borrowController.processBorrow(String.valueOf(currentUserId), sId.trim(), itemId);
        switch (status) {
            case STUDENT_NOT_FOUND:
                JOptionPane.showMessageDialog(this, "Student not found.");
                break;
            case ITEM_NOT_FOUND:
                JOptionPane.showMessageDialog(this, "Item not found.", "Error", JOptionPane.ERROR_MESSAGE);
                break;
            case DUPLICATE_LOAN:
                JOptionPane.showMessageDialog(this, "Student already has this item.", "Duplicate", JOptionPane.ERROR_MESSAGE);
                break;
            case ITEM_UNAVAILABLE: {
                LibraryItem item    = controller.getItemById(itemId);
                UserAccount student = controller.findStudentById(sId.trim());
                if (item != null && student != null) handleWaitlistAddition(student, item);
                break;
            }
            case SUCCESS:
                JOptionPane.showMessageDialog(this, "Borrow successful!");
                refreshTable();
                break;
            default:
                break;
        }
    }

    private void handleCatalogueReturnAction() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an item."); return; }
        String      itemId  = (String) catalogueModel.getValueAt(catalogueTable.convertRowIndexToModel(row), 0);
        LibraryItem item    = controller.getItemById(itemId);
        if (item == null) return;
        String      sId     = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (sId == null || sId.isBlank()) return;
        UserAccount student = controller.findStudentById(sId.trim());
        if (student == null) { JOptionPane.showMessageDialog(this, "Student not found."); return; }

        controller.returnItem(String.valueOf(currentUserId), student, item);
        BorrowController.WaitlistResult result =
                borrowController.fulfilFirstWaitlistEntry(String.valueOf(currentUserId), item);
        if (result.entry != null && result.success && result.student != null)
            JOptionPane.showMessageDialog(this, "Assigned to " + result.student.getName() + " from waitlist.");
        refreshTable();
        JOptionPane.showMessageDialog(this, "Return processed successfully.");
    }

    private void handleWaitlistAddition(UserAccount s, LibraryItem item) {
        if (JOptionPane.showConfirmDialog(this,
                "\"" + item.getTitle() + "\" is unavailable.\nAdd " + s.getName() + " to the waitlist?",
                "Waitlist", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            borrowController.addToWaitlist(String.valueOf(currentUserId), s, item);
            JOptionPane.showMessageDialog(this, s.getName() + " added to the waitlist.");
        }
    }

    private void handleFulfillWaitlistEntry() {
        int idx = waitlistTable.getSelectedRow();
        if (idx == -1) { JOptionPane.showMessageDialog(this, "Select a waitlist entry to fulfill."); return; }

        String raw = new ArrayList<>(controller.getWaitlist()).get(idx);
        WaitlistEntry we = WaitlistEntry.parse(raw);
        if (we == null) { JOptionPane.showMessageDialog(this, "Could not read waitlist entry."); return; }

        UserAccount student = controller.findStudentById(we.studentId());
        LibraryItem item    = controller.getInventory().stream()
                .filter(i -> i.getTitle().equalsIgnoreCase(we.itemTitle()))
                .findFirst().orElse(null);

        if (student == null || item == null) { JOptionPane.showMessageDialog(this, "Student or item not found."); return; }
        if (item.getAvailableCopies() <= 0) {
            JOptionPane.showMessageDialog(this, "No available copies. Student remains on waitlist.",
                    "Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (controller.fulfillWaitlistEntry(String.valueOf(currentUserId), student, item, idx)) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "\"" + item.getTitle() + "\" issued to " + student.getName() + ".");
        }
    }

    private void handleReturnFromLoans() {
        int row = loansTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item to return."); return; }
        int mRow = loansTable.convertRowIndexToModel(row);

        String studentId = (String) loansModel.getValueAt(mRow, 1);
        String itemId    = (String) loansModel.getValueAt(mRow, 2);
        String itemTitle = (String) loansModel.getValueAt(mRow, 3);

        UserAccount student = controller.findStudentById(studentId);
        if (student == null) { JOptionPane.showMessageDialog(this, "Student not found."); return; }

        LibraryItem item        = controller.getItemById(itemId);
        boolean     inCatalogue = item != null;

        if (!inCatalogue) {
            item = controller.findLoanItemByTitle(studentId, itemTitle);
        }
        if (item == null) { JOptionPane.showMessageDialog(this, "Borrow record not found."); return; }

        final LibraryItem finalItem = item;
        String msg = inCatalogue
                ? "Return \"" + itemTitle + "\" for " + student.getName() + "?"
                : "\"" + itemTitle + "\" has been removed from the catalogue.\n"
                + "Remove this borrowed item from " + student.getName() + "'s record?";

        if (JOptionPane.showConfirmDialog(this, msg, "Confirm Return",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        if (inCatalogue) {
            controller.returnItem(String.valueOf(currentUserId), student, finalItem);
            BorrowController.WaitlistResult result =
                    borrowController.fulfilFirstWaitlistEntry(String.valueOf(currentUserId), finalItem);
            if (result.entry != null && result.success && result.student != null)
                JOptionPane.showMessageDialog(this, "Assigned to " + result.student.getName() + " from waitlist.");
            JOptionPane.showMessageDialog(this, "Return processed successfully.");
        } else {
            controller.removeOrphanedLoan(String.valueOf(currentUserId), student, finalItem);
            JOptionPane.showMessageDialog(this,
                    "Borrow record removed.\n(Item no longer exists in the catalogue.)",
                    "Removed", JOptionPane.WARNING_MESSAGE);
        }
        refreshTable();
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    public void refreshTable() {
        catalogueModel.setRows(GuiUtils.buildCatalogueRows(controller));
        refreshLoansTable();
        refreshWaitlistTable();
    }

    private void refreshLoansTable() {
        List<Object[]> rows = new ArrayList<>();
        for (LoanView lv : controller.getActiveLoans())
            rows.add(new Object[]{
                    lv.studentName(), lv.studentId(),
                    lv.itemId(),
                    lv.itemTitle(), lv.itemType(),
                    lv.borrowDate(), lv.dueDate(), lv.status()
            });
        loansModel.setRows(rows);
        if (loansTable.getColumnModel().getColumnCount() > 2) {
            loansTable.getColumnModel().getColumn(2).setMinWidth(0);
            loansTable.getColumnModel().getColumn(2).setMaxWidth(0);
            loansTable.getColumnModel().getColumn(2).setPreferredWidth(0);
            loansTable.getColumnModel().getColumn(2).setWidth(0);
        }
    }

    private void refreshWaitlistTable() {
        List<Object[]> rows = new ArrayList<>();
        int pos = 1;
        for (String raw : controller.getWaitlist()) {
            WaitlistEntry we = WaitlistEntry.parse(raw);
            if (we != null) {
                rows.add(new Object[]{pos++, we.studentName(), we.studentId(), we.itemTitle()});
            } else {
                rows.add(new Object[]{pos++, "Unknown", "", raw});
            }
        }
        waitlistModel.setRows(rows);
    }

    /** Filters the Catalogue card to matching items and filters Loans card by Student Name/ID or Item Title. */
    public void applySearch(List<LibraryItem> matches, String rawQuery) {
        // 1. Filter Catalogue using the LibraryItem list (Item-centric)
        GuiUtils.applyItemFilter(sorter, matches);

        // 2. Filter Loans (Checks Student Name, Student ID, and Item matches)
        if ((matches == null || matches.isEmpty()) && (rawQuery == null || rawQuery.isBlank())) {
            loansSorter.setRowFilter(null);
        } else {
            final String query = (rawQuery != null) ? rawQuery.toLowerCase().trim() : "";

            loansSorter.setRowFilter(new RowFilter<VirtualTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends VirtualTableModel, ? extends Integer> entry) {
                    String name  = entry.getStringValue(0).toLowerCase();
                    String id    = entry.getStringValue(1).toLowerCase();
                    String title = entry.getStringValue(3).toLowerCase();

                    // Include row if Student Name or Student ID matches the raw text query
                    if (!query.isEmpty() && (name.contains(query) || id.contains(query))) {
                        return true;
                    }

                    // Also include row if the book title in this loan matches any of the item search results
                    if (matches != null) {
                        for (LibraryItem m : matches) {
                            if (title.equalsIgnoreCase(m.getTitle())) return true;
                        }
                    }
                    return false;
                }
            });
        }
    }
}