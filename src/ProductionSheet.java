// Author: Tengonciang
import java.util.ArrayList;

// Sent to the main branch with the job order: roster by number and the count per size.
public class ProductionSheet extends Report {
    private JobOrder jo;

    public ProductionSheet(JobOrderManager manager, JobOrder jo) {
        super("Production Sheet " + jo.code(), manager);
        this.jo = jo;
    }

    public ArrayList<String> generate() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(String.format("%-14s%s", "Team", jo.getTeamName() + " (" + jo.getSport() + ")"));
        lines.add(String.format("%-14s%s", "Item", jo.getQuantity() + " x " + jo.getItemType().getLabel()));
        lines.add(String.format("%-14s%s", "Rush", jo.isRush() ? "YES" : "No"));
        lines.add(String.format("%-14s%s", "Target date", Today.show(jo.getTargetDate())));
        if (jo.getDesign().current() != null) {
            lines.add(String.format("%-14s%s", "Design", "v" + jo.getDesign().current().getVersionNo() + " - "
                    + jo.getDesign().current().getNote()));
        }
        lines.add("");
        lines.addAll(rosterLines(jo));
        return lines;
    }

    protected String fileName() { return "production-sheet-" + jo.getJoNumber(); }
}
