// ListTest.java
//
// Copyright (c) 1998 Rice University.

//----------------------------------------------------------------------
// SimpleList object
//
//	"which" is simply a number identifying the thread, for debugging
//	purposes.
//----------------------------------------------------------------------

class SimpleList implements Runnable {
  private int which;
  private List list1, list2;

  public SimpleList(int w, List l1, List l2) {
    which = w;
    list1 = l1;
    list2 = l2;
  }

  public void run() {
    for (int num = 0; num < 10; num++) {
	System.out.print("*** thread " + which + " looped " + num + " times\n");
	if (which % 2 == 0) {
	  list1.append("String " + num);	
	  list2.sortedInsert("String " + num, 
			     Math.round(Math.random() * 1000));
	}
	else {
	  ListElement elem;

	  System.out.println((String)list1.remove());
	  elem = list2.sortedRemove();
	  System.out.println((String)elem.getItem() + 
			     " key: " + elem.getKey());
	}
    }
  }

}


//----------------------------------------------------------------------
// ListTest object
//----------------------------------------------------------------------

class ListTest {
  private static NachosThread t1,t2;
  private static SimpleList o1, o2;
  private static List l1,l2,sl1,sl2;

  public static void main(String argv[]) {

    Debug.init("");
    Debug.println('t', "Entering SimpleTest, phase 1");

    l1 = new List();
    l2 = new List();
    sl1 = new SynchList();
    sl2 = new SynchList();
    t1 = new NachosThread("Testthread 1");
    t2 = new NachosThread("Testthread 2");
    o1 = new SimpleList(0,l1,l2);
    o2 = new SimpleList(1,l1,l2);

    o1.run();
    o2.run();

    System.out.println("Entering SimpleTest, phase 2");    

    o1 = new SimpleList(1,sl1,sl2);
    o2 = new SimpleList(2,sl1,sl2);

    t1.fork(o1);
    t2.fork(o2);

    Scheduler.start();
  }
}
