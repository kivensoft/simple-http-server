package cn.kivensoft.util;

/** 键值对泛型类
 * @author kiven
 *
 */
final public class Pair3 <T1, T2, T3> {
	public T1 first;
	public T2 second;
	public T3 three;
	
	public Pair3() { super(); }
	
	public Pair3(T1 first, T2 second, T3 three) {
		this.first = first;
		this.second = second;
		this.three = three;
	}
	
	public static <T1, T2, T3> Pair3<T1, T2, T3> of(T1 first, T2 second, T3 three) {
		return new Pair3<T1, T2, T3>(first, second, three);
	}
	
	public T1 getFirst() {
		return first;
	}
	
	public void setFirst(T1 key) {
		this.first = key;
	}
	
	public T2 getSecond() {
		return second;
	}
	
	public void setSecond(T2 value) {
		this.second = value;
	}

	public T3 getThree() {
		return three;
	}

	public void setThree(T3 three) {
		this.three = three;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((three == null) ? 0 : three.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pair3<?, ?, ?> other = (Pair3<?, ?, ?>) obj;
		if (first == null) {
			if (other.first != null) return false;
		}
		else if (!first.equals(other.first)) return false;
		if (second == null) {
			if (other.second != null) return false;
		}
		else if (!second.equals(other.second)) return false;
		if (three == null) {
			if (other.three != null) return false;
		}
		else if (!three.equals(other.three)) return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second + ", three=" + three + "]";
	}

}
