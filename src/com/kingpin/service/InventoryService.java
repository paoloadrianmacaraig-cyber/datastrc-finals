package com.kingpin.service;

import com.kingpin.dsa.SearchAlgorithms;
import com.kingpin.dsa.SortAlgorithms;
import com.kingpin.model.Product;
import com.kingpin.util.FileStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service managing inventory operations for Kingpin MNL.
 * Enforces the invariant that products are always maintained in sorted order by product code,
 * enabling O(log n) Binary Search for fast cashier lookups.
 */
public class InventoryService {
    private final String inventoryFilePath;
    private final List<Product> products;

    public InventoryService(String inventoryFilePath) {
        this.inventoryFilePath = inventoryFilePath;
        this.products = new ArrayList<>();
        loadAndSortInventory();
    }

    /**
     * Loads products from CSV and executes Insertion Sort to guarantee the list
     * is sorted by product code ascending.
     */
    public void loadAndSortInventory() {
        this.products.clear();
        List<Product> loaded = FileStorage.loadInventory(inventoryFilePath);
        this.products.addAll(loaded);

        // Sort by product code using Insertion Sort
        SortAlgorithms.insertionSortProductsByCode(this.products);
    }

    public void saveInventory() {
        FileStorage.saveInventory(inventoryFilePath, this.products);
    }

    /**
     * Looks up a product by exact code using Binary Search in O(log n) time.
     */
    public Product findByCode(String code) {
        int index = SearchAlgorithms.binarySearchProductByCode(products, code);
        if (index >= 0) {
            return products.get(index);
        }
        return null;
    }

    /**
     * Searches products matching partial name using Linear Search in O(n) time.
     */
    public List<Product> searchByName(String query) {
        return SearchAlgorithms.linearSearchProductsByName(products, query);
    }

    /**
     * Adds a new product to inventory.
     * Uses binary search to prevent duplicate codes, then uses insertion sort's
     * insertion step to maintain sorted order in O(n) time.
     */
    public boolean addProduct(Product product) {
        if (product == null || product.getCode().isEmpty()) {
            return false;
        }
        if (findByCode(product.getCode()) != null) {
            return false; // Code already exists
        }

        SortAlgorithms.insertInSortedOrder(products, product);
        saveInventory();
        return true;
    }

    /**
     * Records replenishment stock received from the main warehouse/branch.
     */
    public boolean recordStockReceived(String code, int quantity) {
        Product p = findByCode(code);
        if (p == null || quantity <= 0) {
            return false;
        }
        p.addStock(quantity);
        saveInventory();
        return true;
    }

    /**
     * Deducts stock immediately upon order confirmation.
     */
    public boolean deductStock(String code, int quantity) {
        Product p = findByCode(code);
        if (p == null || p.getStock() < quantity) {
            return false;
        }
        p.deductStock(quantity);
        saveInventory();
        return true;
    }

    /**
     * Restores previously deducted stock if an order is cancelled.
     */
    public boolean restoreStock(String code, int quantity) {
        Product p = findByCode(code);
        if (p == null) {
            return false;
        }
        p.addStock(quantity);
        saveInventory();
        return true;
    }

    /**
     * Updates an existing product's metadata.
     */
    public boolean updateProduct(String code, String newName, String newCategory, double newPrice, int newThreshold) {
        Product p = findByCode(code);
        if (p == null) {
            return false;
        }
        p.setName(newName);
        p.setCategory(newCategory);
        p.setPrice(newPrice);
        p.setLowStockThreshold(newThreshold);
        saveInventory();
        return true;
    }

    /**
     * Removes a product by code from the catalog.
     */
    public boolean removeProduct(String code) {
        int index = SearchAlgorithms.binarySearchProductByCode(products, code);
        if (index >= 0) {
            products.remove(index);
            saveInventory();
            return true;
        }
        return false;
    }

    /**
     * Returns all products currently at or below their low-stock threshold.
     */
    public List<Product> getLowStockProducts() {
        List<Product> lowStock = new ArrayList<>();
        for (Product p : products) {
            if (p.isLowStock()) {
                lowStock.add(p);
            }
        }
        return lowStock;
    }

    public List<Product> getAllProducts() {
        return Collections.unmodifiableList(products);
    }
}
