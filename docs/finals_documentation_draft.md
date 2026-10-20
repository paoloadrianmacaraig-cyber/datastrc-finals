# Documentation Draft: Kingpin MNL POS & Order Queue System
**Course:** Data Structures & Algorithms (DSA) Finals Project  
**Section:** C2B | **File Target:** `finals_C2B_LastNames.docx`  

---

# SECTION 1: INTRODUCTION

## 1.1 Client & Business Background
**Client Name:** Kingpin MNL (Streetwear & Apparel Retail)  
**Nature of Business:** An independent streetwear e-commerce brand operating a retail sub-branch / pop-up store.  
**Relationship & Architecture:** The sub-branch relies on the central main warehouse/branch for merchandise inventory replenishment and regularly transmits daily sales, inventory depletion, and restocking summaries back to the parent organization.

## 1.2 Problem Statement & Pain Points (from Client Interview)
1. **Overselling Vulnerability:** During peak customer footfall, multiple cashiers and counters risk promising the same limited streetwear items (hoodies, limited jackets) if stock is only deducted at pickup/serving time.
2. **Fulfillment Bottlenecks & Order Jams:** Without a structured order queue, fulfillment staff manually sort paper receipts, leading to order mix-ups, delayed serving, and unfair service order.
3. **Queue Cancellation Disruptions:** When customers cancel an order in line, traditional POS systems or manual processes either lose track of inventory or break the sequence of other waiting customers.
4. **Slow Manual Product Searches:** Looking up item codes across physical binders or long spreadsheets wastes cashier time during high customer volume.
5. **Lack of Synchronized Restocking Insights:** The sub-branch struggles to notify the main branch regarding exactly which items are high-velocity best-sellers and which are approaching dangerously low stock.

## 1.3 Project Objectives
- Develop an automated POS and Order Queue application implementing custom data structures and algorithms without relying on third-party collections.
- Ensure strict **FIFO (First-In, First-Out)** order fulfillment using a custom singly-linked queue with $O(1)$ enqueue and dequeue operations.
- Provide a mathematically sound **Queue Cancellation by Rotation ($O(n)$)** mechanism that maintains relative arrival sequence while restoring stock.
- Guarantee sub-second product lookups using **Binary Search ($O(\log n)$)** on code-sorted inventory and **Linear Search ($O(n)$)** for partial name queries.
- Generate reliable analytical reports (Best Sellers, Low-Stock Restocking) using **Merge Sort ($O(n \log n)$)** executed on defensive copies to safeguard master inventory indexing.
- Ensure seamless cross-session persistence through CSV file storage.

## 1.4 Transactions Table

| Transaction ID | Transaction Name | User Role | Input Data | Process / Algorithm | Expected Output |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TR-01** | Search Product (Code) | Cashier | Product Code (`KP-101`) | Iterative Binary Search ($O(\log n)$) on code-sorted list | Product details, stock availability, unit price |
| **TR-02** | Search Product (Name) | Cashier | Name keyword ("hoodie") | Linear Search ($O(n)$) across catalog | Filtered table of matching products |
| **TR-03** | Add Item to Cart | Cashier | Product Code, Quantity | Stock validation against current inventory | Cart line item created; subtotal recalculated |
| **TR-04** | Confirm Order | Cashier | Cash tendered | Stock deduction, Order number generation, Enqueue to `OrderQueue` ($O(1)$) | Order confirmed, queued at rear, change computed |
| **TR-05** | Peek Next Order | Packaging / Cook | None | Inspect `front` pointer ($O(1)$) | Order number, items, timestamp of next in line |
| **TR-06** | Serve Order | Packaging / Cook | None | `OrderQueue.dequeue()` ($O(1)$), mark SERVED, append to `sales_history.csv` | Order dequeued, receipt printed, saved to history |
| **TR-07** | Cancel Order | Cashier / Manager | Target Order Number | `OrderQueue.cancel()` ($O(n)$ rotation), inventory stock restoration | Target order removed; stock restored; queue order preserved |
| **TR-08** | Restock Inventory | Manager | Product Code, Quantity received | Look up product via Binary Search; increment stock; save to CSV | Updated stock count |
| **TR-09** | Add New Product | Manager | Code, Name, Category, Price, Stock, Min Threshold | Duplicate check via Binary Search; `insertInSortedOrder()` ($O(n)$ insertion step) | Product inserted in sorted order; CSV updated |
| **TR-10** | Best Sellers Report | Manager / Owner | None | Sales aggregation, Merge Sort on defensive copy ($O(n \log n)$) descending | Ranked table of top selling streetwear items |
| **TR-11** | Low-Stock Report | Manager / Owner | None | Filter `stock <= threshold`, Merge Sort on copy ($O(n \log n)$) ascending | Critical stock table sorted by lowest stock |
| **TR-12** | Main Branch Summary | Manager / Owner | None | Consolidate sales, top sellers, and low-stock restock requests | Formatted dispatch report for parent company |

