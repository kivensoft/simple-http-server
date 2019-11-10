package cn.kivensoft.util;

/** 键值对泛型类
 * @author kiven
 *
 */
final public class Tuple2 <T1, T2> {
	public T1 first;
	public T2 second;
	
	public Tuple2() { super(); }
	
	public Tuple2(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}
	
	final public static <T1, T2, T3> Tuple2<T1, T2> of(T1 first, T2 second) {
		return new Tuple2<T1, T2>(first, second);
	}

	final public T1 getFirst() {
		return first;
	}
	
	final public void setFirst(T1 key) {
		this.first = key;
	}
	
	final public T2 getSecond() {
		return second;
	}
	
	final public void setSecond(T2 value) {
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
		Tuple2<?, ?> other = (Tuple2<?, ?>) obj;
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
		return "Tuple2 [first = " + first + ", second = " + second + "]";
	}

}
