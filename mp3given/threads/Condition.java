// Condition.java
//
// The following class defines a "condition variable".  A condition
// variable does not have a value, but threads may be queued, waiting
// on the variable.  These are only operations on a condition variable: 
//
//	Wait() -- release the lock, relinquish the CPU until signaled, 
//		then re-acquire the lock
//
//	Signal() -- wake up a thread, if there are any waiting on 
//		the condition
//
//	Broadcast() -- wake up all threads waiting on the condition
//
// All operations on a condition variable must be made while
// the current thread has acquired a lock.  Indeed, all accesses
// to a given condition variable must be protected by the same lock.
// In other words, mutual exclusion must be enforced among threads calling
// the condition variable operations.
//
// In Nachos, condition variables are assumed to obey *Mesa*-style
// semantics.  When a Signal or Broadcast wakes up another thread,
// it simply puts the thread on the ready list, and it is the responsibility
// of the woken thread to re-acquire the lock (this re-acquire is
// taken care of within Wait()).  By contrast, some define condition
// variables according to *Hoare*-style semantics -- where the signalling
// thread gives up control over the lock and the CPU to the woken thread,
// which runs immediately and gives back control over the lock to the 
// signaller when the woken thread leaves the critical section.
//
// The consequence of using Mesa-style semantics is that some other thread
// can acquire the lock, and change data structures, before the woken
// thread gets a chance to run.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

class Condition {
  private String name;
  private List waitingThreads;  // who's waiting on this condition?

  public Condition(String debugName) {
    name = debugName;
    waitingThreads = new List();
  }

  public void wait(Lock conditionLock) {

    Debug.ASSERT(conditionLock.isHeldByCurrentThread(),
		 "Non-owner tried to manipulate condition variable.");
    
    Debug.printf('s', "Thread %s waiting on condition variable %s\n",
		 NachosThread.currentThread().getName(), name);

    conditionLock.release();

    int oldLevel = Interrupt.setLevel(Interrupt.IntOff);// disable interrupts
    waitingThreads.append(NachosThread.currentThread());
    NachosThread.thisThread().sleep();
    Interrupt.setLevel(oldLevel);	// re-enable interrupts

    Debug.printf('s', "Trying to reacquire condition %s's lock (%s) for " +
		 "thread %s\n", name, conditionLock.getName(),
		 NachosThread.thisThread().getName());

    conditionLock.acquire();

    Debug.printf('s', "Reacquired condition %s's lock (%s) for " +
		 "thread %s\n", name, conditionLock.getName(),
		 NachosThread.thisThread().getName());

  }

  public void signal(Lock conditionLock) {

    Debug.ASSERT(conditionLock.isHeldByCurrentThread(),
		  "Can't signal unless we own the lock!");

    Debug.printf('s', "Signalling condition %s\n", name);

    int oldLevel = Interrupt.setLevel(Interrupt.IntOff);// disable interrupts

    NachosThread newThread = (NachosThread)(waitingThreads.remove());
    if (newThread != null) {
      Debug.printf('s', "Waking up thread %s\n", newThread.getName());
      Scheduler.readyToRun(newThread);
    }

    Interrupt.setLevel(oldLevel);
  }    

  public void broadcast(Lock conditionLock) {

    Debug.ASSERT(conditionLock.isHeldByCurrentThread(),
		 "Can't signal unless we own the lock!");

    Debug.printf('s', "Broadcasting condition %s\n", name);

    int oldLevel = Interrupt.setLevel(Interrupt.IntOff);// disable interrupts

    NachosThread newThread = (NachosThread)(waitingThreads.remove());
    while (newThread != null) {
      Debug.printf('s', "Waking thread %s\n", newThread.getName());
      Scheduler.readyToRun(newThread);
      newThread = (NachosThread)(waitingThreads.remove());
    }

    Interrupt.setLevel(oldLevel);

  }
}
