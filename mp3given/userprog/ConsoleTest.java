// ConsoleTest.java
//
//	Class for testing the Console hardware device.
//
// Copyright (c) 1998 Rice University.

import java.io.*;


class ConsoleTest {

  // Objects needed for the console test.  Threads making
  // I/O requests wait on a Semaphore to delay until the I/O completes.

  private static Console console;
  private static Semaphore readAvail;
  private static Semaphore writeDone;


  //----------------------------------------------------------------------
  // ConsoleTest.run()
  // 	Test the console by echoing characters typed at the input onto
  //	the output.  Stop when the user types a 'q'.
  //----------------------------------------------------------------------

  public static void run(String in, String out) {
    char ch;

    Debug.println('c', "ConsoleTest: starting");

    readAvail = new Semaphore("read avail", 0);
    writeDone = new Semaphore("write done", 0);
    try {
      console = new Console(in, out, new ConsHandler(readAvail), 
			    new ConsHandler(writeDone));
    } catch (IOException e) {
      Debug.println('c', "ConsoleTest: IO Error!");
    }

    while (true) {
      //Debug.println('c', "ConsoleTest: 0");
      readAvail.P();		// wait for character to arrive
      //Debug.println('c', "ConsoleTest: 1");
      try {
	ch = console.getChar();
      } catch (IOException e) {
	Debug.println('c', "ConsoleTest: should not happen!");
	return;
      }

      try {
	console.putChar(ch);	// echo it!
      } catch (IOException e) {
	Debug.println('c', "ConsoleTest: should not happen!");
	return;
      }
      //Debug.println('c', "ConsoleTest: 2");
      writeDone.P();            // wait for write to finish
      //Debug.println('c', "ConsoleTest: 3");
      if (ch == 'q') return;    // if q, quit
    }
  }

}


//----------------------------------------------------------------------
// Console interrupt handlers
// 	Wake up the thread that requested the I/O.
//----------------------------------------------------------------------


class ConsHandler implements Runnable {
  private Semaphore semaphore;

  public ConsHandler(Semaphore s) {
    semaphore = s;
  }

  public void run() {
    //Debug.println('c', "ConsoleTest: 10 " + semaphore.name);
    semaphore.V();
  }

}
