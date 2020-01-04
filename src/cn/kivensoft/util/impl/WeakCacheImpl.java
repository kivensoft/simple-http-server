package cn.kivensoft.util.impl;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import cn.kivensoft.util.WeakCache;

/** 弱键值的字典缓存，键和值都是弱引用, 支持线程安全与不安全两种模式
 * @author kiven
 * @version 1.0.0
 */
final public class WeakCacheImpl<K, V> implements WeakCache<K, V> {
	private WeakHashMap<K, WeakReference<V>> map = new WeakHashMap<>();
	private Object lock;

	/** 构造函数, 缺省为线程不安全 */
	public WeakCacheImpl() {
		super();
	}

	/** 构造函数, 可选择线程安全或不安全方式
	 * @param isSynchronized true表示支持线程安全, false表示线程不安全
	 */
	public WeakCacheImpl(boolean isSynchronized) {
		super();
		if (isSynchronized) lock = new Object();
	}

	/** 测试是否包含指定的键
	 * @param key 指定的键
	 * @return true包含, false不包含
	 */
	@Override
	public final boolean containsKey(K key) {
		return get(key) != null;
	}

	/** 测试缓存是否线程安全的 */
	@Override
	public final boolean isSync() {
		return lock != null;
	}

	/** 设置键值
	 * @param key 键
	 * @param value 值
	 * @return 原来的旧值, 没有旧值返回null
	 */
	@Override
	public final V put(K key, V value) {
		WeakReference<V> ref = new WeakReference<>(value);
		if (lock == null) ref = map.put(key, ref);
		else synchronized (lock) { ref = map.put(key, ref); }
		return ref == null ? null : ref.get();
	}

	/** 获取指定键的值
	 * @param key 键
	 * @return 值, 找不到返回null
	 */
	@Override
	public final V get(K key) {
		WeakReference<V> ref;
		if (lock == null) ref = map.get(key);
		else synchronized (lock) { ref = map.get(key); }
		return ref == null ? null : ref.get();
	}

	/** 清除所有缓存 */
	@Override
	public final void clear() {
		if (lock == null) map.clear();
		else synchronized (lock) { map.clear(); }
	}

	/** 获取缓存大小 */
	@Override
	public final int size() {
		if (lock == null) return map.size();
		else synchronized (lock) { return map.size(); }
	}

	/** 判断缓存是否为空
	 * @return true表示缓存为空, false不为空
	 */
	@Override
	public final boolean isEmpty() { return size() == 0; }

	/** 回收被gc掉的空间 */
	@Override
	public final void cycle() { size(); }

}
