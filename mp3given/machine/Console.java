// console.java
//
//	Class to simulate a serial port to a console device.
//	A console has input (a keyboard) and output (a display).
//	These are each simulated by operations on UNIX files.
//	The simulated device is asynchronous,
//	so we have to invoke the interrupt handler (after a simulated
//	delay), to signal that a byte has arrived and/or that a written
//	byte has departed.
//
//  DO NOT CHANGE -- part of the machine emulation
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

import java.io.*;

// The following class defines a hardware console device.
// Input and output to the device is simulated by reading 
// and writing to UNIX files ("readFile" and "writeFile").
//
// Since the device is asynchronous, the interrupt handler "readAvail" 
// is called when a character has arrived, ready to be read in.
// The interrupt handler "writeDone" is called when an output character 
// has been "put", so that the next character can be written.

class Console {

  private InputStream readFile;         // UNIX file emulating the keyboard 
  private PrintStream writeFile;	// UNIX file emulating the display

  private Runnable writeHandler; 	// Interrupt handler to call when 
                                        // the PutChar I/O completes
  private Runnable readHandler; 	// Interrupt handler to call when 
				       // a character arrives from the keyboard
  private boolean putBusy;		// Is a PutChar operation in progress?
		          		// If so, you can't do another one!
  private boolean charAvail;            // Is an incoming character available?
  private char incoming;  		// Contains the character to be read,
				        // if there is one available. 

  private ConsRdIntHandler consoleReadPoll;
  private ConsWrIntHandler consoleWriteDone;

  //----------------------------------------------------------------------
  // Console
  // 	Initialize the simulation of a hardware console device.
  //
  //	"rdFile" -- UNIX file simulating the keyboard (null -> use stdin)
  //	"wrFile" -- UNIX file simulating the display (null -> use stdout)
  // 	"readAvail" is the interrupt handler called when a character arrives
  //		from the keyboard
  // 	"writeDone" is the interrupt handler called when a character has
  //		been output, so that it is ok to request the next char be
  //		output
  //----------------------------------------------------------------------

  public Console(String rdFile, String wrFile, Runnable readAvail, 
		 Runnable writeDone) throws IOException {
	 
    if (rdFile == null) {
      readFile = System.in;
      // keyboard = stdin
    }
    else {
      readFile = new FileInputStream(rdFile);	
      // should be read-only
    }

    if (wrFile == null) {
      writeFile = System.out; 
      // display = stdout
    }
    else {
      writeFile = new PrintStream(new FileOutputStream(wrFile));
      // Yes, this is deprecated, and we will get a warning from the
      // compiler.  However, this is necessary, because we may need to
      // set writeFile = System.out as above, and System.out is an
      // instance of the deprecated class PrintStream.  (Why they've
      // deprecated the class and not changed System.out is a very
      // good question.)
    }

    // set up the stuff to emulate asynchronous interrupts
    writeHandler = writeDone;
    readHandler = readAvail;
    putBusy = false;
    charAvail = false;
    
    consoleReadPoll = new ConsRdIntHandler(this);
    consoleWriteDone = new ConsWrIntHandler(this);

    // start polling for incoming packets
    Interrupt.schedule(consoleReadPoll, Statistics.ConsoleTime, 
		       Interrupt.ConsoleReadInt);

    // This leaves the possibility of getting an interrupt after the
    // user is done with us; we won't be collected until after this
    // interrupt expires.  Shouldn't be an issue, since it just
    // signals the console that a character is ready.

    // However, this will cause readHandler.run() to be called,
    // perhaps after the stuff is done.  Is it going to be necessary
    // to take care of this stuff?

  }


  //----------------------------------------------------------------------
  // checkCharAvail()
  // 	Periodically called to check if a character is available for
  //	input from the simulated keyboard (eg, has it been typed?).
  //
  //	Only read it in if there is buffer space for it (if the previous
  //	character has been grabbed out of the buffer by the Nachos kernel).
  //	Invoke the "read" interrupt handler, once the character has been 
  //	put into the buffer. 
  //----------------------------------------------------------------------

  public void checkCharAvail() {
    char c;
    int i;

    // schedule the next time to poll for a packet
    Interrupt.schedule(consoleReadPoll, Statistics.ConsoleTime, 
		       Interrupt.ConsoleReadInt);

    try {
      i = readFile.available();
    } catch (IOException e) {
      Debug.print('+', "Console.checkCharAvail(): IO Error!");
      return;
    }

    // do nothing if character is already buffered, or none to be read
    if ((charAvail == true) || i == 0)
      return;	  

    //Debug.println('c', "i=" + i);

    // otherwise, read character and tell user about it
    try {
      c = (char)readFile.read();
    } catch (IOException e) {
      Debug.print('+', "Console.checkCharAvail(): IO Error!");
      return;
    }

    incoming = c ;
    charAvail = true;
    Nachos.stats.numConsoleCharsRead++;
    readHandler.run();
  }

  //----------------------------------------------------------------------
  // writeDone()
  // 	Internal routine called when it is time to invoke the interrupt
  //	handler to tell the Nachos kernel that the output character has
  //	completed.
  //----------------------------------------------------------------------

  public void writeDone() {
    putBusy = false;
    Nachos.stats.numConsoleCharsWritten++;
    writeHandler.run();
  }

  //----------------------------------------------------------------------
  // getChar()
  // 	Read a character from the input buffer, if there is any there.
  //	Either return the character, or throw EOFException if none buffered.
  //----------------------------------------------------------------------

  public char getChar() throws IOException {
    if (charAvail) {
      charAvail = false;
      return incoming;
    }
    else 
      throw new IOException();
  }

  // do we want getChar() to throw an IOException or EOFException?  We
  // can do either.

  //----------------------------------------------------------------------
  // putChar()
  // 	Write a character to the simulated display, schedule an interrupt 
  //	to occur in the future, and return.
  //----------------------------------------------------------------------

  public void putChar(char ch) throws IOException {
    if (!putBusy) {
      writeFile.print(ch);
      putBusy = true;
      Interrupt.schedule(consoleWriteDone, Statistics.ConsoleTime, 
			 Interrupt.ConsoleWriteInt);
    }
    else
      throw new IOException();
  }
}

// console read interrupt handler class
//
class ConsRdIntHandler implements Runnable {
  private Console console;

  public ConsRdIntHandler(Console cons) {
    console = cons;
  }
  
  public void run() {
    console.checkCharAvail();
  }
}


// console write interrupt handler class
//
class ConsWrIntHandler implements Runnable {
  private Console console;

  public ConsWrIntHandler(Console cons) {
    console = cons;
  }
  
  public void run() {
    console.writeDone();
  }
}

