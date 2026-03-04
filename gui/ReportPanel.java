package gui;

import model.*;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class ReportPanel extends JPanel {
    private final LibraryManager manager;
    private final JTextArea reportArea;

    public ReportPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        reportArea.setBackground(new Color(245, 245, 245));

        JButton genBtn = new JButton("Generate Analytics Report");
        genBtn.setFont(new Font("Arial", Font.BOLD, 14));
        genBtn.addActionListener(e -> generateReport());

        add(new JScrollPane(reportArea), BorderLayout.CENTER);
        add(genBtn, BorderLayout.SOUTH);
    }

    private void generateReport() {
        List<LibraryItem> items = manager.getInventory();
        List<Student> students = manager.getStudents();
        List<String> waitlist = manager.getWaitlist();
        LocalDate today = LocalDate.now();

        int totalPhysicalItems = 0;
        int totalUnitsBorrowed = 0;

        // Type-specific counts
        int bookUnits = 0, magUnits = 0, journalUnits = 0;
        StringBuilder overdueList = new StringBuilder();

        // 1. Calculate Physical Totals and Category Totals
        for (LibraryItem item : items) {
            totalPhysicalItems += item.getTotalCopies();

            String type = item.getType().toLowerCase();
            if (type.contains("book")) bookUnits += item.getTotalCopies();
            else if (type.contains("magazine")) magUnits += item.getTotalCopies();
            else if (type.contains("journal")) journalUnits += item.getTotalCopies();
        }

        // 2. NEW LOGIC: Calculate "Units on Loan" by scanning Students (Source of Truth)
        for (Student s : students) {
            totalUnitsBorrowed += s.getCurrentLoans().size();

            for (BorrowRecord record : s.getCurrentLoans()) {
                if (record != null && record.isOverdue()) {
                    overdueList.append("   - [!] OVERDUE: ").append(record.getItem().getTitle())
                            .append("\n        Borrower: ").append(s.getName())
                            .append(" (").append(s.getStudentId()).append(")")
                            .append("\n        Due Date: ").append(record.getDueDate()).append("\n\n");
                }
            }
        }

        // 3. Derived Value
        int totalUnitsAvailable = totalPhysicalItems - totalUnitsBorrowed;

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("        SLCAS SMART LIBRARY ANALYTICS REPORT        \n");
        sb.append("        Generated On: ").append(today).append("\n");
        sb.append("====================================================\n\n");

        sb.append("I. PHYSICAL INVENTORY QUANTITIES:\n");
        sb.append(" --------------------------------------------------\n");
        sb.append(" - Total Physical Units in System:    ").append(totalPhysicalItems).append("\n");
        sb.append(" - Total Unique Titles Registered:    ").append(items.size()).append("\n");
        sb.append(" - Current Units on Borrow:             ").append(totalUnitsBorrowed).append("\n");
        sb.append(" - Current Units on Shelf:            ").append(totalUnitsAvailable).append("\n");

        sb.append("\nII. QUANTITY BREAKDOWN BY CATEGORY:\n");
        sb.append(" --------------------------------------------------\n");
        sb.append(" - Books (Total Units):               ").append(bookUnits).append("\n");
        sb.append(" - Magazines (Total Units):           ").append(magUnits).append("\n");
        sb.append(" - Journals (Total Units):            ").append(journalUnits).append("\n");

        sb.append("\nIII. OVERDUE & WAITING LIST:\n");
        sb.append(" --------------------------------------------------\n");
        if (overdueList.length() == 0) {
            sb.append(" - Status: All borrowed items are within due dates.\n");
        } else {
            sb.append(overdueList.toString());
        }

        sb.append("\nWaitlist Entries: ").append(waitlist.size()).append("\n");
        if (!waitlist.isEmpty()) {
            for (String entry : waitlist) {
                sb.append("   > ").append(entry).append("\n");
            }
        }

        sb.append("\nIV. SYSTEM VERIFICATION (RECURSIVE):\n");
        sb.append(" --------------------------------------------------\n");
        int recursiveCount = countItemsRecursively(items, 0);
        sb.append(" Recursive Title Count: ").append(recursiveCount).append("\n");
        sb.append("====================================================");

        reportArea.setText(sb.toString());
    }

    private int countItemsRecursively(List<LibraryItem> items, int index) {
        if (index >= items.size()) return 0;
        return 1 + countItemsRecursively(items, index + 1);
    }
}