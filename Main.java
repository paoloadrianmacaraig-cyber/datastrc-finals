import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * ============================================================================
 * KINGPIN MNL - POS & ORDER QUEUE SYSTEM
 * DSA Finals Project (C2B)
 * ============================================================================
 * Complete Self-Contained System:
 * - Data Structure: Custom Singly-Linked List Queue (Front & Rear pointers, FIFO)
 * - Searching: Iterative Binary Search O(log n) & Linear Search O(n)
 * - Sorting: Insertion Sort O(n^2) & Merge Sort O(n log n) on defensive copies
 * - Fulfillment: Immediate stock deduction on confirm, cancellation via queue rotation
 * - Persistence: Self-seeding CSV storage for cross-session reporting
 * ============================================================================
 */
public class Main {
    private static final String INVENTORY_FILE = "inventory.csv";
    private static final String SALES_HISTORY_FILE = "sales_history.csv";

    private final InventoryManager inventoryManager;
    private final OrderManager orderManager;
    private final Scanner scanner;

    public Main() {
        this.inventoryManager = new InventoryManager(INVENTORY_FILE);
        this.orderManager = new OrderManager(SALES_HISTORY_FILE, inventoryManager);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        printBanner();

        List<Product> lowStock = inventoryManager.getLowStockProducts();
        if (!lowStock.isEmpty()) {
            printWarning("ALERT: " + lowStock.size() + " product(s) currently at or below low-stock threshold!");
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Select an option (1-7): ");
            switch (choice.trim()) {
                case "1":
                    handleTakeOrderMenu();
                    break;
                case "2":
                    handleOrderQueueMenu();
                    break;
                case "3":
                    handleInventoryMenu();
                    break;
                case "4":
                    handleSearchMenu();
                    break;
                case "5":
                    handleReportsMenu();
                    break;
                case "6":
                    runDSAVerificationTests();
                    break;
                case "7":
                    printInfo("Exiting Kingpin MNL POS. All transactions saved. Goodbye!");
                    running = false;
                    break;
                default:
                    printError("Invalid choice. Please enter a number between 1 and 7.");
            }
        }
    }

    // =========================================================================
    // MENUS
    // =========================================================================

    private void printMainMenu() {
        System.out.println("\n" + BOLD + "================ MAIN MENU ================" + RESET);
        System.out.println(" [1] Take Order (POS Cashier & Cart)");
        System.out.println(" [2] Order Queue (Serve / Peek / Cancel via Rotation)");
        System.out.println(" [3] Inventory Management (Restock / Add / Edit)");
        System.out.println(" [4] Search Records (Binary & Linear Search)");
        System.out.println(" [5] Managerial Reports (Merge Sort Best Sellers / Daily Sales)");
        System.out.println(" [6] Run DSA Verification Test Suite");
        System.out.println(" [7] Exit Application");
        System.out.println(BOLD + "===========================================" + RESET);
        int pending = orderManager.getPendingCount();
        if (pending > 0) {
            System.out.println(YELLOW + "Active Orders in Queue: " + pending + RESET);
        }
    }

    // 1. TAKE ORDER
    private void handleTakeOrderMenu() {
        boolean inOrder = true;
        while (inOrder) {
            printHeader("POS - ORDER TAKING");
            displayCart(orderManager.getCart(), orderManager.getCartTotal());

            System.out.println("\nActions:");
            System.out.println(" [1] Add Product to Cart by Code [Binary Search O(log n)]");
            System.out.println(" [2] Search Product by Name [Linear Search O(n)] & Add");
            System.out.println(" [3] Remove Product from Cart");
            System.out.println(" [4] Clear Cart");
            System.out.println(" [5] Confirm Order & Enqueue [Deduct Stock -> Assign Order #]");
            System.out.println(" [6] Return to Main Menu");

            String choice = prompt("Select action (1-6): ");
            switch (choice.trim()) {
                case "1":
                    String code = prompt("Enter Product Code (e.g. KP-101): ").toUpperCase();
                    Product p = inventoryManager.findByCode(code);
                    if (p == null) {
                        printError("Product '" + code + "' not found.");
                        break;
                    }
                    System.out.printf("Selected: %s | Price: PHP %.2f | Stock Available: %d\n", p.getName(), p.getPrice(), p.getStock());
                    int qty = promptInt("Enter Quantity: ");
                    if (qty <= 0) {
                        printError("Quantity must be greater than zero.");
                    } else if (orderManager.addToCart(code, qty)) {
                        printSuccess("Added " + qty + "x " + p.getName() + " to cart.");
                    } else {
                        printError("Insufficient stock (Available: " + p.getStock() + ").");
                    }
                    break;

                case "2":
                    String query = prompt("Enter product name keyword: ");
                    List<Product> matches = inventoryManager.searchByName(query);
                    if (matches.isEmpty()) {
                        printWarning("No products matched '" + query + "'.");
                    } else {
                        displayProductTable(matches);
                        String addCode = prompt("Enter Product Code to add (or press Enter to cancel): ").toUpperCase();
                        if (!addCode.isEmpty()) {
                            Product sel = inventoryManager.findByCode(addCode);
                            if (sel != null) {
                                int addQty = promptInt("Enter Quantity: ");
                                if (orderManager.addToCart(addCode, addQty)) {
                                    printSuccess("Added to cart.");
                                } else {
                                    printError("Insufficient stock or invalid quantity.");
                                }
                            } else {
                                printError("Invalid product code.");
                            }
                        }
                    }
                    break;

                case "3":
                    if (orderManager.getCart().isEmpty()) {
                        printWarning("Cart is empty.");
                    } else {
                        String remCode = prompt("Enter Product Code to remove: ").toUpperCase();
                        if (orderManager.removeFromCart(remCode)) {
                            printSuccess("Item removed from cart.");
                        } else {
                            printError("Item not found in cart.");
                        }
                    }
                    break;

                case "4":
                    orderManager.clearCart();
                    printInfo("Cart cleared.");
                    break;

                case "5":
                    if (orderManager.getCart().isEmpty()) {
                        printError("Cannot confirm an empty cart.");
                        break;
                    }
                    double total = orderManager.getCartTotal();
                    System.out.printf(BOLD + "Total Due: PHP %,.2f\n" + RESET, total);
                    double cash = promptDouble("Enter Cash Tendered (PHP): ");
                    if (cash < total) {
                        printError(String.format("Insufficient cash. PHP %.2f still required.", (total - cash)));
                        break;
                    }
                    try {
                        Order confirmed = orderManager.confirmOrder(cash);
                        printSuccess("Order #" + confirmed.getOrderNumber() + " CONFIRMED and enqueued into Order Queue!");
                        System.out.printf("Change: PHP %,.2f\n", confirmed.getChange());
                        printInfo("Stock deducted immediately to prevent overselling.");
                        inOrder = false;
                    } catch (Exception e) {
                        printError("Order confirmation failed: " + e.getMessage());
                    }
                    break;

                case "6":
                    inOrder = false;
                    break;

                default:
                    printError("Invalid option.");
            }
        }
    }

