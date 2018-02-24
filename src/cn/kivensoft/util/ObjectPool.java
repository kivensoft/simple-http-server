package cn.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 对象池，采用弱引用方式保存使用过的对象，减少创建对象的次数，降低内存占用率
 * 使用方法: 
 *     static ObjectPool<T> x = new ObjectPool<>(() -> new T());
 *     PoolItem<A> item = x.get();
 *     A a = item.get();
 *     item.recycle();
 * @author kiven lee
 * @version 1.0
 * @date 2015-09-27
 */
final public class ObjectPool<T> {
	
	private class Item implements PoolItem<T> {
		private T value;
		private WeakReference<Item> _self, _next;
		private Item(T value) {
			this.value = value;
			_self = new WeakReference<>(this);
		}
		@Override public T get() { return value; }
		@Override public void recycle() { ObjectPool.this.push(this); }
	}

	// 全局无锁非阻塞堆栈头部指针
	private AtomicReference<WeakReference<Item>> head = new AtomicReference<>();
	// 对象生产工厂
	private Supplier<T> objFactory;
	// 对象清除工厂
	private Consumer<T> objClear;
	
	public ObjectPool(Supplier<T> objFactory) {
		super();
		this.objFactory = objFactory;
	}

	public ObjectPool(Supplier<T> objFactory, Consumer<T> objClear) {
		this(objFactory);
		this.objClear = objClear;
	}

	// 无锁非阻塞弹出栈顶元素
	private PoolItem<T> pop() {
		WeakReference<Item> old_head, new_head;
		Item item;
		do {
			old_head = head.get();
			if (old_head == null) return null;
			item = old_head.get();
			if (item == null) {
				head.compareAndSet(old_head, null);
				return null;
			}
			new_head = item._next;
		} while (!head.compareAndSet(old_head, new_head));
		item._next = null;
		return item;
	}
		
	// 无锁非阻塞元素压入栈顶
	private void push(Item value) {
		if (value._next != null) return;
		if (objClear != null) objClear.accept(value.get());
		WeakReference<Item> old_head;
		do {
			old_head = head.get();
			if (old_head != null && old_head.get() != null)
				value._next = old_head;
		} while (!head.compareAndSet(old_head, value._self));
	}
		
	/** 获取缓存中的PoolItem实例, 缓存没有则新建一个实例返回 */
	public PoolItem<T> get() {
		PoolItem<T> value = pop();
		if (value == null) {
			try {
				value = new Item(objFactory.get());
			} catch (Exception e) { }
		}
		return value;
	}
	
	/** 获取缓存中的PoolItem实例数组, 缓存没有则新建 */
	public PoolItem<T>[] get(int count) {
		@SuppressWarnings("unchecked")
		PoolItem<T>[] values = (PoolItem<T>[])(new Object[count]);
		for (int i = 0; i < count; ++i)
			values[i] = get();
		return values;
	}

	/** 清除缓存中的所有实例 */
	public void clear() {
		WeakReference<Item> old_head = head.getAndSet(null);
		while (old_head != null) {
			Item item = old_head.get();
			if (item == null) break;
			old_head = item._next;
			item.value = null;
			item._self = null;
			item._next = null;
		}
	}
	
	/** 获取当前缓存的对象数量 */
	public int size() {
		int count = 0;
		WeakReference<Item> old_head = head.get();
		while (old_head != null) {
			Item item = old_head.get();
			if (item == null) break;
			old_head = item._next;
			++count;
		}
		return count;
	}

}
