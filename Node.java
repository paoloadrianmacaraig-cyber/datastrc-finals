/**
 * Generic node for custom singly-linked data structures.
 * Used by OrderQueue to maintain FIFO order with front and rear pointers.
 *
 * @param <T> Type of data held in the node.
 */
public class Node<T> {
    private T data;
    private Node<T> next;

    public Node(T data) {
        this.data = data;
        this.next = null;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Node<T> getNext() {
        return next;
    }

    public void setNext(Node<T> next) {
        this.next = next;
    }
}
