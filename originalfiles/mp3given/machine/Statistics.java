// Statistics.java
//	Class for managing statistics about Nachos performance.
//
// DO NOT CHANGE -- these stats are maintained by the machine emulation.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.


// The following class defines the statistics that are to be kept
// about Nachos behavior -- how much time (ticks) elapsed, how
// many user instructions executed, etc.
//
// The fields in this class are public to make it easier to update.

class Statistics {

  // Constants used to reflect the relative time an operation would
  // take in a real system.  A "tick" is a just a unit of time -- if you 
  // like, a microsecond.
  //
  // Since Nachos kernel code is directly executed, and the time spent
  // in the kernel measured by the number of calls to enable interrupts,
  // these time constants are none too exact.
  
  public static final int UserTick = 1;
  // advance for each user-level instruction 
  public static final int SystemTick =	10;
  // advance each time interrupts are enabled
  public static final int RotationTime = 500;
  // time disk takes to rotate one sector
  public static final int SeekTime = 500;
  // time disk takes to seek past one track
  public static final int ConsoleTime = 100;
  // time to read or write one character
  public static final int NetworkTime = 100;
  // time to send or receive one packet
  public static final int TimerTicks = 40;
  // (average) time between timer interrupts


  // instance variables

  public int totalTicks;        // Total time running Nachos
  public int idleTicks;        	// Time spent idle (no threads to run)
  public int systemTicks; 	// Time spent executing system code
  public int userTicks;         // Time spent executing user code
				// (this is also equal to # of
				// user instructions executed)

  public int numDiskReads;		// number of disk read requests
  public int numDiskWrites;		// number of disk write requests
  public int numConsoleCharsRead;  // number of chars read from the keyboard
  public int numConsoleCharsWritten;// number of chars written to the display
  public int numPageFaults;	// number of virtual memory page faults
  public int numPacketsSent;	// number of packets sent over the network
  public int numPacketsRecvd;  // number of packets received over the network


  //----------------------------------------------------------------------
  // statistics
  // 	Initialize performance metrics to zero, at system startup.
  //----------------------------------------------------------------------

  public Statistics() {
    totalTicks = idleTicks = systemTicks = userTicks = 0;
    numDiskReads = numDiskWrites = 0;
    numConsoleCharsRead = numConsoleCharsWritten = 0;
    numPageFaults = numPacketsSent = numPacketsRecvd = 0;
  }

  //----------------------------------------------------------------------
  // print
  // 	Print performance metrics, when we've finished everything
  //	at system shutdown.
  //----------------------------------------------------------------------

  public void print() {

    Debug.printf('+', "Ticks: total %d, idle %d, system %d, user %d\n", 
		 new Integer(totalTicks), new Integer(idleTicks), 
		 new Integer(systemTicks), new Integer(userTicks));
    Debug.printf('+', "Disk I/O: reads %d, writes %d\n", 
		 new Integer(numDiskReads), new Integer(numDiskWrites));
    Debug.printf('+', "Console I/O: reads %d, writes %d\n", 
		 new Integer(numConsoleCharsRead), 
		 new Integer(numConsoleCharsWritten));
    Debug.printf('+', "Paging: faults %d\n", new Integer(numPageFaults));
    Debug.printf('+', "Network I/O: packets received %d, sent %d\n", 
		 new Integer(numPacketsRecvd), new Integer(numPacketsSent));

  }

  public void printElapsedTicks()
  {
    Debug.printf('+', "Ticks: total %d\n", new Integer(totalTicks)); 
  }

}
