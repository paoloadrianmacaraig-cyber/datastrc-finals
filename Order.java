import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Order entity representing a confirmed transaction in the Kingpin MNL POS.
 *
 * OOP Concepts:
 * - Encapsulation: Manages financial totals, line items, and payment state changes.
 * - Comparable Interface: Natural ordering by sequential order number.
 */
public class Order implements Comparable<Order> {
    private int orderNumber;
    private String timestamp;
    private List<OrderItem> items;
    private double totalAmount;
    private double cashReceived;
    private double change;
    private String status; // "PENDING", "SERVED", "CANCELLED"

    public Order(int orderNumber, String timestamp) {
        this(orderNumber, timestamp, new ArrayList<>(), 0.0, 0.0, 0.0, "PENDING");
    }

    public Order(int orderNumber, String timestamp, List<OrderItem> items, double totalAmount,
                 double cashReceived, double change, String status) {
        this.orderNumber = orderNumber;
        this.timestamp = timestamp;
        this.items = (items != null) ? items : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.cashReceived = cashReceived;
        this.change = Math.max(0.0, change);
        this.status = (status != null) ? status : "PENDING";
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
        if (item != null) {
            this.items.add(item);
            recalculateTotal();
        }
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

    @Override
    public int compareTo(Order other) {
        if (other == null) return 1;
        return Integer.compare(this.orderNumber, other.orderNumber);
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i).serialize());
            if (i < items.size() - 1) {
                sb.append(";");
            }
        }
        return String.format("%d,%s,%s,%.2f,%.2f,%.2f,%s",
                orderNumber,
                timestamp,
                sb.toString(),
                totalAmount,
                cashReceived,
                change,
                status);
    }

    public static Order fromCsv(String line, Map<String, Product> productLookup) {
        String[] parts = line.split(",");
        if (parts.length < 7) {
            throw new IllegalArgumentException("Malformed order CSV: " + line);
        }
        int orderNum = Integer.parseInt(parts[0].trim());
        String time = parts[1].trim();
        String serialized = parts[2].trim();
        double total = Double.parseDouble(parts[3].trim());
        double cash = Double.parseDouble(parts[4].trim());
        double chg = Double.parseDouble(parts[5].trim());
        String st = parts[6].trim();

        List<OrderItem> list = new ArrayList<>();
        if (!serialized.isEmpty()) {
            for (String token : serialized.split(";")) {
                String[] sub = token.split(":");
                if (sub.length >= 3) {
                    String code = sub[0];
                    int qty = Integer.parseInt(sub[1]);
                    double pr = Double.parseDouble(sub[2]);
                    String name = (productLookup != null && productLookup.containsKey(code))
                            ? productLookup.get(code).getName()
                            : code;
                    list.add(new OrderItem(code, name, pr, qty));
                }
            }
        }
        return new Order(orderNum, time, list, total, cash, chg, st);
    }
}
