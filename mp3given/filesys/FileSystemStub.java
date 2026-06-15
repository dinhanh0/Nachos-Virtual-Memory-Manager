// FileSystemStub.java
//	Class to represent the Nachos file system.
//
//	A file system is a set of files stored on disk, organized
//	into directories.  Operations on the file system have to
//	do with "naming" -- creating, opening, and deleting files,
//	given a textual file name.  Operations on an individual
//	"open" file (read, write, close) are to be found in the OpenFile
//	class (openfile.h).
//
//	We define two separate implementations of the file system. 
//	This ("Stub") version just re-defines the Nachos file system 
//	operations as operations on the native UNIX file system on the machine
//	running the Nachos simulation.  This is provided in case the
//	multiprogramming and virtual memory assignments (which make use
//	of the file system) are done before the file system assignment.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

import java.io.*;

class FileSystemStub implements FileSystem {

  public FileSystemStub(boolean format) {}

  public boolean create(String name, long initialSize) { 
    FileOutputStream fsFile;

    try {
      fsFile = new FileOutputStream(name);
      fsFile.close();    
    } catch (IOException e) {
      return false;
    }

    return true; 
  }

  public OpenFile open(String name) {
    RandomAccessFile file;

    try {
      file = new RandomAccessFile(name, "rw");
    }
    catch (IOException e) {
      return null;
    }

    return new OpenFileStub(file);
  }

  public boolean remove(String name) { 
    File file;

    file = new File(name);
    return file.delete();
  }
  
}
