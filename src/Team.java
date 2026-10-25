// Author: Macaraig
import java.util.ArrayList;

// A team that has ordered from the branch, with the numbers of all its job orders.
public class Team {
    private String name;
    private String contactPerson;
    private String contactNo;
    private ArrayList<Integer> joNumbers = new ArrayList<>();

    public Team(String name, String contactPerson, String contactNo) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.contactNo = contactNo;
    }

    public void addJobOrder(int joNumber, String contactPerson, String contactNo) {
        joNumbers.add(joNumber);
        this.contactPerson = contactPerson;   // keep the latest contact
        this.contactNo = contactNo;
    }

    public String getName() { return name; }
    public String getContactPerson() { return contactPerson; }
    public String getContactNo() { return contactNo; }
    public ArrayList<Integer> getJoNumbers() { return joNumbers; }
}
