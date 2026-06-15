// Interrupt.java
//	Class to simulate hardware interrupts.
//
//	The hardware provides a routine (SetLevel) to enable or disable
//	interrupts.
//
//	In order to emulate the hardware, we need to keep track of all
//	interrupts the hardware devices would cause, and when they
//	are supposed to occur.  
//
//  DO NOT CHANGE -- part of the machine emulation
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

class Interrupt {

  // String definitions for debugging messages

  static final String intLevelNames[] = { "off", "on"};
  static final String intTypeNames[] = { "timer", "disk", "console write", 
			"console read", "network send", "network recv"};

  // Interrupts can be disabled (IntOff) or enabled (IntOn)
  static final int IntOff = 0, IntOn = 1;

  // Nachos can be running kernel code (SystemMode), user code (UserMode),
  // or there can be no runnable thread, because the ready list 
  // is empty (IdleMode).
  static final int IdleMode = 0, SystemMode = 1, UserMode=2;

  // IntType records which hardware device generated an interrupt.
  // In Nachos, we support a hardware timer device, a disk, a console
  // display and keyboard, and a network.
  static final int TimerInt=0, DiskInt=1, ConsoleWriteInt=2, 
    ConsoleReadInt=3, NetworkSendInt=4, NetworkRecvInt=5;


  static int level;		// are interrupts enabled or disabled?
  static List pending;		// the list of interrupts scheduled
	           		// to occur in the future
  static boolean inHandler;	// TRUE if we are running an interrupt handler
  static boolean yieldOnReturn;	// TRUE if we are to context switch
				// on return from the interrupt handler
  static int status;         	// idle, kernel mode, user mode



  //----------------------------------------------------------------------
  // Interrupt
  // 	Initialize the simulation of hardware device interrupts.
  //	
  //	Interrupts start disabled, with no interrupts pending, etc.
  //----------------------------------------------------------------------

 static {
    level = IntOff;
    pending = new List();
    inHandler = false;
    yieldOnReturn = false;
    status = SystemMode;
  }

  // idle, kernel, user
  public static int getStatus() { 
    return status; 
  }
  public static void setStatus(int st) { 
    status = st; 
  }


  //----------------------------------------------------------------------
  // changeLevel
  // 	Change interrupts to be enabled or disabled, without advancing 
  //	the simulated time (normally, enabling interrupts advances the time).
  //
  //	Used internally.
  //
  //	"old" -- the old interrupt status
  //	"now" -- the new interrupt status
  //----------------------------------------------------------------------
  public static void changeLevel(int old, int now) {
    level = now;
    if (Debug.isEnabled('i')) {
      Debug.println('i',"\tinterrupts: " + intLevelNames[old] + " -> " + 
		    intLevelNames[now]);
    }
  }


  // getLevel()
  // return current interrrup level
  //
  public static int getLevel() {
    return level;
  }


  //----------------------------------------------------------------------
  // setLevel
  // 	Change interrupts to be enabled or disabled, and if interrupts
  //	are being enabled, advance simulated time by calling OneTick().
  //
  // Returns:
  //	The old interrupt status.
  // Parameters:
  //	"now" -- the new interrupt status
  //----------------------------------------------------------------------

  public static int setLevel(int now) {
    int old = level;
    
    if ( !((now == IntOff) || (inHandler == false)) ) {
      System.out.println("Interrupt.setlevel: something's wrong");
      return old;                               // interrupt handlers are 
						// prohibited from enabling 
						// interrupts
    }
    changeLevel(old, now);			// change to new state
    if ((now == IntOn) && (old == IntOff))
      oneTick();				// advance simulated time
    return old;
  }

  //----------------------------------------------------------------------
  // enable
  // 	Turn interrupts on.  Who cares what they used to be? 
  //	Used in ThreadRoot, to turn interrupts on when first starting up
  //	a thread.
  //----------------------------------------------------------------------

