package cn.kivensoft.util;

import java.util.HashMap;


/** 简单的实现LRU缓存的字典类，具备超时和同步功能，创建时可选
 * @author Kiven Lee
 * @version 3.0.0
 * 3.0.0 修改内部实现代码，优化速度
 * 2.0.0 增加过期时间判断，优化并发访问性能
 * 1.0.0 简单实现LRU缓存
 */
final public class LruCache<K, V> {
	
	private final HashMap<K, CacheObject<K, V>> data;
	private final int cacheSize;
	private final long expire;
	private final Object lock;
	private transient CacheObject<K, V> head, tail;

	/** 缺省构建函数, 默认不限制大小, 没有过期时间, 非线程安全 */
	public LruCache() {
		this(0, 0, false, 0);
	}

	public LruCache(int cacheSize) {
		this(cacheSize, 0, false);
	}

	public LruCache(int cacheSize, long expire) {
		this(cacheSize, expire, false);
	}

	public LruCache(int cacheSize, long expire, boolean isSynchronlzed) {
		this(cacheSize, expire, isSynchronlzed, cacheSize > 256 ? 256 : cacheSize);
	}

	public LruCache(int cacheSize, long expire, boolean isSynchronlzed, int initSize) {
		this.cacheSize = cacheSize;
		this.expire = expire;
		this.lock = isSynchronlzed ? new Object() : null;
		this.data = initSize == 0 ? new HashMap<>() : new HashMap<>(initSize);
	}

	private final static boolean isExpired(long expire) {
		return expire > 0 && expire < System.currentTimeMillis();
	}

	private final static long makeExpire(long expire) {
		return expire == 0 ? 0 : System.currentTimeMillis() + expire;
	}
	
	private final void removeItem(CacheObject<K, V> cacheObject) {
		if (cacheObject.prev != null) cacheObject.prev.next = cacheObject.next;
		else head = cacheObject.next;
		if (cacheObject.next != null) cacheObject.next.prev = cacheObject.prev;
		else tail = cacheObject.prev;
	}
	
	private final void addLast(CacheObject<K, V> cacheObject) {
		cacheObject.next = null;
		if (head == null) head = cacheObject;
		if (tail != null) tail.next = cacheObject;
		cacheObject.prev = tail;
		tail = cacheObject;
	}

	public final V get(K key) {
		CacheObject<K, V> ret;
		if (lock == null) ret = _get(key);
		else synchronized (lock) { ret = _get(key); }
		return ret == null ? null : ret.value;
	}
	
	public final V put(K key, V value) {
		CacheObject<K, V> ret;
		if (lock == null) ret = _put(key, value);
		else synchronized (lock) { ret = _put(key, value); }
		return ret == null || isExpired(ret.expire) ? null : ret.value;
	}

	public final V remove(K key) {
		CacheObject<K, V> ret;
		if (lock == null) ret = _remove(key);
		else synchronized (lock) { ret = _remove(key); }
		return ret == null || isExpired(ret.expire) ? null : ret.value;
	}

	public final void clear() {
		if (lock == null) _clear();
		else synchronized (lock) { _clear(); }
	}
	
	public final void cycle() {
		if (expire == 0) return;
		if (lock == null) _cycle();
		else synchronized (lock) { _cycle(); }
	}

	public final int size() {
		return data.size();
	}
	
	public final boolean isEmpty() {
		return data.size() == 0;
	}

	public final int getCacheSize() {
		return cacheSize;
	}
	
	private final CacheObject<K, V> _get(K key) {
		CacheObject<K, V> co = data.get(key);
		if (co != null) {
			if (cacheSize > 0) removeItem(co);
			if (expire > 0 && isExpired(co.expire)) {
				data.remove(key);
				co = null;
			} else {
				if (expire > 0) co.expire = makeExpire(expire);
				if (cacheSize > 0) addLast(co);
			}
		}
		return co;
	}
	
	private final CacheObject<K, V> _put(K key, V value) {
		CacheObject<K, V> co = new CacheObject<>(key, value, makeExpire(expire));
		CacheObject<K, V> old = data.put(key, co);
		if (cacheSize > 0) {
			if (old != null) removeItem(old);
			else if (data.size() > cacheSize) {
				data.remove(head.key);
				removeItem(head);
			}
			addLast(co);
		}
		return old;
	}

	private final CacheObject<K, V> _remove(K key) {
		CacheObject<K, V> co = data.remove(key);
		if (cacheSize > 0 && co != null) removeItem(co);
		return co;
	}

	private final void _clear() {
		data.clear();
		head = null;
		tail = null;
	}
	
	private final void _cycle() {
		CacheObject<K, V> co = head;
		while (co != null) {
			if (isExpired(co.expire)) {
				data.remove(co.key);
				removeItem(co);
			}
			co = co.next;
		}
	}

	static class CacheObject<K, V> {
		final K key;
		V value;
		long expire;
		CacheObject<K, V> prev, next;
		
		CacheObject(K key, V value, long expire) {
			this.key = key;
			this.value = value;
			this.expire = expire;
		}
	}
	
}
