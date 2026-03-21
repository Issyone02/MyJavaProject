# SLCAS v8 — Smart Library Circulation & Automation System
## MIVA Open University

A comprehensive Java desktop application for library management featuring MVC architecture, advanced search/sort algorithms, undo/redo functionality, and real-time analytics.

---

## Project Structure

```
/model       — Data models and business entities
  ├── LibraryItem (abstract)     — Base class for all library items
  ├── Book, Magazine, Journal    — Concrete item types
  ├── Borrowable (interface)      — Checkout/return contract
  ├── UserAccount                 — User account with role-based access and SHA-256 auth
  ├── BorrowRecord                — Individual loan tracking
  ├── LibraryDatabase            — Central data store with cache
  ├── SystemLog                  — Audit trail entries
  └── WaitlistEntry, LoanView,   — DTOs for UI display
      StudentSummary, BorrowSummary, OverdueLoanView

/controller  — Business logic and coordination
  ├── LibraryManager            — Main controller implementing LibraryController
  ├── LibraryController (interface) — API contract for GUI
  ├── LibraryChangeListener (interface) — Observer pattern for UI updates
  ├── BorrowController          — Borrow/return workflow handler
  ├── SearchEngine              — Linear and binary search with auto-selection
  ├── SortEngine                — Insertion Sort, Merge Sort, Quick Sort
  └── AuthController (interface) — Authentication contract

/gui         — Swing-based user interface
  ├── MainWindow                — Primary application frame
  ├── ViewPanel                 — Catalogue browser + Reports
  ├── BorrowPanel               — Borrow/return with 3-card layout
  ├── AdminPanel                — Inventory & user management
  ├── StudentPanel              — Student account management
  ├── SearchSortPanel          — Advanced search interface
  ├── StaffManagementPanel     — Staff administration
  ├── LogsPanel                — System audit log viewer
  ├── LoginDialog              — Secure authentication
  └── VirtualTableModel        — Lazy-loading table data model

/utils       — Utility classes and helpers
  ├── FileHandler              — Binary serialization & export
  ├── IDGenerator              — Unique ID generation
  ├── AuthManager              — SHA-256 password hashing & credential management
  ├── GuiUtils                 — GUI helper methods and dialog utilities
  └── DataSeeder               — Demo data generation

/main        — Application entry point
  └── LibraryApp               — Main class with startup logic
```

---

## How to Run

### Compile and Run
```bash
javac -d out -cp flatlaf-3.6.jar model/*.java controller/*.java gui/*.java utils/*.java main/*.java
java -cp out:flatlaf-3.6.jar main.LibraryApp
```

> On Windows, use semicolons instead of colons: `-cp out;flatlaf-3.6.jar`

### Or using an IDE
Import as a standard Java project, add `flatlaf-3.6.jar` as a library dependency, and run `main.LibraryApp`

---

## Login Accounts

| Role      | ID       | Password  | Permissions |
|-----------|----------|-----------|-------------|
| Admin     | 30114413 | Ol@l3r3   | Full access: inventory, students, staff management, logs |
| Admin     | 1002     | Admin456  | Full access: inventory, students, staff management, logs |
| Librarian | 1001     | Staff123  | Limited access: borrow/return, catalogue view, search |

**Security Features:**
- SHA-256 password hashing with Base64 encoding
- Failed login attempt logging
- Password confirmation required for sensitive operations (delete, edit)
- Role-based UI visibility (admin-only features hidden from librarians)

---

## Demo Data

Auto-generated on first run:
- **25 items** — Books, Magazines, Journals with varied availability
- **10 students** — Sample student accounts with realistic names
- **3 waitlist entries** — Pre-populated waitlist for testing
- **Demo staff accounts** — Created automatically if missing (1001, 1002)

---

## Troubleshooting

If login fails or data appears empty, delete all `.dat` files in the project directory and restart:

```bash
rm -f inventory.dat students.dat waitlist.dat system_users.dat
```

Old `.dat` files from previous versions may be incompatible due to serialization changes.

---

## Application Interface

### Tab Layout

1. **View Items** — Catalogue table with Reports button
   - Reports dialog includes: category distribution table, most borrowed items, current overdue loans
   - Inventory summary cards: Total Catalogue, Books, Magazines, Journals, Borrowed, Waitlist
   - Donut chart visualization of catalogue composition
   - Export report to text file

2. **Borrow/Return** — Three-card layout:
   - **Catalogue** — Browse and borrow/return items
   - **Active Loans** — View all current loans with status
   - **Waitlist** — Manage reservation queue

3. **Admin** — Four sub-tabs:
   - **Inventory Management** — Add, edit, delete items (admin-only edit/delete)
   - **Student Records** — Manage student accounts
   - **Staff Management** — Add/remove staff accounts (admin-only)
   - **Logs** — System audit trail (admin-only)

4. **Search & Sort** — Advanced filtering:
   - Click column header to select the search/sort field
   - Dropdown to choose sort algorithm: Insertion Sort, Merge Sort, Quick Sort
   - Search auto-selects Linear or Binary based on whether data is currently sorted

### Menu Bar

