// ProgTest.java
//	Test class for demonstrating that Nachos can load
//	a user program and execute it.  
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

import java.io.*;

//----------------------------------------------------------------------
// StartProcess
// 	Run a user program.  Open the executable, load it into
//	memory, and jump to it.
//----------------------------------------------------------------------

class ProgTest implements Runnable {

  static String execName;

  public ProgTest(String filename) {
    NachosThread t = new NachosThread("ProgTest thread");

    Debug.println('a', "starting ProgTest");
    execName = filename;
    t.fork(this);
  }

  public void run() {
    RandomAccessFile executable;
    AddrSpace space;
    
    try {
      executable = new RandomAccessFile(execName, "r");
    }
    catch (IOException e) {
      Debug.println('+', "Unable to open executable file: " + execName);
      return;
    }

    try {
      space = new AddrSpace(executable);
    }
    catch (IOException e) {
      Debug.println('+', "Unable to read executable file: " + execName);
      return;
    }


    NachosThread.thisThread().setSpace(space);

    space.initRegisters();		// set the initial register values
    space.restoreState();		// load page table register

    Machine.run();			// jump to the user progam
    Debug.ASSERT(false);		// machine->Run never returns;
					// the address space exits
					// by doing the syscall "exit"
  }

}
