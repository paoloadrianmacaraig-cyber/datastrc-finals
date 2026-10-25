// Author: Tengonciang
import java.util.ArrayList;

public class Roster {
    private ArrayList<RosterEntry> entries = new ArrayList<>();
    private LinkedStack<RosterAction> undoStack = new LinkedStack<>();

    // Returns null if added, or the reason it was not added.
    public String add(RosterEntry entry) {
        String problem = checkNumber(entry.getJerseyNo(), -1);
        if (problem != null) {
            return problem;
        }
        entries.add(entry);
        undoStack.push(new RosterAction("ADD", entries.size() - 1, null, entry));
        return null;
    }

    // row starts at 1, the way it is shown on screen
    public RosterEntry remove(int row) {
        if (row < 1 || row > entries.size()) {
            return null;
        }
        RosterEntry removed = entries.remove(row - 1);
        undoStack.push(new RosterAction("REMOVE", row - 1, removed, null));
        return removed;
    }

    public String edit(int row, RosterEntry newEntry) {
        if (row < 1 || row > entries.size()) {
            return "No player in row " + row + ".";
        }
        String problem = checkNumber(newEntry.getJerseyNo(), row - 1);
        if (problem != null) {
            return problem;
        }
        RosterEntry old = entries.set(row - 1, newEntry);
        undoStack.push(new RosterAction("EDIT", row - 1, old, newEntry));
        return null;
    }

    // Reverses the last add, remove or edit. Returns a short description, or null if nothing to undo.
    public String undo() {
        RosterAction last = undoStack.pop();
        if (last == null) {
            return null;
        }
        switch (last.getType()) {
            case "ADD":
                entries.remove(last.getRow());
                return "took back adding " + describe(last.getAfter());
            case "REMOVE":
                entries.add(last.getRow(), last.getBefore());
                return "put back " + describe(last.getBefore());
            default:
                entries.set(last.getRow(), last.getBefore());
                return "changed back " + describe(last.getBefore());
        }
    }

    // a number can only be used once per roster (blank numbers are fine)
    private String checkNumber(String jerseyNo, int skipIndex) {
        if (jerseyNo.isEmpty()) {
            return null;
        }
        int found = Searcher.findJerseyNo(entries, jerseyNo);
        if (found != -1 && found != skipIndex) {
            return "Number " + jerseyNo + " is already used by " + entries.get(found).displayName() + ".";
        }
        return null;
    }

    private String describe(RosterEntry e) {
        return "#" + e.displayNumber() + " " + e.displayName();
    }

    // Copies another roster, used for repeat orders. Undo history is not copied.
    public int copyFrom(Roster other, int limit) {
        int copied = 0;
        for (RosterEntry e : other.entries) {
            if (copied == limit) {
                break;
            }
            entries.add(e);
            copied++;
        }
        return copied;
    }

    // used when loading from file, so it does not go into the undo stack
    public void load(RosterEntry entry) {
        entries.add(entry);
    }

    // the undo history only lasts while the roster is being edited
    public void clearUndo() {
        undoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public int count() {
        return entries.size();
    }

    public ArrayList<RosterEntry> getEntries() {
        return entries;
    }

    // a copy sorted by jersey number; the original keeps the order it was typed in
    public ArrayList<RosterEntry> sortedByNumber() {
        ArrayList<RosterEntry> copy = new ArrayList<>(entries);
        Sorter.insertionSort(copy, RosterEntry::compareByNumber);
        return copy;
    }

    // how many of each size, indexed by Size.ordinal()
    public int[] sizeSummary(boolean shorts) {
        int[] count = new int[Size.values().length];
        for (RosterEntry e : entries) {
            Size s = shorts ? e.getShortsSize() : e.getTopSize();
            if (s != null) {
                count[s.ordinal()]++;
            }
        }
        return count;
    }
}
