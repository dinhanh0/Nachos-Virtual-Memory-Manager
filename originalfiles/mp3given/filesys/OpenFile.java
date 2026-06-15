// OpenFile.java
//	Class for opening, closing, reading and writing to 
//	individual files.  The operations supported are similar to
//	the UNIX ones -- type 'man open' to the UNIX prompt.
//
//	There are two implementations.  One is a "STUB" that directly
//	turns the file operations into the underlying UNIX operations.
//	(cf. comment in filesys.h).
//
//	The other is the "real" implementation, that turns these
//	operations into read and write disk sector requests. 
//	In this baseline implementation of the file system, we don't 
//	worry about concurrent accesses to the file system
//	by different threads -- this is part of the assignment.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

public interface OpenFile {
  // Set the position from which to
  // start reading/writing -- UNIX lseek
  public void seek(long position);
  // Read/write bytes from the file,
  // bypassing the implicit position.
  public int readAt(byte into[], int index, int numBytes, long position);
  public int writeAt(byte from[], int index, int numBytes, long position);

  // Read/write bytes from the file,
  // starting at the implicit position.
  // Return the # actually read/written,
  // and increment position in file.
  public int read(byte into[], int index, int numBytes);
  public int write(byte from[], int index, int numBytes);

  // Return the number of bytes in the
  // file (this interface is simpler
  // than the UNIX idiom -- lseek to
  // end of file, tell, lseek back
  public long length();

}
