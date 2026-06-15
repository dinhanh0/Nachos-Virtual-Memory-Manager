// FileSystemTest.java
//	Simple test routines for the file system.  
//
//	We implement:
//	   Copy -- copy a file from UNIX to Nachos
//	   Print -- cat the contents of a Nachos file 
//	   Perftest -- a stress test for the Nachos file system
//		read and write a really large file in tiny chunks
//		(won't work on baseline system!)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

import java.io.*;

class FileSystemTest {
  // make it small, just to be difficult
  private static final int TransferSize = 10;

  //----------------------------------------------------------------------
  // copy
  // 	Copy the contents of the UNIX file "from" to the Nachos file "to"
  //----------------------------------------------------------------------
  
  public static void copy(String from, String to) {
    File fp;
    FileInputStream fs;
    OpenFile openFile;
    int amountRead;
    long fileLength;
    byte buffer[];

    // Open UNIX file
    fp = new File(from);
    if (!fp.exists()) {
      Debug.printf('+', "Copy: couldn't open input file %s\n", from);
      return;
    }

    // Figure out length of UNIX file
    fileLength = fp.length();

    // Create a Nachos file of the same length
    Debug.printf('f', "Copying file %s, size %d, to file %s\n", from,
		 new Long(fileLength), to);
    if (!Nachos.fileSystem.create(to, (int)fileLength)) {	 
      // Create Nachos file
      Debug.printf('+', "Copy: couldn't create output file %s\n", to);
      return;
    }
    
    openFile = Nachos.fileSystem.open(to);
    Debug.ASSERT(openFile != null);
    
    // Copy the data in TransferSize chunks
    buffer = new byte[TransferSize];
    try {
      fs = new FileInputStream(fp);
      while ((amountRead = fs.read(buffer)) > 0)
	openFile.write(buffer, 0, amountRead);	
    } catch (IOException e) {
      Debug.print('+', "Copy: data copy failed\n");      
      return;
    }
    // Close the UNIX and the Nachos files
    //delete openFile;
    try {fs.close();} catch (IOException e) {}
  }

  //----------------------------------------------------------------------
  // Print
  // 	Print the contents of the Nachos file "name".
  //----------------------------------------------------------------------

  public static void print(String name) {
    OpenFile openFile;    
    int i, amountRead;
    byte buffer[];

    if ((openFile = Nachos.fileSystem.open(name)) == null) {
      Debug.printf('+', "Print: unable to open file %s\n", name);
      return;
    }
    
    buffer = new byte[TransferSize];
    while ((amountRead = openFile.read(buffer, 0, TransferSize)) > 0)
      for (i = 0; i < amountRead; i++)
	Debug.printf('+', "%c", new Byte(buffer[i]));

    return;
  }

  //----------------------------------------------------------------------
  // PerformanceTest
  // 	Stress the Nachos file system by creating a large file, writing
  //	it out a bit at a time, reading it back a bit at a time, and then
  //	deleting the file.
  //
  //	Implemented as three separate routines:
  //	  FileWrite -- write the file
  //	  FileRead -- read the file
  //	  PerformanceTest -- overall control, and print out performance #'s
  //----------------------------------------------------------------------

  private static final String FileName = "TestFile";
  private static final String ContentString = "1234567890";
  private static final int ContentSize = ContentString.length();
  private static final byte Contents[] = new byte[ContentSize];
  //private static final int FileSize = ContentSize * 5000;
  private static final int FileSize = ContentSize * 300;

  static {
    ContentString.getBytes(0, ContentSize, Contents, 0);
  }

  private static void fileWrite() {
    OpenFile openFile;    
    int i, numBytes;
    
    Debug.printf('+', "Sequential write of %d byte file, in %d byte chunks\n", 
		 new Integer(FileSize), new Integer(ContentSize));
    if (!Nachos.fileSystem.create(FileName, FileSize)) {
      Debug.printf('+', "Perf test: can't create %s\n", FileName);
      return;
    }
    openFile = Nachos.fileSystem.open(FileName);
    if (openFile == null) {
      Debug.printf('+', "Perf test: unable to open %s\n", FileName);
      return;
    }
    for (i = 0; i < FileSize; i += ContentSize) {
      numBytes = openFile.write(Contents, 0, ContentSize);
      if (numBytes < 10) {
	Debug.printf('+', "Perf test: unable to write %s\n", FileName);
	return;
      }
    }
  }

  private static void fileRead() {
    OpenFile openFile;    
    byte buffer[] = new byte[ContentSize];
    int i, numBytes;

    Debug.printf('+',"Sequential read of %d byte file, in %d byte chunks\n", 
		 new Integer(FileSize), new Integer(ContentSize));

    if ((openFile = Nachos.fileSystem.open(FileName)) == null) {
      Debug.printf('+', "Perf test: unable to open file %s\n", FileName);
      return;
    }
    for (i = 0; i < FileSize; i += ContentSize) {
      numBytes = openFile.read(buffer, 0, ContentSize);
      if ((numBytes < 10) || !byteCmp(buffer, Contents, ContentSize)) {
	Debug.printf('+', "Perf test: unable to read %s\n", FileName);
	return;
      }
    }
  }

  private static boolean byteCmp(byte a[], byte b[], int len) {
    for (int i = 0; i < len; i++)
      if (a[i] != b[i]) return false;
    return true;
  }

  public static void performanceTest() {
    Debug.print('+', "Starting file system performance test:\n");
    Nachos.stats.print();
    fileWrite();
    fileRead();
    if (!Nachos.fileSystem.remove(FileName)) {
      Debug.printf('+', "Perf test: unable to remove %s\n", FileName);
      return;
    }
    Nachos.stats.print();
  }

}
