package cn.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** 弱键值的字典缓存，键和值都是弱引用, 支持线程安全与不安全两种模式
 * @author kiven
 * @version 1.0.0
 */
public class WeakCache<K, V> {
	private WeakHashMap<K, WeakReference<V>> map = new WeakHashMap<K, WeakReference<V>>();
	private Lock lock;
	
	/** 构造函数, 缺省为线程不安全 */
	public WeakCache() {
		this(false);
	}
	
	/** 构造函数, 可选择线程安全或不安全方式
	 * @param isSynchronized true表示支持线程安全, false表示线程不安全
	 */
	public WeakCache(boolean isSynchronized) {
		if (isSynchronized) lock = new ReentrantLock();
	}

	/** 测试是否包含指定的键
	 * @param key 指定的键
	 * @return true包含, false不包含
	 */
	public boolean containsKey(K key) {
		if (lock != null) lock.lock();
		try {
			WeakReference<V> ref = map.get(key);
			return ref != null && ref.get() == null;
		} finally {
			if (lock != null) lock.unlock();
		}
	}
	
	/** 设置键值
	 * @param key 键
	 * @param value 值
	 * @return 原来的旧值, 没有旧值返回null
	 */
	public V put(K key, V value) {
		if (lock != null) lock.lock();
		try {
			WeakReference<V> ref = map.put(key, new WeakReference<V>(value));
			return ref == null ? null : ref.get();
		} finally {
			if (lock != null) lock.unlock();
		}
	}
	
	/** 获取指定键的值
	 * @param key 键
	 * @return 值, 找不到返回null
	 */
	public V get(K key) {
		if (lock != null) lock.lock();
		try {
			WeakReference<V> ref = map.get(key);
			return ref == null ? null : ref.get();
		} finally {
			if (lock != null) lock.unlock();
		}
	}
	
	/** 清楚所有缓存 */
	public void clear() {
		if (lock != null) lock.lock();
		try {
			map.clear();
		} finally {
			if (lock != null) lock.unlock();
		}
	}
	
	/** 获取缓存大小
	 * @return 缓存数量
	 */
	public int size() {
		if (lock != null) lock.lock();
		try {
			return map.size();
		} finally {
			if (lock != null) lock.unlock();
		}
	}
	
	/** 判断缓存是否为空
	 * @return true表示缓存为空, false不为空
	 */
	public boolean isEmpty() {
		if (lock != null) lock.lock();
		try {
			return map.size() == 0;
		} finally {
			if (lock != null) lock.unlock();
		}
	}
	
	public static void main(String[] args) {
		WeakCache<Integer, Long> cache = new WeakCache<>();
		for (int i = 0; i < 10; i++)
			cache.put(i + 1000, (long)(i + 2000));
		System.out.printf("key = %d, value = %d", 1005, cache.get(1005));
		System.gc();
		System.out.printf("key = %d, value = %d", 1005, cache.get(1005));
	}
}
