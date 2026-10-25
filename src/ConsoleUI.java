// Author: Tengonciang
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

// All the menus. This class only reads input and prints results;
// the actual work is done by JobOrderManager and the other classes.
public class ConsoleUI {
    // realistic limits for what the branch types in
    private static final double MIN_PRICE = 300;
    private static final double MAX_PRICE = 3000;
    private static final int MAX_QUANTITY = 500;
    private static final double MAX_CASH_OVER = 1000;   // biggest change we expect to give back
    private static final int MAX_MONTHS_AHEAD = 6;
    private static final String[] SPORTS = {"Basketball", "Volleyball", "Football", "Badminton", "Esports", "Others"};

    // what each kind of name may contain (no commas, since the data files use them)
    private static final String TEAM_NAME = "[A-Za-z0-9ñÑ .'&()-]{2,30}";
    private static final String PERSON_NAME = "[A-Za-zñÑ .'-]{2,40}";
    private static final String PLAYER_NAME = "[A-Za-zñÑ .'-]{1,20}";

    private JobOrderManager manager = new JobOrderManager();
    private DataStore dataStore = new DataStore("data");
    private Scanner scanner = new Scanner(System.in);

    // true while a form can be left by typing "cancel"
    private boolean cancelAllowed = false;

    // thrown by the input helpers when the user types "cancel"
    private static class CancelInput extends RuntimeException {
    }

    public void run() {
        Today.load("data/settings.txt");
        dataStore.loadAll(manager);
        Screen.title();
        boolean running = true;
        try {
            while (running) {
                showMainMenu();
                int choice = readInt("Choice: ", 0, 8);
                switch (choice) {
                    case 1: newJobOrder(); break;
                    case 2: rosterMenu(); break;
                    case 3: designMenu(); break;
                    case 4: paymentsMenu(); break;
                    case 5: productionMenu(); break;
                    case 6: releaseMenu(); break;
                    case 7: lookupMenu(); break;
                    case 8: reportsMenu(); break;
                    default: running = false;
                }
            }
        } catch (NoSuchElementException e) {
            // input ended (for example when the demo inputs come from a file)
        }
        save();
        Screen.blank();
        Screen.line("Data saved. Goodbye!");
    }

    private void showMainMenu() {
        int forRelease = 0;
        for (JobOrder jo : manager.getJobOrders()) {
            forRelease += (jo.getStatus() == Status.FOR_RELEASE) ? 1 : 0;
        }
        Screen.bar(null);
        Screen.pair("Rush line", manager.getRushLine().size() + " of " + JobOrderManager.RUSH_LIMIT);
        Screen.pair("Regular line", String.valueOf(manager.getRegularLine().size()));
        Screen.pair("For release", String.valueOf(forRelease));
        Screen.blank();
        Screen.option(1, "New Job Order");
        Screen.option(2, "Roster");
        Screen.option(3, "Design");
        Screen.option(4, "Payments");
        Screen.option(5, "Production Line");
        Screen.option(6, "Release Job Order");
        Screen.option(7, "Lookup");
        Screen.option(8, "Reports");
        Screen.option(0, "Save and Exit");
        Screen.blank();
    }

    // ================= 1. NEW JOB ORDER =================

    private void newJobOrder() {
        Screen.bar("New Job Order");
        Screen.line("Type 'cancel' at any prompt to go back. Nothing saves until the end.");
        cancelAllowed = true;
        try {
            fillJobOrder();
        } catch (CancelInput e) {
            Screen.blank();
            Screen.line("Cancelled. Nothing was saved.");
        } finally {
            cancelAllowed = false;
        }
    }

    private void fillJobOrder() {
        Screen.section("Team");
        String team = readName("Team name: ", TEAM_NAME, false,
                "Team names need 2 to 30 letters or numbers (spaces and . ' & ( ) - are OK).");
        Team existing = manager.findTeam(team);
        boolean sameTeam = false;
        while (existing != null && !sameTeam) {
            Screen.line(existing.getName() + " ordered before (contact: " + existing.getContactPerson()
                    + ", " + existing.getContactNo() + ").");
            sameTeam = readYesNo("Is this the same team? (Y/N): ");
            existing = sameTeam ? existing : null;
            team = sameTeam ? team : renameTeam(team);
            existing = sameTeam ? existing : manager.findTeam(team);
        }
        String contactPerson = readName("Contact person: ", PERSON_NAME, false,
                "Names need 2 to 40 letters (spaces and . ' - are OK).");
        String contactNo = readMobile("Contact number: ");

        Screen.section("Order");
        String sport = readSport();
        Screen.blank();
        Screen.line("Item type");
        Screen.subOption(1, "Jersey only");
        Screen.subOption(2, "Jersey + shorts set");
        ItemType itemType = (readInt("Choice: ", 1, 2) == 1) ? ItemType.JERSEY_ONLY : ItemType.JERSEY_SET;
        Screen.blank();
        String unit = (itemType == ItemType.JERSEY_SET) ? "set" : "piece";
        int quantity = readInt("Quantity (minimum " + JobOrder.MIN_QUANTITY + "): ", JobOrder.MIN_QUANTITY, MAX_QUANTITY);
        double unitPrice = readAmount("Price per " + unit + ": ", MIN_PRICE, MAX_PRICE);
        LocalDate target = readFutureDate("Target date (YYYY-MM-DD): ");
        boolean rush = readYesNo("Rush order? (Y/N): ");

        double total = quantity * unitPrice;
        Screen.section("Review");
        Screen.pair("Team", team + (existing != null ? " (returning team)" : ""));
        Screen.pair("Contact", contactPerson + ", " + contactNo);
        Screen.pair("Order", quantity + " x " + itemType.getLabel() + ", " + sport);
        Screen.pair("Price", Screen.peso(unitPrice) + " per " + unit);
        Screen.pair("Total", Screen.peso(total));
        Screen.pair("Downpayment", Screen.peso(total * JobOrder.DOWNPAYMENT_RATE)
                + " (" + (int) (JobOrder.DOWNPAYMENT_RATE * 100) + "%)");
        Screen.pair("Target date", Today.show(target));
        Screen.pair("Rush", rush ? "Yes" : "No");
        Screen.blank();
        if (!readYesNo("Save this job order? (Y/N): ")) {
            Screen.blank();
            Screen.line("Not saved.");
            return;
        }
        cancelAllowed = false;      // it's saved now, so "cancel" no longer applies

        JobOrder jo = manager.createJobOrder(team, contactPerson, contactNo, sport, itemType, quantity,
                unitPrice, rush, target);
        save();
        Screen.blank();
        Screen.line(jo.code() + " saved as a draft.");
        if (existing != null) {
            offerCopy(jo, existing, team);
        }
    }

