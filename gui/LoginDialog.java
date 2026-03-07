package gui;

import utils.AuthManager;
import utils.FileHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginDialog extends JDialog {
   // THis is for Login Dialog Box
    private final JTextField idField;
    private final JPasswordField passField;
    private boolean succeeded;
    private int loggedInUserId;

    public LoginDialog(Frame parent) {
        super(parent, "MIVA SLCAS - Secure Login Page", true);

        boolean isDarkMode = FileHandler.loadThemePreference();
        Color bg = isDarkMode ? new Color(45, 45, 48) : new Color(240, 240, 240);
        Color fg = isDarkMode ? Color.WHITE : Color.BLACK;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblId = new JLabel("Staff/Admin ID:");
        lblId.setForeground(fg);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblId, gbc);

        idField = new JTextField(15);
        idField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        idField.setForeground(fg);
        idField.setCaretColor(fg);
        gbc.gridx = 1;
        panel.add(idField, gbc);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setForeground(fg);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblPass, gbc);

        passField = new JPasswordField(15);
        passField.setBackground(isDarkMode ? new Color(60, 60, 60) : Color.WHITE);
        passField.setForeground(fg);
        passField.setCaretColor(fg);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        JButton btnLogin = new JButton("Login");
        btnLogin.setPreferredSize(new Dimension(100, 35));
        btnLogin.addActionListener(this::handleLogin);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(btnLogin, gbc);

        // Initializing Enter key
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
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

    // This is Login Field
    private void handleLogin(ActionEvent e) {

        String idRaw = idField.getText().trim();
        String pass = new String(passField.getPassword());

        // If any field is empty, a pop - up to fill both fields with appropriate characters
        if (idRaw.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both ID and Password.", "Login Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // If ID / Passwords are valid, proceed to Main Window
            int id = Integer.parseInt(idRaw);
            if (AuthManager.validate(id, pass)) {
                loggedInUserId = id;
                succeeded = true;
                dispose();
            } else {
                // If ID/Password not valid, then throw this pop-up
                JOptionPane.showMessageDialog(this, "Access Denied: Invalid Credentials", "Security Alert", JOptionPane.ERROR_MESSAGE);


                FileHandler.logStealthActivity("FAILED LOGIN ATTEMPT for ID: " + idRaw);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "System ID must be a numeric value.", "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSucceeded() { return succeeded; }
    public int getLoggedInUserId() { return loggedInUserId; }
}