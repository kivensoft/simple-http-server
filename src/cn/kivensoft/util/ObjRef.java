package cn.kivensoft.util;

public class ObjRef<T> {
	private T ref;

	public ObjRef() {
		super();
	}

	public ObjRef(T ref) {
		super();
		this.ref = ref;
	}

	public T get() {
		return ref;
	}

	public void set(T ref) {
		this.ref = ref;
	}
}
