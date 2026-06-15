// Semaphore.java
//
// Any implementation of a synchronization routine needs some
// primitive atomic operation.  We assume Nachos is running on
// a uniprocessor, and thus atomicity can be provided by
// turning off interrupts.  While interrupts are disabled, no
// context switch can occur, and thus the current thread is guaranteed
// to hold the CPU throughout, until interrupts are reenabled.
//
// Because some of these routines might be called with interrupts
// already disabled (Semaphore::V for one), instead of turning
// on interrupts at the end of the atomic operation, we always simply
// re-set the interrupt state back to its original value (whether
// that be disabled or enabled).
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.



// The following class defines a "semaphore" whose value is a non-negative
// integer.  The semaphore has only two operations P() and V():
//
//	P() -- waits until value > 0, then decrement
//
//	V() -- increment, waking up a thread waiting in P() if necessary
// 
// Note that the interface does *not* allow a thread to read the value of 
// the semaphore directly -- even if you did read the value, the
// only thing you would know is what the value used to be.  You don't
// know what the value is now, because by the time you get the value
// into a register, a context switch might have occurred,
// and some other thread might have called P or V, so the true value might
// now be different.

class Semaphore {

  public String name;        // useful for debugging
  private int value;          // semaphore value, always >= 0
  private List queue;      // threads waiting in P() for the value to be > 0


  //----------------------------------------------------------------------
  // Semaphore
  // 	Initialize a semaphore, so that it can be used for synchronization.
  //
  //	"debugName" is an arbitrary name, useful for debugging.
  //	"initialValue" is the initial value of the semaphore.
  //----------------------------------------------------------------------

  public Semaphore(String debugName, int initialValue) {
    name = debugName;
    value = initialValue;
    queue = new List();
  }


  //----------------------------------------------------------------------
  // P
  // 	Wait until semaphore value > 0, then decrement.  Checking the
  //	value and decrementing must be done atomically, so we
  //	need to disable interrupts before checking the value.
  //
  //	Note that Thread::Sleep assumes that interrupts are disabled
  //	when it is called.
  //----------------------------------------------------------------------

  public void P() {
    int oldLevel = Interrupt.setLevel(Interrupt.IntOff);// disable interrupts
    
    while (value == 0) { 			// semaphore not available
	queue.append(NachosThread.thisThread());  // so go to sleep
	NachosThread.thisThread().sleep();
    } 
    value--; 					// semaphore available, 
						// consume its value
    
    Interrupt.setLevel(oldLevel);	// re-enable interrupts
  }

  //----------------------------------------------------------------------
  // V
  // 	Increment semaphore value, waking up a waiter if necessary.
  //	As with P(), this operation must be atomic, so we need to disable
  //	interrupts.  Scheduler.readyToRun() assumes that threads
  //	are disabled when it is called.
  //----------------------------------------------------------------------

  public void V() {
    NachosThread thread;
    int oldLevel = Interrupt.setLevel(Interrupt.IntOff);

    thread = (NachosThread)queue.remove();
    if (thread != null)	   // make thread ready, consuming the V immediately
	Scheduler.readyToRun(thread);
    value++;
    Interrupt.setLevel(oldLevel);
  }

}