    // 2. ORDER QUEUE
    private void handleOrderQueueMenu() {
        boolean inQueue = true;
        while (inQueue) {
            printHeader("ORDER QUEUE MANAGEMENT (FIFO)");
            displayQueue(orderManager.getPendingOrders());

            System.out.println("\nActions:");
            System.out.println(" [1] Peek Next Order in Queue [O(1)]");
            System.out.println(" [2] Serve Next Order [O(1) Dequeue -> Print Receipt -> Append History]");
            System.out.println(" [3] Cancel Order [O(n) Queue Rotation -> Restore Stock]");
            System.out.println(" [4] Return to Main Menu");

            String choice = prompt("Select action (1-4): ");
            switch (choice.trim()) {
                case "1":
                    Order next = orderManager.peekOrder();
                    if (next == null) {
                        printInfo("No pending orders in the queue.");
                    } else {
                        printHeader("NEXT IN LINE (FRONT OF QUEUE)");
                        System.out.printf("Order Number : #%d\n", next.getOrderNumber());
                        System.out.printf("Placed At    : %s\n", next.getTimestamp());
                        System.out.printf("Total Amount : PHP %,.2f\n", next.getTotalAmount());
                        System.out.printf("Total Items  : %d\n", next.getTotalUnits());
                        for (OrderItem it : next.getItems()) {
                            System.out.printf("  - %dx %s (PHP %.2f)\n", it.getQuantity(), it.getProductName(), it.getSubtotal());
                        }
                    }
                    break;

                case "2":
                    Order served = orderManager.serveNextOrder();
                    if (served == null) {
                        printWarning("Queue is empty. No pending orders to serve.");
                    } else {
                        printSuccess("Order #" + served.getOrderNumber() + " has been SERVED successfully!");
                        printReceipt(served);
                    }
                    break;

                case "3":
                    if (orderManager.getPendingCount() == 0) {
                        printWarning("No pending orders available to cancel.");
                        break;
                    }
                    int cancelNum = promptInt("Enter Order Number to cancel: ");
                    Order cancelled = orderManager.cancelOrder(cancelNum);
                    if (cancelled != null) {
                        printSuccess("Order #" + cancelNum + " CANCELLED via FIFO queue rotation!");
                        printInfo("Restored stock to inventory:");
                        for (OrderItem it : cancelled.getItems()) {
                            System.out.printf("  - Restored +%d units to [%s] %s\n", it.getQuantity(), it.getProductCode(), it.getProductName());
                        }
                    } else {
                        printError("Order #" + cancelNum + " was not found in the active pending queue.");
                    }
                    break;

                case "4":
                    inQueue = false;
                    break;

                default:
                    printError("Invalid option.");
            }
        }
    }

    // 3. INVENTORY
    private void handleInventoryMenu() {
        boolean inInv = true;
        while (inInv) {
            printHeader("INVENTORY MANAGEMENT (Code-Sorted)");
            System.out.println(" [1] View Master Inventory Table");
            System.out.println(" [2] Record Stock Received from Main Branch (Restock)");
            System.out.println(" [3] Add New Product [Insertion Step maintains sort]");
            System.out.println(" [4] Edit Product Details (Price, Name, Threshold)");
            System.out.println(" [5] Remove Product from Catalog");
            System.out.println(" [6] View Low-Stock Alerts");
            System.out.println(" [7] Return to Main Menu");

            String choice = prompt("Select action (1-7): ");
            switch (choice.trim()) {
                case "1":
                    displayProductTable(inventoryManager.getAllProducts());
                    break;

                case "2":
                    printHeader("RESTOCK FROM MAIN BRANCH");
                    String rCode = prompt("Enter Product Code to restock: ").toUpperCase();
                    Product rProduct = inventoryManager.findByCode(rCode);
                    if (rProduct == null) {
                        printError("Product code not found.");
                        break;
                    }
                    System.out.printf("Current Stock for [%s] %s: %d units\n", rCode, rProduct.getName(), rProduct.getStock());
                    int addedQty = promptInt("Enter quantity received from Main Branch: ");
                    if (inventoryManager.recordStockReceived(rCode, addedQty)) {
                        printSuccess("Restocked successfully. New stock: " + rProduct.getStock() + " units.");
                    } else {
                        printError("Invalid quantity.");
                    }
                    break;

                case "3":
                    printHeader("ADD NEW PRODUCT");
                    String newCode = prompt("Enter Product Code (e.g. KP-113): ").toUpperCase();
                    if (inventoryManager.findByCode(newCode) != null) {
                        printError("A product with code '" + newCode + "' already exists!");
                        break;
                    }
                    String newName = prompt("Enter Product Name: ");
                    String newCat = prompt("Enter Category: ");
                    double newPrice = promptDouble("Enter Unit Price (PHP): ");
                    int newStock = promptInt("Enter Initial Stock: ");
                    int newThresh = promptInt("Enter Low-Stock Threshold: ");

                    Product np = new Product(newCode, newName, newCat, newPrice, newStock, newThresh);
                    if (inventoryManager.addProduct(np)) {
                        printSuccess("Product [" + newCode + "] added in sorted order!");
                    } else {
                        printError("Failed to add product.");
                    }
                    break;

                case "4":
                    printHeader("EDIT PRODUCT DETAILS");
                    String eCode = prompt("Enter Product Code to edit: ").toUpperCase();
                    Product eP = inventoryManager.findByCode(eCode);
                    if (eP == null) {
                        printError("Product code not found.");
                        break;
                    }
                    String editName = prompt("New Name (or Enter to keep '" + eP.getName() + "'): ");
                    if (editName.trim().isEmpty()) editName = eP.getName();

                    String editCat = prompt("New Category (or Enter to keep '" + eP.getCategory() + "'): ");
                    if (editCat.trim().isEmpty()) editCat = eP.getCategory();

                    String priceInput = prompt("New Price (or Enter to keep " + eP.getPrice() + "): ");
                    double editPrice = priceInput.trim().isEmpty() ? eP.getPrice() : Double.parseDouble(priceInput.trim());

                    String threshInput = prompt("New Threshold (or Enter to keep " + eP.getLowStockThreshold() + "): ");
                    int editThresh = threshInput.trim().isEmpty() ? eP.getLowStockThreshold() : Integer.parseInt(threshInput.trim());

                    if (inventoryManager.updateProduct(eCode, editName, editCat, editPrice, editThresh)) {
                        printSuccess("Product details updated.");
                    } else {
                        printError("Failed to update product.");
                    }
                    break;

                case "5":
                    printHeader("REMOVE PRODUCT");
                    String delCode = prompt("Enter Product Code to remove: ").toUpperCase();
                    Product delP = inventoryManager.findByCode(delCode);
                    if (delP == null) {
                        printError("Product code not found.");
                        break;
                    }
                    String confirm = prompt("Delete '" + delP.getName() + "'? (Y/N): ");
                    if ("Y".equalsIgnoreCase(confirm.trim())) {
                        if (inventoryManager.removeProduct(delCode)) {
                            printSuccess("Product removed from catalog.");
                        } else {
                            printError("Failed to remove product.");
                        }
                    }
                    break;

                case "6":
                    List<Product> low = inventoryManager.getLowStockProducts();
                    printHeader("LOW-STOCK PRODUCTS (" + low.size() + " items)");
                    displayProductTable(low);
                    break;

                case "7":
                    inInv = false;
                    break;

                default:
                    printError("Invalid option.");
            }
        }
    }

