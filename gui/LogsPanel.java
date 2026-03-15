package gui;

import controller.LibraryController;
import model.SystemLog;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Displays the system audit log. Admin-only sub-tab under Admin panel. */
public class LogsPanel extends JPanel {

    private final LibraryController controller;
    private final VirtualTableModel model;

    /** Creates system audit log panel with controller and initializes UI components. */
    public LogsPanel(LibraryController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("System Audit Log");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        model = new VirtualTableModel(new String[]{"Staff", "Timestamp", "Action", "Details"});
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setToolTipText("Reload the audit log");
        refreshBtn.addActionListener(e -> refreshTable());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title,      BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        add(header,                 BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshTable();
    }

    /** Refreshes the table with current system logs from controller. */
    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (SystemLog log : controller.getSystemLogs())
            rows.add(new Object[]{log.getUserId(), log.getTimestamp(), log.getAction(), log.getDetails()});
        model.setRows(rows);
    }
}