**File**
- **Save Data** (Ctrl+S) — Manual persistence trigger
- **Export Data** — Text report or binary backup (password required)
- **Import Backup** — Restore from binary backup (password required)
- **Exit** (Ctrl+Q) — Saves and closes

**Edit**
- **Undo** (Ctrl+Z) — Reverts last operation
- **Redo** (Ctrl+Y) — Re-applies undone operation

**View**
- Ctrl+1–4 — Jump to View Items, Borrow/Return, Admin, Search & Sort tabs

**Help / About**
- User Manual dialog with keyboard shortcut reference
- About dialog listing the development team

### Top Bar
- **Global search field** — Searches across all tabs in real time; auto-selects Linear or Binary search and shows the algorithm used in the status bar
- **Logout button** — Ends session and returns to login dialog

### Status Bar
- Greeting with current user's name (Good Morning / Afternoon / Evening)
- Overdue indicator — shows count; click to open a colour-coded overdue items popup
- Current date

---

## Key Features

### Undo/Redo System
- **Deep copy state management** — Complete snapshot before every operation
- **Stack-based history** — Undo/redo using Java `Stack<LibraryState>`, capped at 50 saved states
- **Atomic operations** — Borrow + waitlist removal handled as single undoable action

### Frequency Cache
- **Most-accessed items tracking** — Array-based fixed-size cache (10 items)
- **LFU eviction policy** — When cache is full, the item with the lowest access count is replaced
- **Insertion sort ordering** — Cache re-sorted after every access to keep highest-count item at index 0

### Search & Sort Algorithms
- **Linear Search** — Used when data is unsorted; O(n)
- **Binary Search** — Auto-selected when data is sorted; O(log n) with adjacent-match expansion
- **Insertion Sort** — User-selectable; efficient for small datasets
- **Merge Sort** — User-selectable; guaranteed O(n log n), stable
- **Quick Sort** — User-selectable; in-place with median-of-three pivot, O(n log n) average

### Data Persistence
- **Binary serialization** — Fast save/load using Java Object Serialization
- **SHA-256 password hashing** — Secure credential storage
- **Automatic backup** — State saved before every operation for undo support

---

## Requirements Coverage

### Object-Oriented Programming
- **Abstraction** — `LibraryItem` abstract base class with `Book`, `Magazine`, `Journal`
- **Interface** — `Borrowable` defines checkout/return contract; `LibraryController`, `AuthController`, `LibraryChangeListener` define system contracts
- **Polymorphism** — `LibraryController` interface with `LibraryManager` implementation; items resolved via `LibraryItem` reference
- **Inheritance** — `Book`, `Magazine`, `Journal` extend abstract `LibraryItem`
- **Encapsulation** — Private fields with validated setters

### Data Structures
- **ArrayList** — Catalogue storage, student lists, loan records
- **Queue (LinkedList)** — Waitlist FIFO management
- **Stack** — Undo/redo history (`Stack<LibraryState>`)
- **Array** — Fixed-size frequency cache (10 items)

### Algorithms
- **Sorting:** Insertion Sort, Merge Sort, Quick Sort (selectable via dropdown)
- **Searching:** Linear (unsorted data), Binary (auto-selected when data is sorted)
- **Recursion:** Merge sort (recursive divide-and-conquer), quick sort (recursive partitioning), `countItemsByCategoryRecursively()` in ViewPanel (recursive list traversal for Reports)

### GUI Implementation
- **Layout Managers:** CardLayout (BorrowPanel), GridBagLayout (forms), BorderLayout (panels), FlowLayout (toolbars)
- **Components:** JTable with custom renderers, JTabbedPane, JDialog, JMenuBar
- **Events:** ActionListener, KeyListener (Enter key shortcuts), WindowListener
- **Advanced:** Custom cell renderers (color-coded status), tooltips, keyboard mnemonics

### Advanced Features
- **Timers** — Periodic overdue status updates
- **File Chooser** — Export/import dialogs
- **Input Validation** — Real-time field validation with error dialogs
- **Custom Renderers** — Color-coded table rows (overdue=red, available=green, unavailable=amber)
- **Keyboard Shortcuts** — Alt+B (borrow), Alt+R (return), Ctrl+S (save), Ctrl+Q (exit), Ctrl+Z/Y (undo/redo), Ctrl+1–4 (switch tabs)

### Persistence & Reporting
- **Binary Serialization** — All data persisted via Java Object Serialization
- **Text Export** — Reports exported to .txt files
- **Backup/Restore** — Full system state import/export
- **Audit Logging** — All operations logged with timestamp, user, and details

---

## Architecture Overview

### MVC Pattern
- **Model** — `LibraryDatabase`, `LibraryItem`, `UserAccount` (data + business rules)
- **View** — `gui.*` package (Swing components, no business logic)
- **Controller** — `LibraryManager` (coordinates between Model and View)

### Observer Pattern
- `LibraryChangeListener` interface allows UI components to react to data changes
- `LibraryManager.fireChange()` notifies all registered listeners

### DTO Pattern
- `LoanView`, `StudentSummary`, `BorrowSummary`, `OverdueLoanView`, `WaitlistEntry` — Read-only data transfer for UI
- Separation of presentation data from domain models

### Strategy Pattern
- `SearchEngine` with selectable algorithms
- Algorithm selection based on data characteristics and requirements

---
