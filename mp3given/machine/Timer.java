// Timer.java
//	Class to emulate a hardware timer device.
//
//      A hardware timer generates a CPU interrupt every X milliseconds.
//      This means it can be used for implementing time-slicing.
//
//      We emulate a hardware timer by scheduling an interrupt to occur
//      every time stats->totalTicks has increased by TimerTicks.
//
//      In order to introduce some randomness into time-slicing, if "doRandom"
//      is set, then the interrupt is comes after a random number of ticks.
//
//	Remember -- nothing in here is part of Nachos.  It is just
//	an emulation for the hardware that Nachos is running on top of.
//
//  DO NOT CHANGE -- part of the machine emulation
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.


class Timer extends Thread {
  private static final int TimerInterval = 10; // milliseconds

  private String name;
  private Runnable handler;
  private boolean randomize;
  private boolean realtime;
  private PendingInterrupt intrpt;

  //----------------------------------------------------------------------
  //      Initialize a hardware timer device.  Save the object to call
  //	on each interrupt, and then arrange for the timer to start
  //	generating interrupts.
  //
  //      "timerHandler" is the interrupt handler object for the timer device.
  //		Its run() method is called with interrupts disabled 
  //              every time the timer expires.
  //      "doRandom" -- if true, arrange for the interrupts to occur
  //		at random, instead of fixed, intervals.
  //----------------------------------------------------------------------

  public Timer(Runnable timerHandler, boolean doRandom, boolean doRealTime)  {
    name = "unnamed";
    handler = timerHandler;
    randomize = doRandom;
    realtime = doRealTime;
    if (realtime) {
      start();
    } else {
      intrpt = Interrupt.schedule(this, 
				  timeOfNextInterrupt(), Interrupt.TimerInt);
    }
  }

  public Timer(Runnable timerHandler, boolean doRandom)  {
    this(timerHandler, doRandom, false);
  }

  //----------------------------------------------------------------------
  // finalize()
  // 	called when the timer is garbage collected
  //----------------------------------------------------------------------

  protected void finalize() {
    Debug.print('t', "Deleting timer:" + name + "\n");
  }


  public void run() {
    if (realtime) {
      while (true) {
	try {
	  sleep(TimerInterval);
	}
	catch (InterruptedException e) {
	  Debug.print('t', "Timer was interrupted:" + name + "\n");
	}
	handler.run();
      }
    }
    else {
      // schedule the next timer device interrupt
      intrpt = Interrupt.schedule(this, 
				  timeOfNextInterrupt(), Interrupt.TimerInt);

      // invoke the Nachos interrupt handler for this device
      handler.run();
    }
  }
  
  //----------------------------------------------------------------------
  // Cancel
  // 	Cancel the timer object
  //----------------------------------------------------------------------
  
  public void cancel() {
    Debug.print('t', "Cancelling timer: " + name + "\n");
    if (realtime)
      stop();
    else
      intrpt.cancel();
  }    


  //----------------------------------------------------------------------
  // timeOfNextInterrupt
  //      Return when the hardware timer device will next cause an interrupt.
  //	If randomize is turned on, make it a (pseudo-)random delay.
  //----------------------------------------------------------------------

  private int timeOfNextInterrupt() {
    if (randomize)
      return 1 + Math.abs(Nachos.random.nextInt())
		   % (Statistics.TimerTicks * 2);
    else
      return Statistics.TimerTicks; 
  }

}

