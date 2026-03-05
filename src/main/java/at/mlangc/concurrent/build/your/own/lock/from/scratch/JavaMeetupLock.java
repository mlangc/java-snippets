package at.mlangc.concurrent.build.your.own.lock.from.scratch;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class JavaMeetupLock implements SimpleLock {
	private final AtomicReference<Thread> owner = new AtomicReference<>();
	private final ConcurrentLinkedQueue<Thread> waiters = new ConcurrentLinkedQueue<>();
	private final boolean fair;

	// Maybe explain volatile better, or on a dedicated slide
	// Maybe add volatile first
	private int entries;

	public JavaMeetupLock(boolean fair) {
		this.fair = fair;
	}

	@Override
	public void lock() {
		if (owner.get() == Thread.currentThread()) {
			entries++;
			return;
		}

		if (!fair && owner.compareAndSet(null, Thread.currentThread())) {
			entries++;
			return;
		}

		waiters.add(Thread.currentThread());
		while (true) {
			if (waiters.peek() == Thread.currentThread() && owner.compareAndSet(null, Thread.currentThread())) {
				entries++;
				waiters.remove();
				return;
			} else {
				LockSupport.park();
			}
		}
	}

	@Override
	public void unlock() {
		if (owner.get() != Thread.currentThread()) {
			throw new IllegalMonitorStateException();
		}

		if (--entries == 0) {
			owner.set(null);

			LockSupport.unpark(waiters.peek());
		}
	}

	@Override
	public boolean hasCheckedUnlock() {
		// Explain more
		return true;
	}

	@Override
	public boolean isReentrant() {
		return true;
	}
}
