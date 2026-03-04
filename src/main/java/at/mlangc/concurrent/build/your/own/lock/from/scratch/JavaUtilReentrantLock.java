package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.locks.ReentrantLock;

public class JavaUtilReentrantLock implements SimpleLock {
	private final ReentrantLock impl = new ReentrantLock();

	@Override
	public void lock() {
		impl.lock();
	}

	@Override
	public void unlock() {
		impl.unlock();
	}

	@Override
	public boolean isReentrant() {
		return true;
	}

	@Override
	public boolean hasCheckedUnlock() {
		return true;
	}
}
