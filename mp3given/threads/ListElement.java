// ListElement.java
//
//     	Class of list elements.
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
// Internal data structures kept public so that List operations can
// access them directly.


class ListElement {
  ListElement next;		// next element on list, 
                                // NULL if this is the last
  long key;		    	// priority, for a sorted list
  Object item; 	          	// pointer to item on the list

  //----------------------------------------------------------------------
  // 	Initialize a list element, so it can be added somewhere on a list.
  //
  //	"item" is the item to be put on the list.  It can be any
  //		subclass of Object.
  //	"sortKey" is the priority of the item, if any.
  //----------------------------------------------------------------------

  public ListElement(Object o, long sortKey) { 
    item = o;
    key = sortKey;
    next = null;	// assume we'll put it at the end of the list 
  }

  public long getKey() {
    return key;
  }

  public Object getItem() {
    return item;
  }
};
