//////////////////////////////////////////////////////////////////////
//    MemManager.java
//    CS323 MP3
/////////////////////////////////////////////////////////////////////


public class MemManager{

    public static final int NumSwapPages=4096;
  
    /* Replacement Policy */
    public static final int ESC = 0;
    public static final int LRU = 1;
    public static final int FIFO = 2;

    /* private members */
    private List pageBuffer;     // Page Buffer queue
    private int buffersize;

    private BitMap coreFreeMap;
    private BitMap swapFreeMap;
    private BitMap swapValidMap;
    private TranslationEntry [] coreOwners;
    private TranslationEntry [] swapOwners;

    private int[] queue;      //for ESC and FIFO
    private int queueCounter; //revolving counter for ESC and FIFO

    private OpenFile swapfile;
    private Semaphore mutex;
    private Lock test;

    private int hbits;
    private int bitMask;
    private int lruUseBit;
    private int counter;          //for LRU, 
				  //to track the start position of each search 

    private int[] history;        //for LRU
    private Timer history_timer;  //for LRU

    private int policy;
    private int formatCounter;

    
    ///////////////////////////////////////////////////////////////////
    //     CoreFreeMap: A frame is said to be free only if it does not belong
    //     the page buffer, and does not belong to any process.
    //
    //     CoreOwners: If a frame has the contents of some process's page
    //     then its entry in core owners points to the corresponding page
    //     table entry.
    ///////////////////////////////////////////////////////////////////
    
    MemManager( int pbuffer_in , int hbits_in )
    {
        formatCounter = 0;  //display variable 

        // WARNING: coreFreeMap (and like wise swapFreeMap) bits are CLEAR 
        // when free, not set when free; 
        coreFreeMap = new BitMap(Machine.NumPhysPages);
        coreOwners = new TranslationEntry[Machine.NumPhysPages];
    
        swapFreeMap =  new BitMap(NumSwapPages);
        swapValidMap = new BitMap(NumSwapPages);
        swapOwners =   new TranslationEntry[NumSwapPages];
    
        int physFrame;
        for (physFrame=0; physFrame<Machine.NumPhysPages; physFrame++)
          coreOwners[physFrame] = null;
    
        int swapFrame;
        for (swapFrame=0; swapFrame<NumSwapPages; swapFrame++)
          swapOwners[swapFrame] = null;
    
        mutex=new Semaphore("mutex for memory manager data structures",1);
        
        Debug.ASSERT(Nachos.fileSystem != null);
        Debug.ASSERT(Nachos.fileSystem.create("nachos.bs",0)); 
        swapfile=Nachos.fileSystem.open("nachos.bs");
        Debug.ASSERT(swapfile!=null);
        
        buffersize = pbuffer_in;
	//Initializes the page buffer
        if ( buffersize > 0 ) {
          Debug.printf('p',"Pagebuffering enabled with %d pages\n", new Integer(buffersize) );
          pageBuffer=new List();
          for(int bufferFrame=0; bufferFrame<buffersize; bufferFrame++) {
	      int page = Machine.NumPhysPages - bufferFrame - 1;
    	      pageBuffer.append(new Integer(page));
    	      coreFreeMap.mark(page);
    	  }
        } else {
          Debug.println('p',"Page Buffering Disabled\n");
        }
        
    
        hbits = hbits_in;
        if ( hbits > 0 )
        {
           Debug.printf('p',"LRU enabled with %d bits\n", new Integer(hbits) );
           bitMask = ( 1 << hbits ) - 1;  // Bit Mask (also Max Value)
	   lruUseBit = 1 << (hbits - 1);  //value to shift in if use bit is set
           history = new int [Machine.NumPhysPages];
	   
	   /* initialize time handler -- this will call recordHistory at */ 
	   /*  regular intervals                                         */
	   PageTimer pageTimer = new PageTimer();
           history_timer = new Timer( pageTimer, false, false );
           policy = LRU;
        }
        else if( hbits < 0 )
        {
           Debug.printf('p',"FIFO enabled\n", new Integer(hbits) );
   	   //set up FIFO queue -- you need to fill the queue appropriately
           queue = new int[Machine.NumPhysPages - buffersize];
	   queueCounter = 0;
           policy = FIFO;
        } else {
           Debug.println('p',"Enhanced Second Chance enabled");
	   //set up queue for ESC scheduling
           queue = new int[Machine.NumPhysPages - buffersize];
	   queueCounter = 0;
           policy = ESC;
        }
	//clear the counter for LRU
        counter = 0;
    }
    
    
    int memAvail()
    {
        return ( coreFreeMap.numClear()+swapFreeMap.numClear() );
    }
     
    
    void clear(TranslationEntry[] pageTable, int numPages)
    {
      int swapFrame;
    
      for (int i = 0; i < numPages; i ++)
        if (pageTable[i].legal)
        {
          if (pageTable[i].valid) {
    	    // Free physical frames
    	    coreFreeMap.clear(pageTable[i].physicalPage);
    	    coreOwners[pageTable[i].physicalPage] = null; 
          }
          else if ((swapFrame = swapSearch(pageTable[i])) != -1)
          {
    	    // Free swap frames.
    	    swapFreeMap.clear(swapFrame);
    	    swapValidMap.clear(swapFrame);
    	    swapOwners[swapFrame]=null;
          }
       }
    }
     
