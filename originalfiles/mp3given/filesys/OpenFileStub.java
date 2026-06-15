// OpenFileStub.java
//	Class for opening, closing, reading and writing to 
//	individual files.  The operations supported are similar to
//	the UNIX ones -- type 'man open' to the UNIX prompt.
//
//	There are two implementations.  This one ("Stub") directly
//	turns the file operations into the underlying UNIX operations.
//	(cf. comment in filesys.h).
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

import java.io.*;

class OpenFileStub implements OpenFile {

  private RandomAccessFile file;
  private long currentOffset;

  // Open a file 
  public OpenFileStub(RandomAccessFile f) {
    file = f; 
    currentOffset = 0; 
  }


  // Set the position from which to
  // start reading/writing -- UNIX lseek
  public void seek(long position) {
    currentOffset = position;
  }    


  // Read/write bytes from the file,
  // bypassing the implicit position.

  public int readAt(byte into[], int index, int numBytes, long position) { 
    int len;

    try {
      file.seek(position);
      len = file.read(into, index, numBytes);
    } catch (IOException e) {
      return -1;
    }
    return len;
  }

  public int writeAt(byte from[], int index, int numBytes, long position) { 
    try {
      file.seek(position);
      file.write(from, index, numBytes);
    } catch (IOException e) {
      return -1;
    }
    return numBytes;
  }	



  // Read/write bytes from the file,
  // starting at the implicit position.
  // Return the # actually read/written,
  // and increment position in file.

  public int read(byte into[], int index, int numBytes) {
    int numRead = readAt(into, index, numBytes, currentOffset); 
    if (numRead > 0) currentOffset += numRead;
    return numRead;
  }

  public int write(byte from[], int index, int numBytes) {
    int numWritten = writeAt(from, index, numBytes, currentOffset); 
    if (numWritten > 0) currentOffset += numWritten;
    return numWritten;
  }

  // Return the number of bytes in the
  // file (this interface is simpler
  // than the UNIX idiom -- lseek to
  // end of file, tell, lseek back

  public long length() {
    long len;

    try {
      len = file.length();
    } catch (IOException e) {
      return -1;
    }
    return len;
  }

}
