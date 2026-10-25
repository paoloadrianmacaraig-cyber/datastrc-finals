// Author: Macaraig
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Searcher {

    // Binary search. jobOrders must be sorted by JO number (it always is,
    // because every new job order gets the next number and goes at the end).
    public static int binarySearchJO(List<JobOrder> jobOrders, int joNumber) {
        int low = 0;
        int high = jobOrders.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int current = jobOrders.get(mid).getJoNumber();
            if (current == joNumber) {
                return mid;
            }
            if (current < joNumber) {
                low = mid + 1;        // target is in the right half
            } else {
                high = mid - 1;       // target is in the left half
            }
        }
        return -1;
    }

    // Binary search for a position (lower bound): the first job order due AFTER the date.
    // sortedByDue must be sorted by target date.
    public static int firstDueAfter(List<JobOrder> sortedByDue, LocalDate date) {
        int low = 0;
        int high = sortedByDue.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (sortedByDue.get(mid).getTargetDate().isAfter(date)) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    // Lower bound on date created. JO numbers are given in order, so the
    // job order list is also in order of the date it was created.
    public static int firstCreatedOnOrAfter(List<JobOrder> jobOrders, LocalDate date) {
        int low = 0;
        int high = jobOrders.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (jobOrders.get(mid).getDateCreated().isBefore(date)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    // Linear search: team names that contain the keyword anywhere.
    public static ArrayList<JobOrder> searchByTeam(List<JobOrder> jobOrders, String keyword) {
        ArrayList<JobOrder> matches = new ArrayList<>();
        String key = keyword.toLowerCase();
        for (JobOrder jo : jobOrders) {
            if (jo.getTeamName().toLowerCase().contains(key)) {
                matches.add(jo);
            }
        }
        return matches;
    }

    // Linear search: index of the player with this jersey number, or -1.
    public static int findJerseyNo(List<RosterEntry> roster, String jerseyNo) {
        for (int i = 0; i < roster.size(); i++) {
            if (roster.get(i).getJerseyNo().equals(jerseyNo)) {
                return i;
            }
        }
        return -1;
    }
}
