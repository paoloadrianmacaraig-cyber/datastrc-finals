/**
 * Product entity model representing retail streetwear items in Kingpin MNL.
 *
 * OOP Concepts:
 * - Encapsulation: Strict data hiding with private fields and mutator validation.
 * - Polymorphism & Interface Implementation: Implements Comparable<Product> for natural
 *   lexicographical ordering by product code.
 */
public class Product implements Comparable<Product> {
    private String code;
    private String name;
    private String category;
    private double price;
    private int stock;
    private int lowStockThreshold;

    public Product(String code, String name, String category, double price, int stock, int lowStockThreshold) {
        this.code = (code != null) ? code.trim().toUpperCase() : "";
        this.name = (name != null) ? name.trim() : "";
        this.category = (category != null) ? category.trim() : "General";
        this.price = Math.max(0.0, price);
        this.stock = Math.max(0, stock);
        this.lowStockThreshold = Math.max(0, lowStockThreshold);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = (code != null) ? code.trim().toUpperCase() : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name != null) ? name.trim() : "";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = (category != null) ? category.trim() : "General";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0.0, price);
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = Math.max(0, lowStockThreshold);
    }

    public boolean isLowStock() {
        return stock <= lowStockThreshold;
    }

    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to deduct must be positive.");
        }
        if (quantity > stock) {
            throw new IllegalArgumentException(String.format("Cannot deduct %d units; only %d in stock.", quantity, stock));
        }
        this.stock -= quantity;
    }

    public void addStock(int quantity) {
        if (quantity > 0) {
            this.stock += quantity;
        }
    }

    @Override
    public int compareTo(Product other) {
        if (other == null) return 1;
        return this.code.compareToIgnoreCase(other.code);
    }

    public String toCsv() {
        return String.format("%s,%s,%s,%.2f,%d,%d",
                code,
                name.replace(",", " "),
                category.replace(",", " "),
                price,
                stock,
                lowStockThreshold);
    }

    public static Product fromCsv(String line) {
        String[] parts = line.split(",");
        if (parts.length < 6) {
            throw new IllegalArgumentException("Malformed product CSV: " + line);
        }
        String code = parts[0].trim();
        String name = parts[1].trim();
        String category = parts[2].trim();
        double price = Double.parseDouble(parts[3].trim());
        int stock = Integer.parseInt(parts[4].trim());
        int threshold = Integer.parseInt(parts[5].trim());
        return new Product(code, name, category, price, stock, threshold);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | PHP %.2f | Stock: %d (Alert <= %d)",
                code, name, category, price, stock, lowStockThreshold);
    }
}
