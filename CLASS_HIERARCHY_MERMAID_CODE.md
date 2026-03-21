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
        +addItem(item) void
        +removeItem(id) void
        +editItem(id, updated) void
        +borrowItem(studentId, itemId) BorrowStatus
        +returnItem(studentId, itemId) void
        +addToWaitlist(studentId, itemId) void
        +removeFromWaitlist(itemId) void
        +undo() void
        +redo() void
        +search(query) SearchResult
        +sortCatalogue(field, algorithm) void
        +getReports() ReportData
    }

    class LibraryChangeListener {
        <<interface>>
        +onLibraryDataChanged() void
    }

    class AuthController {
        <<interface>>
        +validate(userId, password) boolean
        +getFullName(userId) String
        +logFailedAttempt(userId) void
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
        +recordAccess(itemId) void
        +sortCacheDescending() void
    }

    class UserAccount {
        -userId int
        -name String
        -role String
        -passwordHash String
        -currentLoans List
        -borrowHistory List
    }

    class BorrowRecord {
        -itemId String
        -studentId int
        -borrowDate LocalDate
        -dueDate LocalDate
        +isOverdue() boolean
        +getDaysOverdue() long
    }

    %% ── CONTROLLER ──────────────────────────────────────────────────────────

    class LibraryManager {
        -undoHistory Stack
        -redoHistory Stack
        -database LibraryDatabase
        -listeners List
        +saveState() void
        +fireChange() void
    }

    class BorrowController {
        -database LibraryDatabase
        +processBorrow(studentId, itemId) BorrowStatus
        +processReturn(studentId, itemId) void
        +fulfilFirstWaitlistEntry(itemId) void
    }

    class SearchEngine {
        +searchAll(query, catalogue) SearchResult
        +detectSortedField(catalogue) String
    }

    class SortEngine {
        +mergeSort(list, field) void
        +insertionSort(list, field) void
        +quickSort(list, field) void
    }

    %% ── UTILS ───────────────────────────────────────────────────────────────

    class AuthManager {
        <<Singleton>>
        -instance AuthManager
        -staff HashMap
        +getInstance() AuthManager
        +validate(userId, password) boolean
        +getFullName(userId) String
        +logFailedAttempt(userId) void
    }

    %% ── INTERFACE IMPLEMENTATIONS ───────────────────────────────────────────

    Borrowable <|.. LibraryItem
    LibraryController <|.. LibraryManager
    LibraryChangeListener <|.. LibraryManager
    AuthController <|.. AuthManager

    %% ── INHERITANCE ─────────────────────────────────────────────────────────

    LibraryItem <|-- Book
    LibraryItem <|-- Magazine
    LibraryItem <|-- Journal

    %% ── COMPOSITION ─────────────────────────────────────────────────────────

    LibraryManager *-- LibraryDatabase
    LibraryManager *-- BorrowController
    LibraryManager *-- SearchEngine
    LibraryManager *-- SortEngine

    %% ── AGGREGATION ─────────────────────────────────────────────────────────

    LibraryDatabase o-- LibraryItem
    LibraryDatabase o-- UserAccount
    UserAccount o-- BorrowRecord

    %% ── LEGEND & NOTES (unlinked — ELK places as separate cluster) ──────────

    class LEGEND["LEGEND"] {
        dotted line  : implements an interface
        solid line   : extends a parent class
        filled ◆     : owns the other — shares its lifecycle
        hollow ◇     : holds a reference only
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

