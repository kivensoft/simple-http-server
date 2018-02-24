package cn.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** 弱键值的字典缓存，键和值都是弱引用
 * @author kiven
 * @version 1.0.0
 */
public class WeakCache<K, V> {
	private WeakHashMap<K, WeakReference<V>> map = new WeakHashMap<K, WeakReference<V>>();
	
	public boolean containsKey(K key) {
		return map.containsKey(key);
	}
	
	public V put(K key, V value) {
		WeakReference<V> ref = map.put(key, new WeakReference<V>(value));
		return ref == null ? null : ref.get();
	}
	
	public V get(K key) {
		WeakReference<V> ref = map.get(key);
		return ref == null ? null : ref.get();
	}
	
	public void clear() {
		map.clear();
	}
	
	public int size() {
		return map.size();
	}
	
	public boolean isEmpty() {
		return map.size() == 0;
	}
	
	public static void main(String[] args) {
		WeakCache<Integer, Long> cache = new WeakCache<>();
		for (int i = 0; i < 10; i++)
			cache.put(new Integer(i + 1000), new Long(i + 2000));
		System.out.printf("key = %d, value = %d", cache.get(1005));
		System.gc();
		System.out.printf("key = %d, value = %d", cache.get(1005));
	}
}
