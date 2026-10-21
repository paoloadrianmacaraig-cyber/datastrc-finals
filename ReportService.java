import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates managerial and analytical reports for Kingpin MNL.
 *
 * Employs Merge Sort (O(n log n)) on defensive copies to guarantee stable,
 * high-performance sorting without altering the master inventory's code-sorted invariant.
 */
public class ReportService {
    private final InventoryService inventoryService;
    private final OrderService orderService;

    public ReportService(InventoryService inventoryService, OrderService orderService) {
        this.inventoryService = inventoryService;
        this.orderService = orderService;
    }

    /**
     * DTO representing product sales performance.
     */
    public static class ProductSalesStat {
        private final String productCode;
        private final String productName;
        private int unitsSold;
        private double totalRevenue;

        public ProductSalesStat(String productCode, String productName) {
            this.productCode = productCode;
            this.productName = productName;
            this.unitsSold = 0;
            this.totalRevenue = 0.0;
        }

        public void addSale(int units, double revenue) {
            this.unitsSold += units;
            this.totalRevenue += revenue;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getProductName() {
            return productName;
        }

        public int getUnitsSold() {
            return unitsSold;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }
    }

    /**
     * Generates Best Sellers ranking sorted descending by units sold.
     * Uses Merge Sort on an aggregated defensive copy.
     * Time Complexity: O(n log n)
     */
    public List<ProductSalesStat> getBestSellers() {
        Map<String, ProductSalesStat> statMap = new HashMap<>();

        for (Order order : orderService.getSalesHistory()) {
            if ("SERVED".equalsIgnoreCase(order.getStatus())) {
                for (OrderItem item : order.getItems()) {
                    String code = item.getProductCode().toUpperCase();
                    statMap.putIfAbsent(code, new ProductSalesStat(code, item.getProductName()));
                    statMap.get(code).addSale(item.getQuantity(), item.getSubtotal());
                }
            }
        }

        List<ProductSalesStat> statsList = new ArrayList<>(statMap.values());

        // Sort descending by units sold using Merge Sort (Divide & Conquer)
        Comparator<ProductSalesStat> comparator = (a, b) -> Integer.compare(b.getUnitsSold(), a.getUnitsSold());
        return SortAlgorithms.mergeSort(statsList, comparator);
    }

    /**
     * Generates Low-Stock alert report sorted ascending by current stock level (most critical first).
     * Uses Merge Sort on a defensive copy of low-stock products.
     * Time Complexity: O(k log k) where k is the count of low stock items.
     */
    public List<Product> getLowStockReport() {
        List<Product> lowStock = inventoryService.getLowStockProducts();

        // Sort ascending by stock quantity using Merge Sort
        Comparator<Product> comparator = Comparator.comparingInt(Product::getStock);
        return SortAlgorithms.mergeSort(lowStock, comparator);
    }

    public static class DailySalesSummary {
        public String date;
        public int totalOrders;
        public int totalUnitsSold;
        public double totalRevenue;
        public double averageOrderValue;
        public List<Order> matchingOrders = new ArrayList<>();
    }

    public DailySalesSummary getDailySalesSummary(String targetDate) {
        DailySalesSummary summary = new DailySalesSummary();
        summary.date = (targetDate != null && !targetDate.isEmpty()) ? targetDate : "ALL DATES";

        for (Order order : orderService.getSalesHistory()) {
            if ("SERVED".equalsIgnoreCase(order.getStatus())) {
                boolean match = (targetDate == null || targetDate.isEmpty()) ||
                                order.getTimestamp().startsWith(targetDate);
                if (match) {
                    summary.matchingOrders.add(order);
                    summary.totalOrders++;
                    summary.totalRevenue += order.getTotalAmount();
                    summary.totalUnitsSold += order.getTotalUnits();
                }
            }
        }

        if (summary.totalOrders > 0) {
            summary.averageOrderValue = summary.totalRevenue / summary.totalOrders;
        }

        return summary;
    }

    public String generateMainBranchSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("         KINGPIN MNL (SUB-BRANCH) -> MAIN BRANCH SALES REPORT            \n");
        sb.append("========================================================================\n\n");

        DailySalesSummary allTime = getDailySalesSummary(null);
        sb.append(String.format("Total Completed Orders : %d\n", allTime.totalOrders));
        sb.append(String.format("Total Units Dispatched : %d items\n", allTime.totalUnitsSold));
        sb.append(String.format("Total Gross Revenue    : PHP %,.2f\n", allTime.totalRevenue));
        sb.append(String.format("Average Order Value    : PHP %,.2f\n\n", allTime.averageOrderValue));

        sb.append("--- [MERGE SORT] TOP 5 BEST-SELLING PRODUCTS ---\n");
        List<ProductSalesStat> bestSellers = getBestSellers();
        int rank = 1;
        for (ProductSalesStat stat : bestSellers) {
            sb.append(String.format("#%d  %-10s %-30s | Units: %3d | Sales: PHP %,10.2f\n",
                    rank++, stat.getProductCode(),
                    stat.getProductName().length() > 30 ? stat.getProductName().substring(0, 27) + "..." : stat.getProductName(),
                    stat.getUnitsSold(), stat.getTotalRevenue()));
            if (rank > 5) break;
        }

        sb.append("\n--- [MERGE SORT] INVENTORY REPLENISHMENT REQUESTS (LOW STOCK) ---\n");
        List<Product> lowStock = getLowStockReport();
        if (lowStock.isEmpty()) {
            sb.append("All inventory items currently above safety threshold.\n");
        } else {
            for (Product p : lowStock) {
                int needed = (p.getLowStockThreshold() * 3) - p.getStock();
                sb.append(String.format("CRITICAL: [%-8s] %-30s | Current Stock: %2d (Min: %2d) -> Requesting Restock: +%d units\n",
                        p.getCode(),
                        p.getName().length() > 30 ? p.getName().substring(0, 27) + "..." : p.getName(),
                        p.getStock(), p.getLowStockThreshold(), Math.max(needed, 10)));
            }
        }
        sb.append("========================================================================\n");
        return sb.toString();
    }
}