    private String renameTeam(String team) {
        Screen.line("Add something to tell them apart, like \"" + team + " (Tanauan)\".");
        return readName("Team name: ", TEAM_NAME, false, "Team names need 2 to 30 letters or numbers (spaces and . ' & ( ) - are OK).");
    }

    // repeat orders: copy the roster and design from one of the team's past job orders
    private void offerCopy(JobOrder jo, Team existing, String team) {
        Screen.section("Repeat Order");
        Screen.pair("Past orders", joList(existing.getJoNumbers(), jo.getJoNumber()));
        int past = readInt("Copy roster and design from (JO number, 0 to skip): ", 0, 999999);
        JobOrder pastJo = (past == 0) ? null : manager.find(past);
        boolean valid = pastJo != null && pastJo.getTeamName().equalsIgnoreCase(team) && past != jo.getJoNumber();
        if (past != 0 && !valid) {
            Screen.error("That is not one of this team's past orders. Nothing was copied.");
            return;
        }
        if (valid) {
            int copied = manager.copyFromPast(jo, pastJo);
            save();
            Screen.line("Copied " + copied + " players and the design from " + pastJo.code() + ".");
            Screen.line("The team still needs to approve the design.");
        }
    }

    private String joList(ArrayList<Integer> numbers, int skip) {
        StringBuilder sb = new StringBuilder();
        for (int no : numbers) {
            sb.append((no == skip) ? "" : (sb.length() == 0 ? "" : ", ") + "JO-" + no);
        }
        return sb.toString();
    }

    // ================= 2. ROSTER =================

    private void rosterMenu() {
        Screen.bar("Roster");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        Roster roster = jo.getRoster();
        if (!manager.canEditRoster(jo)) {
            Screen.error(jo.code() + " is " + describe(jo).toLowerCase() + ", so the roster can no longer be changed.");
            Screen.blank();
            printLines(Report.rosterLines(jo));
            return;
        }
        boolean inLine = jo.getStatus() == Status.IN_LINE;
        boolean editing = true;
        while (editing) {
            Screen.bar("Roster > " + jo.code() + " " + jo.getTeamName());
            Screen.pair("Players", roster.count() + " of " + jo.getQuantity());
            if (inLine) {
                Screen.line("Already endorsed: players can only be edited until printing starts.");
            }
            Screen.blank();
            Screen.option(1, "Add player");
            Screen.option(2, "Remove player");
            Screen.option(3, "Edit player");
            Screen.option(4, "Undo last change");
            Screen.option(5, "View roster");
            Screen.option(6, "Confirmation slip");
            Screen.option(0, "Done");
            Screen.blank();
            int choice = readInt("Choice: ", 0, 6);
            Screen.blank();
            switch (choice) {
                case 1: addPlayer(jo, inLine); break;
                case 2: removePlayer(roster, inLine); break;
                case 3: editPlayer(jo); break;
                case 4: undo(jo); break;
                case 5: printLines(Report.rosterLines(jo)); break;
                case 6: printSlip(jo); break;
                default: editing = false;
            }
        }
        roster.clearUndo();    // undo only lasts while the roster is open
    }

    private void addPlayer(JobOrder jo, boolean inLine) {
        if (inLine) {
            Screen.error("Players can't be added after endorsing. Use Edit instead.");
            return;
        }
        if (jo.getRoster().count() >= jo.getQuantity()) {
            Screen.error("The roster is full (" + jo.getRoster().count() + " of " + jo.getQuantity()
                    + "). Remove or edit a player instead.");
            return;
        }
        RosterEntry entry = readEntry(jo);
        String problem = jo.getRoster().add(entry);
        if (problem != null) {
            Screen.error(problem + " Not added.");
            return;
        }
        save();
        Screen.blank();
        Screen.line("Added #" + entry.displayNumber() + " " + entry.displayName() + " (" + entry.getTopSize()
                + "). " + jo.getRoster().count() + " of " + jo.getQuantity() + " players.");
    }

