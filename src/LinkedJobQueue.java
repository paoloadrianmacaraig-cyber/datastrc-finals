// Author: Macaraig
import java.util.ArrayList;

// Regular production line: a queue built from a singly linked list.
public class LinkedJobQueue implements JobQueue {
    private class Node {
        JobOrder data;
        Node next;

        Node(JobOrder data) {
            this.data = data;
        }
    }

    private Node front;
    private Node rear;
    private int size;

    public void enqueue(JobOrder jo) {
        Node newNode = new Node(jo);
        if (rear == null) {
            front = newNode;          // line was empty
        } else {
            rear.next = newNode;      // join at the back
        }
        rear = newNode;
        size++;
    }

    public JobOrder dequeue() {
        if (front == null) {
            return null;
        }
        JobOrder first = front.data;
        front = front.next;
        rear = (front == null) ? null : rear;
        size--;
        return first;
    }

    public JobOrder peek() {
        return (front == null) ? null : front.data;
    }

    // Takes one job order out of the line using only dequeue and enqueue,
    // so the rest of the line keeps its order.
    public JobOrder remove(int joNumber) {
        JobOrder removed = null;
        int count = size;
        for (int i = 0; i < count; i++) {
            JobOrder current = dequeue();
            if (current.getJoNumber() == joNumber && removed == null) {
                removed = current;    // leave it out of the line
                continue;
            }
            enqueue(current);         // send it to the back
        }
        return removed;
    }

    // 1 = front of the line, -1 = not in this line
    public int positionOf(int joNumber) {
        int position = 1;
        for (Node n = front; n != null; n = n.next) {
            if (n.data.getJoNumber() == joNumber) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public boolean isFull() { return false; }   // a linked list never runs out of slots

    public ArrayList<JobOrder> toList() {
        ArrayList<JobOrder> list = new ArrayList<>();
        for (Node n = front; n != null; n = n.next) {
            list.add(n.data);
        }
        return list;
    }
}