    //////////////////////////////////////////////////////////////////////
    //  Finds a free frame in the main memory. See the definition above of 
    //  a free frame.
    //////////////////////////////////////////////////////////////////////
    int locateFirst()
    {
        return coreFreeMap.find();
    }
    
    /////////////////////////////////////////////////////////////////////
    // Determe whether a target frame is in the page buffer. If in the 
    // buffer, return true. 
    ////////////////////////////////////////////////////////////////////
    boolean InBuffer( int target ) {
       for (int i=0; i<buffersize; i++)
       {
          Integer f = (Integer)pageBuffer.viewElementAt(i);
	  if (f.intValue() == target)
	  {
	     return true;
	  }
       }
       return false;
    }
    
    //////////////////////////////////////////////////////////////////////
    // MP3: Include your code here
    // Make a frame free using the enhanced second chance page replacement 
    // algorithm, FIFO, N bit LRU replacement.
    // Return the number of the frame that will be paged out
    //////////////////////////////////////////////////////////////////////
    int makeFreeFrame() 
    {
      //MP3 choose a frame from memory to replace based on the algorithm you
      //are implenting.  Be careful not to select a page that is in the page 
      //buffer.  Use the function InBuffer(int target) to check this. 
      //Replace this code entirely but you might want to use some of in your
      //implementation
      
      int victim = counter;

      while (InBuffer(victim))
      {
         //You might notice that without changing the code, FIFO with 
	 //no page buffers will work.  However, when you add page buffers, it
	 //will not.  So I would recommend using the queue and queueCounter for
	 //both cases.  
	 victim = (victim + 1) % Machine.NumPhysPages;
      }
      
      //example revolving counter
      counter = (victim + 1) % Machine.NumPhysPages;
      
      return victim;
    }
    
    
    //////////////////////////////////////////////////////////////////////
    // MP3: Include your code here
    // Implements the page buffering policy described above
    // PTEntry is the page that needs to page in.
    //////////////////////////////////////////////////////////////////////
    void faultIn(TranslationEntry PTEntry) 
    {
      int physPage, bufFrame;
      physPage = coreFreeMap.find();
      
      //MP3 change this function when you implement page buffering and when you
      //need to update the FIFO or ESC queue.

      if( physPage >= 0 )
      { // A free frame is available from main memory.
            
	//MP3 Update the ESC or FIFO queue accordingly here

    	Debug.println('p', "Free Mem, not in page buffer.  ");
    	Debug.printf('p', "Pagein to physical page %d\n", new Integer(physPage));
	//brings the page referenced by PTEntry into physPage in physical memory
    	pageIn( PTEntry, physPage );
      }
      else {
        Debug.println('p', "non-buffered start\n");
        
    	// No main memory free so choose a victim to replace.
    	physPage = makeFreeFrame();
   
        //write the replaced page to the backing store if it is dirty
    	if( coreOwners[physPage].dirty ) {
    	  Debug.printf('p', "Paging out %d \n", new Integer(physPage) );
    	  pageOut( physPage );
    	}
    	else {
    	  coreOwners[physPage].valid = false;
    	}
    	
    	Debug.printf('p', "Sticking it in %d frame\n", new Integer(physPage) );
	
	//page in the new page into main memory
    	pageIn( PTEntry, physPage );
    	
        Debug.println('p', "non-buffered finish\n");
      }
      
    }
    