    private void removePlayer(Roster roster, boolean inLine) {
        if (inLine) {
            Screen.error("Players can't be removed after endorsing. Use Edit instead.");
            return;
        }
        if (roster.count() == 0) {
            Screen.error("The roster is empty.");
            return;
        }
        printRows(roster);
        Screen.blank();
        int row = readInt("Row to remove (0 to go back): ", 0, roster.count());
        if (row == 0) {
            return;
        }
        RosterEntry removed = roster.remove(row);
        save();
        Screen.line("Removed #" + removed.displayNumber() + " " + removed.displayName() + ".");
    }

    private void editPlayer(JobOrder jo) {
        Roster roster = jo.getRoster();
        if (roster.count() == 0) {
            Screen.error("The roster is empty.");
            return;
        }
        printRows(roster);
        Screen.blank();
        int row = readInt("Row to edit (0 to go back): ", 0, roster.count());
        if (row == 0) {
            return;
        }
        Screen.line("Enter the corrected details:");
        String problem = roster.edit(row, readEntry(jo));
        if (problem != null) {
            Screen.error(problem + " Not changed.");
            return;
        }
        save();
        Screen.line("Updated.");
    }

    private void undo(JobOrder jo) {
        String undone = jo.getRoster().undo();
        if (undone == null) {
            Screen.error("Nothing to undo.");
            return;
        }
        save();
        Screen.line("Undo: took back " + undone.replace("took back ", "") + ". "
                + jo.getRoster().count() + " of " + jo.getQuantity() + " players.");
    }

    private void printSlip(JobOrder jo) {
        RosterSlip slip = new RosterSlip(manager, jo);
        Screen.line("-- " + slip.getTitle() + " --");
        Screen.blank();
        slip.print();
        Screen.blank();
        Screen.line("Saved to " + slip.saveToFile());
    }

    private RosterEntry readEntry(JobOrder jo) {
        String name = readName("Player name (blank for none): ", PLAYER_NAME, true,
                "Player names can have up to 20 letters (spaces and . ' - are OK).");
        String number = readJerseyNo("Jersey number (0-99, blank for none): ");
        Size top = readSize("Jersey size (XS to XXXL): ");
        Size shorts = (jo.getItemType() == ItemType.JERSEY_SET) ? readSize("Shorts size (XS to XXXL): ") : null;
        return new RosterEntry(name, number, top, shorts);
    }

    private void printRows(Roster roster) {
        ArrayList<RosterEntry> list = roster.getEntries();
        Screen.line(String.format("%-4s %-4s %-22s %s", "Row", "No.", "Name", "Size"));
        for (int i = 0; i < list.size(); i++) {
            RosterEntry e = list.get(i);
            Screen.line(String.format("%3d  %-4s %-22s %s", i + 1, e.displayNumber(), e.displayName(),
                    e.getTopSize()));
        }
    }

    // ================= 3. DESIGN =================

    private void designMenu() {
        Screen.bar("Design");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        DesignHistory design = jo.getDesign();
        if (jo.getStatus() != Status.DRAFT) {
            Screen.error(jo.code() + " is " + describe(jo).toLowerCase() + ", so the design is locked.");
            Screen.pair("Final design", currentDesign(design));
            return;
        }
        boolean open = true;
        while (open) {
            Screen.bar("Design > " + jo.code() + " " + jo.getTeamName());
            Screen.pair("Current", currentDesign(design));
            Screen.pair("Approved", design.isApproved() ? "Yes" : "Not yet");
            Screen.pair("Revisions", "major " + design.getMajorCount() + " of " + DesignHistory.FREE_MAJOR
                    + " free, minor " + design.getMinorCount() + " of " + DesignHistory.FREE_MINOR + " free");
            Screen.blank();
            Screen.option(1, "Add revision");
            Screen.option(2, "Go back to previous version");
            Screen.option(3, "Approve current version");
            Screen.option(4, "Version history");
            Screen.option(0, "Back");
            Screen.blank();
            int choice = readInt("Choice: ", 0, 4);
            Screen.blank();
            switch (choice) {
                case 1: addDesign(design); break;
                case 2: revertDesign(design); break;
                case 3: approveDesign(design); break;
                case 4: designHistory(design); break;
                default: open = false;
            }
        }
    }

    private String currentDesign(DesignHistory design) {
        DesignVersion current = design.current();
        return (current == null) ? "none yet" : "v" + current.getVersionNo() + " - " + current.getNote();
    }

    private void addDesign(DesignHistory design) {
        String type = "INITIAL";
        if (design.versionCount() > 0) {
            Screen.line("Revision type");
            Screen.subOption(1, "Major (layout or colors)");
            Screen.subOption(2, "Minor (small fixes)");
            type = (readInt("Choice: ", 1, 2) == 1) ? "MAJOR" : "MINOR";
        }
        String note = readText("Short description (up to 60 characters): ", false, 60);
        boolean extra = design.addVersion(type, note, Today.get());
        save();
        Screen.line("Saved as v" + design.current().getVersionNo() + " (" + type.toLowerCase() + ").");
        if (extra) {
            Screen.line("Note: this is beyond the free revisions, so an extra fee may apply.");
        }
    }

