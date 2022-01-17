package mainTestBot.betterJavaUtil;

public class LinkedList<T> {
    public Node<T> head;
    public Node<T> tail;

    public LinkedList() {
        head = null;
        tail = null;
    }

    public void add(T val) {
        Node<T> valNode = new Node<T>(val);
        if (head == null) {
            head = valNode;
            tail = valNode;
        } else {
            tail.next = valNode;
            valNode.prev = tail;
            this.tail = valNode;
        }
    }

    public boolean remove(T val) {
        if (head == null) {
            return false;
        } else {
            Node<T> current = head;
            while (current != null) {
                if (current.val.equals(val)) {
                    if (current.prev != null) {
                        current.prev.next = current.next;
                    } else {
                        head = current.next;
                    }
                    if (current.next != null) {
                        current.next.prev = current.prev;
                    } else {
                        tail = current.prev;
                    }
                    return true;
                }
                current = current.next;
            }
            return false;
        }
    }

    public boolean contains(T val) {
        Node<T> current = head;
        while (current != null) {
            if (current.val.equals(val)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public void print() {
        Node<T> current = head;
        System.out.println("q: ------------------------");
        while (current != null) {
            System.out.println(current.val + ", ");
            current = current.next;
        }
    }
}
