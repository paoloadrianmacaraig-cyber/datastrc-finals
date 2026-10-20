package com.kingpin;

import com.kingpin.dsa.OrderQueue;
import com.kingpin.dsa.SearchAlgorithms;
import com.kingpin.dsa.SortAlgorithms;
import com.kingpin.model.Order;
import com.kingpin.model.OrderItem;
import com.kingpin.model.Product;
import com.kingpin.service.InventoryService;
import com.kingpin.service.OrderService;
import com.kingpin.service.ReportService;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Automated verification test suite validating all DSA requirements:
 * - Queue FIFO Enqueue, Dequeue, Peek, and Cancel via Rotation
 * - Binary Search (O(log n)) and Linear Search (O(n))
 * - Insertion Sort (O(n^2)) and Merge Sort (O(n log n))
 * - Stock deduction on confirm and restoration on cancel
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  RUNNING KINGPIN MNL DSA VERIFICATION TEST SUITE ");
        System.out.println("==================================================");

        int passed = 0;
        int total = 0;

        total++; if (testOrderQueueFIFO()) passed++;
        total++; if (testOrderQueueCancelByRotation()) passed++;
        total++; if (testBinarySearch()) passed++;
        total++; if (testLinearSearch()) passed++;
        total++; if (testInsertionSort()) passed++;
        total++; if (testMergeSort()) passed++;
        total++; if (testOrderLifecycleAndStockRestoration()) passed++;

        System.out.println("==================================================");
        System.out.printf("Test Results: %d / %d tests passed successfully!\n", passed, total);
        System.out.println("==================================================");

        if (passed != total) {
            System.exit(1);
        }
    }

    private static boolean testOrderQueueFIFO() {
        System.out.print("[TEST 1] OrderQueue Enqueue, Dequeue, Peek (O(1))... ");
        OrderQueue queue = new OrderQueue();
        if (!queue.isEmpty() || queue.getSize() != 0) return false;

        Order o1 = new Order(101, "2026-10-20 10:00:00");
        Order o2 = new Order(102, "2026-10-20 10:05:00");
        Order o3 = new Order(103, "2026-10-20 10:10:00");

        queue.enqueue(o1);
        queue.enqueue(o2);
        queue.enqueue(o3);

        if (queue.getSize() != 3) return false;
        if (queue.peek().getOrderNumber() != 101) return false;

        Order d1 = queue.dequeue();
        if (d1.getOrderNumber() != 101 || queue.getSize() != 2) return false;

        Order d2 = queue.dequeue();
        if (d2.getOrderNumber() != 102 || queue.getSize() != 1) return false;

        Order d3 = queue.dequeue();
        if (d3.getOrderNumber() != 103 || !queue.isEmpty()) return false;

        System.out.println("PASSED ✔");
        return true;
    }

    private static boolean testOrderQueueCancelByRotation() {
        System.out.print("[TEST 2] OrderQueue Cancel by Rotation (O(n))... ");
        OrderQueue queue = new OrderQueue();
        Order o1 = new Order(201, "2026-10-20 11:00:00");
        Order o2 = new Order(202, "2026-10-20 11:05:00");
        Order o3 = new Order(203, "2026-10-20 11:10:00");
        Order o4 = new Order(204, "2026-10-20 11:15:00");

        queue.enqueue(o1);
        queue.enqueue(o2);
        queue.enqueue(o3);
        queue.enqueue(o4);

        // Cancel order 202 (in the middle) via rotation
        Order cancelled = queue.cancel(202);
        if (cancelled == null || cancelled.getOrderNumber() != 202) return false;
        if (queue.getSize() != 3) return false;

        // Verify that remaining orders maintain their exact relative FIFO sequence: 201, 203, 204
        if (queue.dequeue().getOrderNumber() != 201) return false;
        if (queue.dequeue().getOrderNumber() != 203) return false;
        if (queue.dequeue().getOrderNumber() != 204) return false;
        if (!queue.isEmpty()) return false;

        System.out.println("PASSED ✔");
        return true;
    }

    private static boolean testBinarySearch() {
        System.out.print("[TEST 3] Binary Search Product Code & Order # (O(log n))... ");
        List<Product> products = new ArrayList<>();
        products.add(new Product("KP-101", "Tee A", "Apparel", 500, 10, 2));
        products.add(new Product("KP-102", "Tee B", "Apparel", 600, 10, 2));
        products.add(new Product("KP-105", "Tee C", "Apparel", 700, 10, 2));
        products.add(new Product("KP-109", "Tee D", "Apparel", 800, 10, 2));

        int idx1 = SearchAlgorithms.binarySearchProductByCode(products, "KP-105");
        if (idx1 != 2) return false;

        int idx2 = SearchAlgorithms.binarySearchProductByCode(products, "KP-999");
        if (idx2 != -1) return false;

        List<Order> orders = new ArrayList<>();
        orders.add(new Order(1001, "2026-10-20 10:00:00"));
        orders.add(new Order(1002, "2026-10-20 10:05:00"));
        orders.add(new Order(1003, "2026-10-20 10:10:00"));

        if (SearchAlgorithms.binarySearchOrderByNumber(orders, 1002) != 1) return false;
        if (SearchAlgorithms.binarySearchOrderByNumber(orders, 9999) != -1) return false;

        System.out.println("PASSED ✔");
        return true;
    }

    private static boolean testLinearSearch() {
        System.out.print("[TEST 4] Linear Search by partial product name (O(n))... ");
        List<Product> products = new ArrayList<>();
        products.add(new Product("KP-101", "Kingpin Classic Oversized Tee", "Apparel", 750, 10, 2));
        products.add(new Product("KP-103", "Manila Underground Hoodie", "Apparel", 1850, 8, 3));
        products.add(new Product("KP-107", "Kingpin Snapback Cap", "Accessories", 650, 15, 4));

        List<Product> teeMatches = SearchAlgorithms.linearSearchProductsByName(products, "tee");
        if (teeMatches.size() != 1 || !teeMatches.get(0).getCode().equals("KP-101")) return false;

        List<Product> kpMatches = SearchAlgorithms.linearSearchProductsByName(products, "kingpin");
        if (kpMatches.size() != 2) return false;

        System.out.println("PASSED ✔");
        return true;
    }

    private static boolean testInsertionSort() {
        System.out.print("[TEST 5] Insertion Sort on Product Codes... ");
        List<Product> list = new ArrayList<>();
        list.add(new Product("KP-109", "Item D", "Apparel", 100, 5, 1));
        list.add(new Product("KP-102", "Item B", "Apparel", 100, 5, 1));
        list.add(new Product("KP-105", "Item C", "Apparel", 100, 5, 1));
        list.add(new Product("KP-101", "Item A", "Apparel", 100, 5, 1));

        SortAlgorithms.insertionSortProductsByCode(list);

        if (!list.get(0).getCode().equals("KP-101")) return false;
        if (!list.get(1).getCode().equals("KP-102")) return false;
        if (!list.get(2).getCode().equals("KP-105")) return false;
        if (!list.get(3).getCode().equals("KP-109")) return false;

        // Test insertion step maintaining sort
        SortAlgorithms.insertInSortedOrder(list, new Product("KP-103", "Item X", "Apparel", 100, 5, 1));
        if (!list.get(2).getCode().equals("KP-103")) return false;

        System.out.println("PASSED ✔");
        return true;
    }

    private static boolean testMergeSort() {
        System.out.print("[TEST 6] Merge Sort on defensive copy (O(n log n))... ");
        List<Product> products = new ArrayList<>();
        products.add(new Product("KP-101", "A", "Apparel", 100, 25, 5));
        products.add(new Product("KP-102", "B", "Apparel", 100, 3, 5));
        products.add(new Product("KP-103", "C", "Apparel", 100, 12, 5));
        products.add(new Product("KP-104", "D", "Apparel", 100, 1, 5));

        // Sort ascending by stock
        List<Product> sortedByStock = SortAlgorithms.mergeSort(products, Comparator.comparingInt(Product::getStock));

        // Original list must NOT be modified
        if (!products.get(0).getCode().equals("KP-101")) return false;

        // Sorted copy must be strictly ascending
        if (sortedByStock.get(0).getStock() != 1) return false;
        if (sortedByStock.get(1).getStock() != 3) return false;
        if (sortedByStock.get(2).getStock() != 12) return false;
        if (sortedByStock.get(3).getStock() != 25) return false;

        System.out.println("PASSED ✔");
        return true;
    }

    private static boolean testOrderLifecycleAndStockRestoration() {
        System.out.print("[TEST 7] Order Lifecycle & Stock Deduction/Restoration... ");
        String testInvPath = "data/test_inventory.csv";
        String testSalesPath = "data/test_sales.csv";

        new File(testInvPath).delete();
        new File(testSalesPath).delete();

        // Seed test inventory
        List<Product> initial = new ArrayList<>();
        initial.add(new Product("KP-TEST", "Test Item", "Apparel", 500.0, 10, 2));
        com.kingpin.util.FileStorage.saveInventory(testInvPath, initial);

        InventoryService invService = new InventoryService(testInvPath);
        OrderService ordService = new OrderService(testSalesPath, invService);

        // Add to cart
        if (!ordService.addToCart("KP-TEST", 3)) return false;

        // Confirm order
        Order confirmed = ordService.confirmOrder(2000.0);
        if (confirmed == null) return false;

        // Check stock was deducted immediately from 10 to 7
        Product pAfterConfirm = invService.findByCode("KP-TEST");
        if (pAfterConfirm.getStock() != 7) return false;

        // Cancel order from queue -> verify stock restored from 7 to 10
        Order cancelled = ordService.cancelOrder(confirmed.getOrderNumber());
        if (cancelled == null) return false;

        Product pAfterCancel = invService.findByCode("KP-TEST");
        if (pAfterCancel.getStock() != 10) return false;

        // Clean up test files
        new File(testInvPath).delete();
        new File(testSalesPath).delete();

        System.out.println("PASSED ✔");
        return true;
    }
}
