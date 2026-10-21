import java.util.List;

/**
 * Utility for formatted console presentation, tables, banners, and receipts.
 */
public class ConsoleUI {

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";

    public static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("  _  _______ _   _  _____ _____ _____ _   _   __  __ _   _ _      ");
        System.out.println(" | |/ /_   _| \\ | |/ ____|  __ \\_   _| \\ | | |  \\/  | \\ | | |     ");
        System.out.println(" | ' /  | | |  \\| | |  __| |__) || | |  \\| | | \\  / |  \\| | |     ");
        System.out.println(" |  <   | | | . ` | | |_ |  ___/ | | | . ` | | |\\/| | . ` | |     ");
        System.out.println(" | . \\ _| |_| |\\  | |__| | |    _| |_| |\\  | | |  | | |\\  | |____ ");
        System.out.println(" |_|\\_\\_____|_| \\_|\\_____|_|   |_____|_| \\_| |_|  |_|_| \\_|______|");
        System.out.println("       --- STREETWEAR & APPAREL POS & ORDER QUEUE SYSTEM ---      " + RESET);
        System.out.println(" [DSA Finals] Custom LinkedList Queue | Binary & Linear Search | Insertion & Merge Sort\n");
    }

    public static void printHeader(String title) {
        System.out.println("\n" + CYAN + BOLD + "=== " + title + " ===" + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + "⚠ " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + "✖ " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "ℹ " + message + RESET);
    }

    public static void displayProductTable(List<Product> products) {
        if (products == null || products.isEmpty()) {
            printWarning("No products found.");
            return;
        }

        System.out.println(BOLD + "+------------+----------------------------------------+---------------+------------+-------+--------+" + RESET);
        System.out.printf(BOLD + "| %-10s | %-38s | %-13s | %10s | %5s | %6s |%n" + RESET,
                "CODE", "PRODUCT NAME", "CATEGORY", "PRICE (PHP)", "STOCK", "STATUS");
        System.out.println("+------------+----------------------------------------+---------------+------------+-------+--------+");

        for (Product p : products) {
            String name = p.getName().length() > 38 ? p.getName().substring(0, 35) + "..." : p.getName();
            String status = p.isLowStock() ? RED + "LOW" + RESET : GREEN + "OK" + RESET;
            System.out.printf("| %-10s | %-38s | %-13s | %10.2f | %5d |   %-4s |%n",
                    p.getCode(), name, p.getCategory(), p.getPrice(), p.getStock(), status);
        }
        System.out.println("+------------+----------------------------------------+---------------+------------+-------+--------+");
        System.out.printf("Total Products: %d\n", products.size());
    }

    public static void displayCart(List<OrderItem> cart, double total) {
        if (cart == null || cart.isEmpty()) {
            printInfo("Current cart is empty.");
            return;
        }

        printHeader("ACTIVE SHOPPING CART");
        System.out.println(BOLD + "+------------+-----------------------------------+-------+------------+------------+" + RESET);
        System.out.printf(BOLD + "| %-10s | %-33s | %5s | %10s | %10s |%n" + RESET,
                "CODE", "PRODUCT NAME", "QTY", "UNIT (PHP)", "TOTAL (PHP)");
        System.out.println("+------------+-----------------------------------+-------+------------+------------+");

        for (OrderItem item : cart) {
            String name = item.getProductName().length() > 33 ? item.getProductName().substring(0, 30) + "..." : item.getProductName();
            System.out.printf("| %-10s | %-33s | %5d | %10.2f | %10.2f |%n",
                    item.getProductCode(), name, item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        System.out.println("+------------+-----------------------------------+-------+------------+------------+");
        System.out.printf(BOLD + "                                               SUBTOTAL: PHP %,10.2f\n" + RESET, total);
    }

    public static void displayQueue(List<Order> queue) {
        if (queue == null || queue.isEmpty()) {
            printInfo("The order queue is currently EMPTY. No orders waiting to be served.");
            return;
        }

        printHeader("ORDER QUEUE (FRONT -> REAR) [Custom Singly-Linked Queue]");
        System.out.println(BOLD + "+-------+-------+---------------------+-------+------------+------------+" + RESET);
        System.out.printf(BOLD + "| POS   | ORD # | TIMESTAMP           | ITEMS | TOTAL (PHP)| STATUS     |%n" + RESET);
        System.out.println("+-------+-------+---------------------+-------+------------+------------+");

        for (int i = 0; i < queue.size(); i++) {
            Order o = queue.get(i);
            String pos = (i == 0) ? (GREEN + "FRONT" + RESET) : (i == queue.size() - 1 ? (CYAN + "REAR " + RESET) : String.format("#%02d  ", i + 1));
            System.out.printf("| %-5s | %-5d | %-19s | %5d | %10.2f | %-10s |%n",
                    pos, o.getOrderNumber(), o.getTimestamp(), o.getTotalUnits(), o.getTotalAmount(), o.getStatus());
        }
        System.out.println("+-------+-------+---------------------+-------+------------+------------+");
        System.out.printf("Pending Orders Waiting: %d\n", queue.size());
    }

    public static void printReceipt(Order order) {
        System.out.println("\n" + BOLD + "==================================================");
        System.out.println("                 KINGPIN MANILA                   ");
        System.out.println("             Sub-Branch Retail Store              ");
        System.out.println("==================================================" + RESET);
        System.out.printf("Receipt / Order No : #%d\n", order.getOrderNumber());
        System.out.printf("Date & Time        : %s\n", order.getTimestamp());
        System.out.printf("Status             : %s\n", order.getStatus());
        System.out.println("--------------------------------------------------");
        System.out.println(BOLD + "ITEM                               QTY   AMOUNT" + RESET);
        System.out.println("--------------------------------------------------");

        for (OrderItem item : order.getItems()) {
            String name = item.getProductName().length() > 24 ? item.getProductName().substring(0, 21) + "..." : item.getProductName();
            System.out.printf("%-24s %3d  PHP %8.2f\n", name, item.getQuantity(), item.getSubtotal());
        }

        System.out.println("--------------------------------------------------");
        System.out.printf(BOLD + "TOTAL AMOUNT DUE                 : PHP %10.2f\n" + RESET, order.getTotalAmount());
        System.out.printf("CASH TENDERED                    : PHP %10.2f\n", order.getCashReceived());
        System.out.printf(BOLD + "CHANGE RETURNED                  : PHP %10.2f\n" + RESET, order.getChange());
        System.out.println("==================================================");
        System.out.println("         THANK YOU FOR SUPPORTING LOCAL!          ");
        System.out.println("    Follow us on IG / TikTok: @kingpin.mnl        ");
        System.out.println("==================================================\n");
    }
}