## 1.5 Scope and Limitations
- **In Scope:** POS cart taking, custom linked-list queue, stock validation and deduction/restoration, binary search, linear search, insertion sort, merge sort, CSV file persistence, receipt printing, and main-branch summary generation.
- **Out of Scope / Deliberate Limitations:**
  - Multi-user network synchronization / client-server sockets (desktop single-instance).
  - Online credit card payment gateway integration (cash payments with change computation).

---

# SECTION 2: PROGRAM DESIGN

## 2.1 System Architecture Flowchart
```
[Start POS Application]
        │
        ├──> [Load CSV Data & Sort Inventory via Insertion Sort]
        │
        ├──> [Main Menu Loop]
                 │
                 ├── [1] POS Cart & Take Order
                 │        ├── Search (Binary / Linear)
                 │        ├── Add to Cart (Check Stock)
                 │        └── Confirm Order ──> Deduct Stock ──> Enqueue to OrderQueue
                 │
                 ├── [2] Order Queue Fulfillment
                 │        ├── Peek Next Order (O(1))
                 │        ├── Serve Next (Dequeue O(1) ──> Print Receipt ──> Append History)
                 │        └── Cancel Order (Rotate Queue O(n) ──> Restore Stock)
                 │
                 ├── [3] Inventory Management
                 │        ├── View Master Catalog (Sorted by Code)
                 │        ├── Restock from Main Branch
                 │        └── Add Product (Maintains Code Order via Insertion Step)
                 │
                 ├── [4] Search Records
                 │        ├── Binary Search Product Code
                 │        ├── Linear Search Product Name
                 │        └── Binary Search Receipt by Order Number
                 │
                 └── [5] Managerial Reports
                          ├── Daily Sales Summary
                          ├── Best Sellers (Merge Sort on Copy)
                          └── Main Branch Synchronized Restocking Report
```

## 2.2 UML Class Diagram (ASCII & PlantUML Notation)

```
+-------------------------------------------------------------+
|                          Node<T>                            |
+-------------------------------------------------------------+
| - data: T                                                   |
| - next: Node<T>                                             |
+-------------------------------------------------------------+
| + Node(data: T)                                             |
| + getData(): T                                              |
| + setData(data: T): void                                    |
| + getNext(): Node<T>                                        |
| + setNext(next: Node<T>): void                              |
+-------------------------------------------------------------+
                              ▲
                              │ uses
+-------------------------------------------------------------+
|                        OrderQueue                           |
+-------------------------------------------------------------+
| - front: Node<Order>                                        |
| - rear: Node<Order>                                         |
| - size: int                                                 |
+-------------------------------------------------------------+
| + enqueue(order: Order): void                               |
| + dequeue(): Order                                          |
| + peek(): Order                                             |
| + cancel(orderNumber: int): Order                           |
| + isEmpty(): boolean                                        |
| + getSize(): int                                            |
| + getAllOrders(): List<Order>                               |
+-------------------------------------------------------------+

+-----------------------------------+   +------------------------------------+
|         SearchAlgorithms          |   |           SortAlgorithms           |
+-----------------------------------+   +------------------------------------+
| + binarySearchProductByCode()     |   | + insertionSortProductsByCode()    |
| + binarySearchOrderByNumber()     |   | + insertInSortedOrder()            |
| + linearSearchProductsByName()    |   | + mergeSort<T>()                   |
+-----------------------------------+   +------------------------------------+
```

