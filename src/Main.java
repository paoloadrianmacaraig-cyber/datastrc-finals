// Author: Tengonciang
// Job Order Queue and Tracking System for MNL Kingpin - Quezon Branch
// DATASTRC Final Project, C2B
//
// Run normally:              java Main
// Start over with samples:   java Main --reset
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--reset")) {
            boolean ok = DataStore.resetFromSample("sample-data", "data");
            System.out.println(ok ? "Data reset to the sample data." : "Could not find the sample-data folder.");
        }
        ConsoleUI ui = new ConsoleUI();
        ui.run();
    }
}
