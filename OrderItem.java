/**
 * OrderItem entity representing an individual item line in a shopping cart or order.
 *
 * OOP Concepts:
 * - Encapsulation: Protects item price, quantity, and subtotal calculation integrity.
 */
public class OrderItem {
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

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    public String serialize() {
        return String.format("%s:%d:%.2f", productCode, quantity, unitPrice);
    }

    @Override
    public String toString() {
        return String.format("%-10s %-28s %3d x PHP %8.2f = PHP %8.2f",
                productCode,
                productName.length() > 28 ? productName.substring(0, 25) + "..." : productName,
                quantity,
                unitPrice,
                getSubtotal());
    }
}
