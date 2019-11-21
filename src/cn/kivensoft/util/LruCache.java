package cn.kivensoft.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/** 简单的实现LRU缓存的字典类，具备超时和同步功能，创建时可选
 * @author Kiven Lee
 * @version 3.0.0
 * 3.0.0 修改内部实现代码，优化速度
 * 2.0.0 增加过期时间判断，优化并发访问性能
 * 1.0.0 简单实现LRU缓存
 */
public class LruCache<K, V> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final HashMap<K, CacheObject> data;
	private final int _cacheSize;
	private final long timeout;
	private final Lock cacheLock;
	private transient CacheObject head, tail;

	public LruCache(int cacheSize) {
		this(cacheSize, 0, true);
	}
	
	public LruCache(int cacheSize, long timeout) {
		this(cacheSize, timeout, true);
	}

	public LruCache(int cacheSize, long timeout, boolean isSynchronlzed) {
		this(cacheSize, timeout, isSynchronlzed, cacheSize > 512 ? 512 : cacheSize);
	}
	
	public LruCache(int cacheSize, long timeout, boolean isSynchronlzed, int initSize) {
		this._cacheSize = cacheSize;
		this.timeout = timeout;
		this.cacheLock = (isSynchronlzed) ? new ReentrantLock() : null;
		this.data = new HashMap<K, CacheObject>(initSize);
	}

	protected final boolean removeEldestEntry(int currentSize) {
		return _cacheSize == 0 ? false : currentSize > _cacheSize;
	}
	
	private final boolean isExpired(long lastAccess) {
		return timeout > 0 && lastAccess + timeout < System.currentTimeMillis();
	}
	
	private final void removeItem(CacheObject cacheObject) {
		if (cacheObject.prev != null)
			cacheObject.prev.next = cacheObject.next;
		else head = cacheObject.next;
		if (cacheObject.next != null)
			cacheObject.next.prev = cacheObject.prev;
		else tail = cacheObject.prev;
	}
	
	private final void addLast(CacheObject cacheObject) {
		cacheObject.next = null;
		if (head == null) head = cacheObject;
		if (tail != null) {
			tail.next = cacheObject;
			cacheObject.prev = tail;
		}
		else cacheObject.prev = null;
		tail = cacheObject;
	}

	public final V _get(K key) {
		CacheObject co = data.get(key);
		if (co != null) {
			removeItem(co);
			if (isExpired(co.lastAccess)) {
				data.remove(key);
				co = null;
			} else {
				co.lastAccess = System.currentTimeMillis();
				addLast(co);
			}
		}
		return co == null ? null : co.value;
	}

	public final V get(K key) {
		if (cacheLock != null) cacheLock.lock();
		CacheObject co = data.get(key);
		if (co != null) {
			removeItem(co);
			if (isExpired(co.lastAccess)) {
				data.remove(key);
				co = null;
			} else {
				co.lastAccess = System.currentTimeMillis();
				addLast(co);
			}
		}
		if (cacheLock != null) cacheLock.unlock();
		return co == null ? null : co.value;
	}
	
	public final V put(K key, V value) {
		CacheObject co = new CacheObject(key, value);
		if (cacheLock != null) cacheLock.lock();
		addLast(co);
		co = data.put(key, co);
		if (co != null) removeItem(co);
		if (cacheLock != null) cacheLock.unlock();
		return co == null || isExpired(co.lastAccess) ? null : co.value;
	}

	public final V remove(K key) {
		if (cacheLock != null) cacheLock.lock();
		CacheObject co = data.remove(key);
		if (co != null) removeItem(co);
		if (cacheLock != null) cacheLock.unlock();
		return co == null || isExpired(co.lastAccess) ? null : co.value;
	}

	public final void clear() {
		if (cacheLock != null) cacheLock.lock();
		data.clear();
		head = null;
		tail = null;
		if (cacheLock != null) cacheLock.unlock();
	}
	
	public final void cycle() {
		if (cacheLock != null) cacheLock.lock();
		CacheObject co = head;
		while (co != null) {
			if (isExpired(co.lastAccess)) {
				data.remove(co.key);
				removeItem(co);
			}
			co = co.next;
		}
		if (cacheLock != null) cacheLock.unlock();
	}

	public final int size() {
		return data.size();
	}
	
	public final boolean isEmpty() {
		return data.size() == 0;
	}

	public final int cacheSize() {
		return _cacheSize;
	}

	
	class CacheObject {
		final K key;
		V value;
		long lastAccess = System.currentTimeMillis();
		CacheObject prev, next;
		
		CacheObject(K key, V value) {
			this.key = key;
			this.value = value;
		}
	}
	
}
