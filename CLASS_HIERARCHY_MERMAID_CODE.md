%%{init: {
  "theme": "base",
  "layout": "elk",
  "elk": {
    "algorithm":                              "layered",
    "elk.direction":                          "DOWN",
    "elk.edgeRouting":                        "ORTHOGONAL",
    "elk.layered.spacing.nodeNodeBetweenLayers": "80",
    "elk.spacing.nodeNode":                   "60",
    "elk.layered.unnecessaryBendpoints":      "true",
    "elk.layered.nodePlacement.strategy":     "LINEAR_SEGMENTS"
  },
  "themeVariables": {
    "primaryColor":        "#E8F4FD",
    "primaryTextColor":    "#2c3e50",
    "primaryBorderColor":  "#888888",
    "lineColor":           "#444444",
    "secondaryColor":      "#FFF8E1",
    "tertiaryColor":       "#F3E5F5",
    "background":          "#ffffff",
    "mainBkg":             "#E8F4FD",
    "nodeBorder":          "#888888",
    "edgeLabelBackground": "#ffffff",
    "attributeBackgroundColorEven": "#f8f9fa",
    "attributeBackgroundColorOdd":  "#ffffff",
    "fontFamily": "ui-monospace, monospace",
    "fontSize":   "13px"
  }
}}%%
classDiagram
    direction TB

    %% ── INTERFACES ──────────────────────────────────────────────────────────

    class Borrowable {
        <<interface>>
        +checkout() boolean
        +checkin() void
        +isAvailable() boolean
    }

    class LibraryController {
        <<interface>>
        +addChangeListener(l) void
        +removeChangeListener(l) void
        +createItem(type, title, author, year, total, userId) String
        +addItem(item, userId) void
        +removeItem(id, userId, reason) boolean
        +updateItem(userId, id, type, title, author, year, total, reason) void
        +borrowItem(userId, student, item) boolean
        +returnItem(userId, student, item) void
        +addToWaitlist(userId, student, item) void
        +fulfillWaitlistEntry(userId, student, item, idx) boolean
        +insertionSortBy(field) void
        +mergeSortBy(field) void
        +quickSortBy(field) void
        +undo(userName) void
        +redo(userName) void
        +saveState(isUndoOrRedo) void
        +getActiveLoans() List
        +getOverdueLoans() List
        +getStudentSummaries() List
        +getBorrowSummary() BorrowSummary
        +getSystemLogs() List
    }

    class LibraryChangeListener {
        <<interface>>
        +onLibraryDataChanged() void
    }

    class AuthController {
        <<interface>>
        +validate(int userId, String password) boolean
        +getFullName(int userId) String
        +logFailedAttempt(String attemptedId) void
    }

    %% ── MODEL: item hierarchy ───────────────────────────────────────────────

    class LibraryItem {
        <<abstract>>
        -id String
        -title String
        -author String
        -year int
        -totalCopies int
        -availableCopies int
        +getType() String
        +checkout() boolean
        +checkin() void
        +isAvailable() boolean
        +setYear(year) void
        +setTotalCopies(n) void
        +setAvailableCopies(n) void
    }

    class Book {
        +getType() String
    }

    class Magazine {
        +getType() String
    }

    class Journal {
        +getType() String
    }

    %% ── MODEL: data ─────────────────────────────────────────────────────────

    class LibraryDatabase {
        -catalogue ArrayList
        -members ArrayList
        -waitlist Queue
        -accessCache LibraryItem[]
        -accessCount int[]
        +addItem(item) void
        +removeItem(id) boolean
        +findItemById(id) LibraryItem
        +addMember(student) boolean
        +removeMember(studentId) void
        +findMemberById(id) UserAccount
        +enqueueWaitlist(entry) void
        +recordAccess(item) void
        +getMostAccessedItems() List
    }

    class UserAccount {
        -accountId String
        -fullName String
        -passwordHash String
        -role String
        -currentLoans List
        -history List
        +checkPassword(raw) boolean
        +isAdmin() boolean
        +addBorrowedItem(item) void
        +returnItem(item) boolean
        +changePassword(newRaw) void
    }

    class BorrowRecord {
        -item LibraryItem
        -borrowDate LocalDate
        -dueDate LocalDate
        -returnDate LocalDate
        +isOverdue() boolean
        +getDaysOverdue() long
        +getDaysRemaining() long
        +setReturnDate(d) void
    }

    class WaitlistEntry {
        <<record>>
        studentName String
        studentId String
        itemTitle String
        +format() String
        +parse(raw) WaitlistEntry
    }

    class SystemLog {
        -userId String
        -timestamp String
        -action String
        -details String
        +getUserId() String
        +getTimestamp() String
        +getAction() String
        +getDetails() String
    }

    %% ── MODEL: DTOs (read-only views returned to GUI) ───────────────────────

    class LoanView {
        <<record>>
        studentName String
        studentId String
        itemTitle String
        itemType String
        borrowDate String
        dueDate String
        status String
    }

    class OverdueLoanView {
        <<record>>
        studentName String
        studentId String
        itemTitle String
        itemType String
        dueDate String
        daysOverdue long
    }

    class StudentSummary {
        <<record>>
        studentId String
        name String
        loanCount int
        itemTitles String
        dueDates String
        hasOverdue boolean
    }

    class BorrowSummary {
        bookTitles int
        magTitles int
        journalTitles int
        bookCopies int
        magCopies int
        journalCopies int
        totalCopies int
        borrowedCount int
        waitlistCount int
        mostBorrowedLines List
        overdueLines List
    }

    %% ── CONTROLLER ──────────────────────────────────────────────────────────

    class LibraryManager {
        -db LibraryDatabase
        -undoHistory Stack
        -redoHistory Stack
        -changeListeners List
        -systemLogs List
        +saveState(isUndoOrRedo) void
        +undo(userName) void
        +redo(userName) void
        +fireChange() void
        +addLog(userId, action, details) void
        +borrowItem(userId, student, item) boolean
        +returnItem(userId, student, item) void
    }

    class BorrowController {
        -controller LibraryController
        +processBorrow(operatorId, studentId, itemId) BorrowStatus
        +addToWaitlist(operatorId, student, item) void
        +fulfilFirstWaitlistEntry(operatorId, item) WaitlistResult
    }

    class SearchEngine {
        <<static utility>>
        +searchAll(items, query) SearchResult
        +detectSortedField(items) String
    }

    class SortEngine {
        <<static utility>>
        +insertionSort(list, field) void
        +mergeSort(list, field) void
        +quickSort(list, field) void
    }

    %% ── UTILS ───────────────────────────────────────────────────────────────

    class AuthManager {
        <<Singleton>>
        -instance AuthManager
        -users HashMap
        +getInstance() AuthManager
        +validate(userId, password) boolean
        +getFullName(userId) String
        +logFailedAttempt(attemptedId) void
        +addUser(id, pass, name, isAdmin) void
        +removeUser(id) void
        +isAdmin(id) boolean
    }

    class FileHandler {
        <<static utility>>
        +saveAll(items, students, waitlist) void
        +loadData() List
        +loadStudents() List
        +loadWaitlist() Queue
        +exportToText(items, students, dest) boolean
        +exportBackup(items, students, dest) boolean
        +importBackup(source) Object[]
        +logStealthActivity(message) void
    }

    class DataSeeder {
        <<static utility>>
        +seedIfEmpty() boolean
    }

    %% ── STARTUP ─────────────────────────────────────────────────────────────

    class LibraryApp {
        <<main>>
        +main(args) void
        +startApp() void
    }

    %% ── GUI ─────────────────────────────────────────────────────────────────

    class LoginDialog {
        <<JDialog>>
        -auth AuthController
        -succeeded boolean
        -loggedInUserId int
        +isSucceeded() boolean
        +getLoggedInUserId() int
    }

    class MainWindow {
        <<JFrame>>
        -controller LibraryController
        -viewPanel ViewPanel
        -adminPanel AdminPanel
        -borrowPanel BorrowPanel
        -studentPanel StudentPanel
        -searchSortPanel SearchSortPanel
        -staffPanel StaffManagementPanel
        -logsPanel LogsPanel
        +onLibraryDataChanged() void
    }

    %% ── INTERFACE IMPLEMENTATIONS ───────────────────────────────────────────

    Borrowable <|.. LibraryItem
    LibraryController <|.. LibraryManager
    LibraryChangeListener <|.. MainWindow
    AuthController <|.. AuthManager

    %% ── INHERITANCE ─────────────────────────────────────────────────────────

    LibraryItem <|-- Book
    LibraryItem <|-- Magazine
    LibraryItem <|-- Journal

    %% ── COMPOSITION ─────────────────────────────────────────────────────────

    LibraryManager *-- LibraryDatabase
    LibraryManager ..> SearchEngine
    LibraryManager ..> SortEngine

    %% ── AGGREGATION ─────────────────────────────────────────────────────────

    LibraryDatabase o-- LibraryItem
    LibraryDatabase o-- UserAccount
    UserAccount o-- BorrowRecord
    LibraryManager o-- SystemLog

    %% ── ASSOCIATIONS / DEPENDENCIES ─────────────────────────────────────────

    BorrowController --> LibraryController
    LibraryManager --> FileHandler
    AuthManager --> FileHandler
    LibraryManager ..> WaitlistEntry
    BorrowController ..> WaitlistEntry
    DataSeeder --> FileHandler
    LibraryApp ..> AuthManager
    LibraryApp ..> DataSeeder
    LibraryApp ..> LoginDialog
    LibraryApp ..> MainWindow

    %% ── DTO DEPENDENCIES (LibraryManager creates and returns these) ──────────

    LibraryManager ..> LoanView
    LibraryManager ..> OverdueLoanView
    LibraryManager ..> StudentSummary
    LibraryManager ..> BorrowSummary

    %% ── LEGEND & ABOUT (unlinked — ELK places as separate cluster) ──────────

    class LEGEND["LEGEND"] {
        dotted line  : implements an interface
        solid line   : extends a parent class
        filled ◆     : owns the other — shares its lifecycle
        hollow ◇     : holds a reference only
        dashed arrow : depends on / creates
        + public     : accessible from anywhere
        - private    : accessible within the class only
    }

    class ABOUT["ABOUT THIS SYSTEM"] {
        LibraryItem is the base for every catalogue entry
        Book Magazine and Journal extend it so they share
        the same borrowing behaviour without repeating code
        ──────────────────────────────────────────────────
        LibraryManager coordinates all library operations
        It delegates borrowing to BorrowController searching
        to SearchEngine and sorting to SortEngine keeping
        each responsibility separate and easy to maintain
        ──────────────────────────────────────────────────
        LibraryDatabase is the single store for all catalogue
        items member accounts and the borrow waitlist
        ──────────────────────────────────────────────────
        AuthManager handles all login and identity checks
        Only one instance exists throughout the application
        to ensure all authentication goes through one place
    }
