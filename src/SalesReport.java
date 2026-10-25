// Author: Tengonciang
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// Orders taken in one month, grouped by sport and ranked by total amount.
public class SalesReport extends Report {
    private YearMonth month;

    // one row of the report
    private static class SportTotal {
        String sport;
        int orders;
        int pieces;
        double amount;

        SportTotal(String sport) {
            this.sport = sport;
        }
    }

    public SalesReport(JobOrderManager manager, YearMonth month) {
        super("Sales for " + month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)), manager);
        this.month = month;
    }

    public ArrayList<String> generate() {
        List<JobOrder> all = manager.getJobOrders();
        LocalDate start = month.atDay(1);
        LocalDate nextStart = month.plusMonths(1).atDay(1);
        // binary search for where the month starts and ends in the job order list
        int from = Searcher.firstCreatedOnOrAfter(all, start);
        int to = Searcher.firstCreatedOnOrAfter(all, nextStart);

        ArrayList<SportTotal> totals = new ArrayList<>();
        int orders = 0;
        double grand = 0;
        for (int i = from; i < to; i++) {
            JobOrder jo = all.get(i);
            if (jo.getStatus() == Status.CANCELLED) {
                continue;
            }
            SportTotal row = findSport(totals, jo.getSport());
            row.orders++;
            row.pieces += jo.getQuantity();
            row.amount += jo.getTotal();
            orders++;
            grand += jo.getTotal();
        }
        Sorter.mergeSort(totals, (a, b) -> Double.compare(b.amount, a.amount));

        ArrayList<String> lines = new ArrayList<>();
        String format = "%-15s %7s %7s %14s";
        lines.add(String.format(format, "Sport", "Orders", "Pieces", "Amount"));
        for (SportTotal t : totals) {
            lines.add(String.format(format, t.sport, t.orders, t.pieces, "P" + money(t.amount)));
        }
        lines.add("");
        lines.add(String.format(format, "Total", orders, "", "P" + money(grand)));
        lines.add("Cancelled job orders are not counted.");
        return lines;
    }

    // linear search through the few sports already listed
    private SportTotal findSport(ArrayList<SportTotal> totals, String sport) {
        for (SportTotal t : totals) {
            if (t.sport.equalsIgnoreCase(sport)) {
                return t;
            }
        }
        SportTotal t = new SportTotal(sport);
        totals.add(t);
        return t;
    }

    protected String fileName() { return "sales-" + month; }
}
