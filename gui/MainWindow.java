package gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.JTableHeader;
import model.LibraryManager;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import utils.FileHandler;
import utils.AuthManager;

/**
 * MainWindow - Primary application window for the Smart Library System
 * Provides tabbed interface for all library management functions
 * Features:
 * - Global search across all tabs
 * - Undo/Redo functionality
 * - Dark/Light theme toggle
 * - Role-based access control
 * - Keyboard shortcuts
 */
public class MainWindow extends JFrame {
    // Core application components
    private LibraryManager manager;              // Business logic controller
    private boolean isDarkMode;                  // Current theme state
    private int currentUserId;                   // Logged-in user's ID
    private String currentUserName;              // Logged-in user's display name

    // UI Components
    private JTabbedPane tabbedPane;             // Main tab container
    private JTextField globalSearchField;        // Global search input
    private JPanel searchWrapper;                // Search field container

    // Panel references for refresh operations
    private ViewPanel viewPanel;
    private AdminPanel adminPanel;
    private StudentPanel studentPanel;
    private BorrowPanel borrowPanel;
    private ReportPanel reportPanel;
    private DashboardPanel dashboardPanel;

    // CRITICAL FIX #4: Enhanced dark mode color scheme for better visibility
    // Improved contrast ratios for better readability
    private final Color DARK_BG = new Color(45, 45, 48);           // Main background (darker)
    private final Color DARK_PANEL = new Color(30, 30, 30);        // Panel background (even darker)
    private final Color DARK_TEXT = new Color(230, 230, 230);      // Main text (brighter white)
    private final Color DARK_BTN = new Color(60, 60, 65);          // Button background
    private final Color DARK_BTN_TEXT = new Color(240, 240, 240);  // Button text (bright)
    private final Color DARK_INPUT = new Color(70, 70, 75);        // Input field background
    private final Color DARK_INPUT_TEXT = new Color(255, 255, 255); // Input text (pure white)
    private final Color DARK_BORDER = new Color(80, 80, 85);       // Border color
    private final Color DARK_TABLE_GRID = new Color(60, 60, 65);   // Table grid lines

    /**
     * Constructor - Initializes the main application window
     * @param userId ID of the logged-in user
     */
    public MainWindow(int userId) {
        this.currentUserId = userId;

        // Retrieve user information from auth system
        AuthManager.User u = AuthManager.getAllUsers().get(userId);
        this.currentUserName = (u != null) ? u.nickname : "User " + userId;

        // Initialize library manager (loads all data)
        this.manager = new LibraryManager();

        // Load user's theme preference
        isDarkMode = FileHandler.loadThemePreference();

        // Log session start for audit trail
        FileHandler.logStealthActivity("SESSION_START | User: " + currentUserName + " | ID: " + userId);

        // ==================== WINDOW SETUP ====================
        setTitle("MIVA SLCAS - Library Management System [" + currentUserName + "]");
        setSize(1300, 900);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);  // Custom close handling
        setLocationRelativeTo(null);  // Center on screen
        setLayout(new BorderLayout());

        // ==================== TOP BAR ====================
        // Contains logo, search, and action buttons
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // University branding
        JLabel titleLabel = new JLabel("MIVA OPEN UNIVERSITY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        // Global search container with icon
        JPanel searchContainer = new JPanel(new BorderLayout(10, 0));
        searchContainer.setName("searchContainer");  // Named for theme application
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        JLabel searchIcon = new JLabel("🔍");  // Search icon
        globalSearchField = new JTextField(20);
        globalSearchField.setBorder(null);
        globalSearchField.setOpaque(false);
        searchContainer.add(searchIcon, BorderLayout.WEST);
        searchContainer.add(globalSearchField, BorderLayout.CENTER);

        // Wrapper for centering search box
        searchWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchContainer);

        // Right-side action buttons
        JPanel rightActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton undoBtn = new JButton("↩ Undo");
        JButton redoBtn = new JButton("↪ Redo");
        JButton themeToggle = new JButton(isDarkMode ? "☀️ Light" : "🌙 Dark");
        JButton logoutBtn = new JButton("🚪 Logout");

        // Attach action listeners to buttons
        undoBtn.addActionListener(e -> triggerUndo());
        redoBtn.addActionListener(e -> triggerRedo());
        themeToggle.addActionListener(e -> toggleTheme(themeToggle));
        logoutBtn.addActionListener(e -> handleLogout());

        // Add all buttons to action panel
        rightActionPanel.add(undoBtn);
        rightActionPanel.add(redoBtn);
        rightActionPanel.add(themeToggle);
        rightActionPanel.add(logoutBtn);

