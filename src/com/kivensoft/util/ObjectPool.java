package com.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** 对象池，采用弱引用方式保存使用过的对象，减少创建对象的次数，降低内存占用率
 * 使用方法: 
 *     class A extends ObjectPool.Item {}
 *     static ObjectPool<A> x = new ObjectPool<>(A.class);
 *     A value = x.get();
 *     value.recycle();
 * @author kiven lee
 * @version 1.0
 * @date 2015-09-27
 */
public class ObjectPool<T extends ObjectPool.Item> {
	
	abstract public static class Item {
		ObjectPool<Item> _pool;
		WeakReference<Item> _self, _next;
		protected void clear() {};
		public void recycle() {
			clear();
			_pool.push(this);
		}
	}
	
	// 全局无锁非阻塞堆栈头部指针
	AtomicReference<WeakReference<Item>> head = new AtomicReference<>();
	Supplier<T> supplier;
	
	public ObjectPool(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	// 无锁非阻塞弹出栈顶元素
	Item pop() {
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
	void push(Item value) {
		if (value._next != null) return;
		WeakReference<Item> old_head;
		do {
			old_head = head.get();
			if (old_head != null && old_head.get() != null)
				value._next = old_head;
		} while (!head.compareAndSet(old_head, value._self));
	}
		
	/** 获取缓存中的ObjectPool实例 */
	@SuppressWarnings("unchecked")
	public T get() {
		T value = (T)pop();
		if (value == null)
			try {
				value = supplier.get();
				value._pool = (ObjectPool<Item>) this;
				value._self = new WeakReference<>(value);
			} catch (Exception e) { }
		return value;
	}
	
	public T[] get(int count) {
		@SuppressWarnings("unchecked")
		T[] values = (T[])(new Object[count]);
		for (int i = 0; i < count; ++i)
			values[i] = get();
		return values;
	}

	public void recycle(Item... values) {
		for(int i = 0, n = values.length; i < n; ++i)
			values[i].recycle();
	}
}
