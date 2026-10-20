package com.kingpin;

import com.kingpin.dsa.SearchAlgorithms;
import com.kingpin.model.Order;
import com.kingpin.model.OrderItem;
import com.kingpin.model.Product;
import com.kingpin.service.InventoryService;
import com.kingpin.service.OrderService;
import com.kingpin.service.ReportService;
import com.kingpin.util.ConsoleUI;

import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * Main application driver for Kingpin MNL POS and Order Queue System.
 *
 * Implements the full order lifecycle, custom LinkedList queue,
 * iterative binary search, linear search, insertion sort, and merge sort.
 */
public class Main {
    private static final String INVENTORY_FILE = "data/inventory.csv";
    private static final String SALES_HISTORY_FILE = "data/sales_history.csv";

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ReportService reportService;
    private final Scanner scanner;

    public Main() {
        // Ensure data directory exists
        new File("data").mkdirs();

        this.inventoryService = new InventoryService(INVENTORY_FILE);
        this.orderService = new OrderService(SALES_HISTORY_FILE, inventoryService);
        this.reportService = new ReportService(inventoryService, orderService);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        ConsoleUI.printBanner();

        // Check for low stock warnings on startup
        List<Product> initialLowStock = inventoryService.getLowStockProducts();
        if (!initialLowStock.isEmpty()) {
            ConsoleUI.printWarning("ALERT: " + initialLowStock.size() + " product(s) currently at or below low-stock threshold!");
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Select an option (1-6): ");
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
                    ConsoleUI.printInfo("Saving state and exiting Kingpin MNL POS. Goodbye!");
                    running = false;
                    break;
                default:
                    ConsoleUI.printError("Invalid option. Please enter a number between 1 and 6.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n" + ConsoleUI.BOLD + "================ MAIN MENU ================" + ConsoleUI.RESET);
        System.out.println(" [1] Take Order (POS Cashier & Cart)");
        System.out.println(" [2] Order Queue (Serve / Peek / Cancel via Rotation)");
        System.out.println(" [3] Inventory Management (Restock from Main Branch / Add / Edit)");
        System.out.println(" [4] Search Records (Binary Search & Linear Search)");
        System.out.println(" [5] Managerial Reports (Merge Sort Best Sellers / Daily Sales)");
        System.out.println(" [6] Exit Application");
        System.out.println(ConsoleUI.BOLD + "===========================================" + ConsoleUI.RESET);
        int pending = orderService.getPendingOrderCount();
        if (pending > 0) {
            System.out.println(ConsoleUI.YELLOW + "Pending Orders in Queue: " + pending + ConsoleUI.RESET);
        }
    }

    // =========================================================================
    // 1. TAKE ORDER MENU
    // =========================================================================

    private void handleTakeOrderMenu() {
        boolean inOrder = true;
        while (inOrder) {
            ConsoleUI.printHeader("POS - ORDER TAKING");
            ConsoleUI.displayCart(orderService.getCurrentCart(), orderService.getCartTotal());

            System.out.println("\nActions:");
            System.out.println(" [1] Add Product to Cart by Code [Binary Search]");
            System.out.println(" [2] Search Product by Name [Linear Search] & Add");
            System.out.println(" [3] Remove Product from Cart");
            System.out.println(" [4] Clear Cart");
            System.out.println(" [5] Confirm Order & Enqueue [Deduct Stock -> Assign Order #]");
            System.out.println(" [6] Return to Main Menu");

            String choice = prompt("Select action (1-6): ");
            switch (choice.trim()) {
                case "1":
                    String code = prompt("Enter Product Code (e.g., KP-101): ").toUpperCase();
                    Product p = inventoryService.findByCode(code);
                    if (p == null) {
                        ConsoleUI.printError("Product with code '" + code + "' not found in inventory.");
                        break;
                    }
                    System.out.printf("Selected: %s | Price: PHP %.2f | Available Stock: %d\n",
                            p.getName(), p.getPrice(), p.getStock());
                    int qty = promptInt("Enter Quantity: ");
                    if (qty <= 0) {
                        ConsoleUI.printError("Quantity must be greater than zero.");
                    } else if (orderService.addToCart(code, qty)) {
                        ConsoleUI.printSuccess("Added " + qty + "x " + p.getName() + " to cart.");
                    } else {
                        ConsoleUI.printError("Failed to add to cart. Insufficient stock (Available: " + p.getStock() + ").");
                    }
                    break;

                case "2":
                    String query = prompt("Enter product name keyword: ");
                    List<Product> matches = inventoryService.searchByName(query);
                    if (matches.isEmpty()) {
                        ConsoleUI.printWarning("No products matched '" + query + "'.");
                    } else {
                        ConsoleUI.displayProductTable(matches);
                        String addCode = prompt("Enter Product Code to add (or press Enter to cancel): ").toUpperCase();
                        if (!addCode.isEmpty()) {
                            Product selected = inventoryService.findByCode(addCode);
                            if (selected != null) {
                                int addQty = promptInt("Enter Quantity: ");
                                if (orderService.addToCart(addCode, addQty)) {
                                    ConsoleUI.printSuccess("Added to cart.");
                                } else {
                                    ConsoleUI.printError("Insufficient stock or invalid quantity.");
                                }
                            } else {
                                ConsoleUI.printError("Invalid product code.");
                            }
                        }
                    }
                    break;

                case "3":
                    if (orderService.getCurrentCart().isEmpty()) {
                        ConsoleUI.printWarning("Cart is empty.");
                    } else {
                        String remCode = prompt("Enter Product Code to remove: ").toUpperCase();
                        if (orderService.removeFromCart(remCode)) {
                            ConsoleUI.printSuccess("Item removed from cart.");
                        } else {
                            ConsoleUI.printError("Item not found in cart.");
                        }
                    }
                    break;

                case "4":
                    orderService.clearCart();
                    ConsoleUI.printInfo("Cart cleared.");
                    break;

                case "5":
                    if (orderService.getCurrentCart().isEmpty()) {
                        ConsoleUI.printError("Cannot confirm an empty cart.");
                        break;
                    }
                    double total = orderService.getCartTotal();
                    System.out.printf(ConsoleUI.BOLD + "Total Due: PHP %,.2f\n" + ConsoleUI.RESET, total);
                    double cash = promptDouble("Enter Cash Tendered (PHP): ");
                    if (cash < total) {
                        ConsoleUI.printError(String.format("Insufficient cash. PHP %.2f still required.", (total - cash)));
                        break;
                    }
                    try {
                        Order confirmed = orderService.confirmOrder(cash);
                        ConsoleUI.printSuccess("Order #" + confirmed.getOrderNumber() + " CONFIRMED and enqueued into Order Queue!");
                        System.out.printf("Change: PHP %,.2f\n", confirmed.getChange());
                        ConsoleUI.printInfo("Stock deducted immediately to prevent overselling.");
                        inOrder = false;
                    } catch (Exception e) {
                        ConsoleUI.printError("Order confirmation failed: " + e.getMessage());
                    }
                    break;

                case "6":
                    inOrder = false;
                    break;

                default:
                    ConsoleUI.printError("Invalid option.");
            }
        }
    }

    // =========================================================================
    // 2. ORDER QUEUE MENU
    // =========================================================================

    private void handleOrderQueueMenu() {
        boolean inQueueMenu = true;
        while (inQueueMenu) {
            ConsoleUI.printHeader("ORDER QUEUE MANAGEMENT (FIFO)");
            ConsoleUI.displayQueue(orderService.getPendingOrders());

            System.out.println("\nActions:");
            System.out.println(" [1] Peek Next Order in Queue [O(1)]");
            System.out.println(" [2] Serve Next Order [O(1) Dequeue -> Print Receipt -> Append History]");
            System.out.println(" [3] Cancel Order [O(n) Queue Rotation -> Restore Stock]");
            System.out.println(" [4] Return to Main Menu");

            String choice = prompt("Select action (1-4): ");
            switch (choice.trim()) {
                case "1":
                    Order next = orderService.peekNextOrder();
                    if (next == null) {
                        ConsoleUI.printInfo("No pending orders in the queue.");
                    } else {
                        ConsoleUI.printHeader("NEXT IN LINE TO BE SERVED (FRONT OF QUEUE)");
                        System.out.printf("Order Number : #%d\n", next.getOrderNumber());
                        System.out.printf("Placed At    : %s\n", next.getTimestamp());
                        System.out.printf("Total Amount : PHP %,.2f\n", next.getTotalAmount());
                        System.out.printf("Items        : %d\n", next.getTotalUnits());
                        for (OrderItem it : next.getItems()) {
                            System.out.printf("  - %dx %s (PHP %.2f)\n", it.getQuantity(), it.getProductName(), it.getSubtotal());
                        }
                    }
                    break;

                case "2":
                    Order served = orderService.serveNextOrder();
                    if (served == null) {
                        ConsoleUI.printWarning("Queue is empty. No pending orders to serve.");
                    } else {
                        ConsoleUI.printSuccess("Order #" + served.getOrderNumber() + " has been SERVED successfully!");
                        ConsoleUI.printInfo("Order recorded into sales history. Printing customer receipt:");
                        ConsoleUI.printReceipt(served);
                    }
                    break;

                case "3":
                    if (orderService.getPendingOrderCount() == 0) {
                        ConsoleUI.printWarning("No pending orders available to cancel.");
                        break;
                    }
                    int cancelNum = promptInt("Enter Order Number to cancel: ");
                    Order cancelled = orderService.cancelOrder(cancelNum);
                    if (cancelled != null) {
                        ConsoleUI.printSuccess("Order #" + cancelNum + " has been CANCELLED via FIFO queue rotation!");
                        ConsoleUI.printInfo("All items restored to inventory stock:");
                        for (OrderItem it : cancelled.getItems()) {
                            System.out.printf("  - Restored +%d units to [%s] %s\n",
                                    it.getQuantity(), it.getProductCode(), it.getProductName());
                        }
                    } else {
                        ConsoleUI.printError("Order #" + cancelNum + " was not found in the active pending queue.");
                    }
                    break;

                case "4":
                    inQueueMenu = false;
                    break;

                default:
                    ConsoleUI.printError("Invalid option.");
            }
        }
    }

    // =========================================================================
    // 3. INVENTORY MANAGEMENT
    // =========================================================================

    private void handleInventoryMenu() {
        boolean inInv = true;
        while (inInv) {
            ConsoleUI.printHeader("INVENTORY MANAGEMENT (Code-Sorted)");
            System.out.println(" [1] View Master Inventory Table");
            System.out.println(" [2] Record Stock Received from Main Branch (Restock)");
            System.out.println(" [3] Add New Product [Insertion Step maintains sort]");
            System.out.println(" [4] Edit Product Details (Price, Name, Min Threshold)");
            System.out.println(" [5] Remove Product from Catalog");
            System.out.println(" [6] View Low-Stock Alerts");
            System.out.println(" [7] Return to Main Menu");

            String choice = prompt("Select action (1-7): ");
            switch (choice.trim()) {
                case "1":
                    ConsoleUI.displayProductTable(inventoryService.getAllProducts());
                    break;

                case "2":
                    ConsoleUI.printHeader("RESTOCK FROM MAIN WAREHOUSE / BRANCH");
                    String rCode = prompt("Enter Product Code to restock: ").toUpperCase();
                    Product rProduct = inventoryService.findByCode(rCode);
                    if (rProduct == null) {
                        ConsoleUI.printError("Product code not found.");
                        break;
                    }
                    System.out.printf("Current Stock for [%s] %s: %d units\n", rCode, rProduct.getName(), rProduct.getStock());
                    int addedQty = promptInt("Enter quantity of units received from Main Branch: ");
                    if (inventoryService.recordStockReceived(rCode, addedQty)) {
                        ConsoleUI.printSuccess(String.format("Restocked successfully. New stock: %d units.", rProduct.getStock()));
                    } else {
                        ConsoleUI.printError("Invalid quantity.");
                    }
                    break;

                case "3":
                    ConsoleUI.printHeader("ADD NEW PRODUCT");
                    String newCode = prompt("Enter Product Code (e.g., KP-113): ").toUpperCase();
                    if (inventoryService.findByCode(newCode) != null) {
                        ConsoleUI.printError("A product with code '" + newCode + "' already exists!");
                        break;
                    }
                    String newName = prompt("Enter Product Name: ");
                    String newCat = prompt("Enter Category (e.g., Apparel, Accessories): ");
                    double newPrice = promptDouble("Enter Unit Price (PHP): ");
                    int newStock = promptInt("Enter Initial Stock Quantity: ");
                    int newThresh = promptInt("Enter Low-Stock Alert Threshold: ");

                    Product np = new Product(newCode, newName, newCat, newPrice, newStock, newThresh);
                    if (inventoryService.addProduct(np)) {
                        ConsoleUI.printSuccess("Product [" + newCode + "] successfully added via Insertion step!");
                    } else {
                        ConsoleUI.printError("Failed to add product.");
                    }
                    break;

                case "4":
                    ConsoleUI.printHeader("EDIT PRODUCT DETAILS");
                    String eCode = prompt("Enter Product Code to edit: ").toUpperCase();
                    Product eP = inventoryService.findByCode(eCode);
                    if (eP == null) {
                        ConsoleUI.printError("Product code not found.");
                        break;
                    }
                    System.out.println("Editing: " + eP);
                    String editName = prompt("New Name (or press Enter to keep '" + eP.getName() + "'): ");
                    if (editName.trim().isEmpty()) editName = eP.getName();

                    String editCat = prompt("New Category (or press Enter to keep '" + eP.getCategory() + "'): ");
                    if (editCat.trim().isEmpty()) editCat = eP.getCategory();

                    String priceInput = prompt("New Price (or press Enter to keep " + eP.getPrice() + "): ");
                    double editPrice = priceInput.trim().isEmpty() ? eP.getPrice() : Double.parseDouble(priceInput.trim());

                    String threshInput = prompt("New Low-Stock Threshold (or press Enter to keep " + eP.getLowStockThreshold() + "): ");
                    int editThresh = threshInput.trim().isEmpty() ? eP.getLowStockThreshold() : Integer.parseInt(threshInput.trim());

                    if (inventoryService.updateProduct(eCode, editName, editCat, editPrice, editThresh)) {
                        ConsoleUI.printSuccess("Product details updated.");
                    } else {
                        ConsoleUI.printError("Failed to update product.");
                    }
                    break;

                case "5":
                    ConsoleUI.printHeader("REMOVE PRODUCT");
                    String delCode = prompt("Enter Product Code to remove: ").toUpperCase();
                    Product delP = inventoryService.findByCode(delCode);
                    if (delP == null) {
                        ConsoleUI.printError("Product code not found.");
                        break;
                    }
                    String confirm = prompt("Are you sure you want to delete '" + delP.getName() + "'? (Y/N): ");
                    if ("Y".equalsIgnoreCase(confirm.trim())) {
                        if (inventoryService.removeProduct(delCode)) {
                            ConsoleUI.printSuccess("Product removed from catalog.");
                        } else {
                            ConsoleUI.printError("Failed to remove product.");
                        }
                    }
                    break;

                case "6":
                    List<Product> lowStock = inventoryService.getLowStockProducts();
                    ConsoleUI.printHeader("LOW-STOCK PRODUCTS (" + lowStock.size() + " items)");
                    ConsoleUI.displayProductTable(lowStock);
                    break;

                case "7":
                    inInv = false;
                    break;

                default:
                    ConsoleUI.printError("Invalid option.");
            }
        }
    }

    // =========================================================================
    // 4. SEARCH MENU
    // =========================================================================

    private void handleSearchMenu() {
        boolean inSearch = true;
        while (inSearch) {
            ConsoleUI.printHeader("SEARCH ENGINE (DSA Algorithms)");
            System.out.println(" [1] Binary Search Product by Exact Code [O(log n)]");
            System.out.println(" [2] Linear Search Product by Name Keyword [O(n)]");
            System.out.println(" [3] Binary Search Receipt / Order by Order Number [O(log n)]");
            System.out.println(" [4] Return to Main Menu");

            String choice = prompt("Select action (1-4): ");
            switch (choice.trim()) {
                case "1":
                    String code = prompt("Enter Product Code to search (e.g., KP-105): ").toUpperCase();
                    long startBin = System.nanoTime();
                    Product p = inventoryService.findByCode(code);
                    long endBin = System.nanoTime();
                    if (p != null) {
                        ConsoleUI.printSuccess(String.format("Found in %d ns via Binary Search (O(log n)):", (endBin - startBin)));
                        System.out.println("  Code:        " + p.getCode());
                        System.out.println("  Name:        " + p.getName());
                        System.out.println("  Category:    " + p.getCategory());
                        System.out.printf("  Price:       PHP %,.2f\n", p.getPrice());
                        System.out.printf("  Stock:       %d units (Threshold: %d)\n", p.getStock(), p.getLowStockThreshold());
                    } else {
                        ConsoleUI.printWarning("Product [" + code + "] not found.");
                    }
                    break;

                case "2":
                    String query = prompt("Enter name keyword: ");
                    long startLin = System.nanoTime();
                    List<Product> matches = inventoryService.searchByName(query);
                    long endLin = System.nanoTime();
                    ConsoleUI.printInfo(String.format("Linear Search completed in %d ns. Matches found: %d", (endLin - startLin), matches.size()));
                    ConsoleUI.displayProductTable(matches);
                    break;

                case "3":
                    int ordNum = promptInt("Enter Order Number to search in history (e.g., 1002): ");
                    long startOrd = System.nanoTime();
                    Order foundOrder = orderService.findOrderInHistory(ordNum);
                    long endOrd = System.nanoTime();
                    if (foundOrder != null) {
                        ConsoleUI.printSuccess(String.format("Order found in %d ns via Binary Search on order history:", (endOrd - startOrd)));
                        ConsoleUI.printReceipt(foundOrder);
                    } else {
                        ConsoleUI.printWarning("Order #" + ordNum + " not found in completed sales history.");
                    }
                    break;

                case "4":
                    inSearch = false;
                    break;

                default:
                    ConsoleUI.printError("Invalid option.");
            }
        }
    }

    // =========================================================================
    // 5. REPORTS MENU
    // =========================================================================

    private void handleReportsMenu() {
        boolean inReports = true;
        while (inReports) {
            ConsoleUI.printHeader("MANAGERIAL REPORTS & ANALYTICS");
            System.out.println(" [1] Daily Sales Summary");
            System.out.println(" [2] Best Sellers Ranking [Merge Sort O(n log n)]");
            System.out.println(" [3] Low-Stock Priority Restock List [Merge Sort O(n log n)]");
            System.out.println(" [4] Main Branch Synchronized Sales & Restock Report");
            System.out.println(" [5] Return to Main Menu");

            String choice = prompt("Select report (1-5): ");
            switch (choice.trim()) {
                case "1":
                    String dateQuery = prompt("Enter Date (YYYY-MM-DD) or press Enter for All Dates: ").trim();
                    ReportService.DailySalesSummary summary = reportService.getDailySalesSummary(dateQuery);
                    ConsoleUI.printHeader("SALES SUMMARY: " + summary.date);
                    System.out.printf("Total Completed Orders : %d\n", summary.totalOrders);
                    System.out.printf("Total Units Sold       : %d items\n", summary.totalUnitsSold);
                    System.out.printf("Total Gross Revenue    : PHP %,.2f\n", summary.totalRevenue);
                    System.out.printf("Average Order Value    : PHP %,.2f\n", summary.averageOrderValue);
                    if (!summary.matchingOrders.isEmpty()) {
                        System.out.println("\nOrders in this period:");
                        for (Order o : summary.matchingOrders) {
                            System.out.printf("  Order #%-5d | %s | %2d items | PHP %,10.2f\n",
                                    o.getOrderNumber(), o.getTimestamp(), o.getTotalUnits(), o.getTotalAmount());
                        }
                    }
                    break;

                case "2":
                    ConsoleUI.printHeader("BEST SELLERS (SORTED VIA MERGE SORT BY UNITS SOLD)");
                    List<ReportService.ProductSalesStat> best = reportService.getBestSellers();
                    if (best.isEmpty()) {
                        ConsoleUI.printInfo("No sales records available yet.");
                    } else {
                        System.out.println(ConsoleUI.BOLD + "+------+------------+-----------------------------------+------------+------------------+" + ConsoleUI.RESET);
                        System.out.printf(ConsoleUI.BOLD + "| %-4s | %-10s | %-33s | %10s | %16s |%n" + ConsoleUI.RESET,
                                "RANK", "CODE", "PRODUCT NAME", "UNITS SOLD", "REVENUE (PHP)");
                        System.out.println("+------+------------+-----------------------------------+------------+------------------+");
                        int rank = 1;
                        for (ReportService.ProductSalesStat stat : best) {
                            String name = stat.getProductName().length() > 33 ? stat.getProductName().substring(0, 30) + "..." : stat.getProductName();
                            System.out.printf("| %-4d | %-10s | %-33s | %10d | PHP %,12.2f |%n",
                                    rank++, stat.getProductCode(), name, stat.getUnitsSold(), stat.getTotalRevenue());
                        }
                        System.out.println("+------+------------+-----------------------------------+------------+------------------+");
                    }
                    break;

                case "3":
                    ConsoleUI.printHeader("LOW-STOCK RESTOCK PRIORITY (SORTED VIA MERGE SORT BY QUANTITY ASCENDING)");
                    List<Product> lowStockSorted = reportService.getLowStockReport();
                    if (lowStockSorted.isEmpty()) {
                        ConsoleUI.printSuccess("All products are well-stocked!");
                    } else {
                        ConsoleUI.displayProductTable(lowStockSorted);
                    }
                    break;

                case "4":
                    String mainBranchReport = reportService.generateMainBranchSummaryReport();
                    System.out.println("\n" + mainBranchReport);
                    break;

                case "5":
                    inReports = false;
                    break;

                default:
                    ConsoleUI.printError("Invalid option.");
            }
        }
    }

    // --- Helpers ---

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
                ConsoleUI.printError("Invalid integer input. Please try again.");
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
                ConsoleUI.printError("Invalid decimal amount. Please try again.");
            }
        }
    }
}
