package com.kingpin.util;

import com.kingpin.model.Order;
import com.kingpin.model.Product;

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
 * Ensures data survives application restarts for main-branch synchronization.
 */
public class FileStorage {

    public static List<Product> loadInventory(String filePath) {
        List<Product> products = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
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
                        System.err.println("Warning: Skipping malformed product line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading inventory file: " + e.getMessage());
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
            System.err.println("Error writing inventory file: " + e.getMessage());
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
                        System.err.println("Warning: Skipping malformed sales history line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading sales history file: " + e.getMessage());
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
            System.err.println("Error appending to sales history file: " + e.getMessage());
        }
    }

    public static void saveSalesHistory(String filePath, List<Order> orders) {
        ensureParentDirectory(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("orderNumber,timestamp,itemsSerialized,totalAmount,cashReceived,change,status");
            writer.newLine();
            for (Order o : orders) {
                writer.write(o.toCsv());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing sales history file: " + e.getMessage());
        }
    }

    private static void ensureParentDirectory(String filePath) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
