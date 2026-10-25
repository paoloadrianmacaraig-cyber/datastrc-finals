// Author: Tengonciang
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;

// Reads and writes the CSV files in the data folder.
public class DataStore {
    private String folder;

    public DataStore(String folder) {
        this.folder = folder;
        new File(folder).mkdirs();    // make the folder if it doesn't exist yet
    }

    private String path(String file) {
        return folder + File.separator + file;
    }

    // ---------- loading ----------

    public void loadAll(JobOrderManager manager) {
        for (String[] f : readRows("joborders.csv")) {
            JobOrder jo = new JobOrder(Integer.parseInt(f[0]), f[1], f[2], f[3], f[4],
                    ItemType.valueOf(f[5]), Integer.parseInt(f[6]), Double.parseDouble(f[7]),
                    Boolean.parseBoolean(f[8]), date(f[9]), date(f[10]));
            jo.setDateEndorsed(date(f[11]));
            jo.setDateReceived(date(f[12]));
            jo.setDateReleased(date(f[13]));
            jo.setStatus(Status.valueOf(f[14]));
            jo.setStage(Stage.valueOf(f[15]));
            jo.getDesign().loadState(Integer.parseInt(f[17]), Integer.parseInt(f[18]), Boolean.parseBoolean(f[16]));
            manager.loadJobOrder(jo);
        }
        manager.sortAfterLoading();

        for (String[] f : readRows("rosters.csv")) {
            JobOrder jo = manager.find(Integer.parseInt(f[0]));
            if (jo != null) {
                Size shorts = f[4].isEmpty() ? null : Size.valueOf(f[4]);
                jo.getRoster().load(new RosterEntry(f[1], f[2], Size.valueOf(f[3]), shorts));
            }
        }
        // saved oldest first, so pushing them in order puts the newest on top
        for (String[] f : readRows("designs.csv")) {
            JobOrder jo = manager.find(Integer.parseInt(f[0]));
            if (jo != null) {
                jo.getDesign().load(new DesignVersion(Integer.parseInt(f[1]), f[2], f[4], date(f[3])));
            }
        }
        for (String[] f : readRows("payments.csv")) {
            JobOrder jo = manager.find(Integer.parseInt(f[0]));
            if (jo != null) {
                jo.addPayment(new Payment(date(f[1]), Double.parseDouble(f[2]), f[3]));
            }
        }
        loadLine(manager, "rushline.csv");
        loadLine(manager, "regularline.csv");
    }

    // the lines are saved front to rear, so enqueuing in file order rebuilds them exactly
    private void loadLine(JobOrderManager manager, String file) {
        for (String[] f : readRows(file)) {
            JobOrder jo = manager.find(Integer.parseInt(f[0]));
            if (jo != null && jo.getStatus() == Status.IN_LINE) {
                manager.loadIntoLine(jo);
            }
        }
    }

    // reads every row except the header; a missing file just gives an empty list
    private ArrayList<String[]> readRows(String file) {
        ArrayList<String[]> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(path(file)))) {
            String line = in.readLine();    // skip header
            while ((line = in.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rows.add(line.split(",", -1));
                }
            }
        } catch (IOException e) {
            // first run: no file yet
        }
        return rows;
    }

    private LocalDate date(String text) {
        return text.isEmpty() ? null : LocalDate.parse(text);
    }

    private String text(LocalDate d) {
        return (d == null) ? "" : d.toString();
    }

    // ---------- saving (called after every change) ----------

    public boolean saveAll(JobOrderManager manager) {
        try {
            saveJobOrders(manager);
            saveLine(manager.getRushLine(), "rushline.csv");
            saveLine(manager.getRegularLine(), "regularline.csv");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void saveJobOrders(JobOrderManager manager) throws IOException {
        try (PrintWriter jos = new PrintWriter(new FileWriter(path("joborders.csv")));
             PrintWriter ros = new PrintWriter(new FileWriter(path("rosters.csv")));
             PrintWriter des = new PrintWriter(new FileWriter(path("designs.csv")));
             PrintWriter pay = new PrintWriter(new FileWriter(path("payments.csv")))) {

            jos.println("joNumber,teamName,contactPerson,contactNo,sport,itemType,quantity,unitPrice,rush,"
                    + "targetDate,dateCreated,dateEndorsed,dateReceived,dateReleased,status,stage,"
                    + "designApproved,majorCount,minorCount");
            ros.println("joNumber,playerName,jerseyNo,topSize,shortsSize");
            des.println("joNumber,versionNo,type,date,note");
            pay.println("joNumber,date,amount,method");

            for (JobOrder jo : manager.getJobOrders()) {
                DesignHistory d = jo.getDesign();
                jos.println(jo.getJoNumber() + "," + jo.getTeamName() + "," + jo.getContactPerson() + ","
                        + jo.getContactNo() + "," + jo.getSport() + "," + jo.getItemType() + ","
                        + jo.getQuantity() + "," + jo.getUnitPrice() + "," + jo.isRush() + ","
                        + text(jo.getTargetDate()) + "," + text(jo.getDateCreated()) + ","
                        + text(jo.getDateEndorsed()) + "," + text(jo.getDateReceived()) + ","
                        + text(jo.getDateReleased()) + "," + jo.getStatus() + "," + jo.getStage() + ","
                        + d.isApproved() + "," + d.getMajorCount() + "," + d.getMinorCount());

                for (RosterEntry e : jo.getRoster().getEntries()) {
                    ros.println(jo.getJoNumber() + "," + e.getPlayerName() + "," + e.getJerseyNo() + ","
                            + e.getTopSize() + "," + (e.getShortsSize() == null ? "" : e.getShortsSize()));
                }
                // the stack lists newest first, so write it backwards (oldest first)
                ArrayList<DesignVersion> versions = d.getVersions();
                for (int i = versions.size() - 1; i >= 0; i--) {
                    DesignVersion v = versions.get(i);
                    des.println(jo.getJoNumber() + "," + v.getVersionNo() + "," + v.getType() + ","
                            + text(v.getDate()) + "," + v.getNote());
                }
                for (Payment p : jo.getPayments()) {
                    pay.println(jo.getJoNumber() + "," + text(p.getDate()) + "," + p.getAmount() + "," + p.getMethod());
                }
            }
        }
    }

    private void saveLine(JobQueue line, String file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(path(file)))) {
            out.println("joNumber");
            for (JobOrder jo : line.toList()) {
                out.println(jo.getJoNumber());
            }
        }
    }

    // copies the sample files over the current data (used for practice runs)
    public static boolean resetFromSample(String sampleFolder, String dataFolder) {
        File[] files = new File(sampleFolder).listFiles();
        if (files == null) {
            return false;
        }
        new File(dataFolder).mkdirs();
        try {
            for (File f : files) {
                Files.copy(f.toPath(), new File(dataFolder, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
