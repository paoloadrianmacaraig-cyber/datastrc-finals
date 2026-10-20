package com.kingpin.service;

import com.kingpin.dsa.OrderQueue;
import com.kingpin.dsa.SearchAlgorithms;
import com.kingpin.model.Order;
import com.kingpin.model.OrderItem;
import com.kingpin.model.Product;
import com.kingpin.util.FileStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service orchestrating the complete lifecycle of orders for Kingpin MNL:
 * Build order -> check stock -> confirm (deduct stock, assign order number, enqueue)
 * -> wait in queue -> serve (dequeue, mark complete, save to history) -> included in reports.
 * Cancel works by rotating the queue to remove the order, then restoring stock.
 */
public class OrderService {
    private final String salesHistoryFilePath;
    private final InventoryService inventoryService;
    private final OrderQueue orderQueue;
    private final List<Order> salesHistory;
    private final List<OrderItem> currentCart;
    private int nextOrderNumber;

    public OrderService(String salesHistoryFilePath, InventoryService inventoryService) {
        this.salesHistoryFilePath = salesHistoryFilePath;
        this.inventoryService = inventoryService;
        this.orderQueue = new OrderQueue();
        this.currentCart = new ArrayList<>();
        this.salesHistory = new ArrayList<>();
        loadSalesHistory();
        initializeNextOrderNumber();
    }

    private void loadSalesHistory() {
        this.salesHistory.clear();
        List<Order> loaded = FileStorage.loadSalesHistory(salesHistoryFilePath, inventoryService.getAllProducts());
        this.salesHistory.addAll(loaded);
    }

    private void initializeNextOrderNumber() {
        int maxOrder = 1000;
        for (Order o : salesHistory) {
            if (o.getOrderNumber() > maxOrder) {
                maxOrder = o.getOrderNumber();
            }
        }
        this.nextOrderNumber = maxOrder + 1;
    }

    // --- Cart Management ---

    public boolean addToCart(String productCode, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        Product product = inventoryService.findByCode(productCode);
        if (product == null) {
            return false;
        }

        // Calculate quantity already staged in current cart
        int existingInCart = 0;
        for (OrderItem item : currentCart) {
            if (item.getProductCode().equalsIgnoreCase(product.getCode())) {
                existingInCart += item.getQuantity();
            }
        }

        if (existingInCart + quantity > product.getStock()) {
            return false; // Insufficient stock
        }

        // Update existing item or add new
        boolean foundInCart = false;
        for (OrderItem item : currentCart) {
            if (item.getProductCode().equalsIgnoreCase(product.getCode())) {
                item.setQuantity(item.getQuantity() + quantity);
                foundInCart = true;
                break;
            }
        }

        if (!foundInCart) {
            currentCart.add(new OrderItem(product.getCode(), product.getName(), product.getPrice(), quantity));
        }

        return true;
    }

    public boolean removeFromCart(String productCode) {
        for (int i = 0; i < currentCart.size(); i++) {
            if (currentCart.get(i).getProductCode().equalsIgnoreCase(productCode)) {
                currentCart.remove(i);
                return true;
            }
        }
        return false;
    }

    public void clearCart() {
        currentCart.clear();
    }

    public List<OrderItem> getCurrentCart() {
        return currentCart;
    }

    public double getCartTotal() {
        double total = 0.0;
        for (OrderItem item : currentCart) {
            total += item.getSubtotal();
        }
        return total;
    }

    // --- Order Confirmation (Enqueue) ---

    /**
     * Confirms current cart into an official order:
     * 1. Validates cash received is sufficient.
     * 2. Checks and immediately deducts stock for all cart items (preventing overselling).
     * 3. Assigns unique sequential order number.
     * 4. Enqueues order into custom OrderQueue in O(1) time.
     * 5. Clears active cart.
     */
    public Order confirmOrder(double cashReceived) {
        if (currentCart.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }
        double total = getCartTotal();
        if (cashReceived < total) {
            throw new IllegalArgumentException(String.format("Insufficient cash. Total: PHP %.2f, Provided: PHP %.2f", total, cashReceived));
        }

        // Verify stock for all items before any deduction
        for (OrderItem item : currentCart) {
            Product p = inventoryService.findByCode(item.getProductCode());
            if (p == null || p.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for: " + item.getProductName());
            }
        }

        // Deduct stock immediately
        for (OrderItem item : currentCart) {
            inventoryService.deductStock(item.getProductCode(), item.getQuantity());
        }

        int assignedOrderNumber = nextOrderNumber++;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        List<OrderItem> orderItems = new ArrayList<>(currentCart);
        Order order = new Order(assignedOrderNumber, timestamp, orderItems, total, cashReceived, cashReceived - total, "PENDING");

        // Enqueue into pending orders queue
        orderQueue.enqueue(order);

        // Reset cart for next customer
        clearCart();

        return order;
    }

    // --- Order Queue Operations ---

    public Order peekNextOrder() {
        return orderQueue.peek();
    }

    /**
     * Serves the front pending order:
     * 1. Dequeues from OrderQueue in O(1) time.
     * 2. Marks order as "SERVED".
     * 3. Appends to persistent sales history CSV.
     * 4. Adds to in-memory sales history list for reporting and binary search lookups.
     */
    public Order serveNextOrder() {
        if (orderQueue.isEmpty()) {
            return null;
        }

        Order served = orderQueue.dequeue();
        served.setStatus("SERVED");

        salesHistory.add(served);
        FileStorage.appendSalesHistory(salesHistoryFilePath, served);

        return served;
    }

    /**
     * Cancels an order from anywhere in the queue using queue rotation in O(n) time.
     * Restores the deducted stock back to inventory upon successful cancellation.
     */
    public Order cancelOrder(int orderNumber) {
        Order cancelled = orderQueue.cancel(orderNumber);
        if (cancelled != null) {
            // Restore inventory stock for each item in the cancelled order
            for (OrderItem item : cancelled.getItems()) {
                inventoryService.restoreStock(item.getProductCode(), item.getQuantity());
            }
        }
        return cancelled;
    }

    public List<Order> getPendingOrders() {
        return orderQueue.getAllOrders();
    }

    public int getPendingOrderCount() {
        return orderQueue.getSize();
    }

    // --- Search in Sales History ---

    /**
     * Performs Binary Search on completed sales history by order number in O(log n) time.
     */
    public Order findOrderInHistory(int orderNumber) {
        int index = SearchAlgorithms.binarySearchOrderByNumber(salesHistory, orderNumber);
        if (index >= 0) {
            return salesHistory.get(index);
        }
        return null;
    }

    public List<Order> getSalesHistory() {
        return salesHistory;
    }
}
