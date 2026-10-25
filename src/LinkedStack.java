// Author: Tengonciang
import java.util.ArrayList;

// Stack made from a singly linked list. The first node is the top.
// Used for the roster undo and for the design history.
public class LinkedStack<T> {
    private class Node {
        T data;
        Node next;

        Node(T data, Node next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node top;
    private int size;

    public void push(T item) {
        top = new Node(item, top);    // new node points to the old top
        size++;
    }

    public T pop() {
        if (top == null) {
            return null;              // nothing to undo or revert
        }
        T item = top.data;
        top = top.next;
        size--;
        return item;
    }

    public T peek() {
        return (top == null) ? null : top.data;
    }

    public boolean isEmpty() { return top == null; }
    public int size() { return size; }

    public void clear() {
        top = null;
        size = 0;
    }

    // items from top to bottom (used when showing or saving the stack)
    public ArrayList<T> toList() {
        ArrayList<T> list = new ArrayList<>();
        for (Node n = top; n != null; n = n.next) {
            list.add(n.data);
        }
        return list;
    }
}
