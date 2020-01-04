package cn.kivensoft.util.impl;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.kivensoft.util.ObjectPool;

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
final public class ObjectPoolImpl<T> implements ObjectPool<T> {

	// 全局无锁非阻塞堆栈头部指针
	private final AtomicReference<PoolItem> head = new AtomicReference<>();

	// 对象生产工厂
	private final Supplier<T> objFactory;
	private final Consumer<T> recycleFunc;

	public ObjectPoolImpl(Supplier<T> objFactory) {
		this(objFactory, null);
	}

	public ObjectPoolImpl(Supplier<T> objFactory, Consumer<T> recycleFunc) {
		super();
		this.objFactory = objFactory;
		this.recycleFunc = recycleFunc;
	}

	/** 获取缓存中的对象并进行处理 */
	@Override
	public final void get(Consumer<T> consumer) {
		Item<T> item = get();
		consumer.accept(item.get());
		item.recycle();
	}

	/** 获取缓存中的对象实例, 缓存没有则新建一个实例返回 */
	@Override
	public final Item<T> get() {
		Item<T> value = pop();
		if (value == null)
			value = new PoolItem(objFactory.get());
		return value;
	}

	/** 清除缓存中的所有实例 */
	@Override
	public void clear() {
		PoolItem top = head.getAndSet(null), next;
		while (top != null) {
			next = top.next;
			top.value = null;
			top.clear();
			top.next = null;
			top = next;
		}
	}

	// 无锁非阻塞弹出栈顶元素
	protected final Item<T> pop() {
		PoolItem top, next;
		// CAS方式取出栈顶元素
		do {
			top = head.get();
			if (top == null) return null;
			next = top.next;
		} while (!head.compareAndSet(top, next));

		// 判断元素是否已经被系统GC回收
		top.value = top.ref();
		if (top.value == null) return null;
		// 设置next为空, 后续才能正常入栈
		top.next = null;
		return top;
	}

	// 无锁非阻塞元素压入栈顶
	protected final void push(PoolItem value) {
		// next不为空, 可能是用户代码重复入栈
		if (value.next != null) return;
		if (recycleFunc != null) recycleFunc.accept(value.get());
		value.value = null;
		PoolItem top;
		do {
			top = head.get();
			// 判断栈顶元素是否有效, 有效则把当前元素的下个元素指向栈顶元素
			if (top != null && top.ref() != null) value.next = top;
		} while (!head.compareAndSet(top, value));
	}

	protected class PoolItem extends WeakReference<T> implements Item<T> {
		private T value;
		private PoolItem next;

		public PoolItem(T value) {
			super(value);
		}

		final private T ref() { return super.get(); }
		@Override final public T get() { return value; }
		@Override final public void recycle() { ObjectPoolImpl.this.push(this); }
	}

}
