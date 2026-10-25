// Author: Macaraig
import java.time.LocalDate;

public class Payment {
    private LocalDate date;
    private double amount;
    private String method;   // Cash, GCash, Bank

    public Payment(LocalDate date, double amount, String method) {
        this.date = date;
        this.amount = amount;
        this.method = method;
    }

    public LocalDate getDate() { return date; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
}
