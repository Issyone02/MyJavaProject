package gui;

import utils.AuthManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class UserManagementDialog extends JDialog {
    private final DefaultTableModel model;

    public UserManagementDialog(Frame parent) {
        super(parent, "System User Database", true);
        setSize(600, 500);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(parent);

        // --- 1. User List Table ---
        String[] cols = {"User ID", "Name", "Role"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        refreshTable();

        // --- 2. Input Form ---
        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5)); // Increased to 4 rows
        form.setBorder(BorderFactory.createTitledBorder("Add / Update User Profile"));

        JTextField idIn = new JTextField();
        JTextField nickIn = new JTextField(); // NEW FIELD
        JTextField passIn = new JPasswordField(); // Security improvement

        JButton saveBtn = new JButton("Save User");
        JButton deleteBtn = new JButton("Delete User");

        form.add(new JLabel("User ID:")); form.add(idIn);
        form.add(new JLabel("Full Name:")); form.add(nickIn);
        form.add(new JLabel("Password:")); form.add(passIn);
        form.add(saveBtn); form.add(deleteBtn);

        // --- 3. Logic ---
        saveBtn.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idIn.getText());
                String nickname = nickIn.getText().trim();
                String pass = passIn.getText().trim();

                if (nickname.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields.");
                    return;
                }

                AuthManager.addUser(id, pass, nickname);
                refreshTable();
                idIn.setText(""); nickIn.setText(""); passIn.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID must be numeric.");
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int id = (int) model.getValueAt(row, 0);
                if (id == AuthManager.SUPER_ADMIN_ID) {
                    JOptionPane.showMessageDialog(this, "The System Owner cannot be deleted!");
                    return;
                }
                AuthManager.removeUser(id);
                refreshTable();
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
    }

    private void refreshTable() {
        model.setRowCount(0);
        Map<Integer, AuthManager.User> users = AuthManager.getAllUsers();
        for (Map.Entry<Integer, AuthManager.User> entry : users.entrySet()) {
            String role = entry.getKey() == AuthManager.SUPER_ADMIN_ID ? "Super Admin" : "Staff";
            model.addRow(new Object[]{entry.getKey(), entry.getValue().nickname, role});
        }
    }
}