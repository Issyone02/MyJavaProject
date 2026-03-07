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
        genBtn.setPreferredSize(new Dimension(0, 45));
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

        int bookUnits = 0, magUnits = 0, journalUnits = 0;
        StringBuilder overdueList = new StringBuilder();

        // 1. Calculate Physical Totals
        for (LibraryItem item : items) {
            totalPhysicalItems += item.getTotalCopies();
            String type = item.getType().toLowerCase();
            if (type.contains("book")) bookUnits += item.getTotalCopies();
            else if (type.contains("magazine")) magUnits += item.getTotalCopies();
            else if (type.contains("journal")) journalUnits += item.getTotalCopies();
        }

        // 2. Scan for Borrowed Units and Overdue items
        for (Student s : students) {
            totalUnitsBorrowed += s.getCurrentLoans().size();
            for (BorrowRecord record : s.getCurrentLoans()) {
                if (record != null && record.isOverdue()) {
                    overdueList.append("   [!] OVERDUE: ").append(record.getItem().getTitle())
                            .append("\n       Student: ").append(s.getName())
                            .append(" (").append(s.getStudentId()).append(")")
                            .append("\n       Due Date: ").append(record.getDueDate()).append("\n\n");
                }
            }
        }

        int totalUnitsAvailable = totalPhysicalItems - totalUnitsBorrowed;

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("        SLCAS SMART LIBRARY ANALYTICS REPORT        \n");
        sb.append("        Current Date: ").append(today).append("\n");
        sb.append("====================================================\n\n");

        sb.append("I. PHYSICAL INVENTORY OVERVIEW\n");
        sb.append(" --------------------------------------------------\n");
        sb.append(String.format(" %-35s %d\n", "Total Physical Units in System:", totalPhysicalItems));
        sb.append(String.format(" %-35s %d\n", "Unique Titles Registered:", items.size()));
        sb.append(String.format(" %-35s %d\n", "Active Borrowed Units:", totalUnitsBorrowed));
        sb.append(String.format(" %-35s %d\n", "Total Units Currently on Shelf:", totalUnitsAvailable));

        sb.append("\nII. CATEGORY BREAKDOWN (TOTAL UNITS)\n");
        sb.append(" --------------------------------------------------\n");
        sb.append(String.format(" %-35s %d\n", "Books:", bookUnits));
        sb.append(String.format(" %-35s %d\n", "Magazines:", magUnits));
        sb.append(String.format(" %-35s %d\n", "Journals:", journalUnits));

        sb.append("\nIII. WAITING LIST & OVERDUE MONITORING\n");
        sb.append(" --------------------------------------------------\n");
        sb.append(" Waitlist Active Entries: ").append(waitlist.size()).append("\n");
        if (!waitlist.isEmpty()) {
            for (String entry : waitlist) {
                // This displays students in Waitlist queue
                sb.append("   > ").append(entry).append("\n");
            }
        }
        sb.append("\nOverdue Status:\n");
        // If overdue item equals to zero, display the below
        if (overdueList.length() == 0) {
            sb.append("   - All loans are currently within their due dates.\n");
        } else {
            // Call out overdue list
            sb.append(overdueList.toString());
        }

        sb.append("\nIV. SYSTEM INTEGRITY VERIFICATION\n");
        sb.append(" --------------------------------------------------\n");
        int recursiveCount = countItemsRecursively(items, 0);
        sb.append(" Recursive Title Integrity Check: ").append(recursiveCount).append(" titles found.\n");
        sb.append("====================================================");

        reportArea.setText(sb.toString());
    }

    //Count the numbers of Items saved in the inventory
    private int countItemsRecursively(List<LibraryItem> items, int index) {
        if (index >= items.size()) return 0;
        return 1 + countItemsRecursively(items, index + 1);
    }
}