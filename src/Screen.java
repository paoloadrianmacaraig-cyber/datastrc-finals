// Author: Tengonciang
// Everything about how the screens look is kept here, so every menu
// uses the same width, spacing, headers and error style.
public class Screen {
    public static final int WIDTH = 72;
    public static final String INDENT = "  ";

    // true right after an empty line, so two empty lines never stack up
    private static boolean lastWasBlank = false;

    private Screen() {
    }

    // called after anything is printed outside this class (like a prompt the user answers)
    public static void printed() {
        lastWasBlank = false;
    }

    // shown once when the program starts
    public static void title() {
        System.out.println("=".repeat(WIDTH));
        System.out.println();
        System.out.println(center("MNL KINGPIN - QUEZON BRANCH"));
        System.out.println(center("Job Order Tracking System"));
        System.out.println(center("From roster to release."));
        System.out.println();
        System.out.println("=".repeat(WIDTH));
        lastWasBlank = false;
        if (Today.isDemo()) {
            blank();
            line("Demo mode: today is set to " + Today.show(Today.get()) + ".");
        }
    }

    // the bar at the top of every screen: where you are on the left, the date on the right
    // path is null for the main menu, or something like "Production Line > Endorse"
    public static void bar(String path) {
        String left = " Home" + (path == null ? "" : " > " + path);
        String right = Today.show(Today.get());
        blank();
        System.out.println("=".repeat(WIDTH));
        System.out.println(left + " ".repeat(Math.max(1, WIDTH - 1 - left.length() - right.length())) + right);
        System.out.println("-".repeat(WIDTH));
        lastWasBlank = false;
        blank();
    }

    // smaller heading inside a screen, e.g. -- Review --
    public static void section(String name) {
        blank();
        line("-- " + name + " --");
    }

    public static void line(String text) {
        System.out.println(text.isEmpty() ? "" : INDENT + text);
        lastWasBlank = text.isEmpty();
    }

    public static void blank() {
        if (!lastWasBlank) {
            System.out.println();
        }
        lastWasBlank = true;
    }

    // label and value lined up in two columns, e.g. "Total          P9,750.00"
    public static void pair(String label, String value) {
        line(String.format("%-16s%s", label, value));
    }

    public static void option(int number, String text) {
        line(number + " - " + text);
    }

    // an option inside a form, indented under its label (e.g. the payment methods)
    public static void subOption(int number, String text) {
        line("  " + number + " - " + text);
    }

    // every error starts with [!] so it stands out from normal text
    public static void error(String message) {
        line("[!] " + message);
    }

    public static String peso(double amount) {
        return "P" + JobOrderManager.money(amount);
    }

    private static String center(String text) {
        return " ".repeat(Math.max(0, (WIDTH - text.length()) / 2)) + text;
    }
}
