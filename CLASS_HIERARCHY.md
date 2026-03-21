# SLCAS — Class Hierarchy Diagram
## COS 202 | MIVA Open University

```
==========================================================================================================================
                                    SLCAS  -  CLASS HIERARCHY DIAGRAM
==========================================================================================================================

  +---------------------------+                  +-----------------------------+                 +-------------------------+
  |       <<interface>>       |                  |        <<interface>>        |                 |      <<interface>>      |
  |         Borrowable        |                  |      LibraryController      |                 |      AuthController     |
  +---------------------------+                  +-----------------------------+                 +-------------------------+
  | + checkout()  : boolean   |                  | + addItem()      : void     |                 | + validate()  : boolean |
  | + checkin()   : void      |                  | + removeItem()   : void     |                 | + getFullName(): String  |
  | + isAvailable(): boolean  |                  | + borrowItem(): BorrowStatus|                 | + logFailed() : void    |
  +---------------------------+                  | + returnItem()   : void     |                 +-------------------------+
               ^                                 | + undo() / redo(): void     |                            ^
               .                                 | + search(): SearchResult    |                            .
          (implements)                           | + ... 24 more methods       |                       (implements)
               .                                 +-----------------------------+                            .
               .                                              ^                                             .
               .                                         (implements)                                       .
               .                                              .                                             .
  +------------+----------+                    +-------------+--------------+                  +------------+----------+
  |       <<abstract>>    |                    |       LibraryManager       |                  |     <<Singleton>>     |
  |        LibraryItem    |                    +----------------------------+                  |       AuthManager     |
  +------------------------+                   | - undoHistory : Stack      |                  +-----------------------+
  | - id          : String |                   | - redoHistory : Stack      |                  | - instance : AuthMgr  |
  | - title       : String |                   | - database    : LibraryDB  |                  | - staff    : HashMap  |
  | - author      : String |                   | - listeners   : List       |                  +-----------------------+
  | - year        : int    |                   +----------------------------+                  | + getInstance()       |
  | - totalCopies : int    |                   | + saveState()  : void      |                  | + validate()          |
  | - available   : int    |                   | + fireChange() : void      |                  | + getFullName()       |
  +------------------------+                   +----------------------------+                  | + logFailed()         |
  | + getType()   : String*|                                |                                  +-----------------------+
  | + checkout()  : boolean|                         (composes *)
  | + checkin()   : void   |                                |
  | + isAvailable(): bool  |             +------------------+------------------+----------------+
  +------------------------+             |                  |                  |                |
           ^                             v                  v                  v                v
           |                    +----------------+  +---------------+  +---------------+  +----------------------+
        (extends)               | BorrowController|  | SearchEngine  |  |  SortEngine   |  |   LibraryDatabase    |
           |                    +----------------+  +---------------+  +---------------+  +----------------------+
    +------+------+             | + processBorrow|  | + searchAll() |  | + mergeSort() |  | - catalogue:ArrayList|
    |      |      |             |     ()         |  | + detectSorted|  | + insertSort()|  | - members  :ArrayList|
    v      v      v             | + processReturn|  |   Field()     |  | + quickSort() |  | - waitlist :Queue    |
 +--------+  +-----------+  +--------+           |  +---------------+  +---------------+  | - cache    :Item[10] |
 |  Book  |  | Magazine  |  | Journal|           |                                         | - count    :int[10]  |
 +--------+  +-----------+  +--------+           |                                         +----------------------+
 |+getType|  | +getType()|  |+getType|  + fulfilFirstWaitlistEntry()                       | + recordAccess()     |
 |   ()   |  |           |  |   ()   |  +----------------+                                 | + sortCacheDesc()    |
 +--------+  +-----------+  +--------+                                                     +-----------+----------+
                                                                                                        |
                                                                                               (aggregates o)
                                                                                                        |
                                                                                                        v
                                                                                            +-----------+----------+
                                                                                            |     UserAccount      |
                                                                                            +----------------------+
                                                                                            | - userId   : int     |
                                                                                            | - name     : String  |
                                                                                            | - role     : String  |
                                                                                            | - passHash : String  |
                                                                                            | - curLoans : List    |
                                                                                            | - borHist  : List    |
                                                                                            +----------------------+
                                                                                            | + getBorrowCount()   |
                                                                                            +-----------+----------+
                                                                                                        |
                                                                                               (aggregates o)
                                                                                                        |
                                                                                                        v
                                                                                            +-----------+----------+
                                                                                            |     BorrowRecord     |
                                                                                            +----------------------+
                                                                                            | - itemId   : String  |
                                                                                            | - studentId: int     |
                                                                                            | - borrowDate: Date   |
                                                                                            | - dueDate  : Date    |
                                                                                            +----------------------+
                                                                                            | + isOverdue(): bool  |
                                                                                            | + getDaysOverdue()   |
                                                                                            +----------------------+


  +----------------------------------+    LibraryChangeListener is implemented by LibraryManager.
  |        <<interface>>             |    fireChange() calls onLibraryDataChanged() on every
  |    LibraryChangeListener         |    registered GUI panel whenever data is mutated,
  +----------------------------------+    removing the need for any screen to poll for updates.
  | + onLibraryDataChanged() : void  |    (Observer Pattern)
  +----------------------------------+
              ^
              . (implemented by LibraryManager)


==========================================================================================================================
  LEGEND
  -------
  .....  (dots)    Implements interface
  -----  (lines)   Extends class (inheritance)
  *---             Composition   : owner controls lifecycle
  o---             Aggregation   : container holds references
   *               Abstract method marker (on getType())
  <<Singleton>>    Single shared instance via getInstance()
==========================================================================================================================
```
