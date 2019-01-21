package cn.kivensoft.util;

/** 键值对泛型类
 * @author kiven
 *
 * @param <K>
 * @param <V>
 */
final public class Pair <K, V> {
	public K first;
	public V second;
	
	public Pair() { super(); }
	
	public Pair(K first, V second) {
		this.first = first;
		this.second = second;
	}
	
	public static <K, V> Pair<K, V> of(K first, V second) {
		return new Pair<K, V>(first, second);
	}
	
	public K getFirst() {
		return first;
	}
	
	public void setFirst(K key) {
		this.first = key;
	}
	
	public V getSecond() {
		return second;
	}
	
	public void setSecond(V value) {
		this.second = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (first == null) {
			if (other.first != null) return false;
		}
		else if (!first.equals(other.first)) return false;
		if (second == null) {
			if (other.second != null) return false;
		}
		else if (!second.equals(other.second)) return false;
		return true;
	}

	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second + "]";
	}
}
