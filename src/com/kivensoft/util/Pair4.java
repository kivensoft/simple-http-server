package com.kivensoft.util;

/** 键值对泛型类
 * @author kiven
 *
 */
final public class Pair4 <T1, T2, T3, T4> {
	private T1 first;
	private T2 second;
	private T3 three;
	private T4 four;
	
	public Pair4() { super(); }
	
	public Pair4(T1 first, T2 second, T3 three, T4 four) {
		this.first = first;
		this.second = second;
		this.three = three;
		this.four = four;
	}
	
	public static <T1, T2, T3, T4> Pair4<T1, T2, T3, T4> of(
			T1 first, T2 second, T3 three, T4 four) {
		return new Pair4<T1, T2, T3, T4>(first, second, three, four);
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

	public T4 getFour() {
		return four;
	}

	public void setFour(T4 four) {
		this.four = four;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((three == null) ? 0 : three.hashCode());
		result = prime * result + ((four == null) ? 0 : four.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Pair4<?, ?, ?, ?> other = (Pair4<?, ?, ?, ?>) obj;
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
		if (four == null) {
			if (other.four != null) return false;
		}
		else if (!four.equals(other.four)) return false;
		
		return true;
	}

	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second
				+ ", three=" + three + ", four=" + four + "]";
	}

}
