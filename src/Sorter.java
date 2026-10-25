// Author: Tengonciang
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Sorter {

    // Merge sort: always O(n log n) and stable (equal items keep their order).
    public static <T> void mergeSort(List<T> list, Comparator<T> cmp) {
        if (list.size() < 2) {
            return;
        }
        int mid = list.size() / 2;
        List<T> left = new ArrayList<>(list.subList(0, mid));
        List<T> right = new ArrayList<>(list.subList(mid, list.size()));
        mergeSort(left, cmp);
        mergeSort(right, cmp);

        int i = 0, j = 0, k = 0;
        while (i < left.size() && j < right.size()) {
            // "<=" is what keeps the sort stable
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                list.set(k++, left.get(i++));
            } else {
                list.set(k++, right.get(j++));
            }
        }
        while (i < left.size()) {
            list.set(k++, left.get(i++));
        }
        while (j < right.size()) {
            list.set(k++, right.get(j++));
        }
    }

    // Quick sort with a median-of-three pivot.
    public static <T> void quickSort(List<T> list, Comparator<T> cmp) {
        quickSort(list, 0, list.size() - 1, cmp);
    }

    private static <T> void quickSort(List<T> list, int low, int high, Comparator<T> cmp) {
        if (low >= high) {
            return;
        }
        int p = partition(list, low, high, cmp);
        quickSort(list, low, p - 1, cmp);
        quickSort(list, p + 1, high, cmp);
    }

    // Lomuto partition, median-of-three pivot
    private static <T> int partition(List<T> list, int low, int high, Comparator<T> cmp) {
        int mid = low + (high - low) / 2;
        if (cmp.compare(list.get(mid), list.get(low)) < 0) swap(list, mid, low);
        if (cmp.compare(list.get(high), list.get(low)) < 0) swap(list, high, low);
        if (cmp.compare(list.get(mid), list.get(high)) < 0) swap(list, mid, high);
        T pivot = list.get(high);     // the middle value of the three is now at high
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                swap(list, ++i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    // Insertion sort: good for short lists and lists that are almost sorted.
    public static <T> void insertionSort(List<T> list, Comparator<T> cmp) {
        for (int i = 1; i < list.size(); i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && cmp.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));    // shift bigger items to the right
                j--;
            }
            list.set(j + 1, key);
        }
    }

    private static <T> void swap(List<T> list, int a, int b) {
        T temp = list.get(a);
        list.set(a, list.get(b));
        list.set(b, temp);
    }
}