    private void revertDesign(DesignHistory design) {
        if (!design.revert()) {
            Screen.error("There is no earlier version to go back to.");
            return;
        }
        save();
        Screen.line("Went back to v" + design.current().getVersionNo() + ".");
    }

    private void approveDesign(DesignHistory design) {
        if (!design.approve()) {
            Screen.error("Add a design first.");
            return;
        }
        save();
        Screen.line("Design v" + design.current().getVersionNo() + " approved.");
    }

    private void designHistory(DesignHistory design) {
        Screen.line(String.format("%-4s %-8s %-17s %s", "Ver", "Type", "Date", "Description"));
        for (DesignVersion v : design.getVersions()) {
            Screen.line(String.format("v%-3d %-8s %-17s %s", v.getVersionNo(), v.getType().toLowerCase(),
                    Today.show(v.getDate()), v.getNote()));
        }
        if (design.versionCount() == 0) {
            Screen.line("No designs yet.");
        }
    }

    // ================= 4. PAYMENTS =================

    private void paymentsMenu() {
        Screen.bar("Payments");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        if (jo.getStatus() == Status.CANCELLED) {
            Screen.error(jo.code() + " is cancelled, so it takes no payments.");
            return;
        }
        boolean open = true;
        while (open) {
            Screen.bar("Payments > " + jo.code() + " " + jo.getTeamName());
            printMoney(jo);
            Screen.blank();
            Screen.option(1, "Record payment");
            Screen.option(2, "Payment history");
            Screen.option(0, "Back");
            Screen.blank();
            int choice = readInt("Choice: ", 0, 2);
            Screen.blank();
            switch (choice) {
                case 1: takePayment(jo); break;
                case 2: paymentHistory(jo); break;
                default: open = false;
            }
        }
    }

    private void printMoney(JobOrder jo) {
        Screen.pair("Status", describe(jo));
        Screen.pair("Total", Screen.peso(jo.getTotal()));
        Screen.pair("Downpayment", Screen.peso(jo.getRequiredDownpayment()) + " required");
        Screen.pair("Paid", Screen.peso(jo.getAmountPaid()));
        Screen.pair("Balance", Screen.peso(jo.getBalance()));
    }

    private void paymentHistory(JobOrder jo) {
        if (jo.getPayments().isEmpty()) {
            Screen.line("No payments yet.");
            return;
        }
        Screen.line(String.format("%-17s %-8s %12s", "Date", "Method", "Amount"));
        for (Payment p : jo.getPayments()) {
            Screen.line(String.format("%-17s %-8s %12s", Today.show(p.getDate()), p.getMethod(),
                    Screen.peso(p.getAmount())));
        }
    }

    // returns true if a payment was recorded
    private boolean takePayment(JobOrder jo) {
        if (jo.getBalance() <= 0) {
            Screen.error(jo.code() + " is fully paid. Nothing to record.");
            return false;
        }
        Screen.section("Record Payment");
        Screen.line("Type 'cancel' at any prompt to go back.");
        cancelAllowed = true;
        try {
            return fillPayment(jo);
        } catch (CancelInput e) {
            Screen.line("Cancelled. No payment was recorded.");
            return false;
        } finally {
            cancelAllowed = false;
        }
    }

    private boolean fillPayment(JobOrder jo) {
        double balance = jo.getBalance();
        Screen.blank();
        Screen.pair("Balance", Screen.peso(balance));
        Screen.blank();
        Screen.line("Method");
        Screen.subOption(1, "Cash");
        Screen.subOption(2, "GCash");
        Screen.subOption(3, "Bank transfer");
        int m = readInt("Choice: ", 1, 3);
        String method = (m == 1) ? "Cash" : (m == 2) ? "GCash" : "Bank";
        // cash can be more than the balance (we give change); a transfer can't
        double max = (m == 1) ? balance + MAX_CASH_OVER : balance;
        String limitNote = (m == 1) ? "Cash can be up to " + Screen.peso(MAX_CASH_OVER) + " over the balance."
                : "A " + method + " payment can't be more than the balance.";
        double amount = readAmountWithNote("Amount received: ", 1, max, limitNote);
        Screen.blank();
        if (!readYesNo("Record a " + method + " payment of " + Screen.peso(amount) + " for " + jo.code() + "? (Y/N): ")) {
            Screen.line("No payment was recorded.");
            return false;
        }
        double change = manager.recordPayment(jo, amount, method);
        save();
        Screen.blank();
        Screen.line("Payment recorded. Balance is now " + Screen.peso(jo.getBalance()) + ".");
        if (change > 0) {
            Screen.pair("Change", Screen.peso(change));
        }
        return true;
    }

    // ================= 5. PRODUCTION LINE =================

    private void productionMenu() {
        boolean open = true;
        while (open) {
            Screen.bar("Production Line");
            Screen.option(1, "View lines");
            Screen.option(2, "Endorse job order");
            Screen.option(3, "Update stage");
            Screen.option(4, "Receive from main branch");
            Screen.option(5, "Cancel job order");
            Screen.option(0, "Back");
            Screen.blank();
            int choice = readInt("Choice: ", 0, 5);
            switch (choice) {
                case 1: showLines(); break;
                case 2: endorse(); break;
                case 3: updateStage(); break;
                case 4: receive(); break;
                case 5: cancel(); break;
                default: open = false;
            }
        }
    }