    // 4. SEARCH
    private void handleSearchMenu() {
        boolean inSearch = true;
        while (inSearch) {
            printHeader("SEARCH ENGINE (DSA Algorithms)");
            System.out.println(" [1] Binary Search Product by Code [O(log n)]");
            System.out.println(" [2] Linear Search Product by Name [O(n)]");
            System.out.println(" [3] Binary Search Receipt by Order Number [O(log n)]");
            System.out.println(" [4] Return to Main Menu");

            String choice = prompt("Select action (1-4): ");
            switch (choice.trim()) {
                case "1":
                    String code = prompt("Enter Product Code (e.g. KP-105): ").toUpperCase();
                    long startB = System.nanoTime();
                    Product p = inventoryManager.findByCode(code);
                    long endB = System.nanoTime();
                    if (p != null) {
                        printSuccess(String.format("Found in %d ns via Binary Search (O(log n)):", (endB - startB)));
                        System.out.println("  Code:        " + p.getCode());
                        System.out.println("  Name:        " + p.getName());
                        System.out.println("  Category:    " + p.getCategory());
                        System.out.printf("  Price:       PHP %,.2f\n", p.getPrice());
                        System.out.printf("  Stock:       %d units (Threshold: %d)\n", p.getStock(), p.getLowStockThreshold());
                    } else {
                        printWarning("Product [" + code + "] not found.");
                    }
                    break;

                case "2":
                    String query = prompt("Enter name keyword: ");
                    long startL = System.nanoTime();
                    List<Product> matches = inventoryManager.searchByName(query);
                    long endL = System.nanoTime();
                    printInfo(String.format("Linear Search completed in %d ns. Matches: %d", (endL - startL), matches.size()));
                    displayProductTable(matches);
                    break;

                case "3":
                    int ordNum = promptInt("Enter Order Number in history: ");
                    long startO = System.nanoTime();
                    Order found = orderManager.findOrderInHistory(ordNum);
                    long endO = System.nanoTime();
                    if (found != null) {
                        printSuccess(String.format("Found in %d ns via Binary Search on order history:", (endO - startO)));
                        printReceipt(found);
                    } else {
                        printWarning("Order #" + ordNum + " not found in completed sales history.");
                    }
                    break;

                case "4":
                    inSearch = false;
                    break;

                default:
                    printError("Invalid option.");
            }
        }
    }

    // 5. REPORTS
    private void handleReportsMenu() {
        boolean inReports = true;
        while (inReports) {
            printHeader("MANAGERIAL REPORTS & ANALYTICS");
            System.out.println(" [1] Daily Sales Summary");
            System.out.println(" [2] Best Sellers Ranking [Merge Sort O(n log n)]");
            System.out.println(" [3] Low-Stock Priority Restock List [Merge Sort O(n log n)]");
            System.out.println(" [4] Main Branch Synchronized Restock Summary");
            System.out.println(" [5] Return to Main Menu");

            String choice = prompt("Select report (1-5): ");
            switch (choice.trim()) {
                case "1":
                    String date = prompt("Enter Date (YYYY-MM-DD) or press Enter for All: ").trim();
                    printHeader("SALES SUMMARY: " + (date.isEmpty() ? "ALL DATES" : date));
                    int count = 0, units = 0;
                    double rev = 0;
                    for (Order o : orderManager.getSalesHistory()) {
                        if ("SERVED".equalsIgnoreCase(o.getStatus())) {
                            if (date.isEmpty() || o.getTimestamp().startsWith(date)) {
                                count++;
                                rev += o.getTotalAmount();
                                units += o.getTotalUnits();
                                System.out.printf("  Order #%-5d | %s | %2d items | PHP %,10.2f\n",
                                        o.getOrderNumber(), o.getTimestamp(), o.getTotalUnits(), o.getTotalAmount());
                            }
                        }
                    }
                    System.out.printf("\nTotal Orders: %d | Units Dispatched: %d | Total Revenue: PHP %,.2f\n",
                            count, units, rev);
                    break;

                case "2":
                    printHeader("BEST SELLERS (SORTED VIA MERGE SORT BY UNITS SOLD)");
                    List<ProductSalesStat> stats = orderManager.getBestSellersStats();
                    if (stats.isEmpty()) {
                        printInfo("No sales records available yet.");
                    } else {
                        System.out.println(BOLD + "+------+------------+-----------------------------------+------------+------------------+" + RESET);
                        System.out.printf(BOLD + "| %-4s | %-10s | %-33s | %10s | %16s |%n" + RESET,
                                "RANK", "CODE", "PRODUCT NAME", "UNITS SOLD", "REVENUE (PHP)");
                        System.out.println("+------+------------+-----------------------------------+------------+------------------+");
                        int rank = 1;
                        for (ProductSalesStat stat : stats) {
                            String name = stat.name.length() > 33 ? stat.name.substring(0, 30) + "..." : stat.name;
                            System.out.printf("| %-4d | %-10s | %-33s | %10d | PHP %,12.2f |%n",
                                    rank++, stat.code, name, stat.unitsSold, stat.revenue);
                        }
                        System.out.println("+------+------------+-----------------------------------+------------+------------------+");
                    }
                    break;

                case "3":
                    printHeader("LOW-STOCK RESTOCK PRIORITY (SORTED VIA MERGE SORT BY STOCK)");
                    List<Product> lowStockSorted = SortAlgorithms.mergeSort(
                            inventoryManager.getLowStockProducts(),
                            Comparator.comparingInt(Product::getStock));
                    if (lowStockSorted.isEmpty()) {
                        printSuccess("All products are well-stocked!");
                    } else {
                        displayProductTable(lowStockSorted);
                    }
                    break;

                case "4":
                    printMainBranchReport();
                    break;

                case "5":
                    inReports = false;
                    break;

                default:
                    printError("Invalid option.");
            }
        }
    }

