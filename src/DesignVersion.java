// Author: Macaraig
import java.time.LocalDate;

public class DesignVersion {
    private int versionNo;
    private String type;      // INITIAL, MAJOR or MINOR
    private String note;
    private LocalDate date;

    public DesignVersion(int versionNo, String type, String note, LocalDate date) {
        this.versionNo = versionNo;
        this.type = type;
        this.note = note;
        this.date = date;
    }

    public int getVersionNo() { return versionNo; }
    public String getType() { return type; }
    public String getNote() { return note; }
    public LocalDate getDate() { return date; }
}
