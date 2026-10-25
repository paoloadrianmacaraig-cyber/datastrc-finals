// Author: Macaraig
import java.time.LocalDate;
import java.util.ArrayList;

// Design versions of one job order. The newest version is on top of the stack.
public class DesignHistory {
    public static final int FREE_MAJOR = 1;
    public static final int FREE_MINOR = 2;

    private LinkedStack<DesignVersion> versions = new LinkedStack<>();
    private int majorCount;
    private int minorCount;
    private int nextVersionNo = 1;
    private boolean approved;

    // type is INITIAL, MAJOR or MINOR. Returns true if this revision is beyond the free ones.
    public boolean addVersion(String type, String note, LocalDate date) {
        versions.push(new DesignVersion(nextVersionNo, type, note, date));
        nextVersionNo++;
        approved = false;    // a new version always needs the team's approval again
        if (type.equals("MAJOR")) {
            majorCount++;
            return majorCount > FREE_MAJOR;
        }
        if (type.equals("MINOR")) {
            minorCount++;
            return minorCount > FREE_MINOR;
        }
        return false;
    }

    // Goes back to the previous version. The first design cannot be removed.
    // Revision counts stay the same because the work was already done.
    public boolean revert() {
        if (versions.size() <= 1) {
            return false;
        }
        versions.pop();
        approved = false;
        return true;
    }

    public boolean approve() {
        if (versions.isEmpty()) {
            return false;
        }
        approved = true;
        return true;
    }

    public void copyCurrentFrom(DesignHistory other, int pastJoNumber, LocalDate date) {
        DesignVersion current = other.current();
        if (current != null) {
            addVersion("INITIAL", "Copied from JO-" + pastJoNumber + ": " + current.getNote(), date);
        }
    }

    // used by DataStore so loading does not change the counts
    public void load(DesignVersion v) {
        versions.push(v);
        nextVersionNo = Math.max(nextVersionNo, v.getVersionNo() + 1);
    }

    public void loadState(int majorCount, int minorCount, boolean approved) {
        this.majorCount = majorCount;
        this.minorCount = minorCount;
        this.approved = approved;
    }

    public DesignVersion current() { return versions.peek(); }
    public boolean isApproved() { return approved; }
    public int getMajorCount() { return majorCount; }
    public int getMinorCount() { return minorCount; }
    public int versionCount() { return versions.size(); }

    public int extraRevisions() {
        return Math.max(0, majorCount - FREE_MAJOR) + Math.max(0, minorCount - FREE_MINOR);
    }

    // newest first
    public ArrayList<DesignVersion> getVersions() {
        return versions.toList();
    }
}
