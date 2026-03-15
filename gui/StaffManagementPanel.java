package gui;

import controller.LibraryManager;
import model.UserAccount;
import utils.AuthManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Staff account management panel (sub-tab under Admin).
 * Supports creating, editing, deleting staff accounts with visible ID and password toggle.
 */
public class StaffManagementPanel extends JPanel {
    private final VirtualTableModel tableModel;
    private final JTable table;
    private final JTextField idIn, nickIn;
    private final JPasswordField passIn;
    private final JCheckBox adminCheck;
    private JButton eyeBtn;
    private final JButton saveBtn, deleteBtn, resetPassBtn, clearBtn;
    private final int currentAdminId;
    private final LibraryManager manager;

    private final java.util.HashMap<Integer, Integer> rowToIdMap = new java.util.HashMap<>();

    public StaffManagementPanel(LibraryManager manager, int currentAdminId) {
        this.manager = manager;
        this.currentAdminId = currentAdminId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Staff list table
        String[] cols = {"Staff ID", "Display Name", "Role"};
        tableModel = new VirtualTableModel(cols);
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Form panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add / Edit Staff"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idIn       = new JTextField();
        nickIn     = new JTextField();
        passIn     = new JPasswordField();
        adminCheck = new JCheckBox("Administrator");

        eyeBtn = new JButton("\u2299");
        eyeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        eyeBtn.setFocusPainted(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeBtn.addActionListener(e -> {
            boolean on = passIn.getEchoChar() == (char) 0;
            passIn.setEchoChar(on ? '\u2022' : (char) 0);
            eyeBtn.setText(on ? "\u2299" : "\u2297");
        });

        JPanel passWrapper = new JPanel(new BorderLayout(0, 0));
        passWrapper.setBorder(passIn.getBorder());
        passIn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        passWrapper.add(passIn, BorderLayout.CENTER);
        passWrapper.add(eyeBtn, BorderLayout.EAST);

        Object[][] fields = {
            {"Staff ID:",     idIn},
            {"Display Name:", nickIn},
            {"Password:",     passWrapper},
            {"Role:",         adminCheck}
        };
        for (int r = 0; r < fields.length; r++) {
            gbc.gridx = 0; gbc.gridy = r; gbc.weightx = 0;
            JLabel lbl = new JLabel((String) fields[r][0]);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            form.add(lbl, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            form.add((Component) fields[r][1], gbc);
        }

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        saveBtn     = new JButton("Save");
        deleteBtn   = new JButton("Delete");
        resetPassBtn = new JButton("Reset Password");
        clearBtn    = new JButton("Clear Form");

        saveBtn.setToolTipText("Save new or updated staff record");
        deleteBtn.setToolTipText("Delete the selected staff member");
        resetPassBtn.setToolTipText("Reset password for the selected staff");
        clearBtn.setToolTipText("Clear the form fields");


        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true);
        deleteBtn.setBorderPainted(false);

        btnPanel.add(saveBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(resetPassBtn);
        btnPanel.add(clearBtn);

        gbc.gridx = 0; gbc.gridy = fields.length; gbc.gridwidth = 2;
        form.add(btnPanel, gbc);

        // Layout
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(form, BorderLayout.CENTER);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // Clicking a table row loads the staff member into the form
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row == -1) return;
                int id = rowToIdMap.get(row);
                UserAccount u = AuthManager.getAllUsers().get(id);
                if (u == null) return;

                idIn.setText(String.valueOf(id));
                idIn.setEditable(false);
                nickIn.setText(u.getFullName());
                passIn.setText("");
                passIn.setEchoChar('\u2022');
                eyeBtn.setText("\u2299");
                adminCheck.setSelected(u.isAdmin());

                boolean isSuperAdmin = (id == AuthManager.SUPER_ADMIN_ID);
                adminCheck.setEnabled(!isSuperAdmin);
                deleteBtn.setEnabled(!isSuperAdmin);
            }
        });

        // Save
        saveBtn.addActionListener(e -> {
            try {
                String idStr = idIn.getText().trim();
                String fullName = nickIn.getText().trim();
                String rawPass = new String(passIn.getPassword()).trim();

                if (idStr.isEmpty() || fullName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Staff ID and Display Name are required.");
                    return;
                }
                int id = Integer.parseInt(idStr);
                boolean isNew = !AuthManager.isUserExists(id);
                if (isNew && rawPass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Password is required for new staff.");
                    return;
                }
                if (!confirmAdmin()) return;

                boolean isAdm = (id == AuthManager.SUPER_ADMIN_ID) || adminCheck.isSelected();
                AuthManager.addUser(id, rawPass.isEmpty() ? null : rawPass, fullName, isAdm);
                manager.addLog(String.valueOf(currentAdminId), isNew ? "CREATE_STAFF" : "UPDATE_STAFF", fullName);
                refreshTable();
                clearForm();
                JOptionPane.showMessageDialog(this, "Staff record saved.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Staff ID must be numeric.");
            }
        });

        // Delete
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            int id = rowToIdMap.get(row);
            if (id == AuthManager.SUPER_ADMIN_ID) {
                JOptionPane.showMessageDialog(this, "The super admin cannot be deleted.");
                return;
            }
            if (!confirmAdmin()) return;
            if (JOptionPane.showConfirmDialog(this, "Delete this staff member?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                AuthManager.removeUser(id);
                manager.addLog(String.valueOf(currentAdminId), "DELETE_STAFF", "ID: " + id);
                refreshTable();
                clearForm();
            }
        });

        // Reset password
        resetPassBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            int targetId = rowToIdMap.get(row);
            String newPass = JOptionPane.showInputDialog(this, "Enter new password:");
            if (newPass != null && !newPass.trim().isEmpty() && confirmAdmin()) {
                AuthManager.resetUserPassword(targetId, newPass.trim());
                manager.addLog(String.valueOf(currentAdminId), "PASS_RESET", "ID: " + targetId);
                JOptionPane.showMessageDialog(this, "Password reset.");
            }
        });

        clearBtn.addActionListener(e -> clearForm());

        refreshTable();
    }

    private boolean confirmAdmin() {
        JPasswordField pf = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(this, pf, "Enter your admin password:",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            boolean ok = AuthManager.validate(currentAdminId, new String(pf.getPassword()));
            if (!ok) JOptionPane.showMessageDialog(this, "Incorrect password.", "Denied", JOptionPane.ERROR_MESSAGE);
            return ok;
        }
        return false;
    }

    public void refreshTable() {
        rowToIdMap.clear();
        List<Object[]> rows = new ArrayList<>();
        int r = 0;
        for (Map.Entry<Integer, UserAccount> entry : AuthManager.getAllUsers().entrySet()) {
            rows.add(new Object[]{ entry.getKey(), entry.getValue().getFullName(), entry.getValue().getRole() });
            rowToIdMap.put(r++, entry.getKey());
        }
        tableModel.setRows(rows);
    }

    private void clearForm() {
        idIn.setText(""); idIn.setEditable(true);
        nickIn.setText("");
        passIn.setText("");
        passIn.setEchoChar('\u2022');
        eyeBtn.setText("\u2299");
        adminCheck.setSelected(false); adminCheck.setEnabled(true);
        deleteBtn.setEnabled(true);
    }
}
