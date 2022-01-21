package v9.betterJavaUtil;

public class HashMap<K, V> {
    public LinkedList<Entry<K, V>>[] table;
    public int size;

    public HashMap(int capacity) {
        table = new LinkedList[capacity];
        size = capacity;
        for (int i = capacity-1; i >= 0; i --) {
            table[i] = new LinkedList<Entry<K,V>>();
        }
    }

    public void put(K key, V val) {
        int index = Math.abs(key.hashCode()) % size;
        LinkedList<Entry<K, V>> bucket = table[index];

        if (bucket.contains(new Entry<>(key, val))) {
            Node<Entry<K, V>> current = bucket.head;
            while (current != null) {
                if (current.val.equals(new Entry<>(key, val))) {
                    current.val.val = val;
                    break;
                }
                current = current.next;
            }
        } else {
            bucket.add(new Entry<>(key, val));
        }
    }

    public V get(K key) {
        int index = Math.abs(key.hashCode()) % size;
        LinkedList<Entry<K, V>> bucket = table[index];
        if (!bucket.contains(new Entry<>(key, null))) {
            return null;
        } else {
            Node<Entry<K, V>> current = bucket.head;
            while (current != null) {
                if (current.val.equals(new Entry<>(key, null))) {
                    return current.val.val;
                }
                current = current.next;
            }
            return null;
        }
    }

    public boolean contains(K key) {
        int index = Math.abs(key.hashCode()) % size;
        LinkedList<Entry<K, V>> bucket = table[index];
        return bucket.contains(new Entry<>(key, null));
    }

    public boolean remove(K key) {
        int index = Math.abs(key.hashCode()) % size;
        LinkedList<Entry<K, V>> bucket = table[index];
        if (!bucket.contains(new Entry<>(key, null))) {
            return false;
        } else {
            return bucket.remove(new Entry<>(key, null));
        }
    }
}
