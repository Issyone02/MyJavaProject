# SLCAS Project Report

## Smart Library Circulation & Automation System
### MIVA Open University

---

## 1. Source Code Folder Structure

```
SLCAS/
├── model/                    # Data models and business entities
│   ├── LibraryItem.java      # Abstract base class for all library items
│   ├── Book.java             # Concrete book implementation
│   ├── Magazine.java         # Concrete magazine implementation
│   ├── Journal.java          # Concrete journal implementation
│   ├── Borrowable.java       # Interface for checkout/return operations
│   ├── UserAccount.java      # Abstract base for user accounts
│   ├── Student.java          # Student user type
│   ├── Admin.java            # Administrator user type
│   ├── Librarian.java        # Librarian user type
│   ├── BorrowRecord.java     # Individual loan transaction record
│   ├── LibraryDatabase.java  # Central data store with frequency cache
│   ├── SystemLog.java        # Audit trail entry
│   ├── WaitlistEntry.java    # Waitlist queue entry
│   ├── LoanView.java         # DTO for active loans display
│   ├── StudentSummary.java   # DTO for student statistics
│   ├── BorrowSummary.java    # DTO for reports
│   ├── OverdueLoanView.java  # DTO for overdue items
│   ├── Book.java
│   ├── Magazine.java
│   └── Journal.java
│
├── controller/                        # Business logic and coordination
│   ├── LibraryController.java         # Interface defining system operations
│   ├── LibraryManager.java            # Main controller implementation
│   ├── BorrowController.java          # Borrow/return workflow handler
│   ├── SearchEngine.java              # Search algorithms (linear, binary, recursive)
│   ├── SortEngine.java                # Sort algorithms (insertion, merge, quick)
│   ├── AuthController.java            # Authentication interface
│   └── LibraryChangeListener.java     # Observer pattern interface
│
├── gui/                               # User interface layer
│   ├── MainWindow.java                # Primary application frame
│   ├── LoginDialog.java               # Secure authentication dialog
│   ├── ViewPanel.java                 # Catalogue browser with reports
│   ├── BorrowPanel.java               # Borrow/return with CardLayout
│   ├── AdminPanel.java                # Inventory and user management
│   ├── StudentPanel.java              # Student account management
│   ├── SearchSortPanel.java           # Advanced search interface
│   ├── StaffManagementPanel.java      # Staff administration
│   ├── LogsPanel.java                 # System audit log viewer
│   ├── DashboardPanel.java            # Analytics dashboard
│   └── VirtualTableModel.java         # Lazy-loading table model
│
├── utils/                             # Utility classes
│   ├── FileHandler.java               # Binary serialization and export
│   ├── AuthManager.java               # SHA-256 password hashing
│   ├── IDGenerator.java               # Unique identifier generation
│   ├── DataSeeder.java                # Demo data generation
│   ├── GuiUtils.java                  # GUI helper methods
│   └── LibraryConfig.java             # System configuration
│
└── main/                              # Application entry point
    └── LibraryApp.java                # Main class with startup logic
```

---

## 2. Class Hierarchy Diagram (UML)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              <<interface>>                                  │
│                              Borrowable                                     │
│                              + checkout(): boolean                          │
│                              + checkin(): void                              │
│                              + isAvailable(): boolean                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      △
                                      │ implements
                                      │
┌─────────────────────────────────────────────────────────────────────────────┐
│                         <<abstract>> LibraryItem                            │
│                         (implements Borrowable, Serializable)               │
│                         - id: String                                        │
│                         - title: String                                     │
│                         - author: String                                    │
│                         - year: int                                         │
│                         - totalCopies: int                                  │
│                         - availableCopies: int                              │
│                         + getType(): String {abstract}                      │
│                         + checkout(): boolean                               │
│                         + checkin(): void                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    △
                    ┌───────────────┼───────────────┐
                    │               │               │
                    │ extends       │ extends       │ extends
                    │               │               │
           ┌────────┴────────┐ ┌────┴────┐ ┌────────┴────────┐
           │      Book       │ │Magazine │ │     Journal     │
           │                 │ │         │ │                 │
           │ + getType()     │ │+ getType│ │ + getType()     │
           │   "Book"        │ │"Magazine│ │   "Journal"     │
           └─────────────────┘ └─────────┘ └─────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         <<abstract>> UserAccount                            │