---

# SECTION 3: CODE INVENTORY

## 3.1 Classes, Methods, and Responsibilities

| Class | Method | Parameters | Return Type | Responsibility / Description |
| :--- | :--- | :--- | :--- | :--- |
| `Node<T>` | `getData()` | none | `T` | Returns payload held by node |
| `Node<T>` | `setNext(Node<T>)` | `Node<T>` | `void` | Updates pointer to successor node |
| `OrderQueue` | `enqueue(Order)` | `Order order` | `void` | Inserts new order at rear ($O(1)$) |
| `OrderQueue` | `dequeue()` | none | `Order` | Removes and returns front order ($O(1)$) |
| `OrderQueue` | `peek()` | none | `Order` | Inspects front order without removing ($O(1)$) |
| `OrderQueue` | `cancel(int)` | `int orderNumber` | `Order` | Rotates queue to excise order ($O(n)$) |
| `SearchAlgorithms`| `binarySearchProductByCode` | `List<Product>, String` | `int` | Binary search index lookup ($O(\log n)$) |
| `SearchAlgorithms`| `binarySearchOrderByNumber` | `List<Order>, int` | `int` | Binary search order receipt ($O(\log n)$) |
| `SearchAlgorithms`| `linearSearchProductsByName`| `List<Product>, String` | `List<Product>` | Linear search partial string match ($O(n)$) |
| `SortAlgorithms`  | `insertionSortProductsByCode`| `List<Product>` | `void` | In-place insertion sort by product code |
| `SortAlgorithms`  | `insertInSortedOrder` | `List<Product>, Product` | `void` | Single insertion sort step maintaining invariant |
| `SortAlgorithms`  | `mergeSort` | `List<T>, Comparator<T>`| `List<T>` | Stable divide-and-conquer sort on copy ($O(n \log n)$) |
| `InventoryService`| `deductStock` | `String, int` | `boolean` | Deducts inventory immediately on order confirm |
| `InventoryService`| `restoreStock`| `String, int` | `boolean` | Restores inventory when order is cancelled |
| `OrderService`    | `confirmOrder`| `double cash` | `Order` | Validates, deducts stock, assigns #, enqueues |
| `ReportService`   | `getBestSellers` | none | `List<ProductSalesStat>` | Aggregates and sorts sales using Merge Sort |

## 3.2 OOP Principles Implemented
1. **Encapsulation:** All model properties (`Product`, `Order`, `OrderItem`, `Node`, `OrderQueue`) use `private` fields exposed via strict getters and setters, enforcing business constraints (such as preventing negative stock or invalid prices).
2. **Abstraction:** High-level services (`OrderService`, `InventoryService`) abstract complex underlying data structures (pointers, divide-and-conquer partitioning) behind intuitive APIs (`enqueue`, `serveNextOrder`, `findByCode`).
3. **Single Responsibility Principle (SRP):** Each class is strictly decoupled: `FileStorage` handles I/O, `OrderQueue` handles FIFO pointers, `SortAlgorithms` handles ordering, and `ConsoleUI` handles presentation formatting.

---

# SECTION 4: CONCEPTS OF DSA

## 4.1 Data Structure: Singly-Linked List Queue (`OrderQueue`)
- **Concept:** A sequential collection governed by FIFO (First-In, First-Out) discipline. Implemented using manual node links with `front` and `rear` reference pointers.
- **Why Chosen:** 
  - Standard queues implemented over arrays or `ArrayList` exhibit $O(n)$ dequeue costs because removing the element at index 0 requires shifting every remaining element leftward.
  - A linked-list queue achieves guaranteed $O(1)$ enqueue and $O(1)$ dequeue.
