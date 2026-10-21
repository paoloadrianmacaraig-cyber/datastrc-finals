import java.util.ArrayList;
import java.util.List;

/**
 * Pure, custom implementations of Searching Algorithms for DSA syllabus.
 *
 * Algorithms implemented:
 * 1. Binary Search: O(log n) iterative search for Product Code and Order Number lookups.
 * 2. Linear Search: O(n) search for partial or multi-keyword product name queries.
 */
public class SearchAlgorithms {

    /**
     * Performs an iterative Binary Search on a list of products sorted by code.
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     *
     * @param products   Master catalog sorted in ascending lexicographical order by code.
     * @param targetCode The exact product code to look up (e.g. "KP-103").
     * @return The 0-based index of the matching product, or -1 if not found.
     */
    public static int binarySearchProductByCode(List<Product> products, String targetCode) {
        if (products == null || targetCode == null || products.isEmpty()) {
            return -1;
        }

        String target = targetCode.trim().toUpperCase();
        int low = 0;
        int high = products.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            String midCode = products.get(mid).getCode().toUpperCase();
            int cmp = midCode.compareTo(target);

            if (cmp == 0) {
                return mid; // Match found
            } else if (cmp < 0) {
                low = mid + 1; // Search right half
            } else {
                high = mid - 1; // Search left half
            }
        }
        return -1;
    }

    /**
     * Performs an iterative Binary Search on sequential order history by order number.
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     *
     * @param orders            List of completed orders sorted sequentially by order number.
     * @param targetOrderNumber Unique order number to search for (e.g. 1004).
     * @return The 0-based index of the order, or -1 if not found.
     */
    public static int binarySearchOrderByNumber(List<Order> orders, int targetOrderNumber) {
        if (orders == null || orders.isEmpty()) {
            return -1;
        }

        int low = 0;
        int high = orders.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midNum = orders.get(mid).getOrderNumber();

            if (midNum == targetOrderNumber) {
                return mid;
            } else if (midNum < targetOrderNumber) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    /**
     * Performs a Linear Search over products to match partial keywords in name or category.
     * Necessary when a cashier does not know the exact product code.
     * Time Complexity: O(n)
     * Space Complexity: O(k) where k is the number of matched products.
     *
     * @param products Master product catalog.
     * @param query    Partial keyword string (e.g. "hoodie", "tee", "cap").
     * @return A list containing all matching products.
     */
    public static List<Product> linearSearchProductsByName(List<Product> products, String query) {
        List<Product> results = new ArrayList<>();
        if (products == null || query == null || query.trim().isEmpty()) {
            return results;
        }

        String q = query.trim().toLowerCase();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            if (p.getName().toLowerCase().contains(q) || p.getCategory().toLowerCase().contains(q)) {
                results.add(p);
            }
        }
        return results;
    }
}
