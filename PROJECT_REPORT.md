# SLCAS - Smart Library Circulation & Automation System
## Project Report | COS 202 | MIVA Open University

## Source Code
### [https://github.com/Issyone02/MyJavaProject](https://github.com/Issyone02/MyJavaProject)

---

## Group Members

| # | Name | Matric No. | Email |
|---|------|-----------|-------|
| 1 | Olalere Isaiah Toluwani | 2024/B/SENG/0830 | olalere.isaiah@miva.edu.ng |
| 2 | Idowu Oluwadamilare | 2024/B/SENG/0291 | idowu.oluwadamilare@miva.edu.ng |
| 3 | Mujeeb Raheem | 2024/B/CSC/0181 | mujeeb.raheem@miva.edu.ng |
| 4 | Akinniyi Adeleke Solomon | 2024/B/CYB/0273 | adeleke.akinniyi@miva.edu.ng |
| 5 | Livingstone Joseph Obochi | 2024/B/CYB/0320 | livingstone.obochi@miva.edu.ng |
| 6 | Folorunsho Oluwatobi | 2024/B/IT/0082 | folorunsho.oluwatobi@miva.edu.ng |
| 7 | Ganiyat Jolayemi Omowunmi | 2024/B/CYB/0337 | ganiyat.jolayemi@miva.edu.ng |
| 8 | Daniel Oluwasemilore Abiodun | 2024/B/SENG/0250 | daniel.abiodun1@miva.edu.ng |
| 9 | Mustapha Abdulafeez | 2024/B/CSC/0212 | abdulafeez.mustapha@miva.edu.ng |
| 10 | Dolapo Opebi Anuoluwapo | 2024/B/CYB/03H | dolapo.opebi@miva.edu.ng |
| 11 | Simon Ochayi Ujor | 2024/B/CYB/0787 | simon.ujor@miva.edu.ng |
| 12 | Divine Okpara | 2024/B/CYB/0397 | divine.okpara@miva.edu.ng |
| 13 | Cherechi Udensi | 2024/B/CYB/0430 | cherechi.udensi@miva.edu.ng |

---

## 1. Description

SLCAS is a Java SE desktop application that automates university library operations: catalogue management, borrowed items due dates tracking, reservation queuing, staff administration, and report generation. The interface is styled with FlatLaf for a modern, consistent appearance across platforms.

Three user roles shape how the system is accessed. **Admins** have full control: they manage the catalogue, register students, administer staff accounts, and access the audit log and reports. Sensitive actions require password re-confirmation. **Librarians** handle daily desk work: processing borrows and returns, managing the waitlist, and searching the catalogue. They cannot modify catalogue structure, manage staff, or view logs. **Students** do not log in; they are account holders whose loans, overdue history, and waitlist reservations are tracked by staff.

The codebase follows a strict **MVC** structure across five packages. The `model` package owns all data (`LibraryItem` abstract base, `Book`/`Magazine`/`Journal` subtypes, `UserAccount`, `BorrowRecord`, `LibraryDatabase`). The `controller` package holds all business logic (`LibraryManager` as central coordinator, `BorrowController`, `SearchEngine`, `SortEngine`). The `gui` package holds ten screens with zero logic, each communicating only through the `LibraryController` interface. Utilities live in `utils` and the entry point is in `main`.

---

## 2. Features

**Catalogue Management:** Admins add, edit, and delete Books, Magazines, and Journals. Copy counts track total and available separately, preserving the in-loan count on edits. Librarians can also add library items.

**Borrow, Return and Waitlist:** Borrows check item existence, student existence, and availability. When an item is returned, the system notifies the librarian if there is a student on the waitlist for that item and prompts them to fulfil the reservation. The waitlist is first-in first-out but supports manual reordering, fulfilment, and removal.

**Student Management:** Admins register and manage student accounts. Each student record stores contact details, a list of active loans, and a full borrow history that persists after items are returned.

**Staff Management:** Admins create and deactivate librarian accounts. Deactivation bars login while preserving the staff member's full audit trail, so historical records remain intact.

**Search and Sort:** Searches auto-detect whether the catalogue is sorted and select Binary or Linear Search accordingly. Three sorts are available via dropdown (Merge, Insertion, Quick Sort), with the field chosen by clicking any column header. A global search bar at the top searches all tabs in real time.

**Reporting and Analytics:** Reports show the most-borrowed items, live overdue loans, category distribution, six inventory summary cards, and a donut chart. The status bar overdue count refreshes every 60 seconds; clicking it opens a colour-coded popup. Reports export to a text file.

