// Author: Macaraig
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

// Keeps all job orders and the two production lines, and handles every step
// of a job order's life: create, endorse, update stage, receive, cancel, release.
public class JobOrderManager {
    // assumptions about the main branch (see Scope and Limitations)
    public static final int PRODUCTION_DAYS = 10;
    public static final int RUSH_PRODUCTION_DAYS = 5;
    public static final int ORDERS_PER_DAY = 2;
    public static final int RUSH_LIMIT = 3;

    private ArrayList<JobOrder> jobOrders = new ArrayList<>();   // always sorted by JO number
    private JobQueue rushLine = new CircularJobQueue(RUSH_LIMIT);
    private JobQueue regularLine = new LinkedJobQueue();
    private TeamTree teams = new TeamTree();
    private int nextJoNumber = 2001;

    // ---------- creating ----------

    public JobOrder createJobOrder(String team, String contactPerson, String contactNo, String sport,
                                   ItemType itemType, int quantity, double unitPrice, boolean rush,
                                   LocalDate targetDate) {
        JobOrder jo = new JobOrder(nextJoNumber, team, contactPerson, contactNo, sport, itemType,
                quantity, unitPrice, rush, targetDate, Today.get());
        nextJoNumber++;
        jobOrders.add(jo);    // goes at the end, so the list stays sorted by number
        teams.addJobOrder(team, contactPerson, contactNo, jo.getJoNumber());
        return jo;
    }

    // Repeat order: copy the roster and current design of a past job order.
    // Returns how many players were copied.
    public int copyFromPast(JobOrder newJo, JobOrder pastJo) {
        int copied = newJo.getRoster().copyFrom(pastJo.getRoster(), newJo.getQuantity());
        newJo.getDesign().copyCurrentFrom(pastJo.getDesign(), pastJo.getJoNumber(), Today.get());
        return copied;
    }

    // ---------- finding ----------

    public JobOrder find(int joNumber) {
        int index = Searcher.binarySearchJO(jobOrders, joNumber);
        return (index == -1) ? null : jobOrders.get(index);
    }

    public ArrayList<JobOrder> searchByTeam(String keyword) {
        return Searcher.searchByTeam(jobOrders, keyword);
    }

    public Team findTeam(String name) {
        return teams.find(name);
    }

    public ArrayList<Team> teamsStartingWith(String prefix) {
        return teams.startsWith(prefix);
    }

    public ArrayList<Team> teamDirectory() {
        return teams.inOrder();
    }

    public ArrayList<JobOrder> teamHistory(Team team) {
        ArrayList<JobOrder> list = new ArrayList<>();
        for (int no : team.getJoNumbers()) {
            list.add(find(no));
        }
        return list;
    }

    // ---------- production line ----------

    // Everything that still blocks this job order from being endorsed (empty list = ready).
    public ArrayList<String> endorseProblems(JobOrder jo) {
        ArrayList<String> problems = new ArrayList<>();
        if (jo.getStatus() != Status.DRAFT) {
            problems.add("Only draft job orders can be endorsed (this one is " + jo.getStatus().label() + ").");
            return problems;
        }
        if (jo.getRoster().count() != jo.getQuantity()) {
            problems.add("Roster incomplete (" + jo.getRoster().count() + " of " + jo.getQuantity() + ").");
        }
        if (!jo.getDesign().isApproved()) {
            problems.add("Design not yet approved.");
        }
        if (jo.getAmountPaid() < jo.getRequiredDownpayment()) {
            problems.add("Downpayment needed: P" + money(jo.getRequiredDownpayment())
                    + " (paid P" + money(jo.getAmountPaid()) + ").");
        }
        return problems;
    }

    // How many orders would be made before a new one:
    // a rush order only waits for the rush line, a regular order waits for both lines.
    public int ordersAhead(boolean rush) {
        return rush ? rushLine.size() : rushLine.size() + regularLine.size();
    }

    public LocalDate estimateFinish(boolean rush) {
        int waitDays = (ordersAhead(rush) + ORDERS_PER_DAY - 1) / ORDERS_PER_DAY;   // rounded up
        int productionDays = rush ? RUSH_PRODUCTION_DAYS : PRODUCTION_DAYS;
        return Today.get().plusDays(productionDays + waitDays);
    }

    public boolean rushLineFull() {
        return rushLine.isFull();
    }

    public void endorse(JobOrder jo, boolean rush) {
        jo.setRush(rush);
        if (rush) {
            rushLine.enqueue(jo);
        } else {
            regularLine.enqueue(jo);
        }
        jo.setStatus(Status.IN_LINE);
        jo.setStage(Stage.NEW);
        jo.setDateEndorsed(Today.get());
    }

    // 1-based place in its own line, or -1 if not in line
    public int placeInLine(JobOrder jo) {
        return jo.isRush() ? rushLine.positionOf(jo.getJoNumber()) : regularLine.positionOf(jo.getJoNumber());
    }

