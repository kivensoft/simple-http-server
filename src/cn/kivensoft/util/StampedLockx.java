package cn.kivensoft.util;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 乐观读写锁扩展子类, 扩展了三个常见场景的读写函数, 适用于读多写少的情况
 * @author kiven lee
 * @version 1.0
 * date: 2019-11-20
 */
final public class StampedLockx extends StampedLock {
	private static final long serialVersionUID = 1L;

	/** 乐观读(非阻塞读), 先尝试乐观读, 校验失败后, 转为悲观读(阻塞读)
	 * @param func 回调函数
	 * @return
	 */
	public <R> R optimisticForRead(Supplier<R> func) {
		long stamp = tryOptimisticRead(); // 非阻塞获取版本信息
		R ret = stamp == 0 ? null : func.get(); // 拷贝变量到线程本地堆栈
		if (stamp == 0 || !validate(stamp)) { // 校验失败
			stamp = readLock(); // 获取读锁
			try {
				ret = func.get(); // 拷贝变量到线程本地堆栈
			} finally {
				unlock(stamp); // 释放悲观锁
			}
		}
		return ret;
	}

	/** 悲观读
	 * @param func 回调函数
	 * @return
	 */
	public <R> R lockForRead(Supplier<R> func) {
		long stamp = readLock();
		try {
			return func.get();
		} finally {
			unlock(stamp);
		}
	}

	/** 悲观写
	 * @param func 回调函数
	 * @return
	 */
	public <R> R lockForWrite(Supplier<R> func) {
		// 涉及对共享资源的修改，使用写锁-独占操作
		long stamp = writeLock();
		try {
			return func.get();
		} finally {
			unlockWrite(stamp);
		}
	}

	/** 悲观写
	 * @param func
	 */
	public void lockForWrite(Runnable func) {
		// 涉及对共享资源的修改，使用写锁-独占操作
		long stamp = writeLock();
		try {
			func.run();
		} finally {
			unlockWrite(stamp);
		}
	}

	/** 条件写, 先悲观读取, 如果条件成立, 尝试升级为写锁, 升级失败则释放读锁, 然后再获取写锁,
	 *      获取成功后, 再次判断条件, 如果成立, 则进行写入
	 * @param pred
	 * @param func
	 */
	public void lockForWrite(Predicate<Void> pred, Runnable func) {
		long stamp = readLock(); // 获取读锁
		try {
			while (pred.test(null)) { // 测试条件是否成立
				long ws = tryConvertToWriteLock(stamp); // 尝试升级为写锁
				if (ws != 0L) { // 升级写锁成功, 操作并中断循环
					stamp = ws;
					func.run();
					break;
				} else { // 升级写锁失败, 释放读锁, 重新获取写锁, 并再次进行条件判断
					unlockRead(stamp);
					stamp = writeLock();
				}
			}
		} finally {
			unlock(stamp);
		}
	}

}
