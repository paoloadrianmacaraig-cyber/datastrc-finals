package com.kingpin.dsa;

import com.kingpin.model.Order;
import com.kingpin.model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, custom implementations of Searching Algorithms required by the DSA syllabus.
 *
 * Algorithms implemented:
 * 1. Binary Search: O(log n) for Product Code and Order Number lookups.
 * 2. Linear Search: O(n) for partial name matching across the inventory.
 */
public class SearchAlgorithms {

    /**
     * Performs an iterative Binary Search on a list of products sorted by product code.
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     *
     * @param products   Master product list, guaranteed to be sorted by product code ascending.
     * @param targetCode The product code to search for (e.g. "KP-103").
     * @return The zero-based index of the matching product, or -1 if not found.
     */
    public static int binarySearchProductByCode(List<Product> products, String targetCode) {
        if (products == null || targetCode == null || products.isEmpty()) {
            return -1;
        }

        String normalizedTarget = targetCode.trim().toUpperCase();
        int low = 0;
        int high = products.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            String midCode = products.get(mid).getCode().toUpperCase();
            int comparison = midCode.compareTo(normalizedTarget);

            if (comparison == 0) {
                return mid; // Match found
            } else if (comparison < 0) {
                low = mid + 1; // Target is in the right half
            } else {
                high = mid - 1; // Target is in the left half
            }
        }

        return -1; // Not found
    }

    /**
     * Performs an iterative Binary Search on sequential order history by order number.
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     *
     * @param orders            List of completed orders sorted sequentially by order number.
     * @param targetOrderNumber The order number to find (e.g. 1004).
     * @return The zero-based index of the order, or -1 if not found.
     */
    public static int binarySearchOrderByNumber(List<Order> orders, int targetOrderNumber) {
        if (orders == null || orders.isEmpty()) {
            return -1;
        }

        int low = 0;
        int high = orders.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midOrderNum = orders.get(mid).getOrderNumber();

            if (midOrderNum == targetOrderNumber) {
                return mid;
            } else if (midOrderNum < targetOrderNumber) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return -1;
    }

    /**
     * Performs a Linear Search over products to match partial or complete product names.
     * Required when a cashier searches with partial keywords without knowing the code.
     * Time Complexity: O(n)
     * Space Complexity: O(k) where k is the number of matching items.
     *
     * @param products Master product catalog.
     * @param query    Partial or full name substring (e.g. "hoodie", "cap", "tee").
     * @return List of all matching products found.
     */
    public static List<Product> linearSearchProductsByName(List<Product> products, String query) {
        List<Product> results = new ArrayList<>();
        if (products == null || query == null || query.trim().isEmpty()) {
            return results;
        }

        String lowerQuery = query.trim().toLowerCase();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            if (product.getName().toLowerCase().contains(lowerQuery) ||
                product.getCategory().toLowerCase().contains(lowerQuery)) {
                results.add(product);
            }
        }

        return results;
    }
}
