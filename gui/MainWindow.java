package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.LibraryManager;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import utils.FileHandler;
import utils.AuthManager;

public class MainWindow extends JFrame {
    private LibraryManager manager;
    private boolean isDarkMode;
    private int currentUserId;
    private String currentUserName;
    private JTabbedPane tabbedPane;
    private JTextField globalSearchField;
    private JPanel searchWrapper;

    private ViewPanel viewPanel;
    private AdminPanel adminPanel;
    private StudentPanel studentPanel;
    private BorrowPanel borrowPanel;
    private ReportPanel reportPanel;
    private DashboardPanel dashboardPanel;

    private final Color DARK_BG = new Color(113, 113, 122);
    private final Color DARK_TEXT = Color.WHITE;
    private final Color DARK_BTN = new Color(70, 70, 75);

    public MainWindow(int userId) {
        this.currentUserId = userId;

        AuthManager.User u = AuthManager.getAllUsers().get(userId);
        this.currentUserName = (u != null) ? u.nickname : "User " + userId;

        this.manager = new LibraryManager();
        isDarkMode = FileHandler.loadThemePreference();

        FileHandler.logStealthActivity("SESSION_START | User: " + currentUserName + " | ID: " + userId);

        setTitle("MIVA SLCAS - Library Management System [" + currentUserName + "]");
        setSize(1300, 900);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- TOP BAR ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel titleLabel = new JLabel("MIVA OPEN UNIVERSITY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel searchContainer = new JPanel(new BorderLayout(10, 0));
        searchContainer.setName("searchContainer");
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        JLabel searchIcon = new JLabel("🔍");
        globalSearchField = new JTextField(20);
        globalSearchField.setBorder(null);
        globalSearchField.setOpaque(false);
        searchContainer.add(searchIcon, BorderLayout.WEST);
        searchContainer.add(globalSearchField, BorderLayout.CENTER);

        searchWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchContainer);

        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton undoBtn = new JButton("↩ Undo");
        JButton redoBtn = new JButton("↪ Redo");
        JButton themeToggle = new JButton(isDarkMode ? "☀️ Light" : "🌙 Dark");
        JButton logoutBtn = new JButton("🚪 Logout");

        // This initiate the Undo, Redo, Theme Toggle, Logout
        undoBtn.addActionListener(e -> triggerUndo());
        redoBtn.addActionListener(e -> triggerRedo());
        themeToggle.addActionListener(e -> toggleTheme(themeToggle));
        logoutBtn.addActionListener(e -> handleLogout());

        rightActionPanel.add(undoBtn);
        rightActionPanel.add(redoBtn);
        rightActionPanel.add(themeToggle);
        rightActionPanel.add(logoutBtn);

        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(searchWrapper, BorderLayout.CENTER);
        topBar.add(rightActionPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // --- TABS ---
        tabbedPane = new JTabbedPane();
        initializeTabs();
        add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index != -1) {
                String title = tabbedPane.getTitleAt(index);
                searchWrapper.setVisible(!title.contains("Dashboard") && !title.contains("Reports"));
                refreshAllPanels();
            }
        });

        // --- STATUS BAR ---
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        boolean isAdmin = AuthManager.isSuperAdmin(currentUserId);
        String roleTitle = isAdmin ? "Administrator" : "Staff Librarian";
        JLabel welcomeLabel = new JLabel(getGreeting() + ", " + currentUserName + " (" + roleTitle + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        statusBar.add(welcomeLabel, BorderLayout.WEST);
        statusBar.add(dateLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        setupGlobalSearch();
        setupKeyboardShortcuts();
        setupWindowClosing();
        updateTheme(isDarkMode);

        setVisible(true);
    }

    private void initializeTabs() {
        viewPanel = new ViewPanel(manager);
        studentPanel = new StudentPanel(manager, currentUserId);
        borrowPanel = new BorrowPanel(manager, currentUserId);
        dashboardPanel = new DashboardPanel(manager, currentUserId);
        adminPanel = new AdminPanel(manager, currentUserId);
        reportPanel = new ReportPanel(manager);

        //  Tab Names
        tabbedPane.addTab("Inventory", viewPanel);
        tabbedPane.addTab("Admin Page", adminPanel);
        tabbedPane.addTab("Student Records", studentPanel);
        tabbedPane.addTab("Borrow & Return", borrowPanel);
        tabbedPane.addTab("Reports", reportPanel);
        tabbedPane.addTab("Visual Dashboard", dashboardPanel);
    }

    // This checks for time of the day and greets the User accordingly
    private String getGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    // This handles refresh of all Tables in the System
    public void refreshAllPanels() {
        if (viewPanel != null) viewPanel.refreshTable();
        if (studentPanel != null) studentPanel.refreshTable();
        if (borrowPanel != null) borrowPanel.refreshTable();
        if (dashboardPanel != null) dashboardPanel.refreshDashboard();
        if (adminPanel != null) adminPanel.refreshTable();
    }

    // Global Search for all
    private void setupGlobalSearch() {
        globalSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { performFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { performFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { performFilter(); }

            // Search filter across all fields
            private void performFilter() {
                String text = globalSearchField.getText().trim();
                Component active = tabbedPane.getSelectedComponent();

                if (active instanceof ViewPanel) ((ViewPanel) active).applyFilter(text);
                else if (active instanceof StudentPanel) ((StudentPanel) active).applyFilter(text);
                else if (active instanceof AdminPanel) ((AdminPanel) active).applyFilter(text);
                else if (active instanceof BorrowPanel) ((BorrowPanel) active).applyFilter(text);
            }
        });
    }

    // Undo action
    private void triggerUndo() {
        manager.undo(currentUserName);
        refreshAllPanels();
    }

    // Redo action
    private void triggerRedo() {
        manager.redo(currentUserName);
        refreshAllPanels();
    }

    // Theme toggle action
    private void toggleTheme(JButton btn) {
        isDarkMode = !isDarkMode;
        btn.setText(isDarkMode ? "☀️ Light" : "🌙 Dark");
        updateTheme(isDarkMode);
        FileHandler.saveThemePreference(isDarkMode);
    }

    private void updateTheme(boolean dark) {
        applyRecursiveTheme(this, dark);
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void applyRecursiveTheme(Component c, boolean dark) {
        Color bg = dark ? DARK_BG : SystemColor.control;
        Color fg = dark ? DARK_TEXT : Color.BLACK;

        if (c instanceof JPanel || c instanceof JScrollPane || c instanceof JTabbedPane) {
            c.setBackground(bg);
            if ("searchContainer".equals(c.getName())) {
                c.setBackground(dark ? DARK_BTN : Color.WHITE);
            }
        }

        if (c instanceof JLabel || c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setForeground(fg);
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                applyRecursiveTheme(child, dark);
            }
        }
    }

    // Log-out action monitor
    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to end your session?", "Logout Confirmation", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            FileHandler.logStealthActivity("SESSION_END | User: " + currentUserName);
            manager.saveState(false);
            this.dispose();
            main.LibraryApp.startApp();
        }
    }

    private void setupWindowClosing() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleLogout(); }
        });
    }

    // This is Keyboard action for Undo and Redo action
    private void setupKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "UndoAction");
        am.put("UndoAction", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { triggerUndo(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "RedoAction");
        am.put("RedoAction", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { triggerRedo(); }
        });
    }
}