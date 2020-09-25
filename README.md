
## Multithreading Examples

Problem 1: This was a Synchronization problem

Threads all trying to update a shared variable results in an unexpected count 

Solution: synchronize on the increment method which then ensure that each thread is updating as required.



Problem 2: Deadlock problem of bad ordering of lock management and release. 

The original implementation was like this:


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
            b.lock();
            Thread.sleep(5_000);
            a.lock();
            assert a.isHeldByCurrentThread() && b.isHeldByCurrentThread();
          } finally {
            a.unlock();
            b.unlock();
          }
        }
  
  
This meant that when the first Thread locked A and slept - the second thread then locked B and slept. 
When both threads wake, they then try to lock the other. Thread 1 tried to lock B and Thread 2 tried to lock A...
resulting in both now waiting until they are unlocked as the opposite thread has locked these.

Solution: This is the correct ordering below


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
            
This means now that both try to lock A, Thread 1 locks A first and sleeps - thread B tries to lock A - but now must
wait until Thread 1 releases the lock for A. Thread A continues, wakes from the sleep then locks B also. This then 
satisfies the assertion because Thread 1 has the lock for A and B....it then proceeds to unlock A and B.

Thread 2 now can lock A as A was released, it sleeps and then successfully locks B resulting in it having access to both
locks and satisfying its assertion. Finally unlocks A and B. 