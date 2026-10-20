import java.util.ArrayList;
import java.util.List;

/**
 * Custom Queue data structure implemented using a Singly-Linked List
 * with dedicated front and rear pointers for strict FIFO (First-In, First-Out) operations.
 *
 * Complexity Characteristics:
 * - Enqueue: O(1) time
 * - Dequeue: O(1) time
 * - Peek:    O(1) time
 * - Cancel:  O(n) time via FIFO queue rotation
 */
public class OrderQueue {
    private Node<Order> front;
    private Node<Order> rear;
    private int size;

    public OrderQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    /**
     * Appends an order to the rear of the queue in O(1) time.
     */
    public void enqueue(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Cannot enqueue a null order.");
        }
        Node<Order> newNode = new Node<>(order);
        if (isEmpty()) {
            front = newNode;
            rear = newNode;
        } else {
            rear.setNext(newNode);
            rear = newNode;
        }
        size++;
    }

    /**
     * Removes and returns the order at the front of the queue in O(1) time.
     */
    public Order dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty. No pending orders to dequeue.");
        }
        Order servedOrder = front.getData();
        front = front.getNext();
        if (front == null) {
            rear = null;
        }
        size--;
        return servedOrder;
    }

    /**
     * Inspects the next order to be served without removing it from the queue in O(1) time.
     */
    public Order peek() {
        if (isEmpty()) {
            return null;
        }
        return front.getData();
    }

    /**
     * Cancels an order from anywhere in the queue using queue rotation in O(n) time.
     * Preserves strict FIFO sequence of all remaining orders.
     */
    public Order cancel(int orderNumber) {
        if (isEmpty()) {
            return null;
        }

        int currentCount = size;
        Order removedOrder = null;

        for (int i = 0; i < currentCount; i++) {
            Order current = dequeue();
            if (current.getOrderNumber() == orderNumber && removedOrder == null) {
                removedOrder = current;
                removedOrder.setStatus("CANCELLED");
            } else {
                enqueue(current);
            }
        }

        return removedOrder;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public int getSize() {
        return size;
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        Node<Order> current = front;
        while (current != null) {
            list.add(current.getData());
            current = current.getNext();
        }
        return list;
    }

    public void clear() {
        front = null;
        rear = null;
        size = 0;
    }
}
