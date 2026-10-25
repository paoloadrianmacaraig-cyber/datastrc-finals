// Author: Tengonciang
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Gives the program's "today". Normally this is the real date. If data/settings.txt
// has a line like demoDate=2026-10-29, that date is used instead so the demo
// always shows the same results.
public class Today {
    // how dates are shown on screen, e.g. "Wed, 11 Nov 2026"
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    private static LocalDate demoDate;

    public static void load(String settingsFile) {
        demoDate = null;
        try (BufferedReader in = new BufferedReader(new FileReader(settingsFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("demoDate=") && line.length() > 9) {
                    demoDate = LocalDate.parse(line.substring(9).trim());
                }
            }
        } catch (IOException | RuntimeException e) {
            demoDate = null;    // no settings file or bad date: just use the real date
        }
    }

    public static LocalDate get() {
        return (demoDate != null) ? demoDate : LocalDate.now();
    }

    public static boolean isDemo() {
        return demoDate != null;
    }

    public static String show(LocalDate date) {
        return (date == null) ? "-" : date.format(FORMAT);
    }
}
