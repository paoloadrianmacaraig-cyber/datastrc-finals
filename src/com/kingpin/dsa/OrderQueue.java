package com.kingpin.dsa;

import com.kingpin.model.Order;
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
     *
     * @param order The confirmed order to enqueue.
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
     *
     * @return The next order to be served.
     * @throws IllegalStateException if the queue is empty.
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
     *
     * @return The order currently at the front, or null if the queue is empty.
     */
    public Order peek() {
        if (isEmpty()) {
            return null;
        }
        return front.getData();
    }

    /**
     * Cancels an order from anywhere in the queue using queue rotation in O(n) time.
     *
     * Why rotation? Direct removal from the middle of a FIFO queue violates
     * pure queue abstract data type semantics. By repeatedly dequeuing and re-enqueuing
     * all non-matching elements exactly `size` times, the target element is extracted
     * while preserving the exact relative FIFO order of all remaining orders.
     *
     * @param orderNumber The unique identifier of the order to cancel.
     * @return The cancelled Order object if found and removed, or null if not found.
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
                // Target found: mark as removed and do NOT re-enqueue
                removedOrder = current;
                removedOrder.setStatus("CANCELLED");
            } else {
                // Not the target (or duplicate check): re-enqueue to back
                enqueue(current);
            }
        }

        return removedOrder;
    }

    /**
     * Checks if the queue has no pending orders.
     *
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty() {
        return front == null;
    }

    /**
     * Returns the total count of pending orders in the queue.
     *
     * @return integer size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns a non-destructive list representation of the pending orders
     * from front to rear for display and reporting purposes.
     *
     * @return List of pending orders.
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

    /**
     * Clears all orders from the queue.
     */
    public void clear() {
        front = null;
        rear = null;
        size = 0;
    }
}
