package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.LibraryManager;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import utils.FileHandler;
import utils.AuthManager;

public class MainWindow extends JFrame {
    private LibraryManager manager;
    private boolean isDarkMode;
    private int currentUserId;
    private JTabbedPane tabbedPane;
    private JTextField globalSearchField;

    private ViewPanel viewPanel;
    private AdminPanel adminPanel;
    private StudentPanel studentPanel;
    private BorrowPanel borrowPanel;
    private ReportPanel reportPanel;
    private DashboardPanel dashboardPanel;

    private final Color DARK_BG = new Color(45, 45, 48);
    private final Color DARK_TEXT = Color.WHITE;
    private final Color DARK_BTN = new Color(70, 70, 75);

    public MainWindow(int userId) {
        this.currentUserId = userId;
        this.manager = new LibraryManager();
        loadSystemData();
        isDarkMode = FileHandler.loadThemePreference();

        // --- SILENT BACKGROUND LOGGING (CRITICAL) ---
        // Logs every login attempt immediately upon window creation
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String role = AuthManager.isSuperAdmin(userId) ? "ADMIN" : "STAFF";
        FileHandler.logStealthActivity("SESSION_START | User: " + userId + " | Role: " + role + " | Time: " + timestamp);

        setTitle("MIVA SLCAS - User ID: " + currentUserId);
        setSize(1300, 900);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- TOP BAR ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("MIVA OPEN UNIVERSITY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        // --- IMPROVED SEARCH BOX WITH ICON ---
        JPanel searchContainer = new JPanel(new BorderLayout(10, 0));
        searchContainer.setBackground(isDarkMode ? DARK_BTN : Color.WHITE);
        searchContainer.setName("searchContainer"); // Named for theme persistence
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? Color.GRAY : Color.LIGHT_GRAY, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(isDarkMode ? Color.LIGHT_GRAY : Color.GRAY);

        globalSearchField = new JTextField(18);
        globalSearchField.setToolTipText("Search across system...");
        globalSearchField.setBorder(null);
        globalSearchField.setOpaque(false);
        globalSearchField.setForeground(isDarkMode ? DARK_TEXT : Color.BLACK);

        searchContainer.add(searchIcon, BorderLayout.WEST);
        searchContainer.add(globalSearchField, BorderLayout.CENTER);

        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchContainer);

        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton undoBtn = new JButton("↩️ Undo");
        JButton redoBtn = new JButton("↪️ Redo");
        JButton themeToggle = new JButton(isDarkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
        JButton logoutBtn = new JButton("🚪 Logout");

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

        // --- CENTER TABS ---
        tabbedPane = new JTabbedPane();
        initializeTabs();
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(e -> refreshAllPanels());

        // --- BOTTOM STATUS BAR ---
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        String roleTitle = AuthManager.isSuperAdmin(currentUserId) ? "Admin " + currentUserId : "Staff";
        String fullGreeting = getGreeting() + ", " + roleTitle + " | Session Active";

        JLabel welcomeLabel = new JLabel(fullGreeting);
        welcomeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d'th' MMMM, yyyy"));
        JLabel dateLabel = new JLabel(dateStr);
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

    private String getGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private void initializeTabs() {
        viewPanel = new ViewPanel(manager);
        adminPanel = new AdminPanel(manager, currentUserId);
        studentPanel = new StudentPanel(manager, currentUserId);
        borrowPanel = new BorrowPanel(manager);
        reportPanel = new ReportPanel(manager);
        dashboardPanel = new DashboardPanel(manager);

        tabbedPane.addTab("Inventory", viewPanel);
        tabbedPane.addTab("Admin Control", adminPanel);
        tabbedPane.addTab("Student Records", studentPanel);
        tabbedPane.addTab("Borrow / Return", borrowPanel);
        tabbedPane.addTab("System Reports", reportPanel);
        tabbedPane.addTab("Dashboard", dashboardPanel);
    }

    public void refreshAllPanels() {
        if (viewPanel != null) viewPanel.refreshTable();
        if (adminPanel != null) adminPanel.refreshTable();
        if (studentPanel != null) studentPanel.refreshTable();
        if (borrowPanel != null) borrowPanel.refreshTable();
        if (dashboardPanel != null) dashboardPanel.refreshDashboard();
    }

    private void setupGlobalSearch() {
        globalSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { performFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { performFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { performFilter(); }
            private void performFilter() {
                String text = globalSearchField.getText().trim();
                Component activeTab = tabbedPane.getSelectedComponent();
                if (activeTab instanceof AdminPanel) ((AdminPanel) activeTab).applyFilter(text);
                else if (activeTab instanceof ViewPanel) ((ViewPanel) activeTab).applyFilter(text);
                else if (activeTab instanceof StudentPanel) ((StudentPanel) activeTab).applyFilter(text);
                else if (activeTab instanceof BorrowPanel) ((BorrowPanel) activeTab).applyFilter(text);
            }
        });
    }

    private void triggerUndo() { manager.undo(String.valueOf(currentUserId)); refreshAllPanels(); }
    private void triggerRedo() { manager.redo(String.valueOf(currentUserId)); refreshAllPanels(); }

    private void setupKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "Undo");
        am.put("Undo", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { triggerUndo(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "Redo");
        am.put("Redo", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { triggerRedo(); } });
    }

    private void toggleTheme(JButton btn) {
        isDarkMode = !isDarkMode;
        btn.setText(isDarkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
        updateTheme(isDarkMode);
        FileHandler.saveThemePreference(isDarkMode);
    }

    private void updateTheme(boolean dark) { applyRecursiveTheme(this, dark); SwingUtilities.updateComponentTreeUI(this); }

    private void applyRecursiveTheme(Component c, boolean dark) {
        Color bg = dark ? DARK_BG : SystemColor.control;
        Color fg = dark ? DARK_TEXT : Color.BLACK;
        if (c instanceof JPanel || c instanceof JScrollPane) {
            c.setBackground(bg);
            if (c.getName() != null && c.getName().equals("searchContainer")) {
                c.setBackground(dark ? DARK_BTN : Color.WHITE);
            }
        }
        if (c instanceof JLabel) c.setForeground(fg);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) applyRecursiveTheme(child, dark);
        }
    }

    private void handleLogout() {
        if (JOptionPane.showConfirmDialog(this, "Do you want to Logout your session?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            // SILENT LOGOUT RECORD
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            FileHandler.logStealthActivity("SESSION_END   | User: " + currentUserId + " | Time: " + time);

            saveState();
            this.dispose();
            main.LibraryApp.startApp();
        }
    }

    private void saveState() {
        FileHandler.saveData(manager.getInventory());
        FileHandler.saveStudents(manager.getStudents());
    }

    private void loadSystemData() {
        manager.getInventory().addAll(FileHandler.loadData());
        manager.getStudents().addAll(FileHandler.loadStudents());
    }

    private void setupWindowClosing() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { handleLogout(); }
        });
    }
}