package cn.kivensoft.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.kivensoft.util.impl.ObjectPoolImpl;

public interface ObjectPool<T> {

	static <T> ObjectPool<T> create(Supplier<T> objFactory) {
		return new ObjectPoolImpl<>(objFactory);
	}

	static <T> ObjectPool<T> create(Supplier<T> objFactory, Consumer<T> recycleFunc) {
		return new ObjectPoolImpl<>(objFactory, recycleFunc);
	}

	interface Item<T> {
		T get();
		void recycle();
	}

	void get(Consumer<T> consumer);
	Item<T> get();
	void clear();
}
