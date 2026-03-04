package at.mlangc.concurrent.build.your.own.lock.from.scratch;

public class BrokenNoopLock implements SimpleLock {
	@Override
	public void lock() {

	}

	@Override
	public void unlock() {

	}
}
