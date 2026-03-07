package gui;

import model.LibraryManager;
import utils.AuthManager;
import utils.FileHandler;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class UserManagementDialog extends JDialog {
    private final DefaultTableModel model;
    private final JCheckBox adminCheck;
    private final JTextField idIn, nickIn; // idIn will now be used for input masking or cleared
    private final JPasswordField passIn;
    private final JButton deleteBtn;
    private final int currentAdminId;
    private final LibraryManager manager;

    // A hidden map to keep track of which row belongs to which ID since the table is now masked
    private final java.util.HashMap<Integer, Integer> rowToIdMap = new java.util.HashMap<>();

    public UserManagementDialog(Frame parent, int currentAdminId, LibraryManager manager) {
        super(parent, "System User Database", true);
        this.currentAdminId = currentAdminId;
        this.manager = manager;
        setSize(600, 600);
        setLayout(new BorderLayout(15, 15));
        setLocationRelativeTo(parent);

        // --- 1. User List Table ---
        String[] cols = {"User ID", "Display Name", "System Role"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(25);
        refreshTable();

        // --- 2. Input Form ---
        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Add / Update User Profile"));

        // SECURITY UPDATE: We use a JPasswordField even for the ID input to keep it consistent
        idIn = new JPasswordField();
        nickIn = new JTextField();
        passIn = new JPasswordField();
        adminCheck = new JCheckBox("Assign as Administrator");

        JButton saveBtn = new JButton("Save / Update User");
        deleteBtn = new JButton("Delete Selected User");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);

        form.add(new JLabel("  User System ID:")); form.add(idIn);
        form.add(new JLabel("  Display Name:")); form.add(nickIn);
        form.add(new JLabel("  Access Password:")); form.add(passIn);
        form.add(new JLabel("  Permissions:")); form.add(adminCheck);
        form.add(saveBtn); form.add(deleteBtn);

        // --- 3. Logic ---

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    // Retrieve the real ID from our hidden map using the row index
                    int id = rowToIdMap.get(row);
                    AuthManager.User u = AuthManager.getAllUsers().get(id);

                    idIn.setText(String.valueOf(id));
                    nickIn.setText(u.nickname);
                    passIn.setText(u.password);
                    adminCheck.setSelected(u.isSuper);
                    idIn.setEditable(false);

                    if (id == AuthManager.SUPER_ADMIN_ID) {
                        adminCheck.setEnabled(false);
                        deleteBtn.setEnabled(false);
                        passIn.setEditable(false);
                        passIn.setBackground(Color.LIGHT_GRAY);
                        idIn.setBackground(Color.LIGHT_GRAY);
                    } else {
                        adminCheck.setEnabled(true);
                        deleteBtn.setEnabled(true);
                        passIn.setEditable(true);
                        passIn.setBackground(Color.WHITE);
                        idIn.setBackground(Color.WHITE);
                    }
                }
            }
        });

        saveBtn.addActionListener(e -> {
            try {
                String idStr = idIn.getText().trim();
                String nickname = nickIn.getText().trim();
                String pass = new String(passIn.getPassword()).trim();

                if (idStr.isEmpty() || nickname.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }

                if (!confirmAdminCredentials()) {
                    JOptionPane.showMessageDialog(this, "Authorization denied. Operation aborted.", "Security Alert", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int id = Integer.parseInt(idStr);
                boolean isAdm = (id == AuthManager.SUPER_ADMIN_ID) ? true : adminCheck.isSelected();

                String action = AuthManager.isUserExists(id) ? "UPDATE_USER" : "CREATE_USER";
                manager.addLog(String.valueOf(currentAdminId), action, "Target User: " + nickname + " (Masked ID)");
                FileHandler.logStealthActivity(action + " by " + currentAdminId + " on ID " + id);

                AuthManager.addUser(id, pass, nickname, isAdm);
                refreshTable();

                idIn.setText(""); idIn.setEditable(true);
                nickIn.setText(""); passIn.setText("");
                passIn.setEditable(true);
                passIn.setBackground(Color.WHITE);
                idIn.setBackground(Color.WHITE);
                adminCheck.setSelected(false);
                adminCheck.setEnabled(true);
                deleteBtn.setEnabled(true);

                JOptionPane.showMessageDialog(this, "User database updated successfully.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID must be a numeric value.");
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int id = rowToIdMap.get(row);

                if (id == AuthManager.SUPER_ADMIN_ID) {
                    JOptionPane.showMessageDialog(this, "Action Denied: The System Owner cannot be deleted.");
                    return;
                }

                if (!confirmAdminCredentials()) return;

                if (JOptionPane.showConfirmDialog(this, "Delete selected user?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    manager.addLog(String.valueOf(currentAdminId), "DELETE_USER", "Removed a User (Masked ID)");
                    FileHandler.logStealthActivity("DELETE_USER by " + currentAdminId + " on ID " + id);

                    AuthManager.removeUser(id);
                    refreshTable();
                    idIn.setText(""); idIn.setEditable(true);
                    nickIn.setText(""); passIn.setText("");
                    passIn.setEditable(true);
                    passIn.setBackground(Color.WHITE);
                    idIn.setBackground(Color.WHITE);
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
    }

    private boolean confirmAdminCredentials() {
        JPasswordField pf = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(this, pf, "Confirm Identity: Enter YOUR Admin Password:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String pass = new String(pf.getPassword());
            return AuthManager.validate(currentAdminId, pass);
        }
        return false;
    }

    private void refreshTable() {
        model.setRowCount(0);
        rowToIdMap.clear(); // Clear the hidden mapping
        Map<Integer, AuthManager.User> users = AuthManager.getAllUsers();
        int currentRow = 0;
        for (Map.Entry<Integer, AuthManager.User> entry : users.entrySet()) {
            String role = entry.getValue().isSuper ? "Administrator" : "Staff Librarian";

            // SECURITY FIX: Show asterisks instead of the actual ID in the table column
            model.addRow(new Object[]{"********", entry.getValue().nickname, role});

            // Store the real ID in our hidden map so we can look it up when the row is clicked
            rowToIdMap.put(currentRow, entry.getKey());
            currentRow++;
        }
    }
}