    private void printMainBranchReport() {
        System.out.println("\n========================================================================");
        System.out.println("         KINGPIN MNL (SUB-BRANCH) -> MAIN BRANCH SALES REPORT            ");
        System.out.println("========================================================================");
        int totalOrders = 0, totalUnits = 0;
        double totalRev = 0;
        for (Order o : orderManager.getSalesHistory()) {
            if ("SERVED".equalsIgnoreCase(o.getStatus())) {
                totalOrders++;
                totalUnits += o.getTotalUnits();
                totalRev += o.getTotalAmount();
            }
        }
        System.out.printf("Total Completed Orders : %d\n", totalOrders);
        System.out.printf("Total Units Dispatched : %d items\n", totalUnits);
        System.out.printf("Total Gross Revenue    : PHP %,.2f\n\n", totalRev);

        System.out.println("--- [MERGE SORT] TOP 5 BEST SELLERS ---");
        List<ProductSalesStat> stats = orderManager.getBestSellersStats();
        int r = 1;
        for (ProductSalesStat s : stats) {
            System.out.printf("#%d  %-10s %-30s | Sold: %3d | Sales: PHP %,10.2f\n",
                    r++, s.code, s.name.length() > 30 ? s.name.substring(0, 27) + "..." : s.name, s.unitsSold, s.revenue);
            if (r > 5) break;
        }

        System.out.println("\n--- [MERGE SORT] CRITICAL RESTOCK REQUESTS ---");
        List<Product> low = SortAlgorithms.mergeSort(inventoryManager.getLowStockProducts(), Comparator.comparingInt(Product::getStock));
        if (low.isEmpty()) {
            System.out.println("All inventory items currently above safety threshold.");
        } else {
            for (Product p : low) {
                int needed = (p.getLowStockThreshold() * 3) - p.getStock();
                System.out.printf("CRITICAL: [%-8s] %-30s | Stock: %2d (Min: %2d) -> Requesting Restock: +%d units\n",
                        p.getCode(), p.getName().length() > 30 ? p.getName().substring(0, 27) + "..." : p.getName(),
                        p.getStock(), p.getLowStockThreshold(), Math.max(needed, 10));
            }
        }
        System.out.println("========================================================================\n");
    }

    // 6. RUN DSA TESTS
    public static void runDSAVerificationTests() {
        System.out.println("\n" + BOLD + "==================================================");
        System.out.println("  RUNNING KINGPIN MNL DSA VERIFICATION TEST SUITE ");
        System.out.println("==================================================" + RESET);
        int passed = 0;

        // Test 1: Queue FIFO
        System.out.print("[TEST 1] OrderQueue Enqueue, Dequeue, Peek (O(1))... ");
        OrderQueue q = new OrderQueue();
        q.enqueue(new Order(1, "2026-10-20 10:00:00"));
        q.enqueue(new Order(2, "2026-10-20 10:01:00"));
        q.enqueue(new Order(3, "2026-10-20 10:02:00"));
        if (q.getSize() == 3 && q.peek().getOrderNumber() == 1 &&
            q.dequeue().getOrderNumber() == 1 &&
            q.dequeue().getOrderNumber() == 2 &&
            q.dequeue().getOrderNumber() == 3 && q.isEmpty()) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        // Test 2: Cancel by Rotation
        System.out.print("[TEST 2] OrderQueue Cancel by Rotation (O(n))... ");
        OrderQueue q2 = new OrderQueue();
        q2.enqueue(new Order(201, "2026-10-20 11:00:00"));
        q2.enqueue(new Order(202, "2026-10-20 11:01:00"));
        q2.enqueue(new Order(203, "2026-10-20 11:02:00"));
        Order cancelled = q2.cancel(202);
        if (cancelled != null && cancelled.getOrderNumber() == 202 && q2.getSize() == 2 &&
            q2.dequeue().getOrderNumber() == 201 && q2.dequeue().getOrderNumber() == 203) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        // Test 3: Binary Search
        System.out.print("[TEST 3] Binary Search Product Code & Order # (O(log n))... ");
        List<Product> pList = new ArrayList<>();
        pList.add(new Product("KP-101", "A", "Apparel", 100, 10, 2));
        pList.add(new Product("KP-105", "B", "Apparel", 200, 10, 2));
        pList.add(new Product("KP-109", "C", "Apparel", 300, 10, 2));
        if (SearchAlgorithms.binarySearchProductByCode(pList, "KP-105") == 1 &&
            SearchAlgorithms.binarySearchProductByCode(pList, "KP-999") == -1) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        // Test 4: Linear Search
        System.out.print("[TEST 4] Linear Search by partial name (O(n))... ");
        List<Product> lList = new ArrayList<>();
        lList.add(new Product("KP-101", "Kingpin Classic Tee", "Apparel", 100, 10, 2));
        lList.add(new Product("KP-102", "Underground Hoodie", "Apparel", 200, 10, 2));
        if (SearchAlgorithms.linearSearchProductsByName(lList, "hoodie").size() == 1) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        // Test 5: Insertion Sort
        System.out.print("[TEST 5] Insertion Sort on Product Codes... ");
        List<Product> sList = new ArrayList<>();
        sList.add(new Product("KP-109", "D", "Apparel", 100, 10, 2));
        sList.add(new Product("KP-101", "A", "Apparel", 100, 10, 2));
        sList.add(new Product("KP-105", "C", "Apparel", 100, 10, 2));
        SortAlgorithms.insertionSortProductsByCode(sList);
        if (sList.get(0).getCode().equals("KP-101") && sList.get(1).getCode().equals("KP-105") && sList.get(2).getCode().equals("KP-109")) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        // Test 6: Merge Sort
        System.out.print("[TEST 6] Merge Sort on defensive copy (O(n log n))... ");
        List<Product> mList = new ArrayList<>();
        mList.add(new Product("KP-1", "A", "Apparel", 100, 25, 2));
        mList.add(new Product("KP-2", "B", "Apparel", 100, 3, 2));
        mList.add(new Product("KP-3", "C", "Apparel", 100, 12, 2));
        List<Product> sortedM = SortAlgorithms.mergeSort(mList, Comparator.comparingInt(Product::getStock));
        if (mList.get(0).getStock() == 25 && sortedM.get(0).getStock() == 3 && sortedM.get(2).getStock() == 25) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        // Test 7: Order Lifecycle & Stock Balance
        System.out.print("[TEST 7] Order Lifecycle & Stock Deduction/Restoration... ");
        InventoryManager im = new InventoryManager("test_inv.csv");
        OrderManager om = new OrderManager("test_sales.csv", im);
        om.addToCart("KP-101", 2);
        int initialStock = im.findByCode("KP-101").getStock();
        Order o = om.confirmOrder(5000);
        int afterConfirmStock = im.findByCode("KP-101").getStock();
        om.cancelOrder(o.getOrderNumber());
        int afterCancelStock = im.findByCode("KP-101").getStock();

        new File("test_inv.csv").delete();
        new File("test_sales.csv").delete();

        if (afterConfirmStock == initialStock - 2 && afterCancelStock == initialStock) {
            System.out.println(GREEN + "PASSED ✔" + RESET);
            passed++;
        } else {
            System.out.println(RED + "FAILED ✖" + RESET);
        }

        System.out.println("==================================================");
        System.out.printf("Test Results: %d / 7 passed successfully!\n", passed);
        System.out.println("==================================================\n");
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";

    public static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("  _  _______ _   _  _____ _____ _____ _   _   __  __ _   _ _      ");
        System.out.println(" | |/ /_   _| \\ | |/ ____|  __ \\_   _| \\ | | |  \\/  | \\ | | |     ");
        System.out.println(" | ' /  | | |  \\| | |  __| |__) || | |  \\| | | \\  / |  \\| | |     ");
        System.out.println(" |  <   | | | . ` | | |_ |  ___/ | | | . ` | | |\\/| | . ` | |     ");
        System.out.println(" | . \\ _| |_| |\\  | |__| | |    _| |_| |\\  | | |  | | |\\  | |____ ");
        System.out.println(" |_|\\_\\_____|_| \\_|\\_____|_|   |_____|_| \\_| |_|  |_|_| \\_|______|");
        System.out.println("       --- STREETWEAR & APPAREL POS & ORDER QUEUE SYSTEM ---      " + RESET);
        System.out.println(" [DSA Finals] Custom LinkedList Queue | Binary & Linear Search | Insertion & Merge Sort\n");
    }

