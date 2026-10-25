// Author: Macaraig
import java.util.ArrayList;

// What both production lines can do. The regular line is a linked list,
// the rush line is a circular array with a fixed capacity.
public interface JobQueue {
    void enqueue(JobOrder jo);
    JobOrder dequeue();
    JobOrder peek();
    JobOrder remove(int joNumber);
    int positionOf(int joNumber);
    int size();
    boolean isEmpty();
    boolean isFull();
    ArrayList<JobOrder> toList();
}