    private void showLines() {
        Screen.bar("Production Line > View Lines");
        Screen.line("-- Rush line (" + manager.getRushLine().size() + " of " + JobOrderManager.RUSH_LIMIT + ") --");
        printLine(manager.getRushLine());
        Screen.section("Regular line (" + manager.getRegularLine().size() + ")");
        printLine(manager.getRegularLine());
    }

    private void printLine(JobQueue line) {
        ArrayList<JobOrder> list = line.toList();
        if (list.isEmpty()) {
            Screen.line("(empty)");
            return;
        }
        String format = "%-3s %-8s %-18s %4s  %-16s  %s";
        Screen.line(String.format(format, "#", "JO", "Team", "Pcs", "Target date", "Stage"));
        for (int i = 0; i < list.size(); i++) {
            JobOrder jo = list.get(i);
            Screen.line(String.format(format, String.valueOf(i + 1), jo.code(), Report.cut(jo.getTeamName(), 18),
                    String.valueOf(jo.getQuantity()), Today.show(jo.getTargetDate()), jo.getStage().label()));
        }
    }

    private void endorse() {
        Screen.bar("Production Line > Endorse");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        Screen.section("Checklist");
        ArrayList<String> problems = manager.endorseProblems(jo);
        for (String p : problems) {
            Screen.error(p);
        }
        if (!problems.isEmpty()) {
            Screen.blank();
            Screen.line(jo.code() + " can't be endorsed yet.");
            return;
        }
        Screen.line("Roster complete, design approved, downpayment paid.");

        Screen.section("Estimate");
        Screen.pair("Target date", Today.show(jo.getTargetDate()));
        boolean rush = jo.isRush();
        boolean rushFull = rush && manager.rushLineFull();
        if (rushFull) {
            Screen.error("The rush line is full (" + JobOrderManager.RUSH_LIMIT + " of " + JobOrderManager.RUSH_LIMIT
                    + "). It can only go to the regular line for now.");
            rush = false;
        }
        LocalDate estimate = showEstimate(rush);
        boolean late = estimate.isAfter(jo.getTargetDate());
        if (late) {
            Screen.error("This order may miss its target date.");
        }
        boolean canSwitch = late && !rush && !manager.rushLineFull();
        if (canSwitch && readYesNo("Make it a rush order instead? (Y/N): ")) {
            rush = true;
            estimate = showEstimate(true);
            late = estimate.isAfter(jo.getTargetDate());
            Screen.line(late ? "[!] Even as a rush order it may be late." : "That makes the target date.");
        }

        Screen.blank();
        String lineName = rush ? "rush" : "regular";
        if (!readYesNo("Endorse " + jo.code() + " " + jo.getTeamName() + " to the " + lineName + " line? (Y/N): ")) {
            Screen.line("Not endorsed.");
            return;
        }
        manager.endorse(jo, rush);
        ProductionSheet sheet = new ProductionSheet(manager, jo);
        String path = sheet.saveToFile();
        save();
        Screen.blank();
        Screen.line("Endorsed. " + jo.code() + " is #" + manager.placeInLine(jo) + " in the " + lineName + " line.");
        Screen.line("Production sheet saved: " + path);
    }

    private LocalDate showEstimate(boolean rush) {
        LocalDate estimate = manager.estimateFinish(rush);
        Screen.pair(rush ? "As rush" : "As regular", manager.ordersAhead(rush) + " orders ahead, done by "
                + Today.show(estimate));
        return estimate;
    }

    private void updateStage() {
        Screen.bar("Production Line > Update Stage");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        if (jo.getStatus() != Status.IN_LINE) {
            Screen.error(jo.code() + " is " + describe(jo).toLowerCase() + ". Only job orders in the line have a stage.");
            return;
        }
        Stage[] stages = Stage.values();
        int first = jo.getStage().ordinal() + 1;
        if (first >= stages.length) {
            Screen.error("Already at the last stage. Use Receive when the jerseys arrive.");
            return;
        }
        Screen.pair("Current stage", jo.getStage().label());
        Screen.blank();
        for (int i = first; i < stages.length; i++) {
            Screen.option(i - first + 1, stages[i].label());
        }
        Screen.blank();
        int pick = readInt("New stage: ", 1, stages.length - first);
        manager.updateStage(jo, stages[first + pick - 1]);
        save();
        Screen.line(jo.code() + " is now at " + jo.getStage().label() + ".");
    }

    private void receive() {
        Screen.bar("Production Line > Receive");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        int place = manager.placeInLine(jo);
        if (!manager.receiveFromMain(jo)) {
            Screen.error(jo.code() + " is " + describe(jo).toLowerCase() + ", not in the production line.");
            return;
        }
        save();
        Screen.blank();
        Screen.line(jo.code() + " " + jo.getTeamName() + " is back from the main branch and ready for release.");
        if (place > 1) {
            Screen.line("It was #" + place + " in line. The orders before it keep their places.");
        }
        Screen.pair("To collect", Screen.peso(jo.getBalance()));
    }

