package gui;

import controller.BorrowController;
import controller.LibraryManager;
import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles borrow, return, and waitlist operations.
 * Uses CardLayout with 3 cards: Catalogue, Active Loans, Waitlist.
 */
public class BorrowPanel extends JPanel {

    private static final String CARD_CATALOGUE = "CATALOGUE";
    private static final String CARD_LOANS     = "LOANS";
    private static final String CARD_WAITLIST  = "WAITLIST";

    private final LibraryManager    manager;
    private final BorrowController  borrowController;
    private final int               currentUserId;

    private final JTable             catalogueTable;
    private final VirtualTableModel  catalogueModel;
    private       TableRowSorter<VirtualTableModel> sorter;

    private final JTable             loansTable;
    private final VirtualTableModel  loansModel;

    private final JTable             waitlistTable;
    private final VirtualTableModel  waitlistModel;

    private final CardLayout cardLayout;
    private final JPanel     cardPanel;

    public BorrowPanel(LibraryManager manager, int userId) {
        this.manager          = manager;
        this.borrowController = new BorrowController(manager);
        this.currentUserId    = userId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header with view toggle buttons
        JPanel header = new JPanel(new BorderLayout());
        JLabel title  = new JLabel("Catalogue");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel viewToggle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton viewCatalogueBtn = new JButton("Catalogue");
        JButton viewLoansBtn     = new JButton("Active Loans");
        JButton viewWaitlistBtn  = new JButton("Waitlist");
        viewCatalogueBtn.setToolTipText("Browse catalogue to borrow/return");
        viewLoansBtn.setToolTipText("View all active loans");
        viewWaitlistBtn.setToolTipText("View and manage the reservation waitlist");
        viewToggle.add(viewCatalogueBtn);
        viewToggle.add(viewLoansBtn);
        viewToggle.add(viewWaitlistBtn);

        header.add(title, BorderLayout.WEST);
        header.add(viewToggle, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        // ── Card 1: Catalogue ─────────────────────────────────────────────────
        String[] catCols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        catalogueModel = new VirtualTableModel(catCols);
        catalogueTable = new JTable(catalogueModel);
        sorter = new TableRowSorter<>(catalogueModel);
        catalogueTable.setRowSorter(sorter);
        catalogueTable.setRowHeight(26);

        catalogueTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    int modelRow = t.convertRowIndexToModel(row);
                    int avail = (Integer) catalogueModel.getValueAt(modelRow, 5);
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

        JPanel catalogueActionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        JButton borrowBtn = new JButton("Borrow Selected");
        JButton returnBtn = new JButton("Return Selected");
        borrowBtn.setToolTipText("Borrow the selected item for a student (Alt+B)");
        returnBtn.setToolTipText("Return the selected item from a student (Alt+R)");
        borrowBtn.setMnemonic(KeyEvent.VK_B);
        returnBtn.setMnemonic(KeyEvent.VK_R);
        borrowBtn.setPreferredSize(new Dimension(180, 36));
        returnBtn.setPreferredSize(new Dimension(180, 36));
        catalogueActionBar.add(borrowBtn);
        catalogueActionBar.add(returnBtn);

        JPanel catalogueCard = new JPanel(new BorderLayout(5, 5));
        catalogueCard.add(new JScrollPane(catalogueTable), BorderLayout.CENTER);
        catalogueCard.add(catalogueActionBar, BorderLayout.SOUTH);

        // ── Card 2: Active Loans ──────────────────────────────────────────────
        String[] loanCols = {"UserAccount Name", "UserAccount ID", "Item Title",
                             "Type", "Borrow Date", "Due Date", "Status"};
        loansModel = new VirtualTableModel(loanCols);
        loansTable = new JTable(loansModel);
        loansTable.setRowHeight(26);
        TableRowSorter<VirtualTableModel> loansSorter = new TableRowSorter<>(loansModel);
        loansTable.setRowSorter(loansSorter);
        loansSorter.setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.ASCENDING)));

        loansTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    String status = (String) loansModel.getValueAt(
                            t.convertRowIndexToModel(row), 6);
                    boolean overdue  = status != null && status.startsWith("OVERDUE");
                    boolean deleted  = status != null && status.startsWith("Item removed");
                    if (deleted) {
                        setBackground(new Color(230, 230, 230));
                        setForeground(new Color(100, 100, 100));
                    } else if (overdue) {
                        setBackground(new Color(255, 220, 220));
                        setForeground(new Color(150, 0, 0));
                    } else {
                        setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 255, 240));
                        setForeground(new Color(0, 100, 0));
                    }
                }
                return this;
            }
        });

        JPanel loansActionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        JButton loansReturnBtn = new JButton("Return Selected");
        loansReturnBtn.setToolTipText("Return the selected loan (removes record if item was deleted)");
        loansReturnBtn.setPreferredSize(new Dimension(180, 36));
        loansActionBar.add(loansReturnBtn);
        loansReturnBtn.addActionListener(e -> handleReturnFromLoans());

        JPanel loansCard = new JPanel(new BorderLayout(5, 5));
        loansCard.add(new JScrollPane(loansTable), BorderLayout.CENTER);
        loansCard.add(loansActionBar, BorderLayout.SOUTH);

        // ── Card 3: Waitlist (styled like Active Loans) ───────────────────────
        String[] waitCols = {"NO.", "UserAccount Name", "UserAccount ID", "Requested Item"};
        waitlistModel = new VirtualTableModel(waitCols);
        waitlistTable = new JTable(waitlistModel);
        waitlistTable.setRowHeight(26);
        waitlistTable.setAutoCreateRowSorter(true);

        waitlistTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 240, 255));
                    setForeground(new Color(80, 50, 120));
                }
                return this;
            }
        });

        JPanel waitlistActionBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        JButton moveUpBtn   = new JButton("\u2191 Up");
        JButton moveDownBtn = new JButton("\u2193 Down");
        JButton fulfillBtn  = new JButton("Fulfill");
        JButton removeBtn   = new JButton("Remove");
        fulfillBtn.setBackground(new Color(70, 160, 70));
        fulfillBtn.setForeground(Color.WHITE);
        fulfillBtn.setOpaque(true);
        fulfillBtn.setBorderPainted(false);
        fulfillBtn.setToolTipText("Issue the requested item to the selected student if copies are available");

        moveUpBtn.addActionListener(e -> {
            int idx = waitlistTable.getSelectedRow();
            if (idx > 0) { manager.moveWaitlistEntry(String.valueOf(currentUserId), idx, true); refreshWaitlistTable(); }
        });
        moveDownBtn.addActionListener(e -> {
            int idx = waitlistTable.getSelectedRow();
            if (idx != -1 && idx < manager.getWaitlist().size() - 1) {
                manager.moveWaitlistEntry(String.valueOf(currentUserId), idx, false); refreshWaitlistTable();
            }
        });
        fulfillBtn.addActionListener(e -> handleFulfillWaitlistEntry());
        removeBtn.addActionListener(e -> {
            int idx = waitlistTable.getSelectedRow();
            if (idx != -1) { manager.removeWaitlistEntry(String.valueOf(currentUserId), idx); refreshWaitlistTable(); }
        });

        waitlistActionBar.add(moveUpBtn);
        waitlistActionBar.add(moveDownBtn);
        waitlistActionBar.add(fulfillBtn);
        waitlistActionBar.add(removeBtn);

        JPanel waitlistCard = new JPanel(new BorderLayout(5, 5));
        waitlistCard.add(new JScrollPane(waitlistTable), BorderLayout.CENTER);
        waitlistCard.add(waitlistActionBar, BorderLayout.SOUTH);

        // Assemble cards
        cardPanel.add(catalogueCard, CARD_CATALOGUE);
        cardPanel.add(loansCard,     CARD_LOANS);
        cardPanel.add(waitlistCard,  CARD_WAITLIST);
        add(cardPanel, BorderLayout.CENTER);

        // Events
        viewCatalogueBtn.addActionListener(e -> { title.setText("Catalogue"); cardLayout.show(cardPanel, CARD_CATALOGUE); });
        viewLoansBtn.addActionListener(e -> { title.setText("Active Loans"); refreshLoansTable(); cardLayout.show(cardPanel, CARD_LOANS); });
        viewWaitlistBtn.addActionListener(e -> { title.setText("Waitlist"); refreshWaitlistTable(); cardLayout.show(cardPanel, CARD_WAITLIST); });
        borrowBtn.addActionListener(e -> handleBorrowAction());
        returnBtn.addActionListener(e -> handleCatalogueReturnAction());

        refreshTable();
    }

    // ── Borrow / Return handlers ──────────────────────────────────────────────

    private void handleBorrowAction() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an item."); return; }
        int mRow = catalogueTable.convertRowIndexToModel(row);
        String itemId = (String) catalogueModel.getValueAt(mRow, 0);
        String sId = JOptionPane.showInputDialog(this, "Enter UserAccount ID:");
        if (sId == null || sId.isBlank()) return;

        BorrowController.BorrowStatus status =
                borrowController.processBorrow(String.valueOf(currentUserId), sId.trim(), itemId);
        switch (status) {
            case STUDENT_NOT_FOUND -> JOptionPane.showMessageDialog(this, "UserAccount not found.");
            case ITEM_NOT_FOUND    -> JOptionPane.showMessageDialog(this, "Item not found.", "Error", JOptionPane.ERROR_MESSAGE);
            case DUPLICATE_LOAN    -> JOptionPane.showMessageDialog(this, "UserAccount already has this item.", "Duplicate", JOptionPane.ERROR_MESSAGE);
            case ITEM_UNAVAILABLE  -> {
                LibraryItem item = manager.getItemById(itemId);
                UserAccount student  = manager.findStudentById(sId.trim());
                if (item != null && student != null) handleWaitlistAddition(student, item);
            }
            case SUCCESS -> {
                JOptionPane.showMessageDialog(this, "Borrow successful!");
                refreshTable();
            }
        }
    }

    private void handleCatalogueReturnAction() {
        int row = catalogueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select an item."); return; }
        String itemId = (String) catalogueModel.getValueAt(catalogueTable.convertRowIndexToModel(row), 0);
        LibraryItem item = manager.getItemById(itemId);
        if (item == null) return;

        String sId = JOptionPane.showInputDialog(this, "Enter UserAccount ID:");
        if (sId == null || sId.isBlank()) return;
        UserAccount student = manager.findStudentById(sId.trim());
        if (student == null) { JOptionPane.showMessageDialog(this, "UserAccount not found."); return; }

        manager.returnItem(String.valueOf(currentUserId), student, item);
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

        String entry = new ArrayList<>(manager.getWaitlist()).get(idx);
        UserAccount student = manager.findStudentById(borrowController.extractStudentIdFromWaitlistEntry(entry));
        int arrowIdx = entry.indexOf(" -> ");
        LibraryItem item = arrowIdx >= 0 ? manager.getInventory().stream()
                .filter(i -> i.getTitle().equalsIgnoreCase(entry.substring(arrowIdx + 4).trim()))
                .findFirst().orElse(null) : null;

        if (student == null || item == null) { JOptionPane.showMessageDialog(this, "UserAccount or item not found."); return; }

        if (item.getAvailableCopies() <= 0) {
            JOptionPane.showMessageDialog(this, "No available copies. UserAccount remains on waitlist.", "Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (manager.fulfillWaitlistEntry(String.valueOf(currentUserId), student, item, idx)) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "\"" + item.getTitle() + "\" issued to " + student.getName() + ".");
        }
    }

    // ── Table refresh methods ─────────────────────────────────────────────────

    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (LibraryItem i : manager.getInventory()) {
            rows.add(new Object[]{
                i.getId(), i.getType(), i.getTitle(), i.getAuthor(),
                i.getYear(), i.getAvailableCopies(), i.getTotalCopies()
            });
        }
        catalogueModel.setRows(rows);
        refreshLoansTable();
        refreshWaitlistTable();
    }

    private void handleReturnFromLoans() {
        int row = loansTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a loan to return."); return; }
        int mRow = loansTable.convertRowIndexToModel(row);
        String studentId = (String) loansModel.getValueAt(mRow, 1);
        String itemTitle = (String) loansModel.getValueAt(mRow, 2);

        UserAccount student = manager.findStudentById(studentId);
        if (student == null) { JOptionPane.showMessageDialog(this, "UserAccount not found."); return; }

        BorrowRecord record = student.getCurrentLoans().stream()
                .filter(r -> r.getItem().getTitle().equals(itemTitle))
                .findFirst().orElse(null);
        if (record == null) { JOptionPane.showMessageDialog(this, "Loan record not found."); return; }

        LibraryItem item = record.getItem();
        boolean itemInCatalogue = manager.getItemById(item.getId()) != null;

        String msg = itemInCatalogue
                ? "Return \"" + itemTitle + "\" for " + student.getName() + "?"
                : "\"" + itemTitle + "\" has been removed from the catalogue.\n"
                  + "Remove this loan from " + student.getName() + "'s record?";

        if (JOptionPane.showConfirmDialog(this, msg, "Confirm Return",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        if (itemInCatalogue) {
            manager.returnItem(String.valueOf(currentUserId), student, item);
            BorrowController.WaitlistResult result =
                    borrowController.fulfilFirstWaitlistEntry(String.valueOf(currentUserId), item);
            if (result.entry != null && result.success && result.student != null)
                JOptionPane.showMessageDialog(this, "Assigned to " + result.student.getName() + " from waitlist.");
            JOptionPane.showMessageDialog(this, "Return processed successfully.");
        } else {
            manager.removeOrphanedLoan(String.valueOf(currentUserId), student, item);
            JOptionPane.showMessageDialog(this,
                    "Loan record removed.\n(Item no longer exists in the catalogue.)",
                    "Removed", JOptionPane.WARNING_MESSAGE);
        }
        refreshTable();
    }

    private void refreshLoansTable() {
        List<Object[]> rows = new ArrayList<>();
        for (UserAccount s : manager.getStudents()) {
            for (BorrowRecord r : s.getCurrentLoans()) {
                boolean deleted = manager.getItemById(r.getItem().getId()) == null;
                String status = deleted
                        ? "Item removed from catalogue"
                        : r.isOverdue()
                            ? "OVERDUE \u2014 " + r.getDaysOverdue() + " day(s)"
                            : "On time \u2014 " + r.getDaysRemaining() + " day(s) left";
                rows.add(new Object[]{
                    s.getName(), s.getStudentId(),
                    r.getItem().getTitle(), r.getItem().getType(),
                    r.getBorrowDate(), r.getDueDate(), status
                });
            }
        }
        loansModel.setRows(rows);
    }

    private void refreshWaitlistTable() {
        List<Object[]> rows = new ArrayList<>();
        int pos = 1;
        for (String entry : manager.getWaitlist()) {
            // Entry format: "Name (ID) -> ItemTitle"
            String studentName = "Unknown";
            String studentId   = "";
            String itemTitle   = entry;
            int arrowIdx = entry.indexOf(" -> ");
            if (arrowIdx > 0) {
                String left = entry.substring(0, arrowIdx);
                itemTitle = entry.substring(arrowIdx + 4);
                int parenOpen = left.indexOf('(');
                int parenClose = left.indexOf(')');
                if (parenOpen > 0 && parenClose > parenOpen) {
                    studentName = left.substring(0, parenOpen).trim();
                    studentId = left.substring(parenOpen + 1, parenClose);
                }
            }
            rows.add(new Object[]{pos++, studentName, studentId, itemTitle});
        }
        waitlistModel.setRows(rows);
    }

    public void applySearch(java.util.List<model.LibraryItem> matches) {
        if (matches == null || sorter == null) {
            if (sorter != null) sorter.setRowFilter(null);
            return;
        }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (model.LibraryItem m : matches) ids.add(m.getId());
        sorter.setRowFilter(new RowFilter<VirtualTableModel, Integer>() {
            @Override public boolean include(Entry<? extends VirtualTableModel, ? extends Integer> entry) {
                return ids.contains((String) entry.getValue(0));
            }
        });
    }
}