    public boolean updateStage(JobOrder jo, Stage stage) {
        if (jo.getStatus() != Status.IN_LINE || stage.ordinal() < jo.getStage().ordinal()) {
            return false;     // stages only move forward
        }
        jo.setStage(stage);
        return true;
    }

    // finished jerseys arrived from the main branch
    public boolean receiveFromMain(JobOrder jo) {
        if (jo.getStatus() != Status.IN_LINE) {
            return false;
        }
        JobQueue line = jo.isRush() ? rushLine : regularLine;
        line.remove(jo.getJoNumber());
        jo.setStatus(Status.FOR_RELEASE);
        jo.setDateReceived(Today.get());
        return true;
    }

    // Returns null if the job order can still be cancelled, or the reason it can't.
    // Checked first so the user isn't asked to confirm something that won't happen.
    public String cancelProblem(JobOrder jo) {
        if (jo.getStatus() == Status.DRAFT) {
            return null;
        }
        if (jo.getStatus() != Status.IN_LINE) {
            return "Only draft or in-line job orders can be cancelled (this one is " + jo.getStatus().label() + ").";
        }
        if (!jo.getStage().isBeforePrinting()) {
            return "Already at " + jo.getStage().label() + ". Printed jerseys can no longer be cancelled.";
        }
        return null;
    }

    // Returns null if cancelled, or the reason it can't be.
    public String cancel(JobOrder jo) {
        String problem = cancelProblem(jo);
        if (problem != null) {
            return problem;
        }
        if (jo.getStatus() == Status.IN_LINE) {
            JobQueue line = jo.isRush() ? rushLine : regularLine;
            line.remove(jo.getJoNumber());
        }
        jo.setStatus(Status.CANCELLED);
        return null;
    }

    // the roster can be changed until printing starts
    public boolean canEditRoster(JobOrder jo) {
        return jo.getStatus() == Status.DRAFT
                || (jo.getStatus() == Status.IN_LINE && jo.getStage().isBeforePrinting());
    }

    // ---------- payments and release ----------

    // Records a payment and returns the change. Only the amount needed to clear
    // the balance is recorded; anything above it is given back as change.
    public double recordPayment(JobOrder jo, double amount, String method) {
        double applied = Math.min(amount, jo.getBalance());
        jo.addPayment(new Payment(Today.get(), applied, method));
        return amount - applied;
    }

    public String release(JobOrder jo) {
        if (jo.getStatus() != Status.FOR_RELEASE) {
            return "Not ready for release (status: " + jo.getStatus().label() + ").";
        }
        if (jo.getBalance() > 0) {
            return "Balance of P" + money(jo.getBalance()) + " must be paid first.";
        }
        jo.setStatus(Status.RELEASED);
        jo.setDateReleased(Today.get());
        return null;
    }

    // ---------- status message for customers ----------

    public String statusMessage(JobOrder jo) {
        DateTimeFormatter f = Today.FORMAT;
        String hello = "Hi " + jo.getContactPerson() + "! Update on " + jo.code()
                + " (" + jo.getTeamName() + "): ";
        String body;
        switch (jo.getStatus()) {
            case DRAFT:
                body = "we're still preparing your order before we send it for production.";
                break;
            case IN_LINE:
                body = "your order is #" + placeInLine(jo) + " in the " + (jo.isRush() ? "rush" : "regular")
                        + " line and is now at the " + jo.getStage().label().toLowerCase() + " stage.";
                break;
            case FOR_RELEASE:
                body = "your jerseys are ready for pickup at the branch!";
                break;
            case RELEASED:
                body = "your jerseys were released on " + jo.getDateReleased().format(f) + ".";
                break;
            default:
                body = "this order was cancelled.";
        }
        String end = " Target date: " + jo.getTargetDate().format(f) + ".";
        if (jo.getBalance() > 0 && jo.getStatus() != Status.CANCELLED) {
            end += " Remaining balance: P" + money(jo.getBalance()) + ".";
        }
        return hello + body + end + " Thank you!";
    }

    // ---------- used by DataStore and the reports ----------

    public void loadJobOrder(JobOrder jo) {
        jobOrders.add(jo);
        teams.addJobOrder(jo.getTeamName(), jo.getContactPerson(), jo.getContactNo(), jo.getJoNumber());
    }

    public void loadIntoLine(JobOrder jo) {
        if (jo.isRush()) {
            rushLine.enqueue(jo);
        } else {
            regularLine.enqueue(jo);
        }
    }

    public void sortAfterLoading() {
        // the file is saved in JO order, so this is almost always already sorted
        Sorter.insertionSort(jobOrders, (a, b) -> Integer.compare(a.getJoNumber(), b.getJoNumber()));
        if (!jobOrders.isEmpty()) {
            nextJoNumber = jobOrders.get(jobOrders.size() - 1).getJoNumber() + 1;
        }
    }

    public ArrayList<JobOrder> getJobOrders() { return jobOrders; }
    public JobQueue getRushLine() { return rushLine; }
    public JobQueue getRegularLine() { return regularLine; }

    public static String money(double amount) {
        return String.format("%,.2f", amount);
    }
}
