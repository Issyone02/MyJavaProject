package gui;

import controller.AuthController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/** Login dialog — validates staff credentials via AuthController. */
public class LoginDialog extends JDialog {

    private final AuthController  auth;
    private final JTextField      idField;
    private final JPasswordField  passField;
    private       boolean         succeeded;
    private       int             loggedInUserId;

    /**
     * Creates login dialog with parent frame and authentication controller.
     * Initializes UI components and sets up event handlers for login functionality.
     * 
     * @param parent Parent frame for the dialog.
     * @param auth   Authentication controller for validating staff credentials.
     */
    public LoginDialog(Frame parent, AuthController auth) {
        super(parent, "MIVA SLCAS - Secure Login Page", true);
        this.auth = auth;

        // Create main panel with CardLayout for switching between views
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Add ID label and text field
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Staff/Admin ID:"), gbc);
        idField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(idField, gbc);

        // Add password label and field
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        passField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        // Add login button with styling and event handler
        JButton btnLogin = new JButton("Login");
        btnLogin.setPreferredSize(new Dimension(100, 35));
        btnLogin.addActionListener(this::handleLogin);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(btnLogin, gbc);

        // Add Enter key listener to submit form from any field
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) handleLogin(null);
            }
        };
        idField.addKeyListener(enterKey);
        passField.addKeyListener(enterKey);

        getContentPane().add(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /** Handles login validation and authentication. Shows appropriate error messages. */
    private void handleLogin(ActionEvent e) {
        String idRaw = idField.getText().trim();
        String pass  = new String(passField.getPassword());

        if (idRaw.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both ID and Password.",
                    "Login Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idRaw);
            if (auth.validate(id, pass)) {
                loggedInUserId = id;
                succeeded = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Access Denied: Invalid Credentials",
                        "Security Alert", JOptionPane.ERROR_MESSAGE);
                auth.logFailedAttempt(idRaw);  // file I/O delegated to AuthController
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "System ID must be a numeric value.",
                    "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Returns true if login was successful. */
    public boolean isSucceeded()   { return succeeded; }
    
    /** Returns the user ID of the logged-in staff member. */
    public int getLoggedInUserId() { return loggedInUserId; }
}
