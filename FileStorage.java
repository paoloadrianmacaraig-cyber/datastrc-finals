import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles persistent CSV file storage for inventory and sales history.
 * Automatically seeds default streetwear catalog if the file is absent.
 */
public class FileStorage {

    public static List<Product> loadInventory(String filePath) {
        List<Product> products = new ArrayList<>();
        File file = new File(filePath);

        // If file does not exist, seed default catalog
        if (!file.exists()) {
            products.addAll(getDefaultSeedProducts());
            saveInventory(filePath, products);
            return products;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        products.add(Product.fromCsv(line));
                    } catch (Exception e) {
                        // ignore malformed line
                    }
                }
            }
        } catch (IOException e) {
            products.addAll(getDefaultSeedProducts());
        }

        if (products.isEmpty()) {
            products.addAll(getDefaultSeedProducts());
            saveInventory(filePath, products);
        }

        return products;
    }

    public static void saveInventory(String filePath, List<Product> products) {
        ensureParentDirectory(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("code,name,category,price,stock,lowStockThreshold");
            writer.newLine();
            for (Product p : products) {
                writer.write(p.toCsv());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving inventory: " + e.getMessage());
        }
    }

    public static List<Order> loadSalesHistory(String filePath, List<Product> inventory) {
        List<Order> orders = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return orders;
        }

        Map<String, Product> productMap = new HashMap<>();
        if (inventory != null) {
            for (Product p : inventory) {
                productMap.put(p.getCode().toUpperCase(), p);
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        orders.add(Order.fromCsv(line, productMap));
                    } catch (Exception e) {
                        // ignore malformed line
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return orders;
    }

    public static void appendSalesHistory(String filePath, Order order) {
        ensureParentDirectory(filePath);
        boolean exists = new File(filePath).exists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            if (!exists) {
                writer.write("orderNumber,timestamp,itemsSerialized,totalAmount,cashReceived,change,status");
                writer.newLine();
            }
            writer.write(order.toCsv());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error appending to sales history: " + e.getMessage());
        }
    }

    private static List<Product> getDefaultSeedProducts() {
        List<Product> list = new ArrayList<>();
        list.add(new Product("KP-101", "Kingpin Classic Oversized Tee - Black", "Apparel", 750.00, 25, 5));
        list.add(new Product("KP-102", "Kingpin Classic Oversized Tee - White", "Apparel", 750.00, 18, 5));
        list.add(new Product("KP-103", "Manila Underground Heavyweight Hoodie", "Apparel", 1850.00, 8, 3));
        list.add(new Product("KP-104", "Cyberpunk Metro Graphic Tee", "Apparel", 850.00, 12, 5));
        list.add(new Product("KP-105", "Kingpin Signature Boxy Fit Tee - Olive", "Apparel", 790.00, 4, 5));
        list.add(new Product("KP-106", "Raw Denim Streetwear Cargo Pants", "Apparel", 1650.00, 6, 3));
        list.add(new Product("KP-107", "Kingpin 5-Panel Snapback Cap", "Accessories", 650.00, 15, 4));
        list.add(new Product("KP-108", "Heavy Canvas Utility Tote Bag", "Accessories", 450.00, 20, 5));
        list.add(new Product("KP-109", "Kingpin Vinyl Holographic Sticker Pack", "Accessories", 150.00, 40, 10));
        list.add(new Product("KP-110", "Reversible Bomber Jacket - MNL Edition", "Apparel", 2450.00, 3, 2));
        list.add(new Product("KP-111", "Acid Wash Vintage Graphic Tee", "Apparel", 890.00, 14, 5));
        list.add(new Product("KP-112", "Industrial Webbing Key Lanyard", "Accessories", 250.00, 30, 8));
        return list;
    }

    private static void ensureParentDirectory(String filePath) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
