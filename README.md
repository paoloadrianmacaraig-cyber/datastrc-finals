# Kingpin MNL - POS & Order Queue System
> **Data Structures & Algorithms (DSA) Finals Project**  
> **Target Sub-Branch:** Kingpin MNL (Streetwear & Apparel Retail)  
> **Course Section:** C2B | 4-Member Group Project  

---

## 📌 1. Project Overview & Client Context

**Kingpin MNL** is a streetwear apparel e-commerce brand operating a retail sub-branch. The sub-branch depends on a centralized main warehouse/branch for inventory replenishment and periodically reports consolidated sales figures back to headquarters.

### Real-World Pain Points Solved
1. **Overselling Prevention:** In high-footfall pop-ups or physical sub-branches, inventory cannot wait until the customer picks up the item to be deducted. Stock is **deducted immediately upon order confirmation** and restored only if cancelled.
2. **Fair & Transparent Order Fulfillment:** Cashiers take orders rapidly while a packaging counter fulfills them in strict arrival order using a **FIFO (First-In, First-Out) Order Queue**.
3. **Queue Cancellation without FIFO Breakage:** When a customer cancels while waiting in line, direct array deletion violates pure queue constraints. The system performs **Queue Rotation** (dequeuing and re-enqueuing active orders) to excise the target order while preserving relative arrival order.
4. **Fast Product Lookup:** Cashiers can scan/enter product codes using **Binary Search ($O(\log n)$)** or search keywords via **Linear Search ($O(n)$)**.
5. **Main-Branch Synchronization:** Generates **Best Sellers** and **Low-Stock Restocking Reports** using **Merge Sort ($O(n \log n)$)** executed on defensive copies to avoid corrupting binary search indexing.

---

## 🚀 2. Quick Start & Execution

### Prerequisites
- Java Development Kit (JDK 11 or higher, tested on JDK 17)

### 1-Click Launch (Windows)
Double-click `run.bat` or run in PowerShell:
```powershell
.\run.bat
```

### Manual Compilation & Execution
```bash
# 1. Compile source files
javac -d bin src/com/kingpin/model/*.java src/com/kingpin/dsa/*.java src/com/kingpin/util/*.java src/com/kingpin/service/*.java src/com/kingpin/*.java

# 2. Run Main POS Application
java -cp bin com.kingpin.Main

# 3. Run Automated DSA Verification Test Suite
java -cp bin com.kingpin.TestRunner
```

---

## 🧠 3. DSA Concepts & Complexity Matrix

| Requirement | Concept / Implementation | Where Applied | Time Complexity | Space Complexity |
| :--- | :--- | :--- | :--- | :--- |
| **Main Data Structure** | Custom Singly-Linked List Queue (`OrderQueue`, `Node<Order>`, `front`, `rear`) | Pending order fulfillment queue | **Enqueue:** $O(1)$<br>**Dequeue:** $O(1)$<br>**Peek:** $O(1)$<br>**Cancel:** $O(n)$ | $O(n)$ auxiliary |
| **Searching Algorithm 1** | Binary Search (Iterative) | Lookup product by code (`KP-101`) in sorted inventory; lookup past receipt by order number | $O(\log n)$ best/avg/worst | $O(1)$ |
| **Searching Algorithm 2** | Linear Search | Search products by partial name substring (e.g., "hoodie", "tee") | $O(n)$ | $O(k)$ matches |
| **Sorting Algorithm 1** | Insertion Sort | Sort products by code upon CSV startup; insertion step when adding a new product | $O(n)$ best (nearly sorted)<br>$O(n^2)$ worst | $O(1)$ in-place |
| **Sorting Algorithm 2** | Merge Sort (Divide & Conquer on copy) | Best sellers ranking (by units sold descending); Low-stock alerts (by quantity ascending) | $O(n \log n)$ guaranteed | $O(n)$ auxiliary copy |

---

## 🔄 4. Order Lifecycle

```mermaid
graph TD
    A[Customer Selects Items] --> B[Check Real-time Stock]
    B --> C[Confirm Order & Tender Cash]
    C -->|Deduct Stock Immediately| D[Assign Sequential Order #]
    D --> E[Enqueue to OrderQueue Rear O(1)]
    E --> F{Fulfillment Counter}
    F -->|Serve Next O(1)| G[Dequeue Front Order]
    G --> H[Mark SERVED -> Print Receipt -> Append to sales_history.csv]
    H --> I[Included in Main Branch Reports]
    F -->|Customer Cancels O(n)| J[Rotate Queue to Excise Order]
    J --> K[Restore Stock back to Inventory]
```

---

## 🛡️ 5. Q&A Defense Arguments (Why Not Alternatives?)

During your oral defense, professors frequently ask why specific data structures or algorithms were not chosen:

1. **Why not `java.util.HashMap` for product lookups?**
   - *Defense:* A hash map provides $O(1)$ average lookups via hashing, which bypasses the search algorithm requirement of the syllabus. Furthermore, hash maps do not maintain the natural lexicographical order needed for code-range scanning and produce collision overhead.
2. **Why not a Binary Search Tree (BST) for the catalog?**
   - *Defense:* Streetwear product codes are issued sequentially (e.g., `KP-101`, `KP-102`, `KP-103`). Inserting sequential keys into an unbalanced BST causes it to degenerate into a linear linked list with worst-case $O(n)$ search, losing all tree advantages. An array-based list maintained with insertion sort guarantees reliable $O(\log n)$ binary search.
3. **Why not QuickSort instead of Merge Sort for reports?**
   - *Defense:* QuickSort has an $O(n^2)$ worst-case performance on already sorted or reverse-sorted data and is inherently unstable. Merge Sort guarantees strict $O(n \log n)$ time across all datasets and preserves stability (items with identical quantities retain their chronological order).
4. **Why not an `ArrayList` for the Order Queue?**
   - *Defense:* Removing an element from the front (`index 0`) of an `ArrayList` requires shifting all remaining $n-1$ elements to the left, degrading dequeue to $O(n)$. A custom singly-linked list with `front` and `rear` pointers achieves true $O(1)$ dequeue.
5. **Why sort copies instead of the master inventory?**
   - *Defense:* Binary search requires the master list to remain strictly sorted by product code. Sorting the master list by sales volume or stock quantity would break binary search. Performing Merge Sort on defensive copies preserves system invariants.

---

## 📁 6. Project Architecture & Code Inventory

```text
src/com/kingpin/
├── Main.java                     # CLI Application Driver & Menu System
├── TestRunner.java               # Automated DSA Verification Test Suite (7/7 Unit Tests)
├── dsa/
│   ├── Node.java                 # Generic Singly-Linked Node<T>
│   ├── OrderQueue.java           # Custom Linked Queue (front, rear, size, cancelByRotation)
│   ├── SearchAlgorithms.java     # BinarySearch (Product & Order) and LinearSearch (Name)
│   └── SortAlgorithms.java       # InsertionSort (Code) and MergeSort (Reports)
├── model/
│   ├── Product.java              # Retail Item Entity with CSV mapping
│   ├── OrderItem.java            # Line item with subtotal calculation
│   └── Order.java                # Order with status, totals, and receipts
├── service/
│   ├── InventoryService.java     # Stock replenishment, inventory queries, binary search
│   ├── OrderService.java         # Order confirmation, queue operations, stock deduction/restoration
│   └── ReportService.java        # Best sellers, low stock, main-branch sync summaries
└── util/
    ├── FileStorage.java          # CSV persistence (inventory.csv, sales_history.csv)
    └── ConsoleUI.java            # Formatted ANSI tables, headers, and receipt layout
```
