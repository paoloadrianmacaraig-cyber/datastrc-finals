// Author: Tengonciang
// One player in a roster. Spare jerseys have no name and no number.
public class RosterEntry {
    private String playerName;   // "" if blank
    private String jerseyNo;     // "" if blank; kept as text so "0" and "00" are different
    private Size topSize;
    private Size shortsSize;     // null when the order is jersey only

    public RosterEntry(String playerName, String jerseyNo, Size topSize, Size shortsSize) {
        this.playerName = playerName;
        this.jerseyNo = jerseyNo;
        this.topSize = topSize;
        this.shortsSize = shortsSize;
    }

    public String getPlayerName() { return playerName; }
    public String getJerseyNo() { return jerseyNo; }
    public Size getTopSize() { return topSize; }
    public Size getShortsSize() { return shortsSize; }

    public boolean hasNumber() {
        return !jerseyNo.isEmpty();
    }

    public String displayName() {
        return playerName.isEmpty() ? "(no name)" : playerName;
    }

    public String displayNumber() {
        return jerseyNo.isEmpty() ? "--" : jerseyNo;
    }

    // Order used when sorting a roster by jersey number:
    // smaller number first, "0" before "00", blank numbers last.
    public static int compareByNumber(RosterEntry a, RosterEntry b) {
        if (!a.hasNumber() || !b.hasNumber()) {
            return Boolean.compare(!a.hasNumber(), !b.hasNumber());
        }
        int byValue = Integer.compare(Integer.parseInt(a.jerseyNo), Integer.parseInt(b.jerseyNo));
        return (byValue != 0) ? byValue : Integer.compare(a.jerseyNo.length(), b.jerseyNo.length());
    }
}
