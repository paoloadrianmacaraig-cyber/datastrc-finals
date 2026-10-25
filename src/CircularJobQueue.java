// Author: Macaraig
import java.util.ArrayList;

// Rush line: a circular queue in a fixed-size array.
// The main branch only takes a few rush orders at a time, so the line has a limit.
public class CircularJobQueue implements JobQueue {
    private JobOrder[] items;
    private int front;     // index of the first order
    private int rear;      // index of the last order
    private int size;

    public CircularJobQueue(int capacity) {
        items = new JobOrder[capacity];
        front = 0;
        rear = -1;
        size = 0;
    }

    public void enqueue(JobOrder jo) {
        if (isFull()) {
            throw new IllegalStateException("Rush line is full");
        }
        rear = (rear + 1) % items.length;    // wrap around to index 0 after the last slot
        items[rear] = jo;
        size++;
    }

    public JobOrder dequeue() {
        if (isEmpty()) {
            return null;
        }
        JobOrder first = items[front];
        items[front] = null;
        front = (front + 1) % items.length;
        size--;
        return first;
    }

    public JobOrder peek() {
        return isEmpty() ? null : items[front];
    }

    // same idea as the linked version: rotate once and leave out the match
    public JobOrder remove(int joNumber) {
        JobOrder removed = null;
        int count = size;
        for (int i = 0; i < count; i++) {
            JobOrder current = dequeue();
            if (current.getJoNumber() == joNumber && removed == null) {
                removed = current;
                continue;
            }
            enqueue(current);
        }
        return removed;
    }

    public int positionOf(int joNumber) {
        for (int i = 0; i < size; i++) {
            if (items[(front + i) % items.length].getJoNumber() == joNumber) {
                return i + 1;
            }
        }
        return -1;
    }

    public int size() { return size; }
    public int capacity() { return items.length; }
    public boolean isEmpty() { return size == 0; }
    public boolean isFull() { return size == items.length; }

    public ArrayList<JobOrder> toList() {
        ArrayList<JobOrder> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(items[(front + i) % items.length]);
        }
        return list;
    }
}
