package main;

import gui.MainWindow;
import gui.LoginDialog;
import javax.swing.*;

/**
 * Main entry point. Shows login dialog, then launches MainWindow on success.
 */
public class LibraryApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        startApp();
    }

    public static void startApp() {
        utils.AuthManager.loadUsers();
        utils.DataSeeder.seedIfEmpty();
        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            if (login.isSucceeded()) {
                new MainWindow(login.getLoggedInUserId());
            } else {
                System.exit(0);
            }
        });
    }
}
