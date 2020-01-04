package cn.kivensoft.util;

final public class ObjRef<T> {
	private T ref;

	public ObjRef() {
		super();
	}

	public ObjRef(T ref) {
		super();
		this.ref = ref;
	}

	public final T get() {
		return ref;
	}

	public final void set(T ref) {
		this.ref = ref;
	}
}