    ////////////////////////////////////////////////////
    // Interrupt Service routine for maintaining history data
    // MP3 :  Your code here.
    // Note: arg is a dummy argument that is meaningless and should not be used.
    /////////////////////////////////////////////////////
    void recordHistory( int arg )
    {
      //MP3 update the history counter for LRU here.  This function will only
     //get called in LRU.  Remember to update the history for pages currently
     //in the page buffer since their history is still valid.
    }
    
    
    /////////////////////////////////////////////////////////////////
    // Reads in the appropriate page into the physFrame mentioned
    /////////////////////////////////////////////////////////////////
    void pageIn(TranslationEntry PTEntry, int physFrame)
    {
      int  swapFrame;
      int  x;
      byte[] my_buffer = new byte[Machine.PageSize];
    
      // Search swap file for PTEntry.
      swapFrame = swapSearch( PTEntry );
    
      if( swapFrame >= 0 )
      { // Frame found.  Read it in from BS to temporary buffer.
        swapfile.readAt( my_buffer,0,Machine.PageSize, Machine.PageSize * swapFrame);
      }
      else
      { // Frame not found.  Read it from the source file.
        NachosThread.thisThread().space.readSourcePage( my_buffer, PTEntry.virtualPage);
      }
    
      PTEntry.physicalPage = physFrame;
      PTEntry.valid = true;
      coreOwners[physFrame] = PTEntry;
    
      for( x=0; x < Machine.PageSize; x++ )
        Machine.writeMem( PTEntry.virtualPage * Machine.PageSize + x, 1, (int)my_buffer[x] );
    
      PTEntry.use   = false;
      PTEntry.dirty = false;

      //MP3 LRU history update - page has not been used yet
      if (policy == LRU)
      {
         history[physFrame] = 0;
      }
    
      return;
    }
    
    
    /////////////////////////////////////////////////////////////////
    // Reads the appropriate page into a swap frame. 
    /////////////////////////////////////////////////////////////////
    void pageOut(int physFrame) 
    {
      TranslationEntry victim_te = null;
      int[] my_buffer = new int[Machine.PageSize];
      byte[] cbuf = new byte[Machine.PageSize];
      int  swapFrame;
      int  check;
      int  x;
    
      victim_te = coreOwners[ physFrame ];
    
      // Copy memory contents to temporary kernel buffer.
      // Make valid so translation works.
      victim_te.valid = true;
      try {
      for(x = 0; x < Machine.PageSize; x++)
        my_buffer[x] = Machine.readMem( victim_te.virtualPage * Machine.PageSize + x, 1);
      } catch (Exception e)
      {
         System.out.println("Exception reading memory");
      }
      victim_te.valid = false;
    
      // Convert Int array to byte array.  This avoids endian problems.
      for( x = 0; x < Machine.PageSize; x++ )
        cbuf[x] = (byte)(my_buffer[x]);
    
      // Search for previously owned page in the swap file.
      for( swapFrame = 0;
           ( (swapFrame < NumSwapPages) && (victim_te != swapOwners[swapFrame]) );
           swapFrame++ );
    
      if( swapFrame != NumSwapPages )
        { // Previous page found.
          swapFreeMap.mark(swapFrame);
        }
      else
        { // Previous page not found in swap file, so allocate one.
          swapFrame = swapFreeMap.find();
          Debug.ASSERT( swapFrame >= 0 );
          swapOwners[ swapFrame ] = victim_te;
        }
    
      // Write Page to Backing store, because always dirty. 
      check = swapfile.writeAt( cbuf,0, Machine.PageSize, Machine.PageSize * swapFrame );
      Debug.ASSERT( check == Machine.PageSize );
    
      // Clear dirty bit.
      victim_te.dirty = false;
    
      // It may or may not have been changed by the paging scheme, so just for 
      //  good measure.
      victim_te.valid = false;
    
      return;
    }
    
    
    void pageFaultExceptionHandler(int BadVPage) 
    {
      mutex.P();
    
      if (BadVPage >= Machine.pageTableSize || Machine.pageTable[BadVPage].legal == false)
      {
        System.out.println("Illegal memory access by thread : "+NachosThread.thisThread().getName());
        System.out.println("Halting the thread : "+NachosThread.thisThread().getName());
    
        mutex.V();
        // call SC_Exit syscall exception
        Machine.writeRegister(2, Nachos.SC_Exit);
        // The exit status
        Machine.writeRegister(4, 0);
        Nachos.exceptionHandler(Machine.SyscallException);
        // The current execution of this method will never get beyond this
        // position. The thread in whose context the method is executing is
        // halted. This is the reason why the mutex is released right here.
      }
    
      formatCounter++;
      if (formatCounter % 6 == 0)
      {
 	 System.out.println("in = " + BadVPage + ", ");
      }
      else
      {
 	 System.out.print("in = " + BadVPage + ", ");
      }
      faultIn(Machine.pageTable[BadVPage]);
      
      mutex.V();
    }
    
    
    int swapSearch(TranslationEntry PTEntry) 
    {
      int swapFrame;
    
      for (swapFrame=0; swapFrame<NumSwapPages; swapFrame++)
        if (swapOwners[swapFrame]==PTEntry) 
    	return swapFrame;
    
      return -1;
    }

