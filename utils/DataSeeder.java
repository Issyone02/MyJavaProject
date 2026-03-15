package utils;

import model.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Generates demo data on first startup when no data files exist.
 * Creates a realistic library with books, magazines, journals,
 * students with active loans, and staff accounts.
 *
 * The super admin account (ID 30114413, password "Ol@l3r3") is always
 * created by AuthManager — this class does NOT touch it.
 */
public class DataSeeder {

    // Generates demo data when the system has no usable records.
    // Always ensures demo staff accounts exist regardless.
    public static boolean seedIfEmpty() {
        seedStaffAccounts();

        // Check actual loaded data, not just file existence — old .dat files from
        // previous versions may exist but fail to deserialize (returning empty lists)
        List<LibraryItem> existingItems = FileHandler.loadData();
        List<model.UserAccount> existingStudents = FileHandler.loadStudents();

        if (!existingItems.isEmpty() && !existingStudents.isEmpty()) return false;

        List<LibraryItem> items    = createDemoItems();
        List<UserAccount> students = createDemoStudents(items);
        Queue<String>     waitlist = createDemoWaitlist(students, items);

        FileHandler.saveAll(items, students, waitlist);
        return true;
    }

    private static List<LibraryItem> createDemoItems() {
        List<LibraryItem> items = new ArrayList<>();

        // ── Books ────────────────────────────────────────────────────────────
        items.add(makeBook("BK001", "Introduction to Algorithms",       "Thomas Cormen",      2009, 5));
        items.add(makeBook("BK002", "Clean Code",                       "Robert C. Martin",   2008, 3));
        items.add(makeBook("BK003", "Design Patterns",                  "Erich Gamma",        1994, 2));
        items.add(makeBook("BK004", "The Pragmatic Programmer",         "David Thomas",       2019, 4));
        items.add(makeBook("BK005", "Data Structures and Abstractions", "Frank Carrano",      2018, 3));
        items.add(makeBook("BK006", "Artificial Intelligence: A Modern Approach", "Stuart Russell", 2020, 2));
        items.add(makeBook("BK007", "Computer Networking",              "James Kurose",       2021, 3));
        items.add(makeBook("BK008", "Operating System Concepts",        "Abraham Silberschatz", 2018, 2));
        items.add(makeBook("BK009", "Database System Concepts",         "Abraham Silberschatz", 2019, 3));
        items.add(makeBook("BK010", "Discrete Mathematics",             "Kenneth Rosen",      2018, 4));
        items.add(makeBook("BK011", "Java: The Complete Reference",     "Herbert Schildt",    2022, 5));
        items.add(makeBook("BK012", "Python Crash Course",              "Eric Matthes",       2023, 4));
        items.add(makeBook("BK013", "The Art of Computer Programming",  "Donald Knuth",       1997, 2));
        items.add(makeBook("BK014", "Structure and Interpretation of Computer Programs", "Harold Abelson", 1996, 2));
        items.add(makeBook("BK015", "Software Engineering",             "Ian Sommerville",    2015, 3));

        // ── Magazines ────────────────────────────────────────────────────────
        items.add(makeMagazine("MG001", "IEEE Spectrum",                "IEEE",               2024, 6));
        items.add(makeMagazine("MG002", "Communications of the ACM",    "ACM Press",          2024, 4));
        items.add(makeMagazine("MG003", "Wired Magazine",               "Conde Nast",         2024, 5));
        items.add(makeMagazine("MG004", "MIT Technology Review",        "MIT Press",          2023, 3));
        items.add(makeMagazine("MG005", "Scientific American",          "Springer Nature",    2024, 4));

        // ── Journals ─────────────────────────────────────────────────────────
        items.add(makeJournal("JN001", "Journal of Computer Science",          "Elsevier",     2023, 3));
        items.add(makeJournal("JN002", "ACM Computing Surveys",                "ACM Press",    2024, 2));
        items.add(makeJournal("JN003", "IEEE Transactions on Software Eng.",   "IEEE",         2023, 2));
        items.add(makeJournal("JN004", "Journal of Machine Learning Research", "JMLR Inc",     2024, 3));
        items.add(makeJournal("JN005", "Nature Computational Science",         "Springer",     2024, 2));

        return items;
    }

