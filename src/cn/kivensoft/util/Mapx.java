package cn.kivensoft.util;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

final public class Mapx implements Map<String, Object>, Serializable, Cloneable {

	private static final long serialVersionUID = 1L;

	private Map<String, Object> map;
	private StampedLock lock;

	public static final Mapx of(Object... args) {
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

	public final Map<String, Object> getData() {
		return map;
	}

	final public boolean isSync() {
		return lock != null;
	}

	final public void setSync(boolean sync) {
		if (lock == null) {
			if (sync) lock = new StampedLock();
		} else {
			if (!sync) lockWrite(() -> lock = null);
		}
	}

	@SuppressWarnings("unchecked")
	final public <T> T get(String key) {
		return (T) (lock == null ? map.get(key) : optimisticRead(() -> map.get(key)));
	}

	@SuppressWarnings("unchecked")
	final Mapx getMap(String key) {
		Object value = get((Object) key);
		if (value == null) return null;
		else if (value instanceof Mapx) return (Mapx) value;
		else return new Mapx((Map<String, Object>) value);
	}

	final public String getString(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == String.class)
			return (String) value;
		else
			return value.toString();
	}

	final public Integer getInteger(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == Integer.class)
			return (Integer) value;
		else if (value instanceof Number)
			return ((Number)value).intValue();
		else {
			try {
				return Integer.valueOf(value.toString());
			} catch (NumberFormatException e) {
				Logx.E(e, "DictMap.getInteger exception, type = {}, value = {}",
						value.getClass().getName(), value);
				return null;
			}
		}
	}
	
	final public int getInt(String key, int def) {
		Integer ret = getInteger(key);
		return ret == null ? def : ret;
	}

	final public Boolean getBoolean(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == Boolean.class)
			return (Boolean) value;
		else if (value instanceof Number)
			return ((Number) value).intValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
		else {
			return Boolean.valueOf(value.toString());
		}
	}

	final public Float getFloat(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == Float.class)
			return (Float) value;
		else if (value instanceof Number)
			return ((Number) value).floatValue();
		else {
			try {
				return Float.parseFloat(value.toString());
			} catch (NumberFormatException e) {
				Logx.E(e, "DictMap.getFloat exception, type = {}, value = {}",
						value.getClass().getName(), value);
				return null;
			}
		}
	}

	final public Double getDouble(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == Double.class)
			return (Double) value;
		else if (value instanceof Number)
			return ((Number) value).doubleValue();
		else {
			try {
				return Double.parseDouble(value.toString());
			} catch (NumberFormatException e) {
				Logx.E(e, "DictMap.getDouble exception, type = {}, value = {}",
						value.getClass().getName(), value);
				return null;
			}
		}
	}

	final public Date getDate(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == Date.class)
			return (Date) value;
		else if (value instanceof Number)
			return new Date(((Number) value).longValue());
		else
			return Strings.parseDate(value.toString());
	}

	final public LocalDate getLocalDate(String key) {
		Object value = get((Object) key);
		if (value == null)
			return null;
		else if (value.getClass() == LocalDate.class)
			return (LocalDate) value;
		else if (value.getClass() == LocalDateTime.class)
			return ((LocalDateTime) value).toLocalDate();
		else {
			try {
				return LocalDate.parse(value.toString());
			} catch (Exception e) {
				Logx.E(e, "DictMap.getLocalDate exception, type = {}, value = {}",
						value.getClass().getName(), value);
				return null;
			}
		}
	}

	final public LocalTime getLocalTime(String key) {
		Object value = get((Object)key);
		if (value == null)
			return null;
		else if (value.getClass() == LocalTime.class)
			return (LocalTime) value;
		else if (value.getClass() == LocalDateTime.class)
			return ((LocalDateTime) value).toLocalTime();
		else {
			try {
				return LocalTime.parse(value.toString());
			} catch (Exception e) {
				Logx.E(e, "DictMap.getLocalDate exception, type = {}, value = {}",
						value.getClass().getName(), value);
				return null;
			}
		}
	}

	final public LocalDateTime getLocalDateTime(String key) {
		Object value = get((Object)key);
		if (value == null)
			return null;
		else if (value.getClass() == LocalDateTime.class)
			return (LocalDateTime) value;
		else if (value.getClass() == LocalDate.class)
			return LocalDateTime.of(((LocalDate) value), LocalTime.of(0, 0));
		else {
			try {
				return LocalDateTime.parse(value.toString());
			} catch (Exception e) {
				Logx.E(e, "DictMap.getLocalDate exception, type = {}, value = {}",
						value.getClass().getName(), value);
				return null;
			}
		}
	}

	@Override
	final public int size() {
		return lock == null ? map.size() : optimisticRead(map::size);
	}

	@Override
	final public boolean isEmpty() {
		return lock == null ? map.isEmpty() : optimisticRead(map::isEmpty);
	}

	@Override
	final public boolean containsKey(Object key) {
		return lock == null ? map.containsKey(key) : optimisticRead(() -> map.containsKey(key));
	}

	@Override
	final public boolean containsValue(Object value) {
		return lock == null ? map.containsValue(value) : optimisticRead(() -> map.containsValue(value));
	}

	@Override
	final public Object get(Object key) {
		return lock == null ? map.get(key) : optimisticRead(() -> map.get(key));
	}

	@Override
	final public Object put(String key, Object value) {
		return lock == null ? map.put(key, value) : lockWrite(() -> map.put(key, value));
	}

	@Override
	final public Object remove(Object key) {
		return lock == null ? map.remove(key) : lockWrite(() -> map.remove(key));
	}

	@Override
	final public void putAll(Map<? extends String, ? extends Object> m) {
		if (lock == null) map.putAll(m);
		else lockWrite(() -> map.putAll(m));
	}

	@Override
	final public void clear() {
		if (lock == null) map.clear();
		else lockWrite(() -> map.clear());
	}

	@Override
	final public Set<String> keySet() {
		return lock == null ? map.keySet() : optimisticRead(map::keySet);
	}

	@Override
	final public Collection<Object> values() {
		return lock == null ? map.values() : optimisticRead(map::values);
	}

	@Override
	final public Set<Entry<String, Object>> entrySet() {
		return lock == null ? map.entrySet() : optimisticRead(map::entrySet);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mapx clone() {
		Class<?> cls = map.getClass();
		if (cls == HashMap.class)
			return new Mapx((Map<String, Object>)((HashMap<String, Object>)map).clone());
		else if (cls == LinkedHashMap.class)
			return new Mapx((Map<String, Object>)((LinkedHashMap<String, Object>)map).clone());
		else if (cls == TreeMap.class)
			return new Mapx((Map<String, Object>)((TreeMap<String, Object>)map).clone());
		else
			return new Mapx(new HashMap<>(map));
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
