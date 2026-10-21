/**
 * Queue Abstract Data Type (ADT) Interface.
 * Defines the contract for FIFO (First-In, First-Out) operations,
 * fulfilling OOP Abstraction and Interface Segregation principles.
 *
 * @param <T> Element type held in the queue.
 */
public interface QueueADT<T> {

    /**
     * Enqueues an item to the rear of the queue in O(1) time.
     */
    void enqueue(T item);

    /**
     * Dequeues and returns the item from the front of the queue in O(1) time.
     */
    T dequeue();

    /**
     * Peeks at the front item without removing it in O(1) time.
     */
    T peek();

    /**
     * Returns true if the queue contains no elements, false otherwise.
     */
    boolean isEmpty();

    /**
     * Returns the total count of elements currently in the queue.
     */
    int getSize();
}
