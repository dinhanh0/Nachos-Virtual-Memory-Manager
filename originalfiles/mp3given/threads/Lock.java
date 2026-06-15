// Lock.java
//
// The following class defines a "lock".  A lock can be BUSY or FREE.
// There are only two operations allowed on a lock: 
//
//	Acquire -- wait until the lock is FREE, then set it to BUSY
//
//	Release -- set lock to be FREE, waking up a thread waiting
//		in Acquire if necessary
//
// In addition, by convention, only the thread that acquired the lock
// may release it.  As with semaphores, you can't read the lock value
// (because the value might change immediately after you read it).  
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

class Lock {
  private String name;				// for debugging
  // plus some other stuff you'll need to define

  private Semaphore sem;  // semaphore used for implementation of lock
  private NachosThread owner;  // which thread currently holds this lock?

  public Lock(String debugName) {
    name = debugName;
    sem = new Semaphore("Semaphore for lock \"" + debugName + "\"", 1);
    owner = null;
  }

  public void acquire() {

    Debug.printf('s', "Acquiring lock %s for thread %s\n",
		 name, NachosThread.thisThread().getName());

    sem.P();

    owner = NachosThread.thisThread();

    Debug.printf('s', "Acquired lock %s for thread %s\n",
		 name, NachosThread.thisThread().getName());

  }

  public void release() {

    Debug.ASSERT((NachosThread.thisThread() == owner),
		 "A thread which doesn't own the lock tried to " +
		 "release it!\n");

    Debug.printf('s', "Thread %s dropping lock %s\n",
		 NachosThread.thisThread().getName(), name);
    owner = null;
    sem.V();
    Debug.printf('s', "Thread %s dropped lock %s\n",
		 NachosThread.thisThread().getName(), name);

  }

  // a predicate which determines whether or not the lock is held by the
  // current thread.  Used for sanity checks in condition variables.
  public boolean isHeldByCurrentThread()
  {
    return (owner == NachosThread.thisThread());
  }

  public String getName()
  {
    return name;
  }

}
