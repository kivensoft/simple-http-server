package cn.kivensoft.util;

public interface PoolItem<T> {
	T get();
	void recycle();
}
