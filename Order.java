import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a customer order in the Kingpin MNL POS and Order Queue system.
 */
public class Order {
    private int orderNumber;
    private String timestamp;
    private List<OrderItem> items;
    private double totalAmount;
    private double cashReceived;
    private double change;
    private String status; // "PENDING", "SERVED", "CANCELLED"

    public Order(int orderNumber, String timestamp) {
        this.orderNumber = orderNumber;
        this.timestamp = timestamp;
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.cashReceived = 0.0;
        this.change = 0.0;
        this.status = "PENDING";
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

    public int getOrderNumber() {
        return orderNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(double cashReceived) {
        this.cashReceived = cashReceived;
        this.change = Math.max(0.0, cashReceived - totalAmount);
    }

    public double getChange() {
        return change;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        double sum = 0.0;
        for (OrderItem item : items) {
            sum += item.getSubtotal();
        }
        this.totalAmount = sum;
        if (cashReceived > 0) {
            this.change = Math.max(0.0, cashReceived - totalAmount);
        }
    }

    public int getTotalUnits() {
        int count = 0;
        for (OrderItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public String serializeItems() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i).serialize());
            if (i < items.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public String toCsv() {
        return String.format("%d,%s,%s,%.2f,%.2f,%.2f,%s",
                orderNumber,
                timestamp,
                serializeItems(),
                totalAmount,
                cashReceived,
                change,
                status);
    }

    public static Order fromCsv(String csvLine, Map<String, Product> productLookup) {
        String[] parts = csvLine.split(",");
        if (parts.length < 7) {
            throw new IllegalArgumentException("Invalid order CSV: " + csvLine);
        }
        int orderNumber = Integer.parseInt(parts[0].trim());
        String timestamp = parts[1].trim();
        String serializedItems = parts[2].trim();
        double totalAmount = Double.parseDouble(parts[3].trim());
        double cashReceived = Double.parseDouble(parts[4].trim());
        double change = Double.parseDouble(parts[5].trim());
        String status = parts[6].trim();

        List<OrderItem> items = new ArrayList<>();
        if (!serializedItems.isEmpty()) {
            String[] itemTokens = serializedItems.split(";");
            for (String tok : itemTokens) {
                String[] itemParts = tok.split(":");
                if (itemParts.length >= 3) {
                    String code = itemParts[0];
                    int qty = Integer.parseInt(itemParts[1]);
                    double price = Double.parseDouble(itemParts[2]);
                    String name = (productLookup != null && productLookup.containsKey(code))
                            ? productLookup.get(code).getName()
                            : code;
                    items.add(new OrderItem(code, name, price, qty));
                }
            }
        }

        return new Order(orderNumber, timestamp, items, totalAmount, cashReceived, change, status);
    }
}