    private void cancel() {
        Screen.bar("Production Line > Cancel");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        // check first, so we don't ask to confirm something that can't happen
        String problem = manager.cancelProblem(jo);
        if (problem != null) {
            Screen.error(problem);
            return;
        }
        Screen.blank();
        if (!readYesNo("Cancel " + jo.code() + " " + jo.getTeamName() + "? This can't be undone. (Y/N): ")) {
            Screen.line("Not cancelled.");
            return;
        }
        manager.cancel(jo);
        save();
        Screen.blank();
        Screen.line(jo.code() + " cancelled.");
    }

    // ================= 6. RELEASE =================

    private void releaseMenu() {
        Screen.bar("Release");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        if (jo.getStatus() != Status.FOR_RELEASE) {
            Screen.error(jo.code() + " is " + describe(jo).toLowerCase() + ", not ready for release.");
            return;
        }
        Screen.pair("Team", jo.getTeamName());
        Screen.pair("Balance", Screen.peso(jo.getBalance()));
        while (jo.getBalance() > 0) {
            Screen.blank();
            Screen.line("The balance must be paid before release.");
            if (!readYesNo("Record a payment now? (Y/N): ")) {
                Screen.line("Not released.");
                return;
            }
            takePayment(jo);
        }
        Screen.blank();
        if (!readYesNo("Release " + jo.code() + " to " + jo.getTeamName() + "? (Y/N): ")) {
            Screen.line("Not released.");
            return;
        }
        manager.release(jo);
        save();
        Screen.blank();
        Screen.line(jo.code() + " released to " + jo.getTeamName() + ". Thank you!");
    }

    // ================= 7. LOOKUP =================

    private void lookupMenu() {
        boolean open = true;
        while (open) {
            Screen.bar("Lookup");
            Screen.option(1, "By JO number");
            Screen.option(2, "By team name (any part)");
            Screen.option(3, "Teams starting with");
            Screen.option(4, "Team history");
            Screen.option(5, "All teams A to Z");
            Screen.option(6, "Status message for a customer");
            Screen.option(0, "Back");
            Screen.blank();
            int choice = readInt("Choice: ", 0, 6);
            switch (choice) {
                case 1: lookupJobOrder(); break;
                case 2: searchTeams(); break;
                case 3: teamsStartingWith(); break;
                case 4: teamHistory(); break;
                case 5: allTeams(); break;
                case 6: statusMessage(); break;
                default: open = false;
            }
        }
    }

    private void lookupJobOrder() {
        Screen.bar("Lookup > By JO Number");
        JobOrder jo = askJobOrder();
        if (jo != null) {
            printDetails(jo);
        }
    }

    private void searchTeams() {
        Screen.bar("Lookup > By Team Name");
        String keyword = readText("Team name contains: ", false, 30);
        ArrayList<JobOrder> found = manager.searchByTeam(keyword);
        // A to Z by team; the sort is stable, so each team's orders stay in JO order
        Sorter.mergeSort(found, (a, b) -> a.getTeamName().compareToIgnoreCase(b.getTeamName()));
        Screen.blank();
        if (found.isEmpty()) {
            Screen.line("No job orders found for \"" + keyword + "\".");
            return;
        }
        String format = "%-8s %-18s %-13s %s";
        Screen.line(String.format(format, "JO", "Team", "Status", "Target date"));
        for (JobOrder j : found) {
            Screen.line(String.format(format, j.code(), Report.cut(j.getTeamName(), 18), j.getStatus().label(),
                    Today.show(j.getTargetDate())));
        }
        Screen.blank();
        Screen.line(found.size() + " found.");
    }

    private void teamsStartingWith() {
        Screen.bar("Lookup > Teams Starting With");
        String prefix = readText("Team name starts with: ", false, 30);
        Screen.blank();
        printTeams(manager.teamsStartingWith(prefix));
    }

    private void allTeams() {
        Screen.bar("Lookup > All Teams");
        printTeams(manager.teamDirectory());
    }

    private void statusMessage() {
        Screen.bar("Lookup > Status Message");
        JobOrder jo = askJobOrder();
        if (jo == null) {
            return;
        }
        Screen.blank();
        Screen.line("Copy and paste this into Messenger:");
        Screen.blank();
        Screen.line("-".repeat(Screen.WIDTH - 4));
        printWrapped(manager.statusMessage(jo), Screen.WIDTH - 4);
        Screen.line("-".repeat(Screen.WIDTH - 4));
    }

    private void printTeams(ArrayList<Team> teams) {
        if (teams.isEmpty()) {
            Screen.line("No teams found.");
            return;
        }
        for (Team t : teams) {
            int n = t.getJoNumbers().size();
            Screen.line(String.format("%-30s %d order%s", t.getName(), n, n == 1 ? "" : "s"));
        }
    }

    private void teamHistory() {
        Screen.bar("Lookup > Team History");
        String name = readText("Exact team name: ", false, 30);
        Team team = manager.findTeam(name);
        if (team == null) {
            Screen.error("No team with that exact name. Try \"Teams starting with\".");
            return;
        }
        Screen.blank();
        Screen.pair("Team", team.getName());
        Screen.pair("Contact", team.getContactPerson() + ", " + team.getContactNo());
        Screen.blank();
        String format = "%-8s %-11s %4s  %-13s %s";
        Screen.line(String.format(format, "JO", "Sport", "Pcs", "Status", "Created"));
        for (JobOrder jo : manager.teamHistory(team)) {
            Screen.line(String.format(format, jo.code(), jo.getSport(), String.valueOf(jo.getQuantity()),
                    jo.getStatus().label(), Today.show(jo.getDateCreated())));
        }
    }

