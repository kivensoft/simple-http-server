package cn.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** 弱键值的字典缓存，键和值都是弱引用, 支持线程安全与不安全两种模式
 * @author kiven
 * @version 1.0.0
 */
final public class WeakCache<K, V> {
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
		WeakReference<V> ref;
		if (lock != null) {
			lock.lock();
			try {
				ref = map.get(key);
			} finally {
				lock.unlock();
			}
		} else
			ref = map.get(key);
		return ref == null ? false : ref.get() == null;
	}
	
	/** 设置键值
	 * @param key 键
	 * @param value 值
	 * @return 原来的旧值, 没有旧值返回null
	 */
	public V put(K key, V value) {
		WeakReference<V> ref;
		if (lock != null) {
			lock.lock();
			try {
				ref = map.put(key, new WeakReference<V>(value));
			} finally {
				lock.unlock();
			}
		} else
			ref = map.put(key, new WeakReference<V>(value));
		return ref == null ? null : ref.get();
	}
	
	/** 获取指定键的值
	 * @param key 键
	 * @return 值, 找不到返回null
	 */
	public V get(K key) {
		WeakReference<V> ref;
		if (lock != null) {
			lock.lock();
			try {
				ref = map.get(key);
			} finally {
				lock.unlock();
			}
		} else
			ref = map.get(key);
		return ref == null ? null : ref.get();
	}
	
	/** 清楚所有缓存 */
	public void clear() {
		if (lock != null) {
			lock.lock();
			try {
				map.clear();
			} finally {
				lock.unlock();
			}
		} else
			map.clear();
	}
	
	/** 获取缓存大小
	 * @return 缓存数量
	 */
	public int size() {
		int len;
		if (lock != null) {
			lock.lock();
			try {
				len = map.size();
			} finally {
				lock.unlock();
			}
		}
		else len = map.size();
		return len;
	}
	
	/** 判断缓存是否为空
	 * @return true表示缓存为空, false不为空
	 */
	public boolean isEmpty() {
		boolean b;
		if (lock != null) {
			lock.lock();
			try {
				b = map.size() == 0;
			} finally {
				lock.unlock();
			}
		}
		else b = map.size() == 0;
		return b;
	}
	
}
