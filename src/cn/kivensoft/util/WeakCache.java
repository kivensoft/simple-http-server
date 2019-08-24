package cn.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** 弱键值的字典缓存，键和值都是弱引用, 支持线程安全与不安全两种模式
 * @author kiven
 * @version 1.0.0
 */
final public class WeakCache<K, V> {
	private WeakHashMap<K, WeakReference<V>> map = new WeakHashMap<K, WeakReference<V>>();
	private boolean isSynchronized;
	
	/** 构造函数, 缺省为线程不安全 */
	public WeakCache() {
		this.isSynchronized = false;
	}
	
	/** 构造函数, 可选择线程安全或不安全方式
	 * @param isSynchronized true表示支持线程安全, false表示线程不安全
	 */
	public WeakCache(boolean isSynchronized) {
		this.isSynchronized = isSynchronized;
	}

	/** 测试是否包含指定的键
	 * @param key 指定的键
	 * @return true包含, false不包含
	 */
	public boolean containsKey(K key) {
		WeakReference<V> ref;
		if (isSynchronized)
			synchronized (this) {
				ref = map.get(key);
			}
		else ref = map.get(key);
		return ref == null ? false : ref.get() == null;
	}
	
	/** 设置键值
	 * @param key 键
	 * @param value 值
	 * @return 原来的旧值, 没有旧值返回null
	 */
	public V put(K key, V value) {
		WeakReference<V> ref;
		if (isSynchronized)
			synchronized (this) {
				ref = map.put(key, new WeakReference<V>(value));
			}
		else ref = map.put(key, new WeakReference<V>(value));
		return ref == null ? null : ref.get();
	}
	
	/** 获取指定键的值
	 * @param key 键
	 * @return 值, 找不到返回null
	 */
	public V get(K key) {
		WeakReference<V> ref;
		if (isSynchronized)
			synchronized (this) {
				ref = map.get(key);
			}
		else ref = map.get(key);
		return ref == null ? null : ref.get();
	}
	
	/** 清除所有缓存 */
	public void clear() {
		if (isSynchronized)
			synchronized (this) {
				map.clear();
			}
		else map.clear();
	}

	/** 回收被gc掉的空间 */
	public void cycle() {
		size();
	}
	
	/** 获取缓存大小 */
	public int size() {
		int len;
		if (isSynchronized)
			synchronized (this) {
				len = map.size();
			}
		else len = map.size();
		return len;
	}
	
	/** 判断缓存是否为空
	 * @return true表示缓存为空, false不为空
	 */
	public boolean isEmpty() {
		return size() == 0;
	}
	
}