    public static void printHeader(String title) {
        System.out.println("\n" + CYAN + BOLD + "=== " + title + " ===" + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + "⚠ " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + "✖ " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "ℹ " + message + RESET);
    }

    public static void displayProductTable(List<Product> products) {
        if (products == null || products.isEmpty()) {
            printWarning("No products found.");
            return;
        }

        System.out.println(BOLD + "+------------+----------------------------------------+---------------+------------+-------+--------+" + RESET);
        System.out.printf(BOLD + "| %-10s | %-38s | %-13s | %10s | %5s | %6s |%n" + RESET,
                "CODE", "PRODUCT NAME", "CATEGORY", "PRICE (PHP)", "STOCK", "STATUS");
        System.out.println("+------------+----------------------------------------+---------------+------------+-------+--------+");

        for (Product p : products) {
            String name = p.getName().length() > 38 ? p.getName().substring(0, 35) + "..." : p.getName();
            String status = p.isLowStock() ? RED + "LOW" + RESET : GREEN + "OK" + RESET;
            System.out.printf("| %-10s | %-38s | %-13s | %10.2f | %5d |   %-4s |%n",
                    p.getCode(), name, p.getCategory(), p.getPrice(), p.getStock(), status);
        }
        System.out.println("+------------+----------------------------------------+---------------+------------+-------+--------+");
        System.out.printf("Total Products: %d\n", products.size());
    }

    public static void displayCart(List<OrderItem> cart, double total) {
        if (cart == null || cart.isEmpty()) {
            printInfo("Current cart is empty.");
            return;
        }

        printHeader("ACTIVE SHOPPING CART");
        System.out.println(BOLD + "+------------+-----------------------------------+-------+------------+------------+" + RESET);
        System.out.printf(BOLD + "| %-10s | %-33s | %5s | %10s | %10s |%n" + RESET,
                "CODE", "PRODUCT NAME", "QTY", "UNIT (PHP)", "TOTAL (PHP)");
        System.out.println("+------------+-----------------------------------+-------+------------+------------+");

        for (OrderItem item : cart) {
            String name = item.getProductName().length() > 33 ? item.getProductName().substring(0, 30) + "..." : item.getProductName();
            System.out.printf("| %-10s | %-33s | %5d | %10.2f | %10.2f |%n",
                    item.getProductCode(), name, item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        System.out.println("+------------+-----------------------------------+-------+------------+------------+");
        System.out.printf(BOLD + "                                               SUBTOTAL: PHP %,10.2f\n" + RESET, total);
    }

    public static void displayQueue(List<Order> queue) {
        if (queue == null || queue.isEmpty()) {
            printInfo("The order queue is currently EMPTY. No orders waiting to be served.");
            return;
        }

        printHeader("ORDER QUEUE (FRONT -> REAR) [Custom Singly-Linked Queue]");
        System.out.println(BOLD + "+-------+-------+---------------------+-------+------------+------------+" + RESET);
        System.out.printf(BOLD + "| POS   | ORD # | TIMESTAMP           | ITEMS | TOTAL (PHP)| STATUS     |%n" + RESET);
        System.out.println("+-------+-------+---------------------+-------+------------+------------+");

        for (int i = 0; i < queue.size(); i++) {
            Order o = queue.get(i);
            String pos = (i == 0) ? (GREEN + "FRONT" + RESET) : (i == queue.size() - 1 ? (CYAN + "REAR " + RESET) : String.format("#%02d  ", i + 1));
            System.out.printf("| %-5s | %-5d | %-19s | %5d | %10.2f | %-10s |%n",
                    pos, o.getOrderNumber(), o.getTimestamp(), o.getTotalUnits(), o.getTotalAmount(), o.getStatus());
        }
        System.out.println("+-------+-------+---------------------+-------+------------+------------+");
        System.out.printf("Pending Orders Waiting: %d\n", queue.size());
    }

    public static void printReceipt(Order order) {
        System.out.println("\n" + BOLD + "==================================================");
        System.out.println("                 KINGPIN MANILA                   ");
        System.out.println("             Sub-Branch Retail Store              ");
        System.out.println("==================================================" + RESET);
        System.out.printf("Receipt / Order No : #%d\n", order.getOrderNumber());
        System.out.printf("Date & Time        : %s\n", order.getTimestamp());
        System.out.printf("Status             : %s\n", order.getStatus());
        System.out.println("--------------------------------------------------");
        System.out.println(BOLD + "ITEM                               QTY   AMOUNT" + RESET);
        System.out.println("--------------------------------------------------");

        for (OrderItem item : order.getItems()) {
            String name = item.getProductName().length() > 24 ? item.getProductName().substring(0, 21) + "..." : item.getProductName();
            System.out.printf("%-24s %3d  PHP %8.2f\n", name, item.getQuantity(), item.getSubtotal());
        }

        System.out.println("--------------------------------------------------");
        System.out.printf(BOLD + "TOTAL AMOUNT DUE                 : PHP %10.2f\n" + RESET, order.getTotalAmount());
        System.out.printf("CASH TENDERED                    : PHP %10.2f\n", order.getCashReceived());
        System.out.printf(BOLD + "CHANGE RETURNED                  : PHP %10.2f\n" + RESET, order.getChange());
        System.out.println("==================================================");
        System.out.println("         THANK YOU FOR SUPPORTING LOCAL!          ");
        System.out.println("    Follow us on IG / TikTok: @kingpin.mnl        ");
        System.out.println("==================================================\n");
    }

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private int promptInt(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                printError("Invalid integer. Please try again.");
            }
        }
    }

