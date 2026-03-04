package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.atomic.AtomicReference;

public class CompareAndSetLock implements SimpleLock {
	private final AtomicReference<Thread> owner = new AtomicReference<>();
	private int entries;

	@Override
	public void lock() {
		while (owner.get() != Thread.currentThread() && !owner.compareAndSet(null, Thread.currentThread())) {
			Thread.onSpinWait();
		}

		entries++;
	}

	@Override
	public void unlock() {
		if (owner.get() != Thread.currentThread()) {
			throw new IllegalMonitorStateException();
		}

		if (--entries == 0) {
			owner.set(null);
		}
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
