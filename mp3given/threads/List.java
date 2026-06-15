// list.java
//
//     	Class of singly-linked lists of objects.
//
// 	A "ListElement" is allocated for each item to be put on the
//	list; it is de-allocated when the item is removed. This means
//      we don't need to keep a "next" pointer in every object we
//      want to put on a list.
// 
//     	NOTE: Mutual exclusion must be provided by the caller.
//  	If you want a synchronized list, you must use the routines 
//	in synchlist.cc.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

// The following class defines a "list element" -- which is
// used to keep track of one item on a list.  It is equivalent to a
// LISP cell, with a "car" ("next") pointing to the next element on the list,
// and a "cdr" ("item") pointing to the item on the list.
//
// [ <sigh>--make that ``"car" ("item") pointing to the item on the list,
// and "cdr" ("next") pointing to the next element on the list.''  Silly
// systems people.  :-)
//                        -RCC ]
//
// Internal data structures kept public so that List operations can
// access them directly.


// The following class defines a "list" -- a singly linked list of
// list elements, each of which points to a single item on the list.
//
// By using the "Sorted" functions, the list can be kept in sorted
// in increasing order by "key" in ListElement.

class List {
  private static ListElement free;  // freeList of ListElements 
                                    // (for efficiency)
  private static int numFree;  // number of elements in freeList
  private static final int NumFreeMax = 1024;

  private ListElement first; // Head of the list, null if list is empty
  private ListElement last;  // Last element of list

  static {
    free = null;
    numFree = 0;
  }

  //----------------------------------------------------------------------
  //	Initialize a list, empty to start with.
  //	Elements can now be added to the list.
  //----------------------------------------------------------------------

  public void List() { 
    first = last = null; 
  }

  //----------------------------------------------------------------------
  //      Append an "item" to the end of the list.
  //      
  //	Allocate a ListElement to keep track of the item.
  //      If the list is empty, then this will be the only element.
  //	Otherwise, put it at the end.
  //
  //	"item" is the thing to put on the list, it can be a pointer to 
  //		anything.
  //----------------------------------------------------------------------

  public void append(Object item) {
    ListElement element = getElement(item, 0);

    if (isEmpty()) {		// list is empty
      first = element;
      last = element;
    } else {			// else put it after last
      last.next = element;
      last = element;
    }
  }

  //----------------------------------------------------------------------
  //      Put an "item" on the front of the list.
  //      
  //	Allocate a ListElement to keep track of the item.
  //      If the list is empty, then this will be the only element.
  //	Otherwise, put it at the beginning.
  //
  //	"item" is the thing to put on the list, it can be a pointer to 
  //		anything.
  //----------------------------------------------------------------------

  public void prepend(Object item) {
    ListElement element = getElement(item, 0);

    if (isEmpty()) {		// list is empty
      first = element;
      last = element;
    } else {			// else put it before first
      element.next = first;
      first = element;
    }
  }

  //----------------------------------------------------------------------
  //      Remove the first "item" from the front of the list.
  // 
  // Returns:
  //	Pointer to removed item, NULL if nothing on the list.
  //----------------------------------------------------------------------

  public Object remove() {
    if (isEmpty()) 
      return null;
    else {
      ListElement e = sortedRemove();
      Object o = e.getItem();
      freeElement(e); 
      return o;
    }
  }


  //----------------------------------------------------------------------
  //      Returns true if the list is empty (has no items).
  //----------------------------------------------------------------------

  public boolean isEmpty() { 
    if (first == null)
      return true;
    else
      return false; 
  }

  //----------------------------------------------------------------------
  //      Insert an "item" into a list, so that the list elements are
  //	sorted in increasing order by "sortKey".
  //      
  //	Allocate a ListElement to keep track of the item.
  //      If the list is empty, then this will be the only element.
  //	Otherwise, walk through the list, one element at a time,
  //	to find where the new item should be placed.
  //
  //	"item" is the thing to put on the list, it can be any Object
  //		anything.
  //	"sortKey" is the priority of the item.
  //----------------------------------------------------------------------

