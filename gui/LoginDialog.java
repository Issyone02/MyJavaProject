package gui;

import utils.AuthManager;
import utils.FileHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginDialog extends JDialog {
    private final JTextField idField;
    private final JPasswordField passField;
    private boolean succeeded;
    private int loggedInUserId;

    public LoginDialog(Frame parent) {
        super(parent, "SLCAS System Login", true);

        // Load preference before building UI
        boolean isDarkMode = FileHandler.loadThemePreference();
        Color bg = isDarkMode ? new Color(45, 45, 48) : Color.WHITE;
        Color fg = isDarkMode ? Color.WHITE : Color.BLACK;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bg);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblId = new JLabel("User ID:");
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
        btnLogin.addActionListener(this::handleLogin);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);

        getContentPane().add(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void handleLogin(ActionEvent e) {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            String pass = new String(passField.getPassword());
            if (AuthManager.validate(id, pass)) {
                loggedInUserId = id;
                succeeded = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID must be numeric", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSucceeded() { return succeeded; }
    public int getLoggedInUserId() { return loggedInUserId; }
}