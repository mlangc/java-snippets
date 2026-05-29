# CLAUDE.md

This file provides guidance to AI coding assistants working with code in this repository.

## Commands

```bash
# Build and run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "at.mlangc.concurrent.build.your.own.lock.from.scratch.SimpleLockTest"

# Run a single test method
./gradlew test --tests "at.mlangc.concurrent.build.your.own.lock.from.scratch.SimpleLockTest.shouldProperlyProtectSharedCounter"

# Run JMH benchmarks (configured in build.gradle jmh block)
./gradlew jmh

# Run JCStress concurrency stress tests
./gradlew jcstress

# Update dependency versions
./gradlew refreshVersions

# Update Gradle wrapper
./gradlew wrapper --gradle-version latest
```

## Build System

- **Java 25** with `--enable-preview` enabled for all compilation, test, and exec tasks
- **refreshVersions** (`de.fayard.refreshVersions`) manages all dependency versions in `versions.properties` using `_` as
  placeholder in `build.gradle` — never hardcode version strings there
- Three source sets: `main`, `test`, `jmh` (benchmarks), and `jcstress` (concurrency stress tests)

## Architecture

This is a single-module Java research/blog project organized by topic under `src/main/java/at/mlangc/`:

**`concurrent/build/your/own/lock/from/scratch/`** — The main focus area. Implements `SimpleLock` (interface with `lock()`/
`unlock()`/`runWithLock()`) in multiple variants:

- Spin locks: `CompareAndSetLock`, `GetAndSetLock`, variants with backoff and reentrance
- Queue-based: `ClhQueueLock`, `McsLock`, `FancyClhQueueLock`, `ReentrantLikeQueueLock`
- `SimpleLocks` utility wraps any lock to add reentrancy and checked-unlock behavior
- Tested in `SimpleLockTest` (correctness under concurrent load), benchmarked in `AtomicSequenceBenchmark` (JMH), and
  stress-tested in `SimpleLockJcstressTest` (JCStress)

**`concurrent/seqcst/vs/ackrel/`** — Explores `VarHandle` memory ordering semantics. Implements classic mutual exclusion
algorithms (Peterson, Bakery, Dekker) with tweakable memory ordering to demonstrate the difference between `volatile` (sequential
consistency) and `getAcquire`/`setRelease`.

**`concurrent/get/opaque/`** — Explores `VarHandle.getOpaque`/`setOpaque` semantics with small standalone demos.

**`art/of/multiprocessor/programming/`** — Implementations from the "Art of Multiprocessor Programming" textbook: concurrent
queues (ch10), elimination-backoff stacks with exchanger (ch11), sorted linked lists (ch9).

**`benchmarks/`** — Standalone JMH benchmarks for GCD algorithms, modular exponentiation, slotted counters, array iteration,
record hash codes, and `Thread.sleep(0)` behavior.

**`vthreads/`** — Virtual thread demos covering deadlock scenarios and `ThreadLocal` behavior.

## Role of AI coding assistants in this repo

The primary goal of this repository is to foster understanding. This implies that the code in this repository is meant to be
handwritten, unless explicitly stated otherwise.

Assist with explanation, review, and tooling, but do not generate implementations unless explicitly asked.