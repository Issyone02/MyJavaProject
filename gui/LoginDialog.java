package gui;

import utils.AuthManager;
import utils.FileHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Login dialog — validates staff credentials against AuthManager.
 */
public class LoginDialog extends JDialog {
    private final JTextField idField;
    private final JPasswordField passField;
    private boolean succeeded;
    private int loggedInUserId;

    public LoginDialog(Frame parent) {
        super(parent, "MIVA SLCAS - Secure Login Page", true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Staff/Admin ID:"), gbc);
        idField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Password:"), gbc);
        passField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        JButton btnLogin = new JButton("Login");
        btnLogin.setPreferredSize(new Dimension(100, 35));
        btnLogin.addActionListener(this::handleLogin);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(btnLogin, gbc);

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

    private void handleLogin(ActionEvent e) {
        String idRaw = idField.getText().trim();
        String pass = new String(passField.getPassword());
        if (idRaw.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both ID and Password.",
                "Login Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idRaw);
            if (AuthManager.validate(id, pass)) {
                loggedInUserId = id;
                succeeded = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Access Denied: Invalid Credentials",
                    "Security Alert", JOptionPane.ERROR_MESSAGE);
                FileHandler.logStealthActivity("FAILED LOGIN ATTEMPT for ID: " + idRaw);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "System ID must be a numeric value.",
                "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSucceeded() { return succeeded; }
    public int getLoggedInUserId() { return loggedInUserId; }
}
