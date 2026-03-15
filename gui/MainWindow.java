package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import controller.LibraryManager;
import model.LibraryItem;
import utils.FileHandler;
import utils.AuthManager;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main application window.
 *
 * Tabs:
 *   1. View Items    — catalogue with Reports button
 *   2. Borrow/Return — 3 cards: Catalogue, Active Loans, Waitlist
 *   3. Admin         — sub-tabs: Inventory, Students, Staff Management, Logs
 *   4. Search & Sort — column-header field selection + algorithm dropdown
 *
 * Menu: File (Save, Export, Import, Logout, Exit), Edit (Undo, Redo), View (tabs), Help
 */
public class MainWindow extends JFrame {
    private LibraryManager manager;
    private int currentUserId;
    private String currentUserName;

    private JTabbedPane tabbedPane;
    private JTextField globalSearchField;

    private ViewPanel viewPanel;
    private AdminPanel adminPanel;
    private StudentPanel studentPanel;
    private BorrowPanel borrowPanel;
    private SearchSortPanel searchSortPanel;
    private StaffManagementPanel staffPanel;
    private LogsPanel logsPanel;

    // Overdue monitoring
    private JLabel overdueLabel;
    private javax.swing.Timer overdueTimer;
    private javax.swing.Timer pulseTimer;
    private long previousOverdueCount = -1;
    private int pulseStep = 0;

    public MainWindow(int userId) {
        this.currentUserId   = userId;
        this.currentUserName = AuthManager.getFullName(userId);
        this.manager         = new LibraryManager();

        FileHandler.logStealthActivity("SESSION_START | User: " + currentUserName + " | ID: " + userId);

        setTitle("MIVA SLCAS - Library Management System [" + currentUserName + "]");
        setSize(1300, 900);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setJMenuBar(buildMenuBar());
        add(buildTopBar(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        initializeTabs();
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(e -> refreshAllPanels());

        add(buildStatusBar(), BorderLayout.SOUTH);

        setupGlobalSearch();
        setupKeyboardShortcuts();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleExit(); }
        });

