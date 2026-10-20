import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service orchestrating order lifecycle, FIFO queue operations,
 * stock deduction, and cancellation by queue rotation.
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

    public boolean addToCart(String productCode, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        Product product = inventoryService.findByCode(productCode);
        if (product == null) {
            return false;
        }

        int existingInCart = 0;
        for (OrderItem item : currentCart) {
            if (item.getProductCode().equalsIgnoreCase(product.getCode())) {
                existingInCart += item.getQuantity();
            }
        }

        if (existingInCart + quantity > product.getStock()) {
            return false;
        }

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

    public Order confirmOrder(double cashReceived) {
        if (currentCart.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }
        double total = getCartTotal();
        if (cashReceived < total) {
            throw new IllegalArgumentException(String.format("Insufficient cash. Total: PHP %.2f, Provided: PHP %.2f", total, cashReceived));
        }

        for (OrderItem item : currentCart) {
            Product p = inventoryService.findByCode(item.getProductCode());
            if (p == null || p.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for: " + item.getProductName());
            }
        }

        for (OrderItem item : currentCart) {
            inventoryService.deductStock(item.getProductCode(), item.getQuantity());
        }

        int assignedOrderNumber = nextOrderNumber++;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        List<OrderItem> orderItems = new ArrayList<>(currentCart);
        Order order = new Order(assignedOrderNumber, timestamp, orderItems, total, cashReceived, cashReceived - total, "PENDING");

        orderQueue.enqueue(order);
        clearCart();

        return order;
    }

    public Order peekNextOrder() {
        return orderQueue.peek();
    }

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

    public Order cancelOrder(int orderNumber) {
        Order cancelled = orderQueue.cancel(orderNumber);
        if (cancelled != null) {
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
