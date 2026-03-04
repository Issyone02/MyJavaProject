package main;

import gui.MainWindow;
import gui.LoginDialog;
import javax.swing.*;

public class LibraryApp {
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
     * This method handles the lifecycle of the login and main window.
     * It is static so it can be called again by MainWindow during a logout.
     */
    public static void startApp() {
        // Pull in any users we've saved previously so login has something to work with
        utils.AuthManager.loadUsers();

        SwingUtilities.invokeLater(() -> {
            // 1. Bring up the login dialog and wait for the user to interact with it
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);

            // 2. Once they're done, check whether they actually logged in successfully
            if (login.isSucceeded()) {
                // 3. All good — open up the main window for the user that just logged in
                new MainWindow(login.getLoggedInUserId());
            } else {
                // 4. They bailed out or failed — no point keeping the app open
                System.exit(0);
            }
        });
    }
}