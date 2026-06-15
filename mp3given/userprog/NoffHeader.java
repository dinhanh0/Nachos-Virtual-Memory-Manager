/* NoffHeader.java
 *     Classes defining the Nachos Object Code Format
 *
 *     Basically, we only know about three types of segments:
 *	code (read-only), initialized data, and unitialized data
 *
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * Copyright (c) 1998 Rice University.
 * All rights reserved.  See the COPYRIGHT file for copyright notice and
 * limitation of liability and disclaimer of warranty provisions.
 */
import java.io.*;


class NoffSegment {
  long virtualAddr;	        /* location of segment in virt addr space */
  long inFileAddr;	        /* location of segment in this file */
  long size;			/* size of segment */

  public NoffSegment(RandomAccessFile f) throws IOException {
    virtualAddr = convertWord(f);
    inFileAddr = convertWord(f);
    size = convertWord(f);
  }

  static int convertByte(byte raw) {
    if (raw < 0) return 256 + raw;
    else return raw;
  }

  static long convertWord(RandomAccessFile f) throws IOException {
    byte raw[] = new byte[4];
    f.read(raw);
    return convertByte(raw[3]) |
      (convertByte(raw[2]) << 8) | 
      (convertByte(raw[1]) << 16) |
      (convertByte(raw[0]) << 24);
  }

}

class NoffHeader {
  static final long noffMagic = 0xbadfad;

  NoffSegment code;		/* executable code segment */ 
  NoffSegment initData;	        /* initialized data segment */
  NoffSegment uninitData;	/* uninitialized data segment --
				 * should be zero'ed before use 
				 */
  
  // create a NoffHeader object by reading its values from a file
  public NoffHeader(RandomAccessFile f) throws IOException {

    if (NoffSegment.convertWord(f) != noffMagic) {
      Debug.println('a', "NoffHeader: Bad magic value");
      throw new IOException("bad magic value");
    }
    code = new NoffSegment(f);
    initData = new NoffSegment(f);
    uninitData = new NoffSegment(f);
  }

}