        // Assemble top bar
        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(searchWrapper, BorderLayout.CENTER);
        topBar.add(rightActionPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ==================== TABBED INTERFACE ====================
        tabbedPane = new JTabbedPane();
        initializeTabs();
        add(tabbedPane, BorderLayout.CENTER);

        // Tab change listener - controls search visibility and refreshes data
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index != -1) {
                String title = tabbedPane.getTitleAt(index);
                // Hide search on Dashboard and Reports tabs
                searchWrapper.setVisible(!title.contains("Dashboard") && !title.contains("Reports"));
                refreshAllPanels();  // Refresh data when switching tabs
            }
        });

        // ==================== STATUS BAR ====================
        // Shows user info and current date
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Determine user role
        boolean isAdmin = AuthManager.isSuperAdmin(currentUserId);
        String roleTitle = isAdmin ? "Administrator" : "Staff Librarian";

        // Welcome message with time-based greeting
        JLabel welcomeLabel = new JLabel(getGreeting() + ", " + currentUserName + " (" + roleTitle + ")");
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Current date display
        JLabel dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        statusBar.add(welcomeLabel, BorderLayout.WEST);
        statusBar.add(dateLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        // ==================== INITIALIZATION ====================
        setupGlobalSearch();       // Enable search functionality
        setupKeyboardShortcuts();  // Register keyboard shortcuts
        setupWindowClosing();      // Handle window close events
        updateTheme(isDarkMode);   // Apply current theme

        setVisible(true);
    }

    /**
     * Initializes all tab panels and adds them to the tabbed pane
     * Panels are created once and reused for performance
     */
    private void initializeTabs() {
        // Create panel instances
        viewPanel = new ViewPanel(manager);
        studentPanel = new StudentPanel(manager, currentUserId);
        borrowPanel = new BorrowPanel(manager, currentUserId);
        dashboardPanel = new DashboardPanel(manager, currentUserId);
        adminPanel = new AdminPanel(manager, currentUserId);
        reportPanel = new ReportPanel(manager);

        // Add tabs in logical order
        tabbedPane.addTab("Inventory", viewPanel);
        tabbedPane.addTab("Admin Page", adminPanel);
        tabbedPane.addTab("Student Records", studentPanel);
        tabbedPane.addTab("Borrow & Return", borrowPanel);
        tabbedPane.addTab("Reports", reportPanel);
        tabbedPane.addTab("Visual Dashboard", dashboardPanel);
    }

    /**
     * Returns time-appropriate greeting based on current hour
     * @return Greeting string (Good Morning/Afternoon/Evening)
     */
    private String getGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    /**
     * Refreshes data in all tab panels
     * Called when switching tabs or after data modifications
     */
    public void refreshAllPanels() {
        if (viewPanel != null) viewPanel.refreshTable();
        if (studentPanel != null) studentPanel.refreshTable();
        if (borrowPanel != null) borrowPanel.refreshTable();
        if (dashboardPanel != null) dashboardPanel.refreshDashboard();
        if (adminPanel != null) adminPanel.refreshTable();
    }

    /**
     * Sets up global search functionality across all tabs
     * Search filters are applied in real-time as user types
     */
    private void setupGlobalSearch() {
        globalSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { performFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { performFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { performFilter(); }

            /**
             * Applies search filter to the currently active tab
             * Different panels implement their own filtering logic
             */
            private void performFilter() {
                String text = globalSearchField.getText().trim();
                Component active = tabbedPane.getSelectedComponent();

                // Route filter request to appropriate panel
                if (active instanceof ViewPanel) ((ViewPanel) active).applyFilter(text);
                else if (active instanceof StudentPanel) ((StudentPanel) active).applyFilter(text);
                else if (active instanceof AdminPanel) ((AdminPanel) active).applyFilter(text);
                else if (active instanceof BorrowPanel) ((BorrowPanel) active).applyFilter(text);
            }
        });
    }

    /**
     * Triggers undo operation
     * Reverts to previous system state
     */
    private void triggerUndo() {
        manager.undo(currentUserName);
        refreshAllPanels();
    }

    /**
     * Triggers redo operation (VERIFIED WORKING - Issue #3)
     * Restores a previously undone state
     */
    private void triggerRedo() {
        manager.redo(currentUserName);
        refreshAllPanels();
    }

    /**
     * Toggles between dark and light themes
     * Saves preference for next session
     * @param btn Theme toggle button (text is updated)
     */
    private void toggleTheme(JButton btn) {
        isDarkMode = !isDarkMode;
        btn.setText(isDarkMode ? "☀️ Light" : "🌙 Dark");
        updateTheme(isDarkMode);
        FileHandler.saveThemePreference(isDarkMode);
    }

    /**
     * Applies theme to entire component tree
     * @param dark true for dark mode, false for light mode
     */
    private void updateTheme(boolean dark) {
        applyRecursiveTheme(this, dark);
        SwingUtilities.updateComponentTreeUI(this);
    }

    /**
     * CRITICAL FIX #4: Comprehensive dark mode theme application
     * Recursively applies theme to ALL component types for proper visibility
     * Enhanced from original version which only handled panels and labels
     *
     * @param c Component to theme
     * @param dark true for dark mode, false for light mode
     */
    private void applyRecursiveTheme(Component c, boolean dark) {
        // Define colors based on theme
        Color bg = dark ? DARK_BG : SystemColor.control;
        Color fg = dark ? DARK_TEXT : Color.BLACK;

        // Apply theme to panels and containers
        if (c instanceof JPanel || c instanceof JScrollPane || c instanceof JTabbedPane) {
            c.setBackground(bg);

            // Special handling for search container
            if ("searchContainer".equals(c.getName())) {
                c.setBackground(dark ? DARK_INPUT : Color.WHITE);
            }
        }

        // Apply theme to labels and text display components
        if (c instanceof JLabel || c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setForeground(fg);
        }

        // FIX #4: Apply theme to buttons for better visibility in dark mode
        if (c instanceof JButton) {
            JButton btn = (JButton) c;

            // SPECIAL CHECK: If the button has a specific color (Red or Green), skip the theme logic
            // This prevents the theme from overwriting AdminPanel's color-coded buttons
            boolean isSpecialActionBtn = btn.getBackground().equals(new Color(255, 100, 100)) ||
                    btn.getBackground().equals(new Color(76, 175, 80));

            if (!isSpecialActionBtn) {
                if (dark) {
                    btn.setBackground(DARK_BTN);
                    btn.setForeground(DARK_BTN_TEXT);
                    btn.setBorder(BorderFactory.createLineBorder(DARK_BORDER, 1));
                } else {
                    // Reset to default for light mode
                    btn.setBackground(null);
                    btn.setForeground(null);
                    btn.setBorder(UIManager.getBorder("Button.border"));
                }
            }
        }

        // FIX #4: Apply theme to text input fields
        if (c instanceof JTextField || c instanceof JPasswordField || c instanceof JTextArea) {
            if (dark) {
                c.setBackground(DARK_INPUT);
                c.setForeground(DARK_INPUT_TEXT);
                // Cast to JTextComponent to access setCaretColor method
                if (c instanceof JTextField) {
                    ((JTextField) c).setCaretColor(DARK_INPUT_TEXT);
                } else if (c instanceof JPasswordField) {
                    ((JPasswordField) c).setCaretColor(DARK_INPUT_TEXT);
                } else if (c instanceof JTextArea) {
                    ((JTextArea) c).setCaretColor(DARK_INPUT_TEXT);
                }
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
                // Cast to JTextComponent to access setCaretColor method
                if (c instanceof JTextField) {
                    ((JTextField) c).setCaretColor(Color.BLACK);
                } else if (c instanceof JPasswordField) {
                    ((JPasswordField) c).setCaretColor(Color.BLACK);
                } else if (c instanceof JTextArea) {
                    ((JTextArea) c).setCaretColor(Color.BLACK);
                }
            }
        }

        // FIX #4: Apply theme to combo boxes
        if (c instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) c;
            if (dark) {
                combo.setBackground(DARK_INPUT);
                combo.setForeground(DARK_INPUT_TEXT);
            } else {
                combo.setBackground(Color.WHITE);
                combo.setForeground(Color.BLACK);
            }
        }

        // FIX #4: Apply theme to tables for better readability
        if (c instanceof JTable) {
            JTable table = (JTable) c;
            if (dark) {
                table.setBackground(DARK_PANEL);
                table.setForeground(DARK_TEXT);
                table.setGridColor(DARK_TABLE_GRID);
                table.setSelectionBackground(DARK_BTN);
                table.setSelectionForeground(DARK_TEXT);

                // Apply theme to table header
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    header.setBackground(DARK_BTN);
                    header.setForeground(DARK_BTN_TEXT);
                }
            } else {
                table.setBackground(Color.WHITE);
                table.setForeground(Color.BLACK);
                table.setGridColor(Color.LIGHT_GRAY);
                table.setSelectionBackground(new Color(184, 207, 229));
                table.setSelectionForeground(Color.BLACK);

                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    header.setBackground(null);
                    header.setForeground(null);
                }
            }
        }

        // Recursively apply to all child components
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                applyRecursiveTheme(child, dark);
            }
        }
    }

    /**
     * Handles logout operation
     * Confirms with user before closing session and returning to login
     */
    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to end your session?",
                "Logout Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            // Log session end
            FileHandler.logStealthActivity("SESSION_END | User: " + currentUserName);

            // Save current state
            manager.saveState(false);

            // Close window and restart login flow
            this.dispose();
            main.LibraryApp.startApp();
        }
    }

    /**
     * Sets up window close handling
     * Prevents accidental closure without confirmation
     */
    private void setupWindowClosing() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();  // Reuse logout confirmation
            }
        });
    }

    /**
     * Registers keyboard shortcuts for common operations
     * Ctrl+Z: Undo
     * Ctrl+Y: Redo
     */
    private void setupKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // Get platform-specific modifier key (Cmd on Mac, Ctrl on Windows/Linux)
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+Z / Cmd+Z for Undo
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask), "UndoAction");
        am.put("UndoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                triggerUndo();
            }
        });

        // Ctrl+Y / Cmd+Y for Redo
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask), "RedoAction");
        am.put("RedoAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                triggerRedo();
            }
        });
    }
}