  public void sortedInsert(Object item, long sortKey) {
    ListElement element = getElement(item, sortKey);
    ListElement tmp;		// keep track

    if (isEmpty()) {	// if list is empty, put
      first = element;
      last = element;
    } else if (sortKey < first.key) {	
      // item goes on front of list
      element.next = first;
      first = element;
    } else {		// look for first elt in list bigger than item
      for (tmp = first; tmp.next != null; tmp = tmp.next) {
	if (sortKey < tmp.next.key) {
	  element.next = tmp.next;
	  tmp.next = element;
	  return;
	}
      }
      last.next = element;		// item goes at end of list
      last = element;
    }
  }

  //----------------------------------------------------------------------
  //      Remove the first "item" from the front of a sorted list.
  // 
  // Returns:
  //	removed item, null if nothing on the list.
  //----------------------------------------------------------------------

  public ListElement sortedRemove() {
    ListElement element = first;
    ListElement thing;

    if (isEmpty()) 
	return null;

    thing = first;
    if (first == last) {	// list had one item, now has none 
        first = null;
	last = null;
    } else {
      first = element.next;
    }

    return thing;
  }


  //----------------------------------------------------------------------
  //      Peeks at the first "item" from the front of a sorted list.
  // 
  // Returns:
  //	key of first item, null if nothing on the list.
  //----------------------------------------------------------------------

  public Integer sortedPeek() {
    Integer i;

    if (isEmpty()) 
	return null;

    i = new Integer((int)first.getKey()); 

    return i;
  }
  
  
  // invoke the print method for each item on the list
  // (poor Java programmer's Mapcar)
  public void print() {
    for (ListElement le = first; le != null; le = le.next) {
      Printable p =  (Printable)le.item;
      p.print();
    }
  }

  // for efficiency, we maintain a freelist of ListElements to reduce
  // dynamic memory allocation
  private static ListElement getElement(Object o, long sortKey) {
    //System.out.println("get, numFree=" + numFree);
    if (free == null) {
      //System.out.println("not enough");
      return new ListElement(o, sortKey);
    } else {
      ListElement tmp = free;
      free = tmp.next;
      tmp.item = o;
      tmp.key = sortKey;
      tmp.next = null;
      numFree--;
      return tmp;
    }
  }

  // This method 
  // can be optionally used to improve efficiency
  // by returning a ListElement obtained via the sortedRemove() method
  // to the pool of free ListElements.
  public static void freeElement(ListElement e) {
    //System.out.println("free, numFree=" + numFree);
    if (numFree < NumFreeMax) {
      e.next = free;
      free = e;
      numFree++;
    }
    //else System.out.println("too many");
  }
 
  // MP3 method 
  // View the element at the index in the list
  // This method allows you to access an element in the middle of the queue
  // It returns null if you try to access in invalid element.
  public Object viewElementAt(int index)
  {
    ListElement element = first;
    int counter = 0;
    ListElement thing = null;
 
    while (element != null && counter < index)
    {
       element = element.next;
       counter++;
    }
     
    if (element == null)
       return null;
    else return element.getItem();
  }
 
  // MP3 Method
  // Remove the element at the index in the list
  // This method allows you to remove an element in the middle of the queue
  // It returns null if you try to access in invalid element.
  public Object removeElementAt(int index)
  {
     if (isEmpty())
        return null;
     
     ListElement element;
     ListElement prev;
     int counter = 1;
     if (index == 0)
     {
        if (last == first)
	   last = null;
        element = first;
	first = first.next;
     }
     else
     {
        prev = first;
	element = prev.next;
        while (element != null && counter < index)
	{
	   prev = prev.next;
	   element = element.next;
	   counter++;
	}

	if (element != null)
	{
	   prev.next = element.next;
	   if (last == element)
	   {
	      last = prev;
	   }
	}
     }
    
    if (element == null)
       return null;
    else return element.getItem();
  }
}



