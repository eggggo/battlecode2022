package v6.betterJavaUtil;

public class Entry<K, V> {
    public K key;
    public V val;

    public Entry(K key, V val) {
        this.key = key;
        this.val = val;
    }

    @Override
    public boolean equals(Object o) {
        return this.key.equals(((Entry<K, V>)o).key);
    }
}
