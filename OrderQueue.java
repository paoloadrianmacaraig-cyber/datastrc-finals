import java.util.ArrayList;
import java.util.List;

/**
 * Custom Queue Data Structure implemented as a Singly-Linked List
 * using dedicated front and rear pointers for strict FIFO order fulfillment.
 *
 * Implements QueueADT<Order> to uphold OOP Abstraction.
 *
 * Algorithm Complexity:
 * - Enqueue: O(1)
 * - Dequeue: O(1)
 * - Peek:    O(1)
 * - Cancel:  O(n) via Queue Rotation (maintains relative arrival order)
 */
public class OrderQueue implements QueueADT<Order> {
    private Node<Order> front;
    private Node<Order> rear;
    private int size;

    public OrderQueue() {
        this.front = null;
        this.rear = null;
        this.size = 0;
    }

    @Override
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

    @Override
    public Order dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Order queue is empty.");
        }
        Order served = front.getData();
        front = front.getNext();
        if (front == null) {
            rear = null;
        }
        size--;
        return served;
    }

    @Override
    public Order peek() {
        return isEmpty() ? null : front.getData();
    }

    @Override
    public boolean isEmpty() {
        return front == null;
    }

    @Override
    public int getSize() {
        return size;
    }

    /**
     * Cancels an order from anywhere in the queue using FIFO Queue Rotation in O(n) time.
     *
     * Why rotation? Direct arbitrary node removal violates pure queue ADT rules.
     * By dequeuing and re-enqueuing all non-matching orders exactly 'size' times,
     * the target order is cleanly excised while all remaining orders retain
     * their exact relative arrival order.
     *
     * @param orderNumber The order number to excise.
     * @return The cancelled Order object, or null if not found.
     */
    public Order cancel(int orderNumber) {
        if (isEmpty()) {
            return null;
        }

        int currentCount = size;
        Order cancelledOrder = null;

        for (int i = 0; i < currentCount; i++) {
            Order current = dequeue();
            if (current.getOrderNumber() == orderNumber && cancelledOrder == null) {
                cancelledOrder = current;
                cancelledOrder.setStatus("CANCELLED");
                // Do NOT re-enqueue the cancelled order
            } else {
                // Re-enqueue all other orders to preserve arrival sequence
                enqueue(current);
            }
        }

        return cancelledOrder;
    }

    /**
     * Traverses the linked list from front to rear to generate a non-destructive
     * list snapshot for displaying active queue tables.
     */
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