- **Complexity:**
  - Enqueue: $O(1)$
  - Dequeue: $O(1)$
  - Peek: $O(1)$
  - Cancel: $O(n)$ (via rotation)
- **Code Snippet:**
```java
public void enqueue(Order order) {
    Node<Order> newNode = new Node<>(order);
    if (isEmpty()) {
        front = newNode;
        rear = newNode;
    } else {
        rear.setNext(newNode);
        rear = newNode;
    }
    size++;
}

public Order dequeue() {
    if (isEmpty()) throw new IllegalStateException("Queue is empty.");
    Order servedOrder = front.getData();
    front = front.getNext();
    if (front == null) rear = null;
    size--;
    return servedOrder;
}
```

## 4.2 Searching Algorithm 1: Binary Search
- **Concept:** An efficient search algorithm that halves the search space with every comparison by checking the median element.
- **Why Chosen:** Lookups by product code (`KP-101`) occur on every single cashier transaction. Because the inventory is always sorted by code, Binary Search reduces search time from $O(n)$ to $O(\log n)$.
- **Complexity:** $O(\log n)$ Best, Average, and Worst Case; $O(1)$ auxiliary space.
- **Code Snippet:**
```java
public static int binarySearchProductByCode(List<Product> products, String targetCode) {
    int low = 0, high = products.size() - 1;
    String target = targetCode.trim().toUpperCase();
    while (low <= high) {
        int mid = low + (high - low) / 2;
        int cmp = products.get(mid).getCode().compareToIgnoreCase(target);
        if (cmp == 0) return mid;
        else if (cmp < 0) low = mid + 1;
        else high = mid - 1;
    }
    return -1;
}
```

## 4.3 Searching Algorithm 2: Linear Search
- **Concept:** Sequential traversal evaluating every element from index 0 to $n-1$.
- **Why Chosen:** When cashiers search with partial name keywords (e.g. "oversized", "hoodie"), product codes and names are not prefix-sorted against the query. Linear Search is the only algorithmic approach capable of matching substrings across an unsorted text attribute.
- **Complexity:** $O(n)$ Time, $O(1)$ auxiliary space.

## 4.4 Sorting Algorithm 1: Insertion Sort
- **Concept:** Builds the final sorted array one item at a time by repeatedly shifting larger elements to the right to insert the target key into its correct relative position.
- **Why Chosen:** Product inventory loaded from CSV or modified with new items is already mostly sorted. Insertion Sort exhibits an exceptional $O(n)$ adaptive time complexity on nearly-sorted lists and operates in-place without memory overhead.
- **Complexity:** Best Case $O(n)$ (nearly sorted), Worst Case $O(n^2)$, Space $O(1)$.
- **Code Snippet:**
```java
public static void insertionSortProductsByCode(List<Product> products) {
    for (int i = 1; i < products.size(); i++) {
        Product key = products.get(i);
        int j = i - 1;
        while (j >= 0 && products.get(j).getCode().compareToIgnoreCase(key.getCode()) > 0) {
            products.set(j + 1, products.get(j));
            j--;
        }
        products.set(j + 1, key);
    }
}
```

## 4.5 Sorting Algorithm 2: Merge Sort
- **Concept:** A stable Divide-and-Conquer sorting algorithm that recursively bisects a list into halves, sorts each half, and merges the sorted sub-lists into a unified collection.
- **Why Chosen:** Generating managerial reports (Best Sellers and Low-Stock Lists) requires sorting by dynamic attributes (units sold, stock quantity). Sorting the master inventory would break binary search for product codes. Therefore, Merge Sort is executed on defensive copies. Its guaranteed $O(n \log n)$ time and stability ensure consistent reporting.
- **Complexity:** $O(n \log n)$ across all cases; $O(n)$ auxiliary space.
- **Code Snippet:**
```java
public static <T> List<T> mergeSort(List<T> list, Comparator<T> comparator) {
    List<T> copy = new ArrayList<>(list);
    if (copy.size() <= 1) return copy;
    mergeSortRecursive(copy, 0, copy.size() - 1, comparator);
    return copy;
}
```