    private void printDetails(JobOrder jo) {
        Screen.blank();
        Screen.pair("Job order", jo.code() + " " + jo.getTeamName());
        Screen.pair("Status", describe(jo));
        Screen.pair("Contact", jo.getContactPerson() + ", " + jo.getContactNo());
        Screen.pair("Order", jo.getQuantity() + " x " + jo.getItemType().getLabel() + ", " + jo.getSport()
                + (jo.isRush() ? " (rush)" : ""));
        Screen.pair("Price", Screen.peso(jo.getUnitPrice()) + " each");
        Screen.pair("Total", Screen.peso(jo.getTotal()));
        Screen.pair("Paid", Screen.peso(jo.getAmountPaid()));
        Screen.pair("Balance", Screen.peso(jo.getBalance()));
        Screen.pair("Created", Today.show(jo.getDateCreated()));
        Screen.pair("Target date", Today.show(jo.getTargetDate()));
        Screen.pair("Roster", jo.getRoster().count() + " of " + jo.getQuantity() + " players");
        Screen.pair("Design", currentDesign(jo.getDesign()) + (jo.getDesign().isApproved() ? " (approved)" : ""));
        if (jo.getStatus() == Status.IN_LINE) {
            Screen.pair("Place in line", "#" + manager.placeInLine(jo) + " in the " + (jo.isRush() ? "rush" : "regular") + " line");
        }
    }

    // e.g. "In line, at Print" or "For release"
    private String describe(JobOrder jo) {
        String text = jo.getStatus().label();
        return (jo.getStatus() == Status.IN_LINE) ? text + ", at " + jo.getStage().label() : text;
    }

    // ================= 8. REPORTS =================

    private void reportsMenu() {
        boolean open = true;
        while (open) {
            Screen.bar("Reports");
            Screen.option(1, "Due within 7 days");
            Screen.option(2, "Unclaimed orders");
            Screen.option(3, "Unpaid balances");
            Screen.option(4, "Sales for a month");
            Screen.option(5, "Production sheet");
            Screen.option(0, "Back");
            Screen.blank();
            int choice = readInt("Choice: ", 0, 5);
            Report report = null;
            switch (choice) {
                case 1: report = new DueSoonReport(manager, 7); break;
                case 2: report = new UnclaimedReport(manager, 3); break;
                case 3: report = new BalanceReport(manager); break;
                case 4: report = askSalesReport(); break;
                case 5: report = askProductionSheet(); break;
                default: open = false;
            }
            if (report != null) {
                showReport(report);
            }
        }
    }

    private Report askSalesReport() {
        Screen.bar("Reports > Sales");
        return new SalesReport(manager, readMonth("Month (YYYY-MM): "));
    }

    private Report askProductionSheet() {
        Screen.bar("Reports > Production Sheet");
        JobOrder jo = askJobOrder();
        return (jo == null) ? null : new ProductionSheet(manager, jo);
    }

    private void showReport(Report report) {
        Screen.bar("Reports > " + report.getTitle());
        report.print();     // the right generate() runs for whichever report it is
        Screen.blank();
        if (readYesNo("Save to file? (Y/N): ")) {
            Screen.line("Saved to " + report.saveToFile());
        }
    }

    // ================= INPUT HELPERS =================

    private void save() {
        if (!dataStore.saveAll(manager)) {
            Screen.error("Could not save the data files.");
        }
    }

    private void printLines(ArrayList<String> lines) {
        for (String line : lines) {
            Screen.line(line);
        }
    }

