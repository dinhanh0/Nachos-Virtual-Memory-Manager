// AddrSpace.java
//	Class to manage address spaces (executing user programs).
//
//	In order to run a user program, you must:
//
//	1. link with the -N -T 0 option 
//	2. run coff2noff to convert the object file to Nachos format
//		(Nachos object code format is essentially just a simpler
//		version of the UNIX executable object code format)
//	3. load the NOFF file into the Nachos file system
//		(if you haven't implemented the file system yet, you
//		don't need to do this last step)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

import java.io.*;

class AddrSpace {

  static final long UserStackSize = 2048;// increase this as necessary!
  private long BeginCodePage, EndCodeAddr, EndCodePage,
	   BeginInitDataPage, EndInitDataAddr, EndInitDataPage,
	   BeginUninitDataPage, EndUninitDataAddr, EndUninitDataPage,
	   BeginStackPage, EndStackAddr, EndStackPage;

  private NoffHeader noffH;
  TranslationEntry[] pageTable;
  RandomAccessFile executable;
  long numPages;

  //----------------------------------------------------------------------
  // 	Create an address space to run a user program.
  //	Load the program from a file "executable", and set everything
  //	up so that we can start executing user instructions.
  //
  //	Assumes that the object code file is in NOFF format.
  //
  //	First, set up the translation from program memory to physical 
  //	memory.  For now, this is really simple (1:1), since we are
  //	only uniprogramming, and we have a single unsegmented page table
  //
  //	"executable" is the file containing the object code to 
  //    load into memory
  //----------------------------------------------------------------------

  public AddrSpace(RandomAccessFile executable) throws IOException {
    this.executable = executable;
    long size;
    
    noffH = new NoffHeader(executable);

    // how big is address space?
    size = noffH.code.size + noffH.initData.size + noffH.uninitData.size 
                       + UserStackSize;	// we need to increase the size
					// to leave room for the stack
    numPages = (int)(size / Machine.PageSize);
    if (size % Machine.PageSize > 0) numPages++;

    size = numPages * Machine.PageSize;
    
    BeginCodePage = noffH.code.virtualAddr/Machine.PageSize;
    EndCodeAddr = noffH.code.size + noffH.code.virtualAddr;
    EndCodePage = EndCodeAddr/Machine.PageSize;
    BeginInitDataPage = noffH.initData.virtualAddr/Machine.PageSize;
    EndInitDataAddr = noffH.initData.virtualAddr + noffH.initData.size;
    EndInitDataPage = EndInitDataAddr/Machine.PageSize;
    BeginUninitDataPage = noffH.uninitData.virtualAddr/Machine.PageSize;
    EndUninitDataAddr = noffH.uninitData.virtualAddr + noffH.uninitData.size;
    EndUninitDataPage = EndUninitDataAddr/Machine.PageSize;

    if (EndCodePage > EndInitDataPage)
      numPages = (EndCodePage > EndUninitDataPage) ?
                          EndCodePage : EndUninitDataPage;
    else
      numPages = (EndInitDataPage > EndUninitDataPage) ?
                          EndInitDataPage : EndUninitDataPage;


    // Allocate space for stack segment
    BeginStackPage = ++numPages;
    EndStackPage = BeginStackPage + UserStackSize/Machine.PageSize;

    numPages = EndStackPage + 1;


/*  marked for mp3 , to allow virtual memory 

    Debug.ASSERT((numPages <= Machine.NumPhysPages),// check we're not trying
		 "AddrSpace constructor: Not enough memory!");
                                                // to run anything too big --
						// at least until we have
						// virtual memory

*/
    Debug.println('a', "Initializing address space, numPages=" 
		+ numPages + ", size=" + size);

    // first, set up the translation 
    pageTable = new TranslationEntry[(int)numPages];
    for (int i = 0; i < numPages; i++) {
      pageTable[i] = new TranslationEntry();
    }

    // Init the page table
    int i;
    for (i = (int)BeginInitDataPage; i <= EndInitDataPage; i++)
    {
      pageTable[i].virtualPage = i;
      pageTable[i].valid = false;
      pageTable[i].legal = true;
      pageTable[i].readOnly = false;
    }

    for (i = (int)BeginUninitDataPage; i <= EndUninitDataPage; i++)
    {
      pageTable[i].virtualPage = i;
      pageTable[i].valid = false;
      pageTable[i].legal = true;
      pageTable[i].readOnly = false;
    }

    for (i = (int)BeginCodePage; i <= EndCodePage; i++)
    {
      pageTable[i].virtualPage = i;
      pageTable[i].valid = false;
      pageTable[i].legal = true;
      pageTable[i].readOnly = false;
    }

    for (i = (int)BeginStackPage; i <= EndStackPage; i++)
    {
      pageTable[i].virtualPage = i;
      pageTable[i].valid = false;
      pageTable[i].legal = true;
      pageTable[i].readOnly = false;
    }


    if (BeginCodePage == EndInitDataPage || BeginCodePage == EndUninitDataPage)
      pageTable[(int)BeginCodePage].readOnly = false;
    if (EndCodePage == BeginInitDataPage || EndCodePage == BeginUninitDataPage)
      pageTable[(int)EndCodePage].readOnly = false;

    // References to other pages are all invalid
    for (i = 0; i < numPages; i++)
      if (!(i >= BeginCodePage && i <= EndCodePage) &&
          !(i >= BeginInitDataPage && i <= EndInitDataPage) &&
          !(i >= BeginUninitDataPage && i <= EndUninitDataPage)&&
          !(i >= BeginStackPage && i <= EndStackPage)
         )
      {
        pageTable[i].virtualPage = i;
        pageTable[i].valid = false;
        pageTable[i].legal = false;
        pageTable[i].readOnly = false;
      }
  }


