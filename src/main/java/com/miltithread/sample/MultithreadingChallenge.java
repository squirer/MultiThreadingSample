package com.miltithread.sample;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

class MultithreadingChallenge {
    static final class SharedInteger {
        private int i;

        SharedInteger() {
            i = 0;
        }

        /**
         * Added Synchronized keyword here so it becomes thread safe to modify the count of i
         */
        synchronized void increment() {
            i++;
        }

        int get() {
            return i;
        }
    }

    static final class LockOrdering {
        private final ReentrantLock a;
        private final ReentrantLock b;

        LockOrdering() {
            a = new ReentrantLock();
            b = new ReentrantLock();
        }

        /**
         * The original implementation of this method resulted in one thread first locking A, the second thread
         * locking B which meant that each thread could not lock the other lock (deadlock) - both were waiting on
         * the other thread to release the lock before continuing.
         *
         * Original impl:
         *
         * Thread 1:
         *      a.lock
         *      sleep
         *      b.lock
         *
         * Thread 2:
         *      b.lock
         *      sleep
         *      a.lock
         *
         * FIX: the fix is my implementation below - the same order of execution must be maintained between threads.
         *  This means that if the first thread locks A sleeps and locks B the second thread will now try to lock in
         *  the same order meaning it will try to lock A, realise that the other thread has the lock to A and wait.
         *
         *  The result is the first thread has successfully locked both A and B - and can then unlock both locks,
         *  allowing the other thread to continue and obtain its lock for A and then B sequentially.
         *
         *
         * @throws InterruptedException
         */
        void opA() throws InterruptedException {
            try {
                a.lock();
                Thread.sleep(5_000);
                b.lock();
                assert a.isHeldByCurrentThread() && b.isHeldByCurrentThread();
            } finally {
                b.unlock();
                a.unlock();
            }
        }

        void opB() throws InterruptedException {
            try {
                a.lock();
                Thread.sleep(5_000);
                b.lock();
                assert a.isHeldByCurrentThread() && b.isHeldByCurrentThread();
            } finally {
                b.unlock();
                a.unlock();
            }
        }
    }

    /**
     * The problem with this function is that - as it is using a shared integer that was not synchronised
     * any thread that was able to execute the increment step did and so what happens is concurrently the count per
     * thread
     * @throws InterruptedException
     */
    static void problem1() throws InterruptedException {
        final int threads = 10;
        final int incrementsPerThread = 1_000_000;
        final int expected = threads * incrementsPerThread;
        final CountDownLatch cdl = new CountDownLatch(threads);
        final SharedInteger x = new SharedInteger();
        for (int i = 0; i < threads; i++) {
            (new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int i = 0; i < incrementsPerThread; i++) {
                                    x.increment();
                                }
                            } finally {
                                cdl.countDown();
                            }
                        }
                    }))
                    .start();
        }
        cdl.await();
        System.out.println(x.get());
        assert expected == x.get();
    }

    static void problem2() throws InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(2);
        final LockOrdering lo = new LockOrdering();
        (new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            lo.opA();
                        } catch (final InterruptedException e) {
                        } finally {
                            cdl.countDown();
                        }
                    }
                }))
                .start();
        (new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            lo.opB();
                        } catch (final InterruptedException e) {
                        } finally {
                            cdl.countDown();
                        }
                    }
                }))
                .start();
        cdl.await();
    }

    public static void main(final String[] args) throws InterruptedException {
        problem1();
        problem2();
    }
}