    // prints long text in lines of about `width` characters, without cutting words
    private void printWrapped(String text, int width) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            boolean full = line.length() > 0 && line.length() + word.length() + 1 > width;
            if (full) {
                Screen.line(line.toString());
                line = new StringBuilder();
            }
            line.append(line.length() == 0 ? "" : " ").append(word);
        }
        Screen.line(line.toString());
    }

    // reads one line; "cancel" leaves the form when that's allowed
    private String readLine(String prompt) {
        System.out.print(Screen.INDENT + prompt);
        String text = scanner.nextLine().trim();
        Screen.printed();
        if (cancelAllowed && text.equalsIgnoreCase("cancel")) {
            throw new CancelInput();
        }
        return text;
    }

    // accepts "2047" or "JO-2047"; asks again until a real job order is given (0 goes back)
    private JobOrder askJobOrder() {
        JobOrder jo = null;
        boolean asking = true;
        while (asking) {
            String text = readLine("JO number (0 to go back): ").toUpperCase().replace("JO-", "").replace("JO", "").trim();
            int number = text.matches("\\d{1,6}") ? Integer.parseInt(text) : -1;
            jo = (number > 0) ? manager.find(number) : null;
            asking = number != 0 && jo == null;
            if (number < 0) {
                Screen.error("Please type a JO number, like 2047.");
            } else if (asking) {
                Screen.error("No job order JO-" + number + ".");
            }
        }
        Screen.blank();
        return jo;
    }

    private String readText(String prompt, boolean allowBlank, int maxLength) {
        String text = "";
        boolean valid = false;
        while (!valid) {
            text = readLine(prompt);
            if (text.contains(",")) {
                Screen.error("Please don't use commas.");
            } else if (text.isEmpty() && !allowBlank) {
                Screen.error("This can't be blank.");
            } else if (text.length() > maxLength) {
                Screen.error("Please keep it to " + maxLength + " characters.");
            } else {
                valid = true;
            }
        }
        return text;
    }

    // a name that must match a pattern; extra spaces are squeezed out, the letters are kept as typed
    private String readName(String prompt, String pattern, boolean allowBlank, String rule) {
        String text = "";
        boolean valid = false;
        while (!valid) {
            text = readLine(prompt).replaceAll("\\s+", " ");
            valid = (text.isEmpty() && allowBlank) || text.matches(pattern);
            if (!valid) {
                Screen.error(rule);
            }
        }
        return text;
    }

    // Philippine mobile number: 09 and 9 more digits; saved as 09XX-XXX-XXXX
    private String readMobile(String prompt) {
        String digits = "";
        boolean valid = false;
        while (!valid) {
            digits = readLine(prompt).replace("-", "").replace(" ", "");
            valid = digits.matches("09\\d{9}");
            if (!valid) {
                Screen.error("Use an 11-digit mobile number starting with 09, like 0918-555-0112.");
            }
        }
        return digits.substring(0, 4) + "-" + digits.substring(4, 7) + "-" + digits.substring(7);
    }

    // sport from a fixed list, so "basketball" and "Basketball" end up the same in the sales report
    private String readSport() {
        Screen.line("Sports: " + String.join(", ", SPORTS));
        String sport = null;
        while (sport == null) {
            String text = readLine("Sport: ");
            for (String s : SPORTS) {
                sport = s.equalsIgnoreCase(text) ? s : sport;
            }
            if (sport == null) {
                Screen.error("Please pick one of the sports listed above.");
            }
        }
        return sport;
    }

    private int readInt(String prompt, int min, int max) {
        int value = 0;
        boolean valid = false;
        while (!valid) {
            String text = readLine(prompt);
            try {
                value = Integer.parseInt(text);
                valid = value >= min && value <= max;
                if (!valid) {
                    Screen.error("Please enter a number from " + min + " to " + max + ".");
                }
            } catch (NumberFormatException e) {
                Screen.error("Please enter a whole number from " + min + " to " + max + ".");
            }
        }
        return value;
    }

    private double readAmount(String prompt, double min, double max) {
        return readAmountWithNote(prompt, min, max, "");
    }

    private double readAmountWithNote(String prompt, double min, double max, String note) {
        double value = 0;
        boolean valid = false;
        while (!valid) {
            String text = readLine(prompt).replace(",", "").replace("P", "").replace("p", "");
            try {
                value = Math.round(Double.parseDouble(text) * 100) / 100.0;
                valid = value >= min && value <= max;
                if (!valid) {
                    Screen.error(("Please enter an amount from " + Screen.peso(min) + " to " + Screen.peso(max) + ". " + note).trim());
                }
            } catch (NumberFormatException e) {
                Screen.error("Please enter an amount, like 650 or 650.50.");
            }
        }
        return value;
    }

    private LocalDate readFutureDate(String prompt) {
        LocalDate latest = Today.get().plusMonths(MAX_MONTHS_AHEAD);
        LocalDate date = null;
        boolean valid = false;
        while (!valid) {
            try {
                date = LocalDate.parse(readLine(prompt));
                valid = date.isAfter(Today.get()) && !date.isAfter(latest);
                if (!valid) {
                    Screen.error("Pick a date after today and on or before " + Today.show(latest) + ".");
                }
            } catch (DateTimeParseException e) {
                Screen.error("Please use the format YYYY-MM-DD, like 2026-11-20.");
            }
        }
        return date;
    }

    private YearMonth readMonth(String prompt) {
        YearMonth thisMonth = YearMonth.from(Today.get());
        YearMonth month = null;
        while (month == null) {
            try {
                month = YearMonth.parse(readLine(prompt));
                month = month.isAfter(thisMonth) ? null : month;
                if (month == null) {
                    Screen.error("That month hasn't happened yet.");
                }
            } catch (DateTimeParseException e) {
                Screen.error("Please use the format YYYY-MM, like 2026-10.");
            }
        }
        return month;
    }

    private boolean readYesNo(String prompt) {
        String answer = "";
        boolean valid = false;
        while (!valid) {
            answer = readLine(prompt).toUpperCase();
            valid = answer.equals("Y") || answer.equals("N");
            if (!valid) {
                Screen.error("Please type Y or N.");
            }
        }
        return answer.equals("Y");
    }

    private Size readSize(String prompt) {
        Size size = null;
        while (size == null) {
            size = Size.fromText(readLine(prompt));
            if (size == null) {
                Screen.error("Sizes are XS, S, M, L, XL, XXL or XXXL.");
            }
        }
        return size;
    }

    // one or two digits, or blank; kept as text so "0" and "00" stay different
    private String readJerseyNo(String prompt) {
        String text = null;
        while (text == null) {
            text = readLine(prompt);
            if (!text.isEmpty() && !text.matches("\\d{1,2}")) {
                Screen.error("Use a number from 0 to 99 (00 is allowed), or leave it blank.");
                text = null;
            }
        }
        return text;
    }
}
