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
  ├── UserAccount (abstract)      — Base for all user types
  ├── Student, Admin, Librarian   — Role-specific accounts
  ├── BorrowRecord                — Individual loan tracking
  ├── LibraryDatabase            — Central data store with cache
  ├── SystemLog                  — Audit trail entries
  └── WaitlistEntry, LoanView,   — DTOs for UI display
      StudentSummary, BorrowSummary

/controller  — Business logic and coordination
  ├── LibraryManager            — Main controller implementing LibraryController
  ├── LibraryController (interface) — API contract for GUI
  ├── BorrowController          — Borrow/return workflow handler
  ├── SearchEngine              — Linear, binary, recursive search
  └── AuthController            — Authentication interface

/gui         — Swing-based user interface
  ├── MainWindow                — Primary application frame
  ├── ViewPanel                 — Catalogue browser + Reports
  ├── BorrowPanel               — Borrow/return with 3-card layout
  ├── AdminPanel                — Inventory & user management
  ├── StudentPanel              — Student account management
  ├── SearchSortPanel          — Advanced search interface
  ├── StaffManagementPanel     — Staff administration
  ├── LogsPanel                — System audit log viewer
  ├── DashboardPanel           — Analytics dashboard
  ├── LoginDialog              — Secure authentication
  └── VirtualTableModel        — Lazy-loading table data model

/utils       — Utility classes and helpers
  ├── FileHandler              — Binary serialization & export
  ├── IDGenerator              — Unique ID generation
  ├── AuthManager              — SHA-256 password hashing
  ├── LibraryConfig            — System configuration
  └── DataSeeder               — Demo data generation

/main        — Application entry point
  └── LibraryApp               — Main class with startup logic
```

---

## How to Run

### Compile and Run
```bash
javac -d out model/*.java controller/*.java gui/*.java utils/*.java main/*.java
java -cp out main.LibraryApp
```

### Or using an IDE
Import as a standard Java project and run `main.LibraryApp`

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
   - Reports dialog includes: category distribution, most borrowed, overdue items, cache statistics
   - Donut chart visualization
   - Export to text file

2. **Borrow/Return** — Three-card layout:
   - **Catalogue** — Browse and borrow/return items
   - **Active Loans** — View all current loans with status
   - **Waitlist** — Manage reservation queue

3. **Admin** — Four sub-tabs:
   - **Inventory** — Add, edit, delete items (admin-only edit/delete)
   - **Students** — Manage student accounts
   - **Staff Management** — Add/remove staff accounts
   - **Logs** — System audit trail (admin-only)

4. **Search & Sort** — Advanced filtering:
   - Click column header to select field
   - Dropdown for algorithm: Linear, Binary, Insertion Sort, Merge Sort, Quick Sort
   - Auto-algorithm selection based on data characteristics

### File Menu
- **Save Data** — Manual persistence trigger
- **Export Data** — Text report or backup
- **Import Backup** — Restore from backup file
- **Logout** — Return to login dialog
- **Exit** — Close application

---

## Key Features

### Undo/Redo System
- **Deep copy state management** — Complete snapshot before every operation
- **Stack-based history** — Unlimited undo/redo levels
- **Atomic operations** — Borrow + waitlist removal handled as single undoable action

### Frequency Cache
- **Most-accessed items tracking** — Array-based fixed-size cache (5 items)
- **LRU eviction policy** — Least recently accessed item replaced
- **Insertion sort ordering** — Cache maintained in descending access frequency

### Search & Sort Algorithms
- **Linear Search** — Unsorted data or single-item lookup
- **Binary Search** — Sorted data with O(log n) performance
- **Recursive Search** — For demonstration of recursion concepts
- **Insertion Sort** — Small datasets, stable sort
- **Merge Sort** — Large datasets, guaranteed O(n log n)
- **Quick Sort** — In-place sorting with average O(n log n)

### Data Persistence
- **Binary serialization** — Fast save/load using Java Object Serialization
- **SHA-256 password hashing** — Secure credential storage
- **Automatic backup** — State saved before every operation for undo support

---

## Requirements Coverage

### Object-Oriented Programming
- **Abstraction** — `LibraryItem` abstract base class with `Book`, `Magazine`, `Journal`
- **Interface** — `Borrowable` defines checkout/return contract
- **Polymorphism** — `LibraryController` interface with `LibraryManager` implementation
- **Inheritance** — `UserAccount` base with `Student`, `Admin`, `Librarian` subclasses
- **Encapsulation** — Private fields with validated setters

### Data Structures
- **ArrayList** — Catalogue storage, student lists, loan records
- **Queue (LinkedList)** — Waitlist FIFO management
- **Stack** — Undo/redo history (ArrayDeque)
- **Array** — Fixed-size frequency cache (5 items)

### Algorithms
- **Sorting:** Insertion Sort, Merge Sort, Quick Sort (selectable via dropdown)
- **Searching:** Linear, Binary (any sorted field), Recursive (auto-selected)
- **Recursion:** Recursive search, merge sort, quick sort, recursive item counting

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
- **Keyboard Shortcuts** — Alt+B (borrow), Alt+R (return), Enter key form submission

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
- `LoanView`, `StudentSummary`, `BorrowSummary` — Read-only data transfer for UI
- Separation of presentation data from domain models

### Strategy Pattern
- `SearchEngine` with selectable algorithms
- Algorithm selection based on data characteristics and requirements

---
