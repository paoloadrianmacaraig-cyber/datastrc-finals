// Author: Tengonciang
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

// Open job orders due within the next few days, nearest first.
public class DueSoonReport extends Report {
    private int days;

    public DueSoonReport(JobOrderManager manager, int days) {
        super("Due Within " + days + " Days", manager);
        this.days = days;
    }

    public ArrayList<String> generate() {
        ArrayList<JobOrder> open = new ArrayList<>();
        for (JobOrder jo : manager.getJobOrders()) {
            if (jo.isOpen()) {
                open.add(jo);
            }
        }
        // stable sort, so orders with the same date stay in JO order
        Sorter.mergeSort(open, (a, b) -> a.getTargetDate().compareTo(b.getTargetDate()));

        LocalDate today = Today.get();
        int cutoff = Searcher.firstDueAfter(open, today.plusDays(days));

        ArrayList<String> lines = new ArrayList<>();
        String format = "%-8s %-18s %-16s %4s  %s";
        lines.add(String.format(format, "JO", "Team", "Target date", "Days", "Status"));
        for (int i = 0; i < cutoff; i++) {
            JobOrder jo = open.get(i);
            long left = ChronoUnit.DAYS.between(today, jo.getTargetDate());
            String status = (jo.getStatus() == Status.IN_LINE) ? jo.getStage().label() : jo.getStatus().label();
            lines.add(String.format(format, jo.code(), cut(jo.getTeamName(), 18),
                    Today.show(jo.getTargetDate()), String.valueOf(left), status));
        }
        if (cutoff == 0) {
            lines.add("Nothing due in the next " + days + " days.");
        }
        return lines;
    }

    protected String fileName() { return "due-soon"; }
}
