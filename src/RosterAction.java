// Author: Tengonciang
// One change to a roster, saved in the undo stack.
public class RosterAction {
    private String type;          // ADD, REMOVE or EDIT
    private int row;              // position in the roster list
    private RosterEntry before;   // entry before the change (null for ADD)
    private RosterEntry after;    // entry after the change (null for REMOVE)

    public RosterAction(String type, int row, RosterEntry before, RosterEntry after) {
        this.type = type;
        this.row = row;
        this.before = before;
        this.after = after;
    }

    public String getType() { return type; }
    public int getRow() { return row; }
    public RosterEntry getBefore() { return before; }
    public RosterEntry getAfter() { return after; }
}
