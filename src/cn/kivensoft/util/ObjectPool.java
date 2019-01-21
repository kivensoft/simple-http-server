package cn.kivensoft.util;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
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
	
	// 无阻塞弱引用队列, 存放缓存对象
	private final Queue<WeakReference<T>> queue;
	// 对象生产工厂
	private final Supplier<T> objFactory;
	private final Consumer<T> recycleFunc;
	private AtomicInteger count;
	
	public ObjectPool(int capacity, Supplier<T> objFactory) {
		this(capacity, objFactory, null);
	}
	
	public ObjectPool(int capacity, Supplier<T> objFactory, Consumer<T> recycleFunc) {
		super();
		queue = new LinkedBlockingQueue<>(capacity);
		this.objFactory = objFactory;
		this.recycleFunc = recycleFunc;
		count = new AtomicInteger(0);
	}

	/** 获取缓存中的对象实例, 缓存没有则新建一个实例返回 */
	public T get() {
		WeakReference<T> ref;
		while ((ref = queue.poll()) != null) {
			T value = ref.get();
			if (value != null) {
				count.incrementAndGet();
				return value;
			}
		}
		return objFactory.get();
	}
	
	public void recycle(T value) {
		if (recycleFunc != null) recycleFunc.accept(value);
		if (queue.offer(new WeakReference<T>(value)))
			count.decrementAndGet();
	}
	
	/** 清除缓存中的所有实例 */
	public void clear() {
		while (queue.poll() != null)
			count.decrementAndGet();
	}
	
}
