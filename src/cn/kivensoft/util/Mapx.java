package cn.kivensoft.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

final public class Mapx implements Map<String, Object> {

	private Map<String, Object> map;
	private StampedLock lock;

	public static Mapx of(Object... args) {
		Map<String, Object> m = new HashMap<>();
		for (int i = 0, imax = args.length - 1; i < imax; i += 2)
			m.put((String) args[i], args[i + 1]);
		return new Mapx(m);
	}

	public Mapx(Map<String, Object> value) {
		this.map = value;
	}

	public Mapx(Map<String, Object> value, boolean isSync) {
		this.map = value;
		lock = new StampedLock();
	}
	
	public Mapx(boolean isSync) {
		this.map = new HashMap<>();
		lock = new StampedLock();
	}

	public Map<String, Object> getData() {
		return map;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) (lock == null ? map.get(key) : optimisticRead(() -> map.get(key)));
	}

	public String getString(String key) {
		return get(key);
	}

	public Integer getInteger(String key) {
		return get(key);
	}
	
	@Override
	public int size() {
		return lock == null ? map.size() : optimisticRead(map::size);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return lock == null ? map.containsKey(key) : optimisticRead(() -> map.containsKey(key));
	}

	@Override
	public boolean containsValue(Object value) {
		return lock == null ? map.containsValue(value) : optimisticRead(() -> map.containsValue(value));
	}

	@Override
	public Object get(Object key) {
		return lock == null ? map.get(key) : optimisticRead(() -> map.get(key));
	}

	@Override
	public Object put(String key, Object value) {
		return lock == null ? map.put(key, value) : lockWrite(() -> map.put(key, value));
	}

	@Override
	public Object remove(Object key) {
		return lock == null ? map.remove(key) : lockWrite(() -> map.remove(key));
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		if (lock == null) map.putAll(m);
		else lockWrite(() -> map.putAll(m));
	}

	@Override
	public void clear() {
		if (lock == null) map.clear();
		else lockWrite(() -> map.clear());
	}

	@Override
	public Set<String> keySet() {
		return lock == null ? map.keySet() : optimisticRead(map::keySet);
	}

	@Override
	public Collection<Object> values() {
		return lock == null ? map.values() : optimisticRead(map::values);
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return lock == null ? map.entrySet() : optimisticRead(map::entrySet);
	}

	private <R> R optimisticRead(Supplier<R> func) {
		long stamp = lock.tryOptimisticRead(); // 非阻塞获取版本信息
		R ret = func.get(); // 拷贝变量到线程本地堆栈
		if (!lock.validate(stamp)) { // 校验
			stamp = lock.readLock(); // 获取读锁
			try {
				ret = func.get(); // 拷贝变量到线程本地堆栈
			} finally {
				lock.unlock(stamp); // 释放悲观锁
			}
		}
		return ret;
	}

	private <R> R lockWrite(Supplier<R> func) {
		// 涉及对共享资源的修改，使用写锁-独占操作
		long stamp = lock.writeLock();
		try {
			return func.get();
		} finally {
			lock.unlockWrite(stamp);
		}
	}

	private void lockWrite(Runnable func) {
		// 涉及对共享资源的修改，使用写锁-独占操作
		long stamp = lock.writeLock();
		try {
			func.run();
		} finally {
			lock.unlockWrite(stamp);
		}
	}
}