    private double promptDouble(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                printError("Invalid decimal amount. Please try again.");
            }
        }
    }
}

// ============================================================================
// DATA STRUCTURE: GENERIC LINKED LIST NODE
// ============================================================================
class Node<T> {
    private T data;
    private Node<T> next;

    public Node(T data) {
        this.data = data;
        this.next = null;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Node<T> getNext() { return next; }
    public void setNext(Node<T> next) { this.next = next; }
}

// ============================================================================
// DATA STRUCTURE: CUSTOM ORDER QUEUE (FIFO WITH FRONT & REAR POINTERS)
// ============================================================================
class OrderQueue {
    private Node<Order> front;
    private Node<Order> rear;
    private int size;

    public OrderQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    public void enqueue(Order order) {
        if (order == null) throw new IllegalArgumentException("Cannot enqueue null order.");
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
        Order served = front.getData();
        front = front.getNext();
        if (front == null) rear = null;
        size--;
        return served;
    }

    public Order peek() {
        return isEmpty() ? null : front.getData();
    }

    public Order cancel(int orderNumber) {
        if (isEmpty()) return null;
        int currentSize = size;
        Order removed = null;
        for (int i = 0; i < currentSize; i++) {
            Order cur = dequeue();
            if (cur.getOrderNumber() == orderNumber && removed == null) {
                removed = cur;
                removed.setStatus("CANCELLED");
            } else {
                enqueue(cur);
            }
        }
        return removed;
    }

    public boolean isEmpty() { return front == null; }
    public int getSize() { return size; }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        Node<Order> cur = front;
        while (cur != null) {
            list.add(cur.getData());
            cur = cur.getNext();
        }
        return list;
    }
}

// ============================================================================
// SEARCH ALGORITHMS (BINARY SEARCH & LINEAR SEARCH)
// ============================================================================
class SearchAlgorithms {

    public static int binarySearchProductByCode(List<Product> products, String targetCode) {
        if (products == null || targetCode == null || products.isEmpty()) return -1;
        String target = targetCode.trim().toUpperCase();
        int low = 0, high = products.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int cmp = products.get(mid).getCode().toUpperCase().compareTo(target);
            if (cmp == 0) return mid;
            else if (cmp < 0) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    public static int binarySearchOrderByNumber(List<Order> orders, int targetOrderNumber) {
        if (orders == null || orders.isEmpty()) return -1;
        int low = 0, high = orders.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int curNum = orders.get(mid).getOrderNumber();
            if (curNum == targetOrderNumber) return mid;
            else if (curNum < targetOrderNumber) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    public static List<Product> linearSearchProductsByName(List<Product> products, String query) {
        List<Product> results = new ArrayList<>();
        if (products == null || query == null || query.trim().isEmpty()) return results;
        String q = query.trim().toLowerCase();
        for (Product p : products) {
            if (p.getName().toLowerCase().contains(q) || p.getCategory().toLowerCase().contains(q)) {
                results.add(p);
            }
        }
        return results;
    }
}

// ============================================================================
// SORTING ALGORITHMS (INSERTION SORT & MERGE SORT)
// ============================================================================
class SortAlgorithms {

    public static void insertionSortProductsByCode(List<Product> products) {
        if (products == null || products.size() <= 1) return;
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

    public static void insertInSortedOrder(List<Product> products, Product newProduct) {
        if (products == null || newProduct == null) return;
        int i = products.size() - 1;
        products.add(newProduct);
        while (i >= 0 && products.get(i).getCode().compareToIgnoreCase(newProduct.getCode()) > 0) {
            products.set(i + 1, products.get(i));
            i--;
        }
        products.set(i + 1, newProduct);
    }

    public static <T> List<T> mergeSort(List<T> list, Comparator<T> comparator) {
        if (list == null) return new ArrayList<>();
        List<T> copy = new ArrayList<>(list);
        if (copy.size() <= 1) return copy;
        mergeSortRecursive(copy, 0, copy.size() - 1, comparator);
        return copy;
    }

    private static <T> void mergeSortRecursive(List<T> list, int left, int right, Comparator<T> comparator) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSortRecursive(list, left, mid, comparator);
            mergeSortRecursive(list, mid + 1, right, comparator);
            merge(list, left, mid, right, comparator);
        }
    }

    private static <T> void merge(List<T> list, int left, int mid, int right, Comparator<T> comparator) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        List<T> L = new ArrayList<>(n1);
        List<T> R = new ArrayList<>(n2);
        for (int i = 0; i < n1; i++) L.add(list.get(left + i));
        for (int j = 0; j < n2; j++) R.add(list.get(mid + 1 + j));

        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (comparator.compare(L.get(i), R.get(j)) <= 0) {
                list.set(k++, L.get(i++));
            } else {
                list.set(k++, R.get(j++));
            }
        }
        while (i < n1) list.set(k++, L.get(i++));
        while (j < n2) list.set(k++, R.get(j++));
    }
}

