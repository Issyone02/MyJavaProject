package main;

import gui.MainWindow;
import gui.LoginDialog;
import javax.swing.*;

/**
 * LibraryApp - Main entry point for the Smart Library System
 *
 * This is where the application starts. It:
 * 1. Sets the UI look and feel
 * 2. Loads user accounts
 * 3. Shows login dialog
 * 4. Opens main window on successful login
 *
 * The startApp() method is public static so it can be called
 * again when users log out (to restart the login flow).
 */
public class LibraryApp {
    /**
     * Main method - Application entry point
     * Called by JVM when running the program
     */
    public static void main(String[] args) {

        // Before anything else starts, let's make sure the UI looks consistent on every platform
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Kick things off — this is where the app actually begins
        startApp();
    }

    /**
     * Starts the application login flow
     *
     * This method is static and public so it can be called from
     * MainWindow when user logs out (to restart login process).
     *
     * Flow:
     * 1. Load user accounts from disk
     * 2. Show login dialog
     * 3. If login successful, open main window
     * 4. If login failed/cancelled, exit application
     */
    public static void startApp() {

        // Load saved user accounts before showing login
        utils.AuthManager.loadUsers();

        // Show login dialog on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Bring up the login dialog and wait for the user to interact with it
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            // Check if login was successful
            if (login.isSucceeded()) {
                // User logged in - open main window with their user ID
                new MainWindow(login.getLoggedInUserId());

            } else {
                // User cancelled or failed login - exit application
                System.exit(0);
            }
        });
    }
}