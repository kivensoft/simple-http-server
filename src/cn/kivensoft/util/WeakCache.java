package cn.kivensoft.util;

import cn.kivensoft.util.impl.WeakCacheImpl;

public interface WeakCache<K, V> {

	static <K, V> WeakCache<K, V> create() {
		return create(false);
	}
	
	static <K, V> WeakCache<K, V> create(boolean isSynchronized) {
		return new WeakCacheImpl<>(isSynchronized);
	}

	boolean containsKey(K key);
	boolean isSync();
	V put(K key, V value);
	V get(K key);
	void clear();
	int size();
	boolean isEmpty();
	void cycle();
}
