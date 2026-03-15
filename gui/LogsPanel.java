package gui;

import controller.LibraryManager;
import model.SystemLog;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the system audit log in a sortable table (sub-tab under Admin).
 */
public class LogsPanel extends JPanel {
    private final LibraryManager manager;
    private final VirtualTableModel model;

    public LogsPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("System Audit Log");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        String[] cols = {"Staff", "Timestamp", "Action", "Details"};
        model = new VirtualTableModel(cols);
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setToolTipText("Reload the audit log");
        refreshBtn.addActionListener(e -> refreshTable());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshTable();
    }

    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (SystemLog log : manager.getSystemLogs()) {
            rows.add(new Object[]{ log.getUserId(), log.getTimestamp(), log.getAction(), log.getDetails() });
        }
        model.setRows(rows);
    }
}