│                         (implements Serializable)                           │
│                         - id: int                                           │
│                         - name: String                                      │
│                         - email: String                                     │
│                         - passwordHash: String                              │
│                         - currentLoans: List<BorrowRecord>                  │
│                         - loanHistory: List<BorrowRecord>                   │
│                         + isAdmin(): boolean {abstract}                     │
│                         + checkPassword(): boolean                          │
│                         + addBorrowedItem(): void                           │
│                         + returnItem(): boolean                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    △
                    ┌───────────────┼───────────────┐
                    │               │               │
                    │ extends       │ extends       │ extends
                    │               │               │  
           ┌────────┴────────┐ ┌────┴────┐ ┌────────┴────────┐
           │     Student     │ │  Admin  │ │   Librarian     │
           │                 │ │         │ │                 │
           │ + isAdmin()     │ │+ isAdmin│ │ + isAdmin()     │
           │   false         │ │  true   │ │   false         │
           │ + studentId     │ │+ adminId│ │ + staffId       │
           └─────────────────┘ └─────────┘ └─────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         <<interface>>                                       │
│                         LibraryController                                   │
│                         (API contract for GUI)                              │
│                         + borrowItem(): boolean                             │
│                         + returnItem(): void                                │
│                         + getInventory(): List<LibraryItem>                 │
│                         + createItem(): String                              │
│                         + removeItem(): boolean                             │
│                         + findStudentById(): UserAccount                    │
│                         + getBorrowSummary(): BorrowSummary                 │
│                         + undo(): boolean                                   │
│                         + redo(): boolean                                   │
│                         + addChangeListener(): void                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      △
                                      │ implements
                                      │
┌─────────────────────────────────────────────────────────────────────────────┐
│                              LibraryManager                                 │
│                              (Singleton controller)                         │
│                              - db: LibraryDatabase                          │
│                              - undoStack: Deque<LibraryDatabase>            │
│                              - redoStack: Deque<LibraryDatabase>            │
│                              - listeners: List<LibraryChangeListener>       │
│                              - systemLogs: List<SystemLog>                  │
│                              + borrowItem(): boolean                        │
│                              + returnItem(): void                           │
│                              + undo(): boolean                              │
│                              + redo(): boolean                              │
│                              + fireChange(): void                           │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              LibraryDatabase                                │
│                              (Central data store)                           │
│                              - catalogue: ArrayList<LibraryItem>            │
│                              - members: ArrayList<UserAccount>              │
│                              - waitlist: Queue<String>                      │
│                              - cache: LibraryItem[5]                        │
│                              - accessCount: int[5]                          │
│                              + addItem(): void                              │
│                              + removeItem(): boolean                        │
│                              + findItemById(): LibraryItem                  │
│                              + recordAccess(): void                         │
│                              + getMostAccessedItems(): List<LibraryItem>    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Project Report

### 3.1 Description

The Smart Library Circulation & Automation System (SLCAS) is a comprehensive Java desktop application designed to modernize library operations at MIVA Open University. The system provides a complete solution for managing library inventory, tracking book loans, handling student registrations, and generating analytical reports.

Built using Java Swing for the graphical user interface and following the Model-View-Controller (MVC) architectural pattern, SLCAS offers an intuitive interface for librarians and administrators while maintaining clean separation of concerns in the codebase. The system supports multiple user roles (Admin, Librarian, Student) with role-based access control, ensuring that sensitive operations are restricted to authorized personnel.

Key capabilities include real-time catalogue browsing, efficient search and sort operations using multiple algorithms, automated waitlist management, comprehensive undo/redo functionality, and detailed reporting with visual analytics. The application persists data using Java serialization, ensuring fast save/load operations while maintaining data integrity across sessions.

### 3.2 Features

#### Core Library Operations
- **Catalogue Management**: Add, edit, and delete library items (Books, Magazines, Journals) with validation
- **Borrow/Return Processing**: Streamlined workflow for checking out items to students and processing returns
- **Waitlist Management**: FIFO queue system for handling reservations when items are unavailable
- **Student Account Management**: Registration, profile management, and loan history tracking
- **Multi-copy Support**: Track total and available copies for each item independently

#### Advanced Search & Sort
- **Multiple Search Algorithms**: Linear search for unsorted data, binary search for sorted data, recursive search for educational demonstration
- **Multiple Sort Algorithms**: Insertion sort for small datasets, merge sort for guaranteed O(n log n) performance, quick sort for in-place sorting
- **Field-based Operations**: Search and sort by any field (ID, Title, Author, Type, Year, Availability)
- **Auto-algorithm Selection**: System intelligently selects the best algorithm based on data characteristics

#### Undo/Redo System
- **Deep Copy State Management**: Complete snapshots of the database state before every operation
- **Unlimited Undo Levels**: Stack-based history allows reverting any number of operations
- **Atomic Operations**: Complex operations (borrow + waitlist removal) handled as single undoable unit
- **Redo Capability**: Forward navigation through the history stack

