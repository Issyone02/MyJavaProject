package gui;

import controller.LibraryController;
import utils.GuiUtils;


import model.UserAccount;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Staff account management panel (Admin only sub-tab). */
public class StaffManagementPanel extends JPanel {

    private final VirtualTableModel tableModel;
    private final JTable            table;
    private final JTextField        idIn, nickIn;
    private final JPasswordField    passIn;
    private final JCheckBox         adminCheck;
    private       JButton           eyeBtn;
    private final JButton           saveBtn, deleteBtn, resetPassBtn, clearBtn;
    private final int               currentAdminId;
    private final LibraryController controller;

    // Maps visible table row index → integer staff ID so clicks resolve to the right account
    private final java.util.HashMap<Integer, Integer> rowToIdMap = new java.util.HashMap<>();

    public StaffManagementPanel(LibraryController controller, int currentAdminId) {
        this.controller     = controller;
        this.currentAdminId = currentAdminId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"Staff ID", "Display Name", "Role"};
        tableModel = new VirtualTableModel(cols);
        table      = new JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // ── Add/Edit form (GridBagLayout) ─────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Add / Edit Staff"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        idIn       = new JTextField();
        nickIn     = new JTextField();
        passIn     = new JPasswordField();
        adminCheck = new JCheckBox("Administrator");

        // Eye button toggles password visibility
        eyeBtn = new JButton("\u2299");
        eyeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        eyeBtn.setFocusPainted(false); eyeBtn.setBorderPainted(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeBtn.addActionListener(e -> {
            boolean visible = passIn.getEchoChar() == (char) 0;
            passIn.setEchoChar(visible ? '\u2022' : (char) 0);
            eyeBtn.setText(visible ? "\u2299" : "\u2297");
        });

        JPanel passWrapper = new JPanel(new BorderLayout());
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

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        saveBtn      = new JButton("Save");
        deleteBtn    = new JButton("Delete");
        resetPassBtn = new JButton("Reset Password");
        clearBtn     = new JButton("Clear Form");
        saveBtn.setToolTipText("Save new or updated staff record");
        deleteBtn.setToolTipText("Delete the selected staff member");
        resetPassBtn.setToolTipText("Reset password for selected staff");
        clearBtn.setToolTipText("Clear the form fields");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true); deleteBtn.setBorderPainted(false);
        btnPanel.add(saveBtn); btnPanel.add(deleteBtn);
        btnPanel.add(resetPassBtn); btnPanel.add(clearBtn);

        gbc.gridx = 0; gbc.gridy = fields.length; gbc.gridwidth = 2;
        form.add(btnPanel, gbc);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(form, BorderLayout.CENTER);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // Clicking a row loads the staff member's details into the form for editing
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row == -1) return;
                int id = rowToIdMap.get(row);

                UserAccount u = controller.getAllStaff().get(id);
                if (u == null) return;

                idIn.setText(String.valueOf(id));
                idIn.setEditable(false);
                nickIn.setText(u.getFullName());
                passIn.setText(""); passIn.setEchoChar('\u2022');
                eyeBtn.setText("\u2299");
                adminCheck.setSelected(u.isAdmin());

                // Super admin logic: prevent role change, deletion, and password reset
                boolean isSuperAdmin = (id == controller.getSuperAdminId());
                adminCheck.setEnabled(!isSuperAdmin);
                deleteBtn.setEnabled(!isSuperAdmin);
                resetPassBtn.setEnabled(!isSuperAdmin); // Greys out reset button for System Owner
            }
        });

        // ── Save ──────────────────────────────────────────────────────────────
        saveBtn.addActionListener(e -> {
            try {
                String idStr    = idIn.getText().trim();
                String fullName = nickIn.getText().trim();
                String rawPass  = new String(passIn.getPassword()).trim();

                if (idStr.isEmpty() || fullName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Staff ID and Display Name are required.");
                    return;
                }
                int     staffId = Integer.parseInt(idStr);
                boolean isNew   = !controller.staffExists(staffId);
                if (isNew && rawPass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Password is required for new staff.");
                    return;
                }
                if (!confirmAdmin()) return;

                boolean isAdm = (staffId == controller.getSuperAdminId()) || adminCheck.isSelected();
                controller.addOrUpdateStaff(
                        String.valueOf(currentAdminId), staffId,
                        rawPass.isEmpty() ? null : rawPass, fullName, isAdm);
                refreshTable();
                clearForm();
                JOptionPane.showMessageDialog(this, "Staff record saved.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Staff ID must be numeric.");
            }
        });

        // ── Delete ────────────────────────────────────────────────────────────
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            int staffId = rowToIdMap.get(row);
            if (staffId == controller.getSuperAdminId()) {
                JOptionPane.showMessageDialog(this, "The super admin cannot be deleted.");
                return;
            }
            if (!confirmAdmin()) return;
            if (JOptionPane.showConfirmDialog(this, "Delete this staff member?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                controller.removeStaff(String.valueOf(currentAdminId), staffId);
                refreshTable();
                clearForm();
            }
        });

        // ── Reset password ────────────────────────────────────────────────────
        resetPassBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            int    targetId = rowToIdMap.get(row);

            // Extra security check in logic
            if (targetId == controller.getSuperAdminId()) {
                JOptionPane.showMessageDialog(this, "Cannot reset System Owner password.");
                return;
            }

            String newPass  = JOptionPane.showInputDialog(this, "Enter new password:");
            if (newPass != null && !newPass.trim().isEmpty() && confirmAdmin()) {
                controller.resetStaffPassword(
                        String.valueOf(currentAdminId), targetId, newPass.trim());
                JOptionPane.showMessageDialog(this, "Password reset Successful!");
            }
        });

        clearBtn.addActionListener(e -> clearForm());
        refreshTable();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean confirmAdmin() {
        return GuiUtils.confirmPassword(this, controller, currentAdminId, "Enter your admin password:");
    }

    /**
     * Reloads the staff list and rebuilds rowToIdMap so that clicking a row
     * still maps to the correct account after any add or delete operation.
     */
    public void refreshTable() {
        rowToIdMap.clear();
        List<Object[]> rows = new ArrayList<>();
        int r = 0;
        for (Map.Entry<Integer, UserAccount> entry : controller.getAllStaff().entrySet()) {
            rows.add(new Object[]{
                    entry.getKey(), entry.getValue().getFullName(), entry.getValue().getRole()
            });
            rowToIdMap.put(r++, entry.getKey());
        }
        tableModel.setRows(rows);
    }

    private void clearForm() {
        idIn.setText(""); idIn.setEditable(true);
        nickIn.setText("");
        passIn.setText(""); passIn.setEchoChar('\u2022');
        eyeBtn.setText("\u2299");
        adminCheck.setSelected(false); adminCheck.setEnabled(true);
        deleteBtn.setEnabled(true);
        resetPassBtn.setEnabled(true); // Re-enable on clear
    }
}