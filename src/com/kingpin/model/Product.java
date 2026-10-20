package com.kingpin.model;

/**
 * Represents a retail streetwear product in the Kingpin MNL inventory.
 */
public class Product {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase() : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim() : "General";
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStock() {
        return stock <= lowStockThreshold;
    }

    public void deductStock(int quantity) {
        if (quantity <= stock) {
            this.stock -= quantity;
        } else {
            throw new IllegalArgumentException("Cannot deduct " + quantity + " units; only " + stock + " available.");
        }
    }

    public void addStock(int quantity) {
        if (quantity > 0) {
            this.stock += quantity;
        }
    }

    public String toCsv() {
        return String.format("%s,%s,%s,%.2f,%d,%d",
                escapeCsv(code),
                escapeCsv(name),
                escapeCsv(category),
                price,
                stock,
                lowStockThreshold);
    }

    public static Product fromCsv(String csvLine) {
        String[] parts = parseCsvLine(csvLine);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid product CSV line: " + csvLine);
        }
        String code = parts[0];
        String name = parts[1];
        String category = parts[2];
        double price = Double.parseDouble(parts[3]);
        int stock = Integer.parseInt(parts[4]);
        int threshold = Integer.parseInt(parts[5]);
        return new Product(code, name, category, price, stock, threshold);
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] parseCsvLine(String line) {
        java.util.List<String> list = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        list.add(sb.toString().trim());
        return list.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | PHP %.2f | Stock: %d (Min: %d)",
                code, name, category, price, stock, lowStockThreshold);
    }
}
