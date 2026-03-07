package gui;

import model.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JPanel statsCardPanel;

    private final Color colorBook = new Color(255, 183, 178);
    private final Color colorMagazine = new Color(181, 234, 215);
    private final Color colorJournal = new Color(173, 203, 227);
    private final Color colorBorrowed = new Color(160, 210, 255);
    private final Color colorWaitlist = new Color(255, 224, 130);

    private int bookCount, magCount, journalCount, borrowedCount, waitlistCount, totalTitles;

    public DashboardPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Library Analytics Dashboard", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        statsCardPanel = new JPanel(new GridLayout(1, 6, 15, 15));
        statsCardPanel.setOpaque(false);

        ChartPanel chartPanel = new ChartPanel();
        add(chartPanel, BorderLayout.CENTER);
        add(statsCardPanel, BorderLayout.SOUTH);

        refreshDashboard();
    }

    public void refreshDashboard() {
        bookCount = 0;
        magCount = 0;
        journalCount = 0;
        borrowedCount = 0;

        List<LibraryItem> inventory = manager.getInventory();
        List<Student> students = manager.getStudents();
        List<String> waitlist = manager.getWaitlist();

        totalTitles = (inventory != null) ? inventory.size() : 0;
        waitlistCount = (waitlist != null) ? waitlist.size() : 0;

        if (inventory != null) {
            for (LibraryItem item : inventory) {
                String type = item.getType().toLowerCase();
                if (type.contains("book")) bookCount++;
                else if (type.contains("magazine")) magCount++;
                else if (type.contains("journal")) journalCount++;
            }
        }

        if (students != null) {
            for (Student s : students) {
                borrowedCount += s.getCurrentLoans().size();
            }
        }

        statsCardPanel.removeAll();
        statsCardPanel.add(createStatCard("Total Titles", String.valueOf(totalTitles), Color.WHITE, null));
        statsCardPanel.add(createStatCard("Books", String.valueOf(bookCount), colorBook, null));
        statsCardPanel.add(createStatCard("Magazines", String.valueOf(magCount), colorMagazine, null));
        statsCardPanel.add(createStatCard("Journals", String.valueOf(journalCount), colorJournal, null));
        statsCardPanel.add(createStatCard("Borrowed Units", String.valueOf(borrowedCount), colorBorrowed, e -> showBorrowedList()));
        statsCardPanel.add(createStatCard("Waitlist", String.valueOf(waitlistCount), colorWaitlist, e -> showWaitlistDetails()));

        revalidate();
        repaint();
    }

    private JPanel createStatCard(String title, String value, Color bg, java.awt.event.ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        if (action != null) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { action.actionPerformed(null); }
            });
        }

        JLabel tLbl = new JLabel(title, JLabel.CENTER);
        tLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel vLbl = new JLabel(value, JLabel.CENTER);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));

        card.add(tLbl, BorderLayout.NORTH);
        card.add(vLbl, BorderLayout.CENTER);

        return card;
    }

    private void showWaitlistDetails() {
        List<String> waitlist = manager.getWaitlist();
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Waitlist Management", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // MODIFIED: Added dynamic position ranking (#1, #2...)
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < waitlist.size(); i++) {
            listModel.addElement(String.format("#%d - %s", (i + 1), waitlist.get(i)));
        }

        JList<String> jList = new JList<>(listModel);


        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton moveUpBtn = new JButton("↑ Move Up");
        JButton moveDownBtn = new JButton("↓ Move Down");
        JButton removeBtn = new JButton("Remove Selected");
        JButton clearBtn = new JButton("Clear All");
        clearBtn.setForeground(Color.RED);

        Runnable syncAll = () -> {
            Window parent = SwingUtilities.getWindowAncestor(this);
            if (parent instanceof MainWindow) {
                ((MainWindow) parent).refreshAllPanels();
            }
            dialog.dispose();
            showWaitlistDetails();
        };

        moveUpBtn.addActionListener(e -> {
            int idx = jList.getSelectedIndex();
            if (idx > 0) {
                manager.moveWaitlistEntry(String.valueOf(currentUserId), idx, true);
                syncAll.run();
            }
        });

        moveDownBtn.addActionListener(e -> {
            int idx = jList.getSelectedIndex();
            if (idx != -1 && idx < waitlist.size() - 1) {
                manager.moveWaitlistEntry(String.valueOf(currentUserId), idx, false);
                syncAll.run();
            }
        });

        removeBtn.addActionListener(e -> {
            int idx = jList.getSelectedIndex();
            if (idx != -1) {
                manager.removeWaitlistEntry(String.valueOf(currentUserId), idx);
                syncAll.run();
            }
        });

        clearBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dialog, "Clear all waitlist entries?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                manager.clearWaitlist(String.valueOf(currentUserId));
                syncAll.run();
            }
        });

        btnPanel.add(moveUpBtn);
        btnPanel.add(moveDownBtn);
        btnPanel.add(removeBtn);
        btnPanel.add(clearBtn);

        JLabel headerLabel = new JLabel("  Current Queue Rank (Priority at #1):");
        headerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));

        dialog.add(headerLabel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(jList), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setSize(600, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showBorrowedList() {
        StringBuilder sb = new StringBuilder("SLCAS SMART LIBRARY - ACTIVE BORROW REPORT\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("==================================================\n\n");

        for (Student s : manager.getStudents()) {
            if (!s.getCurrentLoans().isEmpty()) {
                sb.append("STUDENT: ").append(s.getName()).append(" (ID: ").append(s.getStudentId()).append(")\n");
                for (BorrowRecord br : s.getCurrentLoans()) {
                    sb.append("   • [").append(br.getItem().getType().toUpperCase()).append("] ")
                            .append(br.getItem().getTitle()).append(" (Borrowed: ").append(br.getBorrowDate()).append(")\n");
                }
                sb.append("--------------------------------------------------\n");
            }
        }
        showScrollablePopup("Active Borrowed Items", sb.toString(), "Active_Borrows");
    }

    private void showScrollablePopup(String title, String content, String filePrefix) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setLayout(new BorderLayout(10, 10));
        JTextArea textArea = new JTextArea(20, 50);
        textArea.setText(content);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportBtn = new JButton("💾 Save as TXT");
        exportBtn.addActionListener(e -> {
            try (FileWriter writer = new FileWriter(filePrefix + "_Report.txt")) {
                writer.write(content);
                JOptionPane.showMessageDialog(dialog, "Report exported.");
            } catch (IOException ex) { JOptionPane.showMessageDialog(dialog, "Export failed."); }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(exportBtn);
        btnPanel.add(closeBtn);
        dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private class ChartPanel extends JPanel {
        public ChartPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 100;
            if (size <= 0) return;

            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            if (totalTitles == 0) {
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawOval(x, y, size, size);
                g2.drawString("No Data Available", x + size/2 - 40, y + size/2);
                return;
            }

            double startAngle = 90;
            startAngle = drawSlice(g2, x, y, size, startAngle, bookCount, colorBook);
            startAngle = drawSlice(g2, x, y, size, startAngle, magCount, colorMagazine);
            drawSlice(g2, x, y, size, startAngle, journalCount, colorJournal);

            g2.setColor(getBackground());
            int holeSize = (int)(size * 0.7);
            g2.fillOval(x + (size - holeSize)/2, y + (size - holeSize)/2, holeSize, holeSize);

            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            String totalStr = "Total Titles: " + totalTitles;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalStr, x + (size - fm.stringWidth(totalStr))/2, y + size/2 + 5);
        }

        private double drawSlice(Graphics2D g2, int x, int y, int s, double start, int count, Color c) {
            if (count == 0) return start;
            double extent = -( (count / (double) totalTitles) * 360.0 );
            g2.setColor(c);
            g2.fillArc(x, y, s, s, (int)start, (int)Math.floor(extent));
            return start + extent;
        }
    }
}