// ============================================================================
// MODELS
// ============================================================================
class Product {
    private String code;
    private String name;
    private String category;
    private double price;
    private int stock;
    private int lowStockThreshold;

    public Product(String code, String name, String category, double price, int stock, int lowStockThreshold) {
        this.code = code != null ? code.trim().toUpperCase() : "";
        this.name = name != null ? name.trim() : "";
        this.category = category != null ? category.trim() : "General";
        this.price = price;
        this.stock = stock;
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int th) { this.lowStockThreshold = th; }
    public boolean isLowStock() { return stock <= lowStockThreshold; }

    public void deductStock(int q) {
        if (q <= stock) this.stock -= q;
        else throw new IllegalArgumentException("Insufficient stock.");
    }
    public void addStock(int q) { if (q > 0) this.stock += q; }

    public String toCsv() {
        return String.format("%s,%s,%s,%.2f,%d,%d", code, name.replace(",", " "), category, price, stock, lowStockThreshold);
    }

    public static Product fromCsv(String line) {
        String[] p = line.split(",");
        return new Product(p[0].trim(), p[1].trim(), p[2].trim(), Double.parseDouble(p[3].trim()), Integer.parseInt(p[4].trim()), Integer.parseInt(p[5].trim()));
    }
}

class OrderItem {
    private String productCode;
    private String productName;
    private double unitPrice;
    private int quantity;

    public OrderItem(String productCode, String productName, double unitPrice, int quantity) {
        this.productCode = productCode;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public double getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSubtotal() { return unitPrice * quantity; }
}

class Order {
    private int orderNumber;
    private String timestamp;
    private List<OrderItem> items;
    private double totalAmount;
    private double cashReceived;
    private double change;
    private String status;

    public Order(int orderNumber, String timestamp) {
        this(orderNumber, timestamp, new ArrayList<>(), 0.0, 0.0, 0.0, "PENDING");
    }

    public Order(int orderNumber, String timestamp, List<OrderItem> items, double totalAmount,
                 double cashReceived, double change, String status) {
        this.orderNumber = orderNumber;
        this.timestamp = timestamp;
        this.items = items != null ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.cashReceived = cashReceived;
        this.change = change;
        this.status = status;
    }

    public int getOrderNumber() { return orderNumber; }
    public String getTimestamp() { return timestamp; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public double getCashReceived() { return cashReceived; }
    public double getChange() { return change; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalUnits() {
        int c = 0;
        for (OrderItem it : items) c += it.getQuantity();
        return c;
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            sb.append(it.getProductCode()).append(":").append(it.getQuantity()).append(":").append(it.getUnitPrice());
            if (i < items.size() - 1) sb.append(";");
        }
        return String.format("%d,%s,%s,%.2f,%.2f,%.2f,%s", orderNumber, timestamp, sb.toString(), totalAmount, cashReceived, change, status);
    }
}

class ProductSalesStat {
    public String code;
    public String name;
    public int unitsSold;
    public double revenue;

    public ProductSalesStat(String code, String name) {
        this.code = code;
        this.name = name;
        this.unitsSold = 0;
        this.revenue = 0.0;
    }
}

// ============================================================================
// SERVICE & MANAGERS (SELF-CONTAINED LOGIC)
// ============================================================================
class InventoryManager {
    private final String filePath;
    private final List<Product> products = new ArrayList<>();

    public InventoryManager(String filePath) {
        this.filePath = filePath;
        load();
    }

    private void load() {
        products.clear();
        File f = new File(filePath);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                br.readLine(); // skip header
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) products.add(Product.fromCsv(line));
                }
            } catch (Exception ignored) {}
        }
        if (products.isEmpty()) {
            seedDefaultProducts();
            save();
        }
        SortAlgorithms.insertionSortProductsByCode(products);
    }

    public void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write("code,name,category,price,stock,lowStockThreshold\n");
            for (Product p : products) {
                bw.write(p.toCsv() + "\n");
            }
        } catch (IOException ignored) {}
    }

    public Product findByCode(String code) {
        int idx = SearchAlgorithms.binarySearchProductByCode(products, code);
        return idx >= 0 ? products.get(idx) : null;
    }

    public List<Product> searchByName(String query) {
        return SearchAlgorithms.linearSearchProductsByName(products, query);
    }

    public boolean addProduct(Product p) {
        if (p == null || findByCode(p.getCode()) != null) return false;
        SortAlgorithms.insertInSortedOrder(products, p);
        save();
        return true;
    }

    public boolean recordStockReceived(String code, int qty) {
        Product p = findByCode(code);
        if (p == null || qty <= 0) return false;
        p.addStock(qty);
        save();
        return true;
    }

    public boolean deductStock(String code, int qty) {
        Product p = findByCode(code);
        if (p == null || p.getStock() < qty) return false;
        p.deductStock(qty);
        save();
        return true;
    }

    public boolean restoreStock(String code, int qty) {
        Product p = findByCode(code);
        if (p == null) return false;
        p.addStock(qty);
        save();
        return true;
    }

    public boolean updateProduct(String code, String name, String cat, double price, int thresh) {
        Product p = findByCode(code);
        if (p == null) return false;
        p.setName(name);
        p.setCategory(cat);
        p.setPrice(price);
        p.setLowStockThreshold(thresh);
        save();
        return true;
    }

    public boolean removeProduct(String code) {
        int idx = SearchAlgorithms.binarySearchProductByCode(products, code);
        if (idx >= 0) {
            products.remove(idx);
            save();
            return true;
        }
        return false;
    }

    public List<Product> getLowStockProducts() {
        List<Product> low = new ArrayList<>();
        for (Product p : products) if (p.isLowStock()) low.add(p);
        return low;
    }

    public List<Product> getAllProducts() { return products; }

    private void seedDefaultProducts() {
        products.add(new Product("KP-101", "Kingpin Classic Oversized Tee - Black", "Apparel", 750.00, 25, 5));
        products.add(new Product("KP-102", "Kingpin Classic Oversized Tee - White", "Apparel", 750.00, 18, 5));
        products.add(new Product("KP-103", "Manila Underground Heavyweight Hoodie", "Apparel", 1850.00, 8, 3));
        products.add(new Product("KP-104", "Cyberpunk Metro Graphic Tee", "Apparel", 850.00, 12, 5));
        products.add(new Product("KP-105", "Kingpin Signature Boxy Fit Tee - Olive", "Apparel", 790.00, 4, 5));
        products.add(new Product("KP-106", "Raw Denim Streetwear Cargo Pants", "Apparel", 1650.00, 6, 3));
        products.add(new Product("KP-107", "Kingpin 5-Panel Snapback Cap", "Accessories", 650.00, 15, 4));
        products.add(new Product("KP-108", "Heavy Canvas Utility Tote Bag", "Accessories", 450.00, 20, 5));
        products.add(new Product("KP-109", "Kingpin Vinyl Holographic Sticker Pack", "Accessories", 150.00, 40, 10));
        products.add(new Product("KP-110", "Reversible Bomber Jacket - MNL Edition", "Apparel", 2450.00, 3, 2));
        products.add(new Product("KP-111", "Acid Wash Vintage Graphic Tee", "Apparel", 890.00, 14, 5));
        products.add(new Product("KP-112", "Industrial Webbing Key Lanyard", "Accessories", 250.00, 30, 8));
    }
}

