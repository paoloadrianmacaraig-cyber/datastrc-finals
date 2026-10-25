// Author: Tengonciang
import java.util.ArrayList;

// Given to the team to check names, numbers and sizes before printing.
public class RosterSlip extends Report {
    private JobOrder jo;

    public RosterSlip(JobOrderManager manager, JobOrder jo) {
        super("Roster Slip " + jo.code(), manager);
        this.jo = jo;
    }

    public ArrayList<String> generate() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(String.format("%-14s%s", "Team", jo.getTeamName()));
        lines.add(String.format("%-14s%s", "Contact", jo.getContactPerson()));
        lines.add("");
        lines.add("Please check every name, number and size.");
        lines.add("Changes are no longer possible once printing starts.");
        lines.add("");
        lines.addAll(rosterLines(jo));
        lines.add("");
        lines.add("Checked by: ______________________     Date: ______________");
        return lines;
    }

    protected String fileName() { return "roster-slip-" + jo.getJoNumber(); }
}