  public static void enable() { 
    setLevel(IntOn); 
  }

  //----------------------------------------------------------------------
  // Interrupt::OneTick
  // 	Advance simulated time and check if there are any pending 
  //	interrupts to be called. 
  //
  //	Two things can cause oneTick to be called:
  //		interrupts are re-enabled
  //		a user instruction is executed
  //----------------------------------------------------------------------
  public static void oneTick() {
    int old = status;

    // advance simulated time
    if (status == SystemMode) {
      Nachos.stats.totalTicks += Statistics.SystemTick;
      Nachos.stats.systemTicks += Statistics.SystemTick;
    } else {					// USER_PROGRAM
      Nachos.stats.totalTicks += Statistics.UserTick;
      Nachos.stats.userTicks += Statistics.UserTick;
    }

    if (Debug.isEnabled('i')) {
      Debug.print('i', "\n== Tick " + Nachos.stats.totalTicks + " ==\n");
    }

    // check any pending interrupts are now ready to fire
    changeLevel(IntOn, IntOff);		// first, turn off interrupts
					// (interrupt handlers run with
					// interrupts disabled)
    while (checkIfDue(false))		// check for pending interrupts
      ;
    changeLevel(IntOff, IntOn);		// re-enable interrupts

    if (yieldOnReturn) {		// if the timer device handler asked 
				// for a context switch, ok to do it now
      yieldOnReturn = false;
      status = SystemMode;		// yield is a kernel routine
      NachosThread.thisThread().Yield();
      status = old;
    }
  }

  public static boolean isInHandler()
  {
     return inHandler;
  }

  //----------------------------------------------------------------------
  // yieldOnReturn
  // 	Called from within an interrupt handler, to cause a context switch
  //	(for example, on a time slice) in the interrupted thread,
  //	when the handler returns.
  //
  //	We can't do the context switch here, because that would switch
  //	out the interrupt handler, and we want to switch out the 
  //	interrupted thread.
  //----------------------------------------------------------------------

  public static void yieldOnReturn() { 
    Debug.ASSERT(inHandler == true);  
    yieldOnReturn = true; 
  }

  //----------------------------------------------------------------------
  // idle
  // 	Routine called when there is nothing in the ready queue.
  //
  //	Since something has to be running in order to put a thread
  //	on the ready queue, the only thing to do is to advance 
  //	simulated time until the next scheduled hardware interrupt.
  //
  //	If there are no pending interrupts, stop.  There's nothing
  //	more for us to do.
  //----------------------------------------------------------------------
  public static void idle() {
    Debug.print('i', "Machine idling; checking for interrupts.\n");
    status = IdleMode;
    if (checkIfDue(true)) {		// check for any pending interrupts
    	while (checkIfDue(false))	// check for any other pending 
	    ;				// interrupts
        yieldOnReturn = false;		// since there's nothing in the
				// ready queue, the yield is automatic
        status = SystemMode;
	return;				// return in case there's now
					// a runnable thread
    }

    // if there are no pending interrupts, and nothing is on the ready
    // queue, it is time to stop.   If the console or the network is 
    // operating, there are *always* pending interrupts, so this code
    // is not reached. Instead, the halt must be invoked by the user program.

    Debug.print('i', "Machine idle.  No interrupts to do.\n");
    System.out.print("No threads ready or runnable, and no pending interrupts.\n");
    System.out.print("Assuming the program completed.\n");
    halt();
  }
  
  //----------------------------------------------------------------------
  // halt
  // 	Shut down Nachos cleanly, printing out performance statistics.
  //----------------------------------------------------------------------
  public static void halt() {
    System.out.print("Machine halting!\n\n");
    Nachos.stats.print();
    Nachos.cleanup();     // Never returns.
  }

