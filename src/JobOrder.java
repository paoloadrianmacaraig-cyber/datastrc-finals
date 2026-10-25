// Author: Macaraig
import java.time.LocalDate;
import java.util.ArrayList;

public class JobOrder {
    public static final int MIN_QUANTITY = 12;
    public static final double DOWNPAYMENT_RATE = 0.50;

    private int joNumber;
    private String teamName;
    private String contactPerson;
    private String contactNo;
    private String sport;
    private ItemType itemType;
    private int quantity;
    private double unitPrice;
    private boolean rush;
    private LocalDate targetDate;
    private LocalDate dateCreated;
    private LocalDate dateEndorsed;    // null until endorsed
    private LocalDate dateReceived;    // null until the finished jerseys arrive
    private LocalDate dateReleased;    // null until claimed
    private Status status = Status.DRAFT;
    private Stage stage = Stage.NEW;

    private Roster roster = new Roster();
    private DesignHistory design = new DesignHistory();
    private ArrayList<Payment> payments = new ArrayList<>();

    public JobOrder(int joNumber, String teamName, String contactPerson, String contactNo, String sport,
                    ItemType itemType, int quantity, double unitPrice, boolean rush,
                    LocalDate targetDate, LocalDate dateCreated) {
        this.joNumber = joNumber;
        this.teamName = teamName;
        this.contactPerson = contactPerson;
        this.contactNo = contactNo;
        this.sport = sport;
        this.itemType = itemType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.rush = rush;
        this.targetDate = targetDate;
        this.dateCreated = dateCreated;
    }

    public double getTotal() {
        return quantity * unitPrice;
    }

    public double getAmountPaid() {
        double paid = 0;
        for (Payment p : payments) {
            paid += p.getAmount();
        }
        return paid;
    }

    public double getBalance() {
        return getTotal() - getAmountPaid();
    }

    public double getRequiredDownpayment() {
        return Math.round(getTotal() * DOWNPAYMENT_RATE * 100) / 100.0;
    }

    public void addPayment(Payment p) {
        payments.add(p);
    }

    public boolean isOpen() {
        return status == Status.DRAFT || status == Status.IN_LINE || status == Status.FOR_RELEASE;
    }

    // getters
    public int getJoNumber() { return joNumber; }
    public String getTeamName() { return teamName; }
    public String getContactPerson() { return contactPerson; }
    public String getContactNo() { return contactNo; }
    public String getSport() { return sport; }
    public ItemType getItemType() { return itemType; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public boolean isRush() { return rush; }
    public LocalDate getTargetDate() { return targetDate; }
    public LocalDate getDateCreated() { return dateCreated; }
    public LocalDate getDateEndorsed() { return dateEndorsed; }
    public LocalDate getDateReceived() { return dateReceived; }
    public LocalDate getDateReleased() { return dateReleased; }
    public Status getStatus() { return status; }
    public Stage getStage() { return stage; }
    public Roster getRoster() { return roster; }
    public DesignHistory getDesign() { return design; }
    public ArrayList<Payment> getPayments() { return payments; }

    // setters (only for the fields that change after the order is made)
    public void setRush(boolean rush) { this.rush = rush; }
    public void setStatus(Status status) { this.status = status; }
    public void setStage(Stage stage) { this.stage = stage; }
    public void setDateEndorsed(LocalDate d) { this.dateEndorsed = d; }
    public void setDateReceived(LocalDate d) { this.dateReceived = d; }
    public void setDateReleased(LocalDate d) { this.dateReleased = d; }

    public String code() {
        return "JO-" + joNumber;
    }
}
