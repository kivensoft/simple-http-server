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
	
	// 全局无锁非阻塞堆栈头部指针
	private AtomicReference<WeakReference<Item>> head = new AtomicReference<>();

	// 对象生产工厂
	private final Supplier<T> objFactory;
	private final Consumer<T> recycleFunc;
	
	public ObjectPool(int capacity, Supplier<T> objFactory) {
		this(capacity, objFactory, null);
	}
	
	public ObjectPool(int capacity, Supplier<T> objFactory, Consumer<T> recycleFunc) {
		super();
		this.objFactory = objFactory;
		this.recycleFunc = recycleFunc;
	}

	/** 获取缓存中的对象实例, 缓存没有则新建一个实例返回 */
	public PoolItem<T> get() {
		PoolItem<T> value = pop();
		if (value == null)
			value = new Item(objFactory.get());
		return value;
	}
	
	/** 清除缓存中的所有实例 */
	public void clear() {
		WeakReference<Item> old_head = head.getAndSet(null);
		while (old_head != null) {
			Item item = old_head.get();
			if (item == null) break;
			old_head = item.next;
			item.value = null;
			item.self = null;
			item.next = null;
		}
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
			new_head = item.next;
		} while (!head.compareAndSet(old_head, new_head));
		item.next = null;
		return item;
	}
			
	// 无锁非阻塞元素压入栈顶
	private void push(Item value) {
		if (value.next != null) return;
		if (recycleFunc != null) recycleFunc.accept(value.get());
		WeakReference<Item> old_head;
		do {
			old_head = head.get();
			if (old_head != null && old_head.get() != null)
				value.next = old_head;
		} while (!head.compareAndSet(old_head, value.self));
	}
		
	private class Item implements PoolItem<T> {
		private T value;
		private WeakReference<Item> self, next;
		
		private Item(T value) {
			this.value = value;
			self = new WeakReference<>(this);
		}
		
		@Override public T get() { return value; }
		
		@Override public void recycle() { ObjectPool.this.push(this); }
	}
}
