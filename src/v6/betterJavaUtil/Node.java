package v6.betterJavaUtil;

public class Node<T> {
    public T val;
    public Node<T> prev = null;
    public Node<T> next = null;
    public Node(T val) {
        this.val = val;
    }
}
