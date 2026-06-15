// TranslationEntry.java
//
// The following class defines an entry in a translation table -- either
// in a page table or a TLB.  Each entry defines a mapping from one 
// virtual page to one physical page.
// In addition, there are some extra bits for access control (valid and 
// read-only) and some bits for usage information (use and dirty).
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

class TranslationEntry {
  public int virtualPage;  	// The page number in virtual memory.
  public int physicalPage;  	// The page number in real memory 
                                // (relative to the start of "mainMemory"
  public boolean valid;  
  // If this bit is not set, the translation is ignored.
  // (In other words, the entry hasn't been initialized.)
  public boolean readOnly;
  // If this bit is set, the user program is not allowed
  // to modify the contents of the page.
  public boolean use; 
  // This bit is set by the hardware every time the
  // page is referenced or modified.
  public boolean dirty;
  // This bit is set by the hardware every time the
  // page is modified.
  public boolean legal;
  //if not set then a reference to the page is illegal
  //if referrenced, appropriate process is aborted
};


