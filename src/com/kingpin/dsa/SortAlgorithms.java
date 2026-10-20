package com.kingpin.dsa;

import com.kingpin.model.Product;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure, custom implementations of Sorting Algorithms required by the DSA syllabus.
 *
 * Algorithms implemented:
 * 1. Insertion Sort: O(n^2) worst case, O(n) best case.
 *    Used to sort inventory by Product Code upon startup and during incremental product additions.
 * 2. Merge Sort: Guaranteed O(n log n) stable divide-and-conquer algorithm.
 *    Used on defensive copies to generate Best Sellers and Low-Stock reports without
 *    breaking the binary-searchable code-sorted invariant of the master inventory.
 */
public class SortAlgorithms {

    /**
     * Performs an in-place Insertion Sort on a product list ordered by product code ascending.
     * Time Complexity: O(n^2) worst/average, O(n) best (nearly sorted).
     * Space Complexity: O(1).
     *
     * @param products The list of products to sort in-place.
     */
    public static void insertionSortProductsByCode(List<Product> products) {
        if (products == null || products.size() <= 1) {
            return;
        }

        int n = products.size();
        for (int i = 1; i < n; i++) {
            Product key = products.get(i);
            int j = i - 1;

            // Shift elements that have greater code than key
            while (j >= 0 && products.get(j).getCode().compareToIgnoreCase(key.getCode()) > 0) {
                products.set(j + 1, products.get(j));
                j--;
            }
            products.set(j + 1, key);
        }
    }

    /**
     * Inserts a single product into an already code-sorted list using the single-step
     * logic of Insertion Sort, maintaining the sorted invariant.
     * Time Complexity: O(n)
     *
     * @param products   Master list (already sorted by code).
     * @param newProduct Product to insert.
     */
    public static void insertInSortedOrder(List<Product> products, Product newProduct) {
        if (products == null || newProduct == null) {
            return;
        }

        int i = products.size() - 1;
        products.add(newProduct); // expand size by 1

        while (i >= 0 && products.get(i).getCode().compareToIgnoreCase(newProduct.getCode()) > 0) {
            products.set(i + 1, products.get(i));
            i--;
        }
        products.set(i + 1, newProduct);
    }

    /**
     * Performs a stable Merge Sort on a defensive copy of a list using a custom Comparator.
     * Guaranteed Time Complexity: O(n log n)
     * Space Complexity: O(n)
     *
     * @param list       Input list to sort (remains unmodified).
     * @param comparator Ordering rule (e.g. descending sales, ascending stock).
     * @param <T>        Element type.
     * @return A newly sorted ArrayList copy.
     */
    public static <T> List<T> mergeSort(List<T> list, Comparator<T> comparator) {
        if (list == null) {
            return new ArrayList<>();
        }
        // Create defensive copy to never disturb source list
        List<T> copy = new ArrayList<>(list);
        if (copy.size() <= 1) {
            return copy;
        }
        mergeSortRecursive(copy, 0, copy.size() - 1, comparator);
        return copy;
    }

    private static <T> void mergeSortRecursive(List<T> list, int left, int right, Comparator<T> comparator) {
        if (left < right) {
            int mid = left + (right - left) / 2;

            // Divide
            mergeSortRecursive(list, left, mid, comparator);
            mergeSortRecursive(list, mid + 1, right, comparator);

            // Conquer (Combine)
            merge(list, left, mid, right, comparator);
        }
    }

    private static <T> void merge(List<T> list, int left, int mid, int right, Comparator<T> comparator) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        List<T> leftList = new ArrayList<>(n1);
        List<T> rightList = new ArrayList<>(n2);

        for (int i = 0; i < n1; i++) {
            leftList.add(list.get(left + i));
        }
        for (int j = 0; j < n2; j++) {
            rightList.add(list.get(mid + 1 + j));
        }

        int i = 0, j = 0, k = left;

        while (i < n1 && j < n2) {
            if (comparator.compare(leftList.get(i), rightList.get(j)) <= 0) {
                list.set(k, leftList.get(i));
                i++;
            } else {
                list.set(k, rightList.get(j));
                j++;
            }
            k++;
        }

        while (i < n1) {
            list.set(k, leftList.get(i));
            i++;
            k++;
        }

        while (j < n2) {
            list.set(k, rightList.get(j));
            j++;
            k++;
        }
    }
}