**Undo and Redo:** Every change is preceded by a full snapshot of the catalogue, members, and waitlist (up to 50). Related actions like borrowing and waitlist removal share one snapshot so they reverse together as a single Undo.

**Audit Log:** Every system action, including login, borrow, return, catalogue changes, and undo, is recorded with a timestamp and the identity of the acting staff member. The log is accessible only to admins and cannot be modified or deleted.

**Security and Persistence:** Passwords are stored as SHA-256 hashes. Role-based access restricts what each staff type can see and do. All data is serialised to binary files automatically before every change.

**Other Features:** keyboard shortcuts; binary backup export and restore; text report export; manual waitlist reordering; duplicate loan prevention; duplicate student ID prevention; admin password reset for staff; deletion blocked while copies are on loan; borrowed count preserved on quantity edit; auto-calculated due dates; multiple export formats; student full borrow history; session login and logout tracking; failed login attempt logging; auto-save before every change; and first-run demo data seeding.

---

## 3. Data Structures Used

| Structure | Java Type | Location | Why |
|---|---|---|---|
| **Stack** | `Stack<LibraryState>` | Undo/redo history in LibraryManager | Last In First Out matches exactly how undo and redo work; each entry saves a copy of all three collections; capped at 50 |
| **Fixed Array** | `LibraryItem[10]`, `int[10]` | Frequency cache in LibraryDatabase | Fixed size controls memory use; parallel arrays store each item alongside its access count for re-ranking and eviction |
| **HashMap** | `HashMap<Integer, UserAccount>`, `HashMap<String, Integer>` | Staff credentials; borrow-count aggregation | O(1) login lookup; O(1) per-item borrow tallying for the most-borrowed report |
| **Queue** | `LinkedList` as `Queue<String>` | Waitlist in LibraryDatabase | First In First Out ensures the first student to reserve is the first served on return |
| **ArrayList** | `ArrayList<LibraryItem>`, `ArrayList<UserAccount>` | Catalogue, members, loans, logs | O(1) indexed access for table rendering; efficient sequential iteration throughout |

---

## 4. Algorithms Chosen and Why

**Binary Search** auto-selects when the catalogue is sorted. It works by repeatedly splitting the list in half until a match is found, then checks neighbouring entries to collect all matching results. This ensures complete results at O(log n) on the primary field; a plain binary search would miss matches on other fields.

**Linear Search** is the fallback for unsorted data. It checks five fields per item with case-insensitive partial matching and requires no preprocessing, keeping results correct after insertions, deletions, or an Undo.

**Merge Sort** is the default sort because it guarantees O(n log n) in every case including worst-case, unlike Quick Sort which slows down on already-sorted data with a fixed pivot. It is also stable, meaning items with equal values keep their previous relative order, which matters for consistent display. Bubble Sort and Selection Sort were ruled out at O(n²).

**Insertion Sort** is used in two places: as a user-selectable catalogue sort and internally to re-rank the ten-item frequency cache after every access. Since only one count changes per access, the cache is nearly always already in order, and insertion sort handles that efficiently.

**Quick Sort** is offered as the third sort option for users who prefer speed. A smarter pivot selection strategy prevents the slowdown that a fixed pivot causes on already-sorted data.

---

## 5. Challenges Faced

**Planning and Architecture.** Designing a sound architecture and following the MVC pattern proved more difficult than anticipated. Early in development, business logic appeared in the wrong layers, meaning a change in one part of the code would unexpectedly break another. Several components had to be rewritten to fix this. The experience showed that early decisions affect everything built after them, and that fixing them mid-development is costly.

**Achieving a Professional Appearance.** Java's default look-and-feel is dated and renders inconsistently across operating systems. We integrated FlatLaf, a library built specifically for this problem, which replaced the entire visual rendering with a modern flat design without requiring changes to any layout code.

**Undo and Redo Correctness.** Our initial undo and redo did not restore the state the user expected. Pressing undo would land on a state the user did not recognise, and performing an undo then a redo would sometimes produce a different result than the original action. The root cause was poor history tracking: snapshots were being saved at the wrong points, and some actions were recording multiple history entries instead of one. 

**Dynamic Algorithm Selection.** Deciding which search and sort algorithms to use required careful analysis of their performance trade-offs. We needed a default sort that worked well in all situations, which ruled out certain options. For search, we built automatic detection of whether the catalogue is currently sorted, so the system picks the faster algorithm when it can and falls back to the safe option otherwise.

**Implementing the Overdue Notification.** Figuring out how to show overdue information to staff without interrupting their work was technically involved. We settled on a background timer that checks for overdue loans every 60 seconds, updates the status bar count, and lets staff click through to see a full breakdown, all while the rest of the application continues to run normally.
