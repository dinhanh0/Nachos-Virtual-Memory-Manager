// SynchList.cc
//	Synchronized List class.
//
//	Implemented by surrounding the List abstraction
//	with synchronization routines.
//
// 	Implemented in "monitor"-style -- surround each procedure with a
// 	lock acquire and release pair, using condition signal and wait for
// 	synchronization.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.



// The following class defines a "synchronized list" -- a list for which:
// these constraints hold:
//	1. Threads trying to remove an item from a list will
//	wait until the list has an element on it.
//	2. One thread at a time can access list data structures

class SynchList extends List {

  private Lock lock;	// enforce mutual exclusive access to the list
  private Condition listEmpty;  // wait in Remove if the list is empty


  //----------------------------------------------------------------------
  //	Allocate and initialize the data structures needed for a 
  //	synchronized list, empty to start with.
  //	Elements can now be added to the list.
  //----------------------------------------------------------------------

  public SynchList() {
    lock = new Lock("SynchList lock");
    listEmpty = new Condition("SynchList condition");
  }


  //----------------------------------------------------------------------
  //      Append an "item" to the end of the list.  Wake up anyone
  //	waiting for an element to be appended.
  //
  //	n"item" is the thing to put on the list, it can be a pointer to 
  //		anything.
  //----------------------------------------------------------------------

  public void append(Object item) {
    lock.acquire();
    super.append(item);
    listEmpty.signal(lock);    	// wake up a waiter, if any
    lock.release();
  }

  //----------------------------------------------------------------------
  //      Put an "item" on the front of the list. Wake up anyone
  //	waiting for an element to be appended.  
  //      
  //	"item" is the thing to put on the list, it can be a pointer to 
  //		anything.
  //----------------------------------------------------------------------

  public void prepend(Object item) {
    lock.acquire();    
    super.prepend(item);
    listEmpty.signal(lock);    	// wake up a waiter, if any
    lock.release();
  }


  //----------------------------------------------------------------------
  //    Remove an "item" from the beginning of the list.  Wait if
  //	the list is empty.
  // Returns:
  //	The removed item. 
  //----------------------------------------------------------------------

  public Object remove() {
    ListElement e = sortedRemove();
    Object o = e.getItem();
    freeElement(e);
    return o;
  }


  //----------------------------------------------------------------------
  //    Insert an "item" into a list, so that the list elements are
  //	sorted in increasing order by "sortKey".
  //    Wakeup anyone waiting for an item to be inserted
  
  //	"item" is the thing to put on the list, it can be any Object
  //		anything.
  //	"sortKey" is the priority of the item.
  //----------------------------------------------------------------------

  public void sortedInsert(Object item, long sortKey) {
    lock.acquire();    
    super.sortedInsert(item, sortKey);
    listEmpty.signal(lock);    	// wake up a waiter, if any
    lock.release();
  }


  //----------------------------------------------------------------------
  //      Remove the first "item" from the front of a sorted list.
  //      Wait if the list is empty
  // 
  // Returns:
  //	removed item, null if nothing on the list.
  //----------------------------------------------------------------------

  public ListElement sortedRemove() {
    lock.acquire();    
    while (isEmpty()) {
      listEmpty.wait(lock);      // wait until list isn't empty
    }
    ListElement o = super.sortedRemove();
    lock.release();
    return o;
  }

}