class OrderManager {
    private final String filePath;
    private final InventoryManager inventoryManager;
    private final OrderQueue queue = new OrderQueue();
    private final List<Order> salesHistory = new ArrayList<>();
    private final List<OrderItem> cart = new ArrayList<>();
    private int nextOrderNumber = 1001;

    public OrderManager(String filePath, InventoryManager inventoryManager) {
        this.filePath = filePath;
        this.inventoryManager = inventoryManager;
        load();
    }

    private void load() {
        salesHistory.clear();
        File f = new File(filePath);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split(",");
                        int ordNum = Integer.parseInt(parts[0].trim());
                        if (ordNum >= nextOrderNumber) nextOrderNumber = ordNum + 1;
                        List<OrderItem> items = new ArrayList<>();
                        if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                            for (String itTok : parts[2].split(";")) {
                                String[] itP = itTok.split(":");
                                String pCode = itP[0];
                                int q = Integer.parseInt(itP[1]);
                                double pr = Double.parseDouble(itP[2]);
                                Product prod = inventoryManager.findByCode(pCode);
                                String pName = prod != null ? prod.getName() : pCode;
                                items.add(new OrderItem(pCode, pName, pr, q));
                            }
                        }
                        salesHistory.add(new Order(ordNum, parts[1].trim(), items,
                                Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                                Double.parseDouble(parts[5]), parts[6].trim()));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void append(Order o) {
        boolean exists = new File(filePath).exists();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            if (!exists) bw.write("orderNumber,timestamp,itemsSerialized,totalAmount,cashReceived,change,status\n");
            bw.write(o.toCsv() + "\n");
        } catch (IOException ignored) {}
    }

    public boolean addToCart(String code, int qty) {
        Product p = inventoryManager.findByCode(code);
        if (p == null || qty <= 0) return false;
        int inCart = 0;
        for (OrderItem it : cart) if (it.getProductCode().equalsIgnoreCase(code)) inCart += it.getQuantity();
        if (inCart + qty > p.getStock()) return false;

        for (OrderItem it : cart) {
            if (it.getProductCode().equalsIgnoreCase(code)) {
                it.setQuantity(it.getQuantity() + qty);
                return true;
            }
        }
        cart.add(new OrderItem(p.getCode(), p.getName(), p.getPrice(), qty));
        return true;
    }

    public boolean removeFromCart(String code) {
        return cart.removeIf(it -> it.getProductCode().equalsIgnoreCase(code));
    }

    public void clearCart() { cart.clear(); }
    public List<OrderItem> getCart() { return cart; }

    public double getCartTotal() {
        double t = 0;
        for (OrderItem it : cart) t += it.getSubtotal();
        return t;
    }

    public Order confirmOrder(double cash) {
        if (cart.isEmpty()) throw new IllegalStateException("Cart is empty.");
        double total = getCartTotal();
        if (cash < total) throw new IllegalArgumentException("Insufficient cash.");

        for (OrderItem it : cart) {
            Product p = inventoryManager.findByCode(it.getProductCode());
            if (p == null || p.getStock() < it.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for " + it.getProductName());
            }
        }
        for (OrderItem it : cart) inventoryManager.deductStock(it.getProductCode(), it.getQuantity());

        int num = nextOrderNumber++;
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Order o = new Order(num, time, new ArrayList<>(cart), total, cash, cash - total, "PENDING");
        queue.enqueue(o);
        clearCart();
        return o;
    }

    public Order peekOrder() { return queue.peek(); }

    public Order serveNextOrder() {
        if (queue.isEmpty()) return null;
        Order served = queue.dequeue();
        served.setStatus("SERVED");
        salesHistory.add(served);
        append(served);
        return served;
    }

    public Order cancelOrder(int num) {
        Order cancelled = queue.cancel(num);
        if (cancelled != null) {
            for (OrderItem it : cancelled.getItems()) {
                inventoryManager.restoreStock(it.getProductCode(), it.getQuantity());
            }
        }
        return cancelled;
    }

    public List<Order> getPendingOrders() { return queue.getAllOrders(); }
    public int getPendingCount() { return queue.getSize(); }
    public List<Order> getSalesHistory() { return salesHistory; }

    public Order findOrderInHistory(int num) {
        int idx = SearchAlgorithms.binarySearchOrderByNumber(salesHistory, num);
        return idx >= 0 ? salesHistory.get(idx) : null;
    }

    public List<ProductSalesStat> getBestSellersStats() {
        Map<String, ProductSalesStat> map = new HashMap<>();
        for (Order o : salesHistory) {
            if ("SERVED".equalsIgnoreCase(o.getStatus())) {
                for (OrderItem it : o.getItems()) {
                    map.putIfAbsent(it.getProductCode(), new ProductSalesStat(it.getProductCode(), it.getProductName()));
                    ProductSalesStat st = map.get(it.getProductCode());
                    st.unitsSold += it.getQuantity();
                    st.revenue += it.getSubtotal();
                }
            }
        }
        List<ProductSalesStat> list = new ArrayList<>(map.values());
        return SortAlgorithms.mergeSort(list, (a, b) -> Integer.compare(b.unitsSold, a.unitsSold));
    }
}