        // macOS: intercept Cmd+Q before the OS kills the process
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((evt, response) -> {
                    if (handleExit()) response.performQuit();
                    else response.cancelQuit();
                });
            }
        }

        startOverdueTimer();

        setVisible(true);
    }

    // ── Menu Bar ─────────────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // File
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem miSave = new JMenuItem("Save Data");
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, mask));
        miSave.addActionListener(e -> {
            manager.saveState(false);
            JOptionPane.showMessageDialog(this, "Data saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        });

        JMenuItem miExport = new JMenuItem("Export Data...");
        miExport.addActionListener(e -> handleExport());

        JMenuItem miImport = new JMenuItem("Import Backup...");
        miImport.addActionListener(e -> handleImport());

        JMenuItem miExit = new JMenuItem("Exit");
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask));
        miExit.addActionListener(e -> handleExit());

        fileMenu.add(miSave);
        fileMenu.addSeparator();
        fileMenu.add(miExport);
        fileMenu.add(miImport);
        fileMenu.addSeparator();
        fileMenu.add(miExit);

        // Edit
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem miUndo = new JMenuItem("Undo");
        miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask));
        miUndo.addActionListener(e -> { manager.undo(currentUserName); refreshAllPanels(); });
        JMenuItem miRedo = new JMenuItem("Redo");
        miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask));
        miRedo.addActionListener(e -> { manager.redo(currentUserName); refreshAllPanels(); });
        editMenu.add(miUndo);
        editMenu.add(miRedo);

        // View
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        String[] tabNames = {"View Items", "Borrow/Return", "Admin", "Search & Sort"};
        for (int i = 0; i < tabNames.length; i++) {
            JMenuItem mi = new JMenuItem(tabNames[i]);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, mask));
            final int idx = i;
            mi.addActionListener(e -> tabbedPane.setSelectedIndex(idx));
            viewMenu.add(mi);
        }

        // Help
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem miHelp = new JMenuItem("User Manuel");
        miHelp.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "SLCAS - Smart Library Circulation & Automation System\n"
            + "================================================================\n\n"
            + "[LOGIN]\n"
            + "• Enter your Staff ID and Password to access the system\n\n"
            + "[DASHBOARD]\n"
            + "• View library analytics: Total Titles, Books, Magazines, Journals\n"
            + "• Monitor Borrowed items and Waitlist queue\n"
            + "• Visual donut chart shows collection distribution\n\n"
            + "[VIEW ITEMS]\n"
            + "• Browse complete catalogue with ID, Type, Title, Author, Year\n"
            + "• Check Available/Total copies for each item\n"
            + "• Generate reports on library inventory\n\n"
            + "[BORROW/RETURN - 3 Views]\n"
            + "1. Catalogue: Select item → Borrow (Alt+B) or Return (Alt+R)\n"
            + "   - Enter UserAccount ID when prompted\n"
            + "   - Unavailable items can be added to waitlist\n"
            + "2. Active Loans: View all current loans with status\n"
            + "   - Green = On time | Red = OVERDUE | Gray = Item removed\n"
            + "3. Waitlist: Manage reservations (Up/Down/Fulfill/Remove)\n"
            + "   - Fulfill issues item to next waitlisted user when available\n\n"
            + "[ADMIN PANEL]\n"
            + "• Inventory: Add/Edit/Delete Books, Magazines, Journals\n"
            + "  - Edit/Delete require Admin password confirmation\n"
            + "• Students: Register/Edit/Delete users, view borrowing status\n"
            + "  - Red bold dates indicate overdue items\n"
            + "• Staff Management: Create staff accounts (Admin only)\n"
            + "  - Set/Reset passwords, assign Admin/Staff roles\n"
            + "[SEARCH & SORT]\n"
            + "• Click column headers (Title/Author/Year) to select sort field\n"
            + "• Choose algorithm: Insertion Sort, Merge Sort, or Quick Sort\n"
            + "• Use global search box (top-right) to find items across tabs\n\n"
            + "[MENU SHORTCUTS]\n"
            + "• Ctrl+S = Save Data | Ctrl+1-5 = Switch tabs | Ctrl+Q = Exit\n"
            + "• File: Save, Export/Import backup, Logout, Exit\n"
            + "• Edit: Undo/Redo actions\n"
            + "• View: Jump to any tab\n\n"
            + "[KEYBOARD TIPS](Note: Alt + Key Works does not work on all systems)\n"
            + "• Alt+B = Borrow selected item | Alt+R = Return selected item\n"
            + "• Press Enter in login fields to submit\n"
            + "• Tables support click-to-sort column headers\n\n"
            + "For technical support, contact MIVA School for Computing.",
            "Help", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(miHelp);

        //About Us 
        JMenu aboutUsMenu = new JMenu("About");
        aboutUsMenu.setMnemonic(KeyEvent.VK_A);
        JMenuItem miAbout = new JMenuItem("About SLCAS");
        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Smart Library Circulation & Automation System (SLCAS)\nMIVA Open University\n\n"
            + "The SLCAS is a comprehensive library management system designed to streamline library operations.\n\n"
            + "Developed by the following students from Miva School for Computing:\n\n"
            + "• Mustapha Abdulafeez (abdulafeez.mustapha@miva.edu.ng) - 2024/B/CSC/0212\n"
            + "• Daniel Oluwasemilore Abiodun (daniel.abiodun1@miva.edu.ng) - 2024/B/SENG/0250\n"
            + "• Livingstone Joseph Obochi (livingstone.obochi@miva.edu.ng) - 2024/B/CYB/0320\n"
            + "• Dolapo Opebi Anuoluwapo (Dolapo.opebi@miva.edu.ng) - 2024/B/CYB/03H\n"
            + "• Simon Ochayi Ujor (simon.ujor@miva.edu.ng) - 2024/B/CYB/0787\n"
            + "• Divine Okpara (Divine.okpara@miva.edu.ng) - 2024/B/CYB/0397\n"
            + "• Idowu Oluwadamilare (idowu.oluwadamilare@miva.edu.ng) - 2024/B/SENG/0291\n"
            + "• Olalere Isaiah Toluwani (olalere.isaiah@miva.edu.ng) - 2024/B/SENG/0830\n"
            + "• Folorunsho Oluwatobi (Folorunsho.Oluwatobi@miva.edu.ng) - 2024/B/IT/0082\n"
            + "• Ganiyat Jolayemi Omowunmi (ganiyat.jolayemi@miva.edu.ng) - 2024/B/CYB/0337\n"
            + "• Akinniyi Adeleke Solomon (adeleke.akinniyi@miva.edu.ng) - 2024/B/CYB/0273\n"
            + "• Mujeeb Raheem (mujeeb.raheem@miva.edu.ng) - 2024/B/CSC/0181\n"
            + "• Cherechi Udensi (cherechi.udensi@miva.edu.ng) - 2024/B/CYB/0430",
            "About", JOptionPane.INFORMATION_MESSAGE));
        aboutUsMenu.add(miAbout);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        menuBar.add(aboutUsMenu);
        return menuBar;
    }

    // ── Export / Import ──────────────────────────────────────────────────────

    private boolean confirmPassword() {
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, pf, "Confirm Password:", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return false;
        boolean ok = utils.AuthManager.validate(currentUserId, new String(pf.getPassword()));
        if (!ok) JOptionPane.showMessageDialog(this, "Incorrect password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
        return ok;
    }

    private void handleExport() {
        if (!confirmPassword()) return;
        String[] options = {"Text File", "Binary Backup"};
        int choice = JOptionPane.showOptionDialog(this, "Export format:", "Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == -1) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            boolean ok = (choice == 0)
                ? FileHandler.exportToText(manager.getInventory(), manager.getStudents(), file)
                : FileHandler.exportBackup(manager.getInventory(), manager.getStudents(), file);
            JOptionPane.showMessageDialog(this, ok ? "Export successful." : "Export failed.");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleImport() {
        if (!confirmPassword()) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Object[] data = FileHandler.importBackup(fc.getSelectedFile());
            if (data != null) {
                manager.saveState(false);
                manager.getInventory().clear();
                manager.getInventory().addAll((List<LibraryItem>) data[0]);
                refreshAllPanels();
                JOptionPane.showMessageDialog(this, "Import successful.");
            } else {
                JOptionPane.showMessageDialog(this, "Import failed — file may be corrupted.");
            }
        }
    }

    // ── Top Bar ──────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("MIVA OPEN UNIVERSITY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel searchContainer = new JPanel(new BorderLayout(10, 0));
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        globalSearchField = new JTextField(25);
        globalSearchField.setBorder(null);
        globalSearchField.setOpaque(false);
        JLabel searchIcon = new JLabel("\u2315");
        searchIcon.setForeground(Color.DARK_GRAY);
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        searchContainer.add(searchIcon, BorderLayout.WEST);
        searchContainer.add(globalSearchField, BorderLayout.CENTER);

        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchContainer);

        JButton logoutBtn = new JButton("\u23FB  Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setBackground(new Color(220, 80, 80));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setOpaque(true);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setToolTipText("End session and return to login");
        logoutBtn.addActionListener(e -> handleLogout());

        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(searchWrapper, BorderLayout.CENTER);
        topBar.add(logoutBtn, BorderLayout.EAST);
        return topBar;
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────

    private void initializeTabs() {
        viewPanel       = new ViewPanel(manager);
        borrowPanel     = new BorrowPanel(manager, currentUserId);
        adminPanel      = new AdminPanel(manager, currentUserId);
        studentPanel    = new StudentPanel(manager, currentUserId);
        searchSortPanel = new SearchSortPanel(manager);
        staffPanel      = new StaffManagementPanel(manager, currentUserId);
        logsPanel       = new LogsPanel(manager);

        // Tab 3: Admin — sub-tabs
        JTabbedPane adminPane = new JTabbedPane(JTabbedPane.TOP);
        adminPane.addTab("Inventory Management", adminPanel);
        adminPane.addTab("Student Records",      studentPanel);
        boolean isAdmin = AuthManager.isAdmin(currentUserId);
        if (isAdmin) {
            adminPane.addTab("Staff Management",     staffPanel);
            adminPane.addTab("Logs",                 logsPanel);
        }

        // Main tabs
        tabbedPane.addTab("View Items",      viewPanel);
        tabbedPane.addTab("Borrow/Return",   borrowPanel);
        tabbedPane.addTab("Admin",           adminPane);
        tabbedPane.addTab("Search & Sort",   searchSortPanel);

        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);
    }

    // ── Status Bar ─────────────────────────────────────────────────────────

    private JLabel searchAlgoLabel;

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)));

        boolean isAdmin = AuthManager.isAdmin(currentUserId);
        JLabel welcomeLabel = new JLabel(getGreeting() + ", " + currentUserName
            + " (" + (isAdmin ? "Administrator" : "Staff Librarian") + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        overdueLabel = new JLabel("Checking overdue...");
        overdueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        overdueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        overdueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overdueLabel.setToolTipText("Click to see overdue details");
        overdueLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showOverdueDialog(); }
        });

        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightStatus.setOpaque(false);

        searchAlgoLabel = new JLabel(" ");
        searchAlgoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        searchAlgoLabel.setForeground(new Color(80, 80, 140));

        JLabel dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        rightStatus.add(searchAlgoLabel);
        rightStatus.add(new JSeparator(SwingConstants.VERTICAL));
        rightStatus.add(dateLabel);

        bar.add(welcomeLabel, BorderLayout.WEST);
        bar.add(overdueLabel, BorderLayout.CENTER);
        bar.add(rightStatus, BorderLayout.EAST);
        return bar;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getGreeting() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Good Morning" : h < 17 ? "Good Afternoon" : "Good Evening";
    }

    public void refreshAllPanels() {
        if (viewPanel != null)       viewPanel.refreshTable();
        if (borrowPanel != null)     borrowPanel.refreshTable();
        if (adminPanel != null)      adminPanel.refreshTable();
        if (studentPanel != null)    studentPanel.refreshTable();
        if (searchSortPanel != null) searchSortPanel.refreshTable();
        if (staffPanel != null)      staffPanel.refreshTable();
        if (logsPanel != null)       logsPanel.refreshTable();
    }

    // Global search — uses SearchEngine (Linear Search if unsorted, Binary Search if sorted)
    private void setupGlobalSearch() {
        globalSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { doSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { doSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { doSearch(); }

            private void doSearch() {
                String text = globalSearchField.getText().trim();
                Component active = tabbedPane.getSelectedComponent();

                if (text.isEmpty()) {
                    searchAlgoLabel.setText(" ");
                    clearAllFilters(active);
                    return;
                }

                // For panels displaying LibraryItem tables, use SearchEngine
                controller.SearchEngine.SearchResult result =
                        controller.SearchEngine.searchAll(manager.getInventory(), text);
                searchAlgoLabel.setText(result.algorithm);

                if (active instanceof JTabbedPane) {
                    Component sub = ((JTabbedPane) active).getSelectedComponent();
                    if (sub instanceof AdminPanel)        ((AdminPanel) sub).applySearch(result.matches);
                    else if (sub instanceof StudentPanel) ((StudentPanel) sub).applyFilter(text);
                } else if (active instanceof ViewPanel)       ((ViewPanel) active).applySearch(result.matches);
                  else if (active instanceof BorrowPanel)     ((BorrowPanel) active).applySearch(result.matches);
                  else if (active instanceof SearchSortPanel) ((SearchSortPanel) active).applySearch(result.matches);
            }

            private void clearAllFilters(Component active) {
                if (active instanceof JTabbedPane) {
                    Component sub = ((JTabbedPane) active).getSelectedComponent();
                    if (sub instanceof AdminPanel)        ((AdminPanel) sub).applySearch(null);
                    else if (sub instanceof StudentPanel) ((StudentPanel) sub).applyFilter("");
                } else if (active instanceof ViewPanel)       ((ViewPanel) active).applySearch(null);
                  else if (active instanceof BorrowPanel)     ((BorrowPanel) active).applySearch(null);
                  else if (active instanceof SearchSortPanel) ((SearchSortPanel) active).applySearch(null);
            }
        });
    }

    private boolean handleExit() {
        if (JOptionPane.showConfirmDialog(this, "Exit the application?", "Exit",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            manager.saveState(false);
            System.exit(0);
            return true;
        }
        return false;
    }

    private void handleLogout() {
        if (JOptionPane.showConfirmDialog(this, "End your session?", "Logout",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            FileHandler.logStealthActivity("SESSION_END | User: " + currentUserName);
            manager.saveState(false);
            dispose();
            main.LibraryApp.startApp();
        }
    }

    private void setupKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "undo");
        am.put("undo", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { manager.undo(currentUserName); refreshAllPanels(); }});
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "redo");
        am.put("redo", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { manager.redo(currentUserName); refreshAllPanels(); }});
    }

    // ── Overdue Timer ────────────────────────────────────────────────────────

    private void startOverdueTimer() {
        overdueTimer = new javax.swing.Timer(60_000, e -> updateOverdueLabel());
        overdueTimer.setInitialDelay(0);
        overdueTimer.start();
        pulseTimer = new javax.swing.Timer(800, e -> {
            if (previousOverdueCount > 0) {
                pulseStep = (pulseStep + 1) % 2;
                overdueLabel.setForeground(pulseStep == 0 ? new Color(190, 20, 20) : new Color(230, 80, 40));
            }
        });
        pulseTimer.start();
    }

    private void updateOverdueLabel() {
        long count = manager.getStudents().stream()
                .flatMap(s -> s.getCurrentLoans().stream())
                .filter(model.BorrowRecord::isOverdue).count();
        boolean changed = (count != previousOverdueCount);
        previousOverdueCount = count;

        if (count == 0) {
            overdueLabel.setText("\u2714 No overdue items");
            overdueLabel.setForeground(new Color(0, 140, 0));
            pulseStep = 0;
        } else {
            String base = "\u26A0 " + count + " overdue loan" + (count == 1 ? "" : "s") + " - click for details";
            if (changed) {
                overdueLabel.setText("\u26A0 NEW: " + count + " overdue - click for details");
                javax.swing.Timer t = new javax.swing.Timer(5_000, ev -> overdueLabel.setText(base));
                t.setRepeats(false); t.start();
            } else overdueLabel.setText(base);
        }
    }

    private void showOverdueDialog() {
        JDialog dlg = new JDialog(this, "Overdue Loans", false);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(760, 420);
        dlg.setLocationRelativeTo(this);

        String[] cols = {"Student", "ID", "Item", "Type", "Due Date", "Days Overdue"};
        VirtualTableModel tm = new VirtualTableModel(cols);
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (model.UserAccount s : manager.getStudents()) {
            for (model.BorrowRecord r : s.getCurrentLoans()) {
                if (r.isOverdue()) {
                    rows.add(new Object[]{ s.getName(), s.getStudentId(),
                        r.getItem().getTitle(), r.getItem().getType(),
                        r.getDueDate(), r.getDaysOverdue() + " day(s)" });
                }
            }
        }
        tm.setRows(rows);

        JTable tbl = new JTable(tm);
        tbl.setRowHeight(26);
        tbl.setAutoCreateRowSorter(true);
        tbl.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? new Color(255, 235, 235) : new Color(255, 220, 220));
                    setForeground(new Color(140, 0, 0));
                }
                return this;
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(closeBtn);

        if (rows.isEmpty()) dlg.add(new JLabel("  No overdue items.", SwingConstants.CENTER), BorderLayout.CENTER);
        else dlg.add(new JScrollPane(tbl), BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
