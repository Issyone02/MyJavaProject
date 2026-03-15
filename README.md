# SLCAS v8 — Smart Library Circulation & Automation System
## MIVA Open University

### Project Structure
```
/model       — LibraryItem, Book, Magazine, Journal, Borrowable, UserAccount,
               Admin, Librarian, Student, BorrowRecord, LibraryDatabase, SystemLog
/controller  — LibraryManager, SearchEngine, BorrowController
/gui         — MainWindow, ViewPanel, BorrowPanel, AdminPanel, StudentPanel,
               SearchSortPanel, StaffManagementPanel, LogsPanel, DashboardPanel,
               LoginDialog, VirtualTableModel
/utils       — FileHandler, IDGenerator, AuthManager, LibraryConfig, DataSeeder
/main        — LibraryApp
```

### How to Run
```bash
javac -d out model/*.java controller/*.java gui/*.java utils/*.java main/*.java
java -cp out main.LibraryApp
```

### Login Accounts
| Role      | ID        | Password  |
|-----------|-----------|-----------|
| Admin     | 30114413  | Ol@l3r3   |
| Admin     | 1002      | Admin456  |
| Librarian | 1001      | Staff123  |

### Demo Data
Auto-generated on first run: 25 items, 10 students, 3 waitlist entries.
Demo staff accounts (1001, 1002) are created automatically on every startup if missing.

### Troubleshooting
If login fails or data appears empty, delete all `.dat` files in the project
directory and restart. Old `.dat` files from previous versions are incompatible:
```bash
rm -f inventory.dat students.dat waitlist.dat system_users.dat
```

### Tab Layout
1. **View Items** — Catalogue table + Reports button (opens analytics dialog with export)
2. **Borrow/Return** — 3 views: Catalogue, Active Loans, Waitlist (CardLayout)
3. **Admin** — 4 sub-tabs: Inventory, Students, Staff Management, Logs
4. **Search & Sort** — Click column header for field, dropdown for algorithm

### File Menu
Save Data, Export Data (text/backup), Import Backup, Logout, Exit

### Requirements Coverage
- OOP: abstract LibraryItem (Book, Magazine, Journal), Borrowable interface, polymorphism
  Abstract UserAccount with borrowing history → Student, Admin, Librarian
  LibraryDatabase composition (ArrayList, Queue, Stack, Array cache)
- Data structures: ArrayList, Queue (waitlist), Stack (undo/redo), Array (cache)
- Sorting: Insertion Sort, Merge Sort, Quick Sort (selectable via dropdown)
- Searching: Linear, Binary (any sorted field), Recursive (auto-selected)
- Recursion: recursive search, merge sort, quick sort, recursive item count
- GUI: tabbed panels, CardLayout, GridBagLayout, BoxLayout, BorderLayout
- Events: button clicks, menu selections, text field updates, timer-triggered overdue
- Advanced: custom renderers, file chooser, timers, input validation, keyboard shortcuts, tooltips
- Persistence: binary serialisation for all data, text export for reports
- Reports: most borrowed, overdue, category distribution (with donut chart)
