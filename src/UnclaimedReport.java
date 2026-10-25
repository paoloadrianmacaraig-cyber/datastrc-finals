// Author: Tengonciang
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

// Finished orders still waiting at the branch, longest wait first.
public class UnclaimedReport extends Report {
    private int minDays;

    public UnclaimedReport(JobOrderManager manager, int minDays) {
        super("Unclaimed Orders", manager);
        this.minDays = minDays;
    }

    public ArrayList<String> generate() {
        ArrayList<JobOrder> waiting = new ArrayList<>();
        for (JobOrder jo : manager.getJobOrders()) {
            if (jo.getStatus() == Status.FOR_RELEASE && daysWaiting(jo) >= minDays) {
                waiting.add(jo);
            }
        }
        Sorter.mergeSort(waiting, (a, b) -> Long.compare(daysWaiting(b), daysWaiting(a)));

        ArrayList<String> lines = new ArrayList<>();
        String format = "%-8s %-18s %-16s %4s  %12s";
        lines.add("Ready for " + minDays + " or more days, longest wait first.");
        lines.add("");
        lines.add(String.format(format, "JO", "Team", "Received", "Days", "Balance"));
        for (JobOrder jo : waiting) {
            lines.add(String.format(format, jo.code(), cut(jo.getTeamName(), 18),
                    Today.show(jo.getDateReceived()), String.valueOf(daysWaiting(jo)), "P" + money(jo.getBalance())));
        }
        if (waiting.isEmpty()) {
            lines.add("No unclaimed orders.");
        }
        return lines;
    }

    private long daysWaiting(JobOrder jo) {
        return ChronoUnit.DAYS.between(jo.getDateReceived(), Today.get());
    }

    protected String fileName() { return "unclaimed"; }
}
