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
    private final JPanel statsCardPanel;

    private final Color colorBook = new Color(255, 183, 178);
    private final Color colorMagazine = new Color(181, 234, 215);
    private final Color colorJournal = new Color(173, 203, 227);
    private final Color colorBorrowed = new Color(160, 210, 255);
    private final Color colorWaitlist = new Color(255, 224, 130);

    private int bookCount, magCount, journalCount, borrowedCount, waitlistCount, totalTitles;

    public DashboardPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        // --- TOP PANEL (Title Only - Undo Removed) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Library Analytics Dashboard", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        statsCardPanel = new JPanel(new GridLayout(1, 6, 10, 10));
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
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));
        card.setCursor(action != null ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));

        JLabel tLbl = new JLabel(title, JLabel.CENTER);
        JLabel vLbl = new JLabel(value, JLabel.CENTER);
        vLbl.setFont(new Font("SansSerif", Font.BOLD, 26));

        card.add(tLbl, BorderLayout.NORTH);
        card.add(vLbl, BorderLayout.CENTER);

        if (action != null) {
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { action.actionPerformed(null); }
            });
        }
        return card;
    }

    private void showBorrowedList() {
        StringBuilder sb = new StringBuilder("MIVA SLCAS - ACTIVE BORROW REPORT\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("--------------------------------------------------\n\n");

        boolean found = false;
        for (Student s : manager.getStudents()) {
            if (!s.getCurrentLoans().isEmpty()) {
                found = true;
                sb.append("STUDENT: ").append(s.getName()).append(" (ID: ").append(s.getStudentId()).append(")\n");
                for (BorrowRecord br : s.getCurrentLoans()) {
                    sb.append("   > [").append(br.getItem().getType().toUpperCase()).append("] ").append(br.getItem().getTitle()).append("\n");
                }
                sb.append("\n");
            }
        }
        showScrollablePopup("Active Borrow Report", found ? sb.toString() : "No active borrows.", "Borrow_Report");
    }

    private void showWaitlistDetails() {
        List<String> waitlist = manager.getWaitlist();
        if (waitlist == null || waitlist.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The Waitlist is currently empty.");
            return;
        }

        StringBuilder sb = new StringBuilder("MIVA SLCAS - WAITLIST QUEUE\n");
        for (int i = 0; i < waitlist.size(); i++) {
            sb.append(String.format("[%d] %s\n", (i + 1), waitlist.get(i)));
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Waitlist Management", true);
        dialog.setLayout(new BorderLayout(10, 10));
        JTextArea textArea = new JTextArea(18, 45);
        textArea.setText(sb.toString());
        textArea.setEditable(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearBtn = new JButton("🗑️ Clear Waitlist");
        clearBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(dialog, "Do you want to clear entire Waitlist?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                manager.clearWaitlist();
                dialog.dispose();
                refreshDashboard();
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(ex -> dialog.dispose());

        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showScrollablePopup(String title, String content, String filePrefix) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setLayout(new BorderLayout(10, 10));
        JTextArea textArea = new JTextArea(18, 45);
        textArea.setText(content);
        textArea.setEditable(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton printBtn = new JButton("🖨️ Print ");
        printBtn.addActionListener(e -> {
            try (FileWriter writer = new FileWriter(filePrefix + ".txt")) {
                writer.write(content);
                JOptionPane.showMessageDialog(dialog, "Saved to " + filePrefix + ".txt");
            } catch (IOException ex) { JOptionPane.showMessageDialog(dialog, "Save failed."); }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(printBtn);
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
            if (totalTitles == 0) return;
            double start = 0;
            start = drawSlice(g2, x, y, size, start, bookCount, colorBook);
            start = drawSlice(g2, x, y, size, start, magCount, colorMagazine);
            drawSlice(g2, x, y, size, start, journalCount, colorJournal);
            g2.setColor(getBackground());
            int hole = (int)(size * 0.6);
            g2.fillOval(x + (size - hole)/2, y + (size - hole)/2, hole, hole);
        }
        private double drawSlice(Graphics2D g2, int x, int y, int s, double start, int count, Color c) {
            if (count == 0) return start;
            double angle = (count / (double) totalTitles) * 360;
            g2.setColor(c);
            g2.fillArc(x, y, s, s, (int)start, (int)Math.ceil(angle));
            return start + angle;
        }
    }
}