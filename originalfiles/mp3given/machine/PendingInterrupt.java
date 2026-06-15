// PendingInterrupt.java
//	Class to represent a pending hardware interrupt.
//
//  DO NOT CHANGE -- part of the machine emulation
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

class PendingInterrupt implements Printable {

  Runnable handler;    // The object (in the hardware device
                       // emulator) to call when the interrupt occurs
  int when;		// When the interrupt is supposed to fire
  int type;		// for debugging
  boolean cancelled;    // was the interrupt cancelled

  //----------------------------------------------------------------------
  // pendingInterrupt
  // 	Initialize a hardware device interrupt that is to be scheduled 
  //	to occur in the near future.
  //
  //	"func" is the procedure to call when the interrupt occurs
  //	"param" is the argument to pass to the procedure
  //	"time" is when (in simulated time) the interrupt is to occur
  //	"kind" is the hardware device that generated the interrupt
  //----------------------------------------------------------------------

  public PendingInterrupt(Runnable h, int time, int kind)  {
    handler = h;
    when = time;
    type = kind;
    cancelled = false;
  }

  //----------------------------------------------------------------------
  // cancel
  // 	Cancel an interrupt
  //
  //	Implementation: mark the interrupt as cancelled
  //----------------------------------------------------------------------
  public void cancel() {

    Debug.printf('i', "Cancelling interrupt handler the %s at time = %d\n", 
		 Interrupt.intTypeNames[type], new Integer(when));

    cancelled = true;
  }


  //----------------------------------------------------------------------
  // print
  // 	Print information about an interrupt that is scheduled to occur.
  //	When, where, why, etc.
  //----------------------------------------------------------------------

  public void print() {
    
    if (cancelled) {
      System.out.print("Interrupt handler " + 
		       Interrupt.intTypeNames[type] + ", cancelled\n");
    }
    else { 
      System.out.println("Interrupt handler " + 
			 Interrupt.intTypeNames[type] +
			 ", scheduled at " + when);
    }
  }

}