  //----------------------------------------------------------------------
  // InitRegisters
  // 	Set the initial values for the user-level register set.
  //
  // 	We write these directly into the "machine" registers, so
  //	that we can immediately jump to user code.  Note that these
  //	will be saved/restored into the currentThread.userRegisters
  //	when this thread is context switched out.
  //----------------------------------------------------------------------

  void initRegisters() {
    int i;
    
    for (i = 0; i < Machine.NumTotalRegs; i++)
      Machine.writeRegister(i, 0);

    // Initial program counter -- must be location of "Start"
    Machine.writeRegister(Machine.PCReg, 0);	

    // Need to also tell MIPS where next instruction is, because
    // of branch delay possibility
    Machine.writeRegister(Machine.NextPCReg, 4);

   // Set the stack register to the end of the address space, where we
   // allocated the stack; but subtract off a bit, to make sure we don't
   // accidentally reference off the end!
    Machine.writeRegister(Machine.StackReg, 
			  (int)numPages * Machine.PageSize - 16);
    Debug.println('a', "Initializing stack register to " +
		(numPages * Machine.PageSize - 16));
  }

  //----------------------------------------------------------------------
  // SaveState
  // 	On a context switch, save any machine state, specific
  //	to this address space, that needs saving.
  //
  //	For now, nothing!
  //----------------------------------------------------------------------

  void saveState() {}

  //----------------------------------------------------------------------
  // RestoreState
  // 	On a context switch, restore the machine state so that
  //	this address space can run.
  //
  //      For now, tell the machine where to find the page table.
  //----------------------------------------------------------------------

  void restoreState() {
    Machine.pageTable = pageTable;
    Machine.pageTableSize = (int)numPages;
  }

void readSourcePage(byte[] buffer, int virtualPage)
{
  long virtualAddr = virtualPage*Machine.PageSize;
  long displacement, numBytes, seek, startAddr;

  if ((virtualPage > BeginStackPage && virtualPage < EndStackPage) ||
      (virtualPage > BeginUninitDataPage && virtualPage < EndUninitDataPage)) 
  {
    return;
  }

  try{

  if (virtualPage > BeginCodePage && virtualPage < EndCodePage)
  {
    seek = noffH.code.inFileAddr + virtualAddr - noffH.code.virtualAddr;
    //executable.readAt(buffer, 0, Machine.PageSize, seek);
    executable.seek(seek);
    executable.read(buffer, 0, Machine.PageSize);
    return;
  }
 if (virtualPage > BeginInitDataPage && virtualPage < EndInitDataPage)
  {
    seek = noffH.initData.inFileAddr + virtualAddr - noffH.initData.virtualAddr;
    //executable.readAt(buffer, 0, Machine.PageSize, seek);
    executable.seek(seek);
    executable.read(buffer, 0,(int)Machine.PageSize);
    return;
  }


  if (virtualPage == BeginCodePage)
  {
    displacement = noffH.code.virtualAddr%Machine.PageSize;
    if (EndCodePage > BeginCodePage)
      numBytes = Machine.PageSize - displacement;
    else
      numBytes = noffH.code.size;
    seek = noffH.code.inFileAddr;
    //executable.readAt(buffer, displacement, numBytes, seek);
    executable.seek(seek);
    executable.read(buffer, (int)displacement, (int)numBytes);
  }
  else if (virtualPage == EndCodePage)
  {
    displacement = 0;
    numBytes = EndCodeAddr%Machine.PageSize + 1;
    seek = noffH.code.inFileAddr + virtualAddr - noffH.code.virtualAddr;
    executable.seek(seek);
    executable.read(buffer, (int)displacement, (int)numBytes);
  }


  if (virtualPage == BeginInitDataPage)
  {
    displacement = noffH.initData.virtualAddr%Machine.PageSize;
    if (EndInitDataPage > BeginInitDataPage)
      numBytes = Machine.PageSize - displacement;
    else
      numBytes = noffH.initData.size;
    seek = noffH.initData.inFileAddr;
    //executable.readAt(buffer, displacement, numBytes, seek);
    executable.seek(seek); 
    executable.read(buffer, (int)displacement, (int)numBytes);
  }
 else if (virtualPage == EndInitDataPage)
  {
    displacement = 0;
    numBytes = EndInitDataAddr%Machine.PageSize + 1;
    seek = noffH.initData.inFileAddr + virtualAddr - noffH.initData.virtualAddr;
    //executable.readAt(buffer, displacement, numBytes, seek);
    executable.seek(seek);
    executable.read(buffer, (int)displacement, (int)numBytes);
  }
  }catch(IOException e){
	System.out.println("Exception in access excutable.");
  }

  return;
}


}