#### Security & Audit
- **SHA-256 Password Hashing**: Secure credential storage with Base64 encoding
- **Role-based Access Control**: Admin-only features (edit/delete items, view logs, manage staff)
- **Comprehensive Audit Logging**: All operations logged with timestamp, user ID, and action details
- **Password Confirmation**: Sensitive operations require re-authentication

#### Reporting & Analytics
- **Real-time Reports**: Category distribution, most borrowed items, overdue loans
- **Frequency Cache**: Tracks most-accessed items using fixed-size array with LRU eviction
- **Visual Analytics**: Donut chart showing catalogue composition
- **Export Capability**: Reports exported to text files for external use

#### User Experience
- **Keyboard Shortcuts**: Alt+B (borrow), Alt+R (return), Enter key form submission
- **Color-coded Status**: Visual indicators for available (green), unavailable (amber), and overdue (red) items
- **Tooltips**: Context-sensitive help throughout the interface
- **Responsive Tables**: Custom renderers with alternating row colors and status highlighting

### 3.3 Data Structures Used

#### ArrayList
**Usage**: Primary storage for catalogue items, student accounts, and loan records.
**Rationale**: ArrayList provides O(1) random access for retrieving items by index and efficient iteration for display purposes. The dynamic resizing capability accommodates growing collections without manual capacity management.

#### Queue (LinkedList Implementation)
**Usage**: Waitlist management for item reservations.
**Rationale**: First-In-First-Out (FIFO) ordering ensures fair allocation of items when they become available. LinkedList implementation provides O(1) enqueue and dequeue operations, essential for efficient waitlist management.

#### Stack (ArrayDeque)
**Usage**: Undo and redo history management.
**Rationale**: LIFO (Last-In-First-Out) structure perfectly matches the semantics of undo operations. ArrayDeque provides efficient push/pop operations without the synchronization overhead of legacy Stack class.

#### Array (Fixed-size)
**Usage**: Frequency cache for tracking most-accessed items.
**Rationale**: Fixed-size array (5 elements) provides O(1) access for cache operations. The fixed size simplifies LRU (Least Recently Used) eviction policy implementation using insertion sort for ranking.

#### HashMap (Implicit via Streams)
**Usage**: While not explicitly declared, stream operations use hash-based collections for filtering and lookup operations.
**Rationale**: Provides O(1) average-case lookup for operations like finding items by ID or checking for duplicate loans.

### 3.4 Algorithms Chosen and Why

#### Searching Algorithms

**Linear Search**
- **When Used**: Unsorted data or single-item lookups
- **Complexity**: O(n) time, O(1) space
- **Rationale**: Simple implementation with no preprocessing required. Efficient for small datasets or when searching through recently added (unsorted) items.

**Binary Search**
- **When Used**: Sorted data fields (ID, Year)
- **Complexity**: O(log n) time, O(1) space
- **Rationale**: Logarithmic time complexity provides significant performance improvement for large datasets. Used after ensuring data is sorted, making it ideal for repeated searches on stable datasets.

**Recursive Search**
- **When Used**: Educational demonstration and specific category counting
- **Complexity**: O(n) time, O(n) stack space
- **Rationale**: Demonstrates recursion concepts required by academic specifications. Used for counting items by category in the reports generation.

#### Sorting Algorithms

**Insertion Sort**
- **When Used**: Small datasets (n < 50), nearly sorted data
- **Complexity**: O(n²) worst case, O(n) best case, O(1) space
- **Rationale**: Efficient for small or nearly sorted datasets due to low overhead. Used in the frequency cache maintenance where only 5 items need sorting.

**Merge Sort**
- **When Used**: Large datasets requiring stable sorting
- **Complexity**: O(n log n) guaranteed, O(n) space
- **Rationale**: Guaranteed O(n log n) performance regardless of input distribution. Stable sort preserves relative order of equal elements, important for consistent display. The space trade-off is acceptable for the reliability benefits.

**Quick Sort**
- **When Used**: Large datasets where in-place sorting is preferred
- **Complexity**: O(n log n) average, O(n²) worst case, O(log n) stack space
- **Rationale**: In-place sorting reduces memory overhead. Generally fastest in practice for random data. Used when memory efficiency is prioritized over stable sorting guarantee.

**Algorithm Selection Strategy**
The system intelligently selects algorithms based on:
1. **Data Size**: Insertion sort for small datasets, merge/quick sort for large
2. **Data State**: Binary search only on confirmed sorted data
3. **Stability Requirements**: Merge sort when stable ordering matters
4. **Space Constraints**: Quick sort when memory is limited, insertion sort for cache