    void spaces(int i)
    {
       if ((i % 10) == i)
          System.out.print(" ");

    }

   void display() { 
     int i, j;

     System.out.println ("\n\nPHYSICAL MEMORY DUMP:");
     System.out.println("F = Frame Number");
     System.out.println("V = Virtual Page Number");
     if (buffersize > 0)
     {
       System.out.println("B = In Page Buffer");
     }
     System.out.print("F\t");
     for (i=0; i<Machine.NumPhysPages; i++)
     {
        spaces(i);
	System.out.print(i +" ");
     }
     System.out.println();
     
     System.out.print("V\t");
     for (i=0; i<Machine.NumPhysPages; i++)
     {
	if (coreFreeMap.test(i))
	{
	   if (coreOwners[i] != null)
	   {
	     spaces(coreOwners[i].virtualPage);
	     System.out.print(coreOwners[i].virtualPage + " ");
	   }
	   else
	     System.out.print(" E "); //empty frame
	}
	else
	{
	   System.out.print(" E "); //empty frame
	}
     }
     System.out.println();

     if (buffersize > 0) 
     {
	System.out.print("B\t");
	for (i=0; i<Machine.NumPhysPages; i++)
	{
	   if (coreFreeMap.test(i))
	   {
	       for (j = 0; j < buffersize; j++)
	       {
		  Integer page = (Integer)pageBuffer.viewElementAt(j);
		  if (page.intValue() == i)
		  {
		     break;
		  }
	       }
	       if (j < buffersize)
	       {
		  System.out.print(" Y "); //in buffer
	       }
	       else
	       {
		  System.out.print(" N "); //not in buffer
	       }
	   }
	   else
	   {
	      System.out.print(" N "); //not in buffer
	   }
	}
	System.out.println();
     }
     
     System.out.println ("\n\nSWAP FILE DUMP:");
     System.out.println("Format is [<swap frame>/<virtual page number>]");
     j=0;
     for (i=0; i<NumSwapPages; i++)
       if (swapFreeMap.test(i)) {
	 Debug.ASSERT(swapOwners[i] != null);
	 j++;
	 if (j % 6 == 0)
	 {
	    System.out.println("["+ i + " / " + swapOwners[i].virtualPage + "]");
	 }
	 else
	 {
	    System.out.print("["+ i + " / " + swapOwners[i].virtualPage + "] ");
	 }
       }

     System.out.println ("\n\nTotal free pages: "+memAvail());
     System.out.println ("MemoryManager dump complete.");
     formatCounter = 0;
   }

} //The end of definition of MemManager
        
    
