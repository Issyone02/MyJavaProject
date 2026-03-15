package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


import controller.LibraryManager;
import controller.LibraryController;
import controller.LibraryChangeListener;
import utils.GuiUtils;
import controller.SearchEngine;
import model.OverdueLoanView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Main application window and observer root. Wires the LibraryManager to all panels. */
public class MainWindow extends JFrame implements LibraryChangeListener {

    private final LibraryController controller;
    private final int               currentUserId;
    private final String            currentUserName;

    private JTabbedPane tabbedPane;
    private JTextField  globalSearchField;

    private ViewPanel            viewPanel;
    private AdminPanel           adminPanel;
    private StudentPanel         studentPanel;
    private BorrowPanel          borrowPanel;
    private SearchSortPanel      searchSortPanel;
    private StaffManagementPanel staffPanel;
    private LogsPanel            logsPanel;

    private JLabel            overdueLabel;
    private javax.swing.Timer overdueTimer;
    private javax.swing.Timer pulseTimer;
    private long              previousOverdueCount = -1;
    private int               pulseStep            = 0;
    private JLabel            searchAlgoLabel;

    public MainWindow(int userId) {
        this.currentUserId = userId;

        // Wire the concrete controller once; hold as interface everywhere else
        this.controller      = new LibraryManager();
        this.currentUserName = controller.getStaffFullName(userId);
        this.controller.addChangeListener(this);

        controller.logSession("SESSION_START", "User: " + currentUserName + " | ID: " + userId);

        setTitle("MIVA SLCAS - Library Management System [" + currentUserName + "]");
        setSize(1300, 900);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setJMenuBar(buildMenuBar());
        add(buildTopBar(),    BorderLayout.NORTH);
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
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.APP_QUIT_HANDLER))
                d.setQuitHandler((ev, r) -> { if (handleExit()) r.performQuit(); else r.cancelQuit(); });
        }

        startOverdueTimer();
        setVisible(true);
    }

    // ── LibraryChangeListener ───────────────────────────────────────

    /**
     * Called automatically after any data mutation — add item, borrow, sort, undo, etc.
     * Refreshes all panels without action code needing to know which panels exist.
     */
    @Override
    public void onLibraryDataChanged() {
        if (SwingUtilities.isEventDispatchThread()) refreshAllPanels();
        else SwingUtilities.invokeLater(this::refreshAllPanels);
    }

    // ── Menu Bar ──────────────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // File menu
        JMenu     fileMenu = new JMenu("File"); fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem miSave   = new JMenuItem("Save Data");
        JMenuItem miExport = new JMenuItem("Export Data...");
        JMenuItem miImport = new JMenuItem("Import Backup...");
        JMenuItem miExit   = new JMenuItem("Exit");
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, mask));
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask));
        miSave.addActionListener(e -> {
            controller.saveState(false);
            JOptionPane.showMessageDialog(this, "Data saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        });
        miExport.addActionListener(e -> handleExport());
        miImport.addActionListener(e -> handleImport());
        miExit.addActionListener(e   -> handleExit());
        fileMenu.add(miSave); fileMenu.addSeparator();
        fileMenu.add(miExport); fileMenu.add(miImport); fileMenu.addSeparator();
        fileMenu.add(miExit);

        // Edit menu
        JMenu     editMenu = new JMenu("Edit"); editMenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem miUndo   = new JMenuItem("Undo");
        JMenuItem miRedo   = new JMenuItem("Redo");
        miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask));
        miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask));
        miUndo.addActionListener(e -> controller.undo(currentUserName));
        miRedo.addActionListener(e -> controller.redo(currentUserName));
        editMenu.add(miUndo); editMenu.add(miRedo);

        // View menu — Ctrl+1..4 switch tabs
        JMenu viewMenu = new JMenu("View"); viewMenu.setMnemonic(KeyEvent.VK_V);
        String[] tabNames = {"View Items", "Borrow/Return", "Admin", "Search & Sort"};
        for (int i = 0; i < tabNames.length; i++) {
            JMenuItem mi = new JMenuItem(tabNames[i]);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, mask));
            final int idx = i;
            mi.addActionListener(e -> tabbedPane.setSelectedIndex(idx));
            viewMenu.add(mi);
        }

        // Help menu
        JMenu     helpMenu = new JMenu("Help"); helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem miHelp   = new JMenuItem("User Manual");
        miHelp.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "SLCAS - Smart Library Circulation & Automation System\n"
            + "========================================================\n\n"
            + "[BORROW/RETURN]\n"
            + "  Catalogue: Select item → Borrow (Alt+B) or Return (Alt+R)\n"
            + "  Active Loans: View all loans, return from here\n"
            + "  Waitlist: Up / Down / Fulfill / Remove\n\n"
            + "[ADMIN PANEL]\n"
            + "  Inventory: Add / Edit / Delete items\n"
            + "  Students: Register / Edit / Delete students\n"
            + "  Staff: Create accounts (Admin only)\n\n"
            + "[SEARCH & SORT]\n"
            + "  Click column header to pick sort field\n"
            + "  Choose Insertion / Merge / Quick Sort\n"
            + "  Global search box (top-right) works across all tabs\n\n"
            + "[SHORTCUTS]\n"
            + "  Ctrl+S = Save | Ctrl+1-4 = Switch tabs | Ctrl+Q = Exit\n"
            + "  Alt+B = Borrow | Alt+R = Return\n",
            "Help", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(miHelp);

        // About menu
        JMenu     aboutMenu = new JMenu("About"); aboutMenu.setMnemonic(KeyEvent.VK_A);
        JMenuItem miAbout   = new JMenuItem("About SLCAS");
        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Smart Library Circulation & Automation System (SLCAS)\n"
            + "MIVA Open University\n\nDeveloped by the Following MIVA Open University School of Computing students:\n\n"
            + "\u2022 Mustapha Abdulafeez \u2014 2024/B/CSC/0212\n"
            + "\u2022 Daniel Oluwasemilore Abiodun \u2014 2024/B/SENG/0250\n"
            + "\u2022 Livingstone Joseph Obochi \u2014 2024/B/CYB/0320\n"
            + "\u2022 Dolapo Opebi Anuoluwapo \u2014 2024/B/CYB/03H\n"
            + "\u2022 Simon Ochayi Ujor \u2014 2024/B/CYB/0787\n"
            + "\u2022 Divine Okpara \u2014 2024/B/CYB/0397\n"
            + "\u2022 Idowu Oluwadamilare \u2014 2024/B/SENG/0291\n"
            + "\u2022 Olalere Isaiah Toluwani \u2014 2024/B/SENG/0830\n"
            + "\u2022 Folorunsho Oluwatobi \u2014 2024/B/IT/0082\n"
            + "\u2022 Ganiyat Jolayemi Omowunmi \u2014 2024/B/CYB/0337\n"
            + "\u2022 Akinniyi Adeleke Solomon \u2014 2024/B/CYB/0273\n"
            + "\u2022 Mujeeb Raheem \u2014 2024/B/CSC/0181\n"
            + "\u2022 Cherechi Udensi \u2014 2024/B/CYB/0430",
            "About", JOptionPane.INFORMATION_MESSAGE));
        aboutMenu.add(miAbout);

        menuBar.add(fileMenu); menuBar.add(editMenu); menuBar.add(viewMenu);
        menuBar.add(helpMenu); menuBar.add(aboutMenu);
        return menuBar;
    }

    // ── Export / Import ───────────────────────────────────────────────────────

    private boolean confirmPassword() {
        return GuiUtils.confirmPassword(this, controller, currentUserId, "Confirm Password:");
    }

    private void handleExport() {
        if (!confirmPassword()) return;
        String[] options = {"Text File", "Binary Backup"};
        int choice = JOptionPane.showOptionDialog(this, "Export format:", "Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice == -1) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File    file = fc.getSelectedFile();
            boolean ok   = (choice == 0)
                    ? controller.exportToText(String.valueOf(currentUserId), file)
                    : controller.exportBinary(String.valueOf(currentUserId), file);
            JOptionPane.showMessageDialog(this, ok ? "Export successful." : "Export failed.");
        }
    }

    private void handleImport() {
        if (!confirmPassword()) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            boolean ok = controller.importFromBackup(
                    String.valueOf(currentUserId), fc.getSelectedFile());
            JOptionPane.showMessageDialog(this,
                    ok ? "Import successful." : "Import failed — file may be corrupted.");
            // refreshAllPanels() fires automatically via onLibraryDataChanged()
        }
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

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
        globalSearchField.setBorder(null); globalSearchField.setOpaque(false);
        JLabel searchIcon = new JLabel("\u2315");
        searchIcon.setForeground(Color.DARK_GRAY);
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        searchContainer.add(searchIcon,        BorderLayout.WEST);
        searchContainer.add(globalSearchField, BorderLayout.CENTER);

        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchContainer);

        JButton logoutBtn = new JButton("\u23FB  Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setBackground(new Color(220, 80, 80));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setOpaque(true); logoutBtn.setBorderPainted(false);
        logoutBtn.setToolTipText("End session and return to login");
        logoutBtn.addActionListener(e -> handleLogout());

        topBar.add(titleLabel,    BorderLayout.WEST);
        topBar.add(searchWrapper, BorderLayout.CENTER);
        topBar.add(logoutBtn,     BorderLayout.EAST);
        return topBar;
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    // Every panel receives LibraryController — none import concrete utilities
    private void initializeTabs() {
        viewPanel       = new ViewPanel(controller);
        borrowPanel     = new BorrowPanel(controller, currentUserId);
        adminPanel      = new AdminPanel(controller, currentUserId);
        studentPanel    = new StudentPanel(controller, currentUserId);
        searchSortPanel = new SearchSortPanel(controller);
        staffPanel      = new StaffManagementPanel(controller, currentUserId);
        logsPanel       = new LogsPanel(controller);

        JTabbedPane adminPane = new JTabbedPane(JTabbedPane.TOP);
        adminPane.addTab("Inventory Management", adminPanel);
        adminPane.addTab("Student Records",      studentPanel);
        if (controller.isAdminUser(currentUserId)) {
            adminPane.addTab("Staff Management", staffPanel);
            adminPane.addTab("Logs",             logsPanel);
        }

        tabbedPane.addTab("View Items",    viewPanel);
        tabbedPane.addTab("Borrow/Return", borrowPanel);
        tabbedPane.addTab("Admin",         adminPane);
        tabbedPane.addTab("Search & Sort", searchSortPanel);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);
    }

    // ── Status Bar ────────────────────────────────────────────────────────────

    /**
     * Three zones:
     *   Left   — greeting and the logged-in user's name and role
     *   Centre — clickable overdue-loan count (opens the overdue dialog)
     *   Right  — active search algorithm name and today's date
     */
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)));

        boolean isAdmin = controller.isAdminUser(currentUserId);
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
        JLabel dateLabel = new JLabel(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rightStatus.add(searchAlgoLabel);
        rightStatus.add(new JSeparator(SwingConstants.VERTICAL));
        rightStatus.add(dateLabel);

        bar.add(welcomeLabel, BorderLayout.WEST);
        bar.add(overdueLabel, BorderLayout.CENTER);
        bar.add(rightStatus,  BorderLayout.EAST);
        return bar;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getGreeting() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Good Morning" : h < 17 ? "Good Afternoon" : "Good Evening";
    }

    // Tells every panel to re-read from the controller and repaint
    public void refreshAllPanels() {
        if (viewPanel       != null) viewPanel.refreshTable();
        if (borrowPanel     != null) borrowPanel.refreshTable();
        if (adminPanel      != null) adminPanel.refreshTable();
        if (studentPanel    != null) studentPanel.refreshTable();
        if (searchSortPanel != null) searchSortPanel.refreshTable();
        if (staffPanel      != null) staffPanel.refreshTable();
        if (logsPanel       != null) logsPanel.refreshTable();
    }

    /**
     * Attaches a DocumentListener to the global search box so the active panel
     * is filtered on every keystroke. SearchEngine picks Binary or Linear Search
     * based on the current sort state; the algorithm name appears in the status bar.
     */
    private void setupGlobalSearch() {
        globalSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { doSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { doSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { doSearch(); }

            private void doSearch() {
                String    text   = globalSearchField.getText().trim();
                Component active = tabbedPane.getSelectedComponent();
                if (text.isEmpty()) { searchAlgoLabel.setText(" "); clearFilters(active); return; }

                SearchEngine.SearchResult result =
                        SearchEngine.searchAll(controller.getInventory(), text);
                searchAlgoLabel.setText(result.algorithm);

                if (active instanceof JTabbedPane sub) {
                    Component sel = sub.getSelectedComponent();
                    if      (sel instanceof AdminPanel  a) a.applySearch(result.matches);
                    else if (sel instanceof StudentPanel s) s.applyFilter(text);
                } else if (active instanceof ViewPanel       v) v.applySearch(result.matches);
                  else if (active instanceof BorrowPanel     b) b.applySearch(result.matches);
                  else if (active instanceof SearchSortPanel s) s.applySearch(result.matches);
            }

            private void clearFilters(Component active) {
                if (active instanceof JTabbedPane sub) {
                    Component sel = sub.getSelectedComponent();
                    if      (sel instanceof AdminPanel  a) a.applySearch(null);
                    else if (sel instanceof StudentPanel s) s.applyFilter("");
                } else if (active instanceof ViewPanel       v) v.applySearch(null);
                  else if (active instanceof BorrowPanel     b) b.applySearch(null);
                  else if (active instanceof SearchSortPanel s) s.applySearch(null);
            }
        });
    }

    private boolean handleExit() {
        if (JOptionPane.showConfirmDialog(this, "Exit the application?", "Exit",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            controller.saveState(false);
            System.exit(0);
            return true;
        }
        return false;
    }

    private void handleLogout() {
        if (JOptionPane.showConfirmDialog(this, "End your session?", "Logout",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            controller.logSession("SESSION_END", "User: " + currentUserName);
            controller.saveState(false);
            controller.removeChangeListener(this);
            dispose();
            main.LibraryApp.startApp();
        }
    }

    // Binds Ctrl+Z / Ctrl+Y to the root pane so they work from any focused component
    private void setupKeyboardShortcuts() {
        InputMap  im   = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am   = getRootPane().getActionMap();
        int       mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "undo");
        am.put("undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { controller.undo(currentUserName); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "redo");
        am.put("redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { controller.redo(currentUserName); }
        });
    }

    // ── Overdue Timer ─────────────────────────────────────────────────────────

    /**
     * Two Swing timers:
     *   overdueTimer — fires every 60 s, refreshes the overdue count label
     *   pulseTimer   — fires every 800 ms, alternates label colour when overdue
     *                  items exist to draw the librarian's attention
     */
    private void startOverdueTimer() {
        overdueTimer = new javax.swing.Timer(60_000, e -> updateOverdueLabel());
        overdueTimer.setInitialDelay(0);
        overdueTimer.start();
        pulseTimer = new javax.swing.Timer(800, e -> {
            if (previousOverdueCount > 0) {
                pulseStep = (pulseStep + 1) % 2;
                overdueLabel.setForeground(
                        pulseStep == 0 ? new Color(190, 20, 20) : new Color(230, 80, 40));
            }
        });
        pulseTimer.start();
    }

    private void updateOverdueLabel() {
        long count   = controller.getOverdueCount();
        boolean changed = (count != previousOverdueCount);
        previousOverdueCount = count;

        if (count == 0) {
            overdueLabel.setText("\u2714 No overdue items");
            overdueLabel.setForeground(new Color(0, 140, 0));
            pulseStep = 0;
        } else {
            String base = "\u26A0 " + count + " overdue loan"
                        + (count == 1 ? "" : "s") + " - click for details";
            if (changed) {
                overdueLabel.setText("\u26A0 NEW: " + count + " overdue - click for details");
                javax.swing.Timer t = new javax.swing.Timer(
                        5_000, ev -> overdueLabel.setText(base));
                t.setRepeats(false); t.start();
            } else {
                overdueLabel.setText(base);
            }
        }
    }

    // Populates the overdue dialog from OverdueLoanView DTOs — no BorrowRecord access here
    private void showOverdueDialog() {
        JDialog dlg = new JDialog(this, "Overdue Loans", false);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(760, 420);
        dlg.setLocationRelativeTo(this);

        String[] cols = {"Student", "ID", "Item", "Type", "Due Date", "Days Overdue"};
        VirtualTableModel tm = new VirtualTableModel(cols);
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        for (OverdueLoanView ov : controller.getOverdueLoans())
            rows.add(new Object[]{ ov.studentName(), ov.studentId(),
                                   ov.itemTitle(), ov.itemType(),
                                   ov.dueDate(), ov.daysOverdue() + " day(s)" });
        tm.setRows(rows);

        JTable tbl = new JTable(tm);
        tbl.setRowHeight(26); tbl.setAutoCreateRowSorter(true);
        tbl.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row%2==0 ? new Color(255,235,235) : new Color(255,220,220));
                    setForeground(new Color(140, 0, 0));
                }
                return this;
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(closeBtn);

        dlg.add(rows.isEmpty()
                ? new JLabel("  No overdue items.", SwingConstants.CENTER)
                : new JScrollPane(tbl), BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }
}