    private static List<UserAccount> createDemoStudents(List<LibraryItem> items) {
        List<UserAccount> students = new ArrayList<>();

        UserAccount s1 = new UserAccount("STU001", "Adaeze Okafor");
        UserAccount s2 = new UserAccount("STU002", "Chinedu Nwosu");
        UserAccount s3 = new UserAccount("STU003", "Fatima Ibrahim");
        UserAccount s4 = new UserAccount("STU004", "Oluwaseun Adeyemi");
        UserAccount s5 = new UserAccount("STU005", "Amina Mohammed");
        UserAccount s6 = new UserAccount("STU006", "Emeka Eze");
        UserAccount s7 = new UserAccount("STU007", "Ngozi Obi");
        UserAccount s8 = new UserAccount("STU008", "Yusuf Abdullahi");
        UserAccount s9 = new UserAccount("STU009", "Blessing Okoro");
        UserAccount s10 = new UserAccount("STU010", "Tunde Bakare");

        // Give some students active loans (borrowed items)
        // s1: has 2 books out, one will be overdue
        borrowForStudent(s1, items.get(0), LocalDate.now().minusDays(20)); // overdue (20 days ago > 14 day loan)
        borrowForStudent(s1, items.get(1), LocalDate.now().minusDays(5));  // on time

        // s2: has 1 magazine out
        borrowForStudent(s2, items.get(15), LocalDate.now().minusDays(3));

        // s3: has 1 journal, overdue
        borrowForStudent(s3, items.get(20), LocalDate.now().minusDays(18)); // overdue

        // s4: has 2 items
        borrowForStudent(s4, items.get(3), LocalDate.now().minusDays(7));
        borrowForStudent(s4, items.get(10), LocalDate.now().minusDays(2));

        // s5: has 1 book
        borrowForStudent(s5, items.get(6), LocalDate.now().minusDays(10));

        // s6-s10: no active loans (some have history)
        addReturnedRecord(s6, items.get(2), LocalDate.now().minusDays(30), LocalDate.now().minusDays(20));
        addReturnedRecord(s7, items.get(4), LocalDate.now().minusDays(25), LocalDate.now().minusDays(15));
        addReturnedRecord(s8, items.get(0), LocalDate.now().minusDays(40), LocalDate.now().minusDays(30));

        students.add(s1);
        students.add(s2);
        students.add(s3);
        students.add(s4);
        students.add(s5);
        students.add(s6);
        students.add(s7);
        students.add(s8);
        students.add(s9);
        students.add(s10);

        return students;
    }

    private static Queue<String> createDemoWaitlist(List<UserAccount> students, List<LibraryItem> items) {
        Queue<String> waitlist = new LinkedList<>();
        // A couple of students waiting for popular items
        waitlist.offer(students.get(5).getName() + " (" + students.get(5).getStudentId() + ") -> " + items.get(0).getTitle());
        waitlist.offer(students.get(7).getName() + " (" + students.get(7).getStudentId() + ") -> " + items.get(1).getTitle());
        waitlist.offer(students.get(9).getName() + " (" + students.get(9).getStudentId() + ") -> " + items.get(0).getTitle());
        return waitlist;
    }

    private static void seedStaffAccounts() {
        // Super admin (30114413 / Ol@l3r3) is created by AuthManager.loadUsers()
        // Add one librarian for demo
        if (!AuthManager.isUserExists(1001)) {
            AuthManager.addUser(1001, "Staff123", "Chioma Adekunle", false);
        }
        // Add one extra admin for demo
        if (!AuthManager.isUserExists(1002)) {
            AuthManager.addUser(1002, "Admin456", "Ibrahim Suleiman", true);
        }
    }

    // ── Helper methods to build items and loans ──────────────────────────────

    private static Book makeBook(String id, String title, String author, int year, int copies) {
        Book b = new Book(id, title, author, year);
        b.setTotalCopies(copies);
        b.setAvailableCopies(copies);
        return b;
    }

    private static Magazine makeMagazine(String id, String title, String author, int year, int copies) {
        Magazine m = new Magazine(id, title, author, year);
        m.setTotalCopies(copies);
        m.setAvailableCopies(copies);
        return m;
    }

    private static Journal makeJournal(String id, String title, String author, int year, int copies) {
        Journal j = new Journal(id, title, author, year);
        j.setTotalCopies(copies);
        j.setAvailableCopies(copies);
        return j;
    }

    // Creates an active loan — decrements available copies
    private static void borrowForStudent(UserAccount student, LibraryItem item, LocalDate borrowDate) {
        if (item instanceof Borrowable) {
            if (item.getAvailableCopies() > 0) {
                item.setAvailableCopies(item.getAvailableCopies() - 1);
                BorrowRecord record = new BorrowRecord(item, borrowDate);
                student.getCurrentLoans().add(record);
            }
        }
    }

    // Creates a completed loan in history (already returned)
    private static void addReturnedRecord(UserAccount student, LibraryItem item,
                                           LocalDate borrowDate, LocalDate returnDate) {
        BorrowRecord record = new BorrowRecord(item, borrowDate);
        record.setReturnDate(returnDate);
        student.getHistory().add(record);
    }
}