### 3.5 Challenges Faced

#### Separating Code into Layers of Concern
**Challenge**: Initially, business logic was mixed with UI code, making maintenance difficult and preventing code reuse.

**Solution**: Implemented strict MVC architecture:
- **Model Layer**: Pure data classes with validation (`LibraryItem`, `UserAccount`, `LibraryDatabase`)
- **View Layer**: Swing components with no business logic, only display and input handling
- **Controller Layer**: `LibraryManager` coordinates between Model and View, handles all business rules

**Benefits**: Each layer can be tested independently, UI can be redesigned without affecting logic, and business rules are centralized in the controller.

#### Duplicate User ID Prevention
**Challenge**: Ensuring unique identifiers for students and staff across the entire system, especially when importing data or creating accounts simultaneously.

**Solution**: Implemented centralized ID generation in `IDGenerator` utility:
- Sequential ID assignment with atomic increment
- Validation checks before account creation
- Conflict detection during data import operations

**Learning**: Centralized ID management is crucial for data integrity in multi-user scenarios.

#### Researching Sort and Search Algorithms
**Challenge**: Understanding when to apply each algorithm for optimal performance, particularly the trade-offs between time and space complexity.

**Key Research Findings**:
- **Binary Search**: Requires sorted data but provides O(log n) performance. Not suitable for frequently modified datasets due to resorting overhead.
- **Merge Sort**: Best for large datasets and when stable sorting is required, but uses O(n) additional space.
- **Quick Sort**: Generally fastest in practice but O(n²) worst case. In-place sorting saves memory.
- **Insertion Sort**: Surprisingly efficient for small datasets (n < 50) due to low constant factors.

**Implementation Strategy**: Created `SearchEngine` and `SortEngine` classes with algorithm selection based on data characteristics, allowing the system to automatically choose the best approach.

#### Implementing Undo and Redo Functionality
**Challenge**: Correctly implementing undo/redo was one of the most complex aspects of the project. Deep copying the entire database state before every operation required careful handling of object references and serialization.

**Technical Challenges**:
1. **Deep Copy Implementation**: Java's default serialization creates shallow copies. Had to ensure all nested objects (`LibraryItem`, `BorrowRecord`, `UserAccount`) properly implement `Serializable`.
2. **Memory Management**: Unlimited undo history could consume excessive memory. Implemented stack size limits with configurable thresholds.
3. **Atomic Operations**: Complex workflows (borrow + waitlist fulfilment) must be treated as single undoable unit. Solution was to save state only after all validation passes but before the actual operation begins.
4. **State Consistency**: Ensuring the undo stack captures valid states only. Invalid operations must not create history entries.

**Solution Architecture**:
```
Before Operation:
1. Validate all inputs
2. Check preconditions
3. Save deep copy of current state to undoStack
4. Clear redoStack (new operation invalidates redo history)
5. Perform operation
6. fireChange() to update UI

Undo Operation:
1. Pop current state from undoStack
2. Push to redoStack
3. Restore previous state
4. fireChange() to update UI
```

**Lessons Learned**:
- Validation must complete before saving state to prevent orphan snapshots
- Deep copying is expensive; minimize object graph size through proper design
- Clear redo stack on new operations to prevent inconsistent state sequences
- Observer pattern (fireChange) essential for keeping UI synchronized with model changes

#### Additional Challenges

**Pattern Matching Compatibility**: Modern Java features (instanceof pattern matching) caused IDE warnings in some environments. Solution was to use traditional casting for maximum compatibility.

**GUI Layout Management**: Creating responsive layouts that work across different screen resolutions required extensive use of `GridBagLayout` with careful constraint management.

**Data Persistence Format**: Initial versions had incompatible `.dat` files between updates. Solution was to document this clearly in README and provide troubleshooting instructions.

---

## 4. Conclusion

The SLCAS project successfully demonstrates the application of object-oriented principles, appropriate data structure selection, and algorithm optimization in a real-world system. The MVC architecture provides a solid foundation for future enhancements, while the comprehensive undo/redo system ensures user confidence in performing operations.

Key achievements include:
- Clean separation of concerns through MVC pattern
- Intelligent algorithm selection based on data characteristics
- Robust undo/redo system with atomic operation support
- Role-based security with comprehensive audit logging
- User-friendly interface with keyboard shortcuts and visual feedback

The challenges faced, particularly in implementing the undo/redo system and researching optimal algorithms, provided valuable learning experiences in software architecture and algorithm analysis. The project meets all academic requirements while providing a practical, usable library management system for MIVA Open University.