  //----------------------------------------------------------------------
  // schedule
  // 	Arrange for the CPU to be interrupted when simulated time
  //	reaches "now + when".
  //
  //	Implementation: just put it on a sorted list.
  //
  //	NOTE: the Nachos kernel should not call this routine directly.
  //	Instead, it is only called by the hardware device simulators.
  //
  //	"handler" is the procedure to call when the interrupt occurs
  //	"arg" is the argument to pass to the procedure
  //	"fromNow" is how far in the future (in simulated time) the 
  //		 interrupt is to occur
  //	"type" is the hardware device that generated the interrupt
  //----------------------------------------------------------------------
  public static PendingInterrupt schedule(Runnable handler, 
					  int fromNow, int type) {
    int when = Nachos.stats.totalTicks + fromNow;
    PendingInterrupt toOccur = 
      new PendingInterrupt(handler, when, type);
Debug.printf('i', "Scheduling interrupt handler the %s at time = %d\n", 
		 intTypeNames[type], new Integer(when));
    Debug.ASSERT(fromNow > 0);

    pending.sortedInsert(toOccur, when);
    return toOccur;
  }



  //----------------------------------------------------------------------
  // checkIfDue
  // 	Check if an interrupt is scheduled to occur, and if so, fire it off.
  //
  // Returns:
  //	TRUE, if we fired off any interrupt handlers
  // Params:
  //	"advanceClock" -- if TRUE, there is nothing in the ready queue,
  //		so we should simply advance the clock to when the next 
  //		pending interrupt would occur (if any).  If the pending
  //		interrupt is just the time-slice daemon, however, then 
  //		we're done!
  //----------------------------------------------------------------------
  private static boolean checkIfDue(boolean advanceClock) {
    int old = status;
    int when;

    Debug.ASSERT(level == IntOff);	// interrupts need to be disabled,
					// to invoke an interrupt handler
    if (Debug.isEnabled('i'))
      dumpState();

    ListElement le = pending.sortedRemove();
    if (le == null)		// no pending interrupts
      return false;			

    PendingInterrupt toOccur = (PendingInterrupt)le.getItem();
    when = (int)le.getKey();
    List.freeElement(le);

    if (advanceClock && when > Nachos.stats.totalTicks) {
      // advance the clock
      Nachos.stats.idleTicks += (when - Nachos.stats.totalTicks);
      Nachos.stats.totalTicks = when;
    } else if (when > Nachos.stats.totalTicks) {// not time yet, put it back
      pending.sortedInsert(toOccur, when);
      return false;
    }


    // Check if there is nothing more to do, and if so, quit
    //    if ((status == IdleMode) && (toOccur->type == TimerInt) &&
    //      pending->IsEmpty()) {

    // If we are in idle mode  and the timeslice timer is the only 
    // pending interrupt, then we are done
    if ((status == IdleMode) && (Nachos.timer != null) &&
        pending.isEmpty()) {
      pending.sortedInsert(toOccur, when);
      return false;
    }


    if (!(toOccur.cancelled)) {

      if (Debug.isEnabled('i')) {
	Debug.printf('i',"Invoking interrupt handler for the %s at time %d\n", 
		     intTypeNames[toOccur.type], new Integer(toOccur.when));
      }
      
      if (Nachos.USER_PROGRAM)
	  Machine.delayedLoad(0, 0);

      inHandler = true;
      status = SystemMode;			// whatever we were doing,
						// we are now going to be
						// running in the kernel
      toOccur.handler.run();            	// call the interrupt handler
      status = old;				// restore the machine status
      inHandler = false;
    }

    return true;
  }


  //----------------------------------------------------------------------
  // dumpState
  // 	Print the complete interrupt state - the status, and all interrupts
  //	that are scheduled to occur in the future.
  //----------------------------------------------------------------------

  public static void dumpState() {
    System.out.println("Time: " + Nachos.stats.totalTicks +
		     ", interrupts " + intLevelNames[level]);

    System.out.print("Pending interrupts:\n");
    pending.print();
    System.out.print("End of pending interrupts\n");
  }


}

