// Author: Tengonciang
import java.util.ArrayList;

// Job orders that still have a balance, highest first.
public class BalanceReport extends Report {

    public BalanceReport(JobOrderManager manager) {
        super("Unpaid Balances", manager);
    }

    public ArrayList<String> generate() {
        ArrayList<JobOrder> owing = new ArrayList<>();
        double total = 0;
        for (JobOrder jo : manager.getJobOrders()) {
            if (jo.getStatus() != Status.CANCELLED && jo.getBalance() > 0) {
                owing.add(jo);
                total += jo.getBalance();
            }
        }
        Sorter.quickSort(owing, (a, b) -> Double.compare(b.getBalance(), a.getBalance()));

        ArrayList<String> lines = new ArrayList<>();
        String format = "%-8s %-15s %10s %10s %10s  %s";
        lines.add(String.format(format, "JO", "Team", "Total", "Paid", "Balance", "Status"));
        for (JobOrder jo : owing) {
            lines.add(String.format(format, jo.code(), cut(jo.getTeamName(), 15),
                    money(jo.getTotal()), money(jo.getAmountPaid()), money(jo.getBalance()), jo.getStatus().label()));
        }
        lines.add("");
        lines.add(String.format("%-46s %10s", "Total unpaid", "P" + money(total)));
        return lines;
    }

    protected String fileName() { return "balances"; }
}
