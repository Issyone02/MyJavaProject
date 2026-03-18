package main;

import gui.MainWindow;
import gui.LoginDialog;
import utils.AuthManager;
import utils.DataSeeder;
import javax.swing.*;

/** Main entry point for the MIVA SLCAS Library Management System. */
public class  LibraryApp {

    /** Application entry point. Sets look and feel and starts the application. */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception e) { e.printStackTrace(); }
        startApp();
    }

    /** Initializes the application: loads users, seeds data, and shows login dialog. */
    public static void startApp() {
        AuthManager.loadUsers();
        DataSeeder.seedIfEmpty();
        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null, AuthManager.getInstance());
            login.setVisible(true);
            if (login.isSucceeded()) new MainWindow(login.getLoggedInUserId());
            else System.exit(0);
        });
    }
}
