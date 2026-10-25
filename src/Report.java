// Author: Tengonciang
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

// Parent of all reports. Each report only has to say how its lines are built.
public abstract class Report {
    protected String title;
    protected JobOrderManager manager;

    public Report(String title, JobOrderManager manager) {
        this.title = title;
        this.manager = manager;
    }

    public abstract ArrayList<String> generate();

    // short name used for the saved file, e.g. "due-soon"
    protected abstract String fileName();

    public String getTitle() {
        return title;
    }

    // shows the report on screen, under the screen's header bar
    public void print() {
        for (String line : generate()) {
            Screen.line(line);
        }
    }

    // saves the report in the reports folder and returns the file path
    public String saveToFile() {
        File folder = new File("reports");
        folder.mkdirs();
        String path = "reports" + File.separator + fileName() + "-" + Today.get() + ".txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            for (String line : header()) {
                out.println(line);
            }
            for (String line : generate()) {
                out.println(line);
            }
        } catch (IOException e) {
            return null;
        }
        return path;
    }

    // the top of a saved report, so the printed copy says where and when it came from
    protected ArrayList<String> header() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("MNL KINGPIN - QUEZON BRANCH");
        lines.add(title.toUpperCase());
        lines.add("As of " + Today.show(Today.get()));
        lines.add("=".repeat(Screen.WIDTH - 2));
        return lines;
    }

    protected static String money(double amount) {
        return JobOrderManager.money(amount);
    }

    protected static String cut(String text, int width) {
        return (text.length() <= width) ? text : text.substring(0, width - 1) + ".";
    }

    // roster sorted by jersey number plus the size count (used by ProductionSheet and RosterSlip)
    protected static ArrayList<String> rosterLines(JobOrder jo) {
        boolean set = jo.getItemType() == ItemType.JERSEY_SET;
        ArrayList<String> lines = new ArrayList<>();
        lines.add(set ? String.format("%4s  %-22s %-7s %s", "No.", "Name", "Jersey", "Shorts")
                      : String.format("%4s  %-22s %s", "No.", "Name", "Size"));
        for (RosterEntry e : jo.getRoster().sortedByNumber()) {
            lines.add(set ? String.format("%4s  %-22s %-7s %s", e.displayNumber(), cut(e.displayName(), 22), e.getTopSize(), e.getShortsSize())
                          : String.format("%4s  %-22s %s", e.displayNumber(), cut(e.displayName(), 22), e.getTopSize()));
        }
        lines.add("");
        lines.add(sizeLine(set ? "Jerseys   " : "Sizes     ", jo.getRoster().sizeSummary(false)));
        if (set) {
            lines.add(sizeLine("Shorts    ", jo.getRoster().sizeSummary(true)));
        }
        return lines;
    }

    private static String sizeLine(String label, int[] count) {
        StringBuilder sb = new StringBuilder(label);
        int total = 0;
        for (Size s : Size.values()) {
            if (count[s.ordinal()] > 0) {
                sb.append(s).append(" ").append(count[s.ordinal()]).append("   ");
                total += count[s.ordinal()];
            }
        }
        sb.append("(total ").append(total).append(")");
        return sb.toString();
    }
}
