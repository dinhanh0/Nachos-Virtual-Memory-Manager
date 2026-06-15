/********************************************************************************
/SynchTest.java                                                                 /
/                                                                               /
/Tests the synchronization primitives for mp2 and the bounded buffer            /
/implementation                                                                 /
/*******************************************************************************/

//BOUNDED BUFFER TEST CLASSES
//Producer class Bounded Buffer Problem 
class Producer implements Runnable
{
   char c;

   public Producer(char c)
   {
      this.c = c;
   }

   public void run()
   {
      Interrupt.oneTick();
      SynchTest.buffer.produce(c);
   }
}

//Consumer class for Bounded Buffer Problem
class Consumer implements Runnable
{
   public void run()
   {
      Interrupt.oneTick();
      SynchTest.buffer.consume();
   }
}

//READER WRITER TEST CLASSES
//Reader class for reader writer testing.  From Page 194 Figure 7.19
//ported to Nachos
class Reader implements Runnable
{
   private int readerNum;
   private Database server;

   public Reader(int r, Database db)
   {
      readerNum = r;
      server = db;
   }
   
   public void run()
   {
      int c;

      for (int i=0; i<4; i++)
      {
	 Interrupt.oneTick();
         System.out.println("reader " + readerNum + " is sleeping");
	 Database.napping();
	 
	 Interrupt.oneTick();

         System.out.println("reader " + readerNum + " wants to read");
         c = server.startRead();
	 
         System.out.println("reader " + readerNum + " is reading.  Count = " + c);
	 Interrupt.oneTick();
	 Database.napping();
         
	 Interrupt.oneTick();
	 c = server.endRead();
	 System.out.println("reader " + readerNum + " is done reading.  Count = " + c);

      }
   }
}

//Writer class for reader writer testing.  From Page 195 Figure 7.20
//ported to Nachos
class Writer implements Runnable
{
   private int writerNum;
   private Database server;

   public Writer(int w, Database db)
   {
      writerNum = w;
      server = db;
   }
   
   public void run()
   {
      for (int i=0; i<4; i++)
      {
	 Interrupt.oneTick();
         System.out.println("writer " + writerNum + " is sleeping");
	 Database.napping();

	 Interrupt.oneTick();
         System.out.println("writer " + writerNum + " wants to write");
         server.startWrite();
	 
         System.out.println("writer " + writerNum + " is writing");
	 Interrupt.oneTick();
	 Database.napping();
         
	 Interrupt.oneTick();
	 server.endWrite();
	 System.out.println("writer " + writerNum + " is done writing");

      }
   }
}


//class for testing boundedBuffer  and readers writers
public class SynchTest
{
   static final int BUFFER_SIZE=7;
   static BoundedBuffer buffer;
   static Lock l;
   static int soFar;
   static char out[];

   public static void addToOutputString(char c)
   {
     l.acquire();
     out[soFar++/2] = c;
     l.release();
      
   }

   //boundedBufferTest
   //Runs the producer consumer algorithm to test the bounded buffer implementation for
   //mp2
   public static void boundedBufferTest()
   {
      NachosThread t;
      String message = new String("4I2fh 8y3otul 9c3alnb 3r1efajdc st3h2i3st,a 1tghwesnf rylopui oh3avvces splansosyeyda etshzee 3b5oiusnad4esds gbgugfnfsesri ntleasstd.w 7 oCootnvgarlaptquwlearttiyosnesw!a! "); 
      String name;
      int producers, consumers;
      Producer p;
      Consumer c;
      l = new Lock("output lock");
      out = new char[message.length()/2+1];
      soFar = 0;


      producers = consumers = message.length();
      buffer = new BoundedBuffer(BUFFER_SIZE);
      

      for (int i=1; i<=producers; i++)
      {
        name = new String("Producer"+i); 
	t = new NachosThread(name);
	p = new Producer(message.charAt(i-1));
	t.fork(p);
      }

      for (int i=1; i<=consumers; i++)
      {
         name = new String("Consumer"+i);
	 t = new NachosThread(name);
	 c = new Consumer();
	 t.fork(c);
      }
      
   }

   //readerWriterTest
   //runs the reader writer problem to test mp2 implementation.
   public static void readerWriterTest()
   {
      NachosThread t;
      Database db = new Database();
      Reader r1 = new Reader(1, db);
      Reader r2 = new Reader(2, db);
      Reader r3 = new Reader(3, db);
      Reader r4 = new Reader(4, db);
      Reader r5 = new Reader(5, db);
      Writer w1 = new Writer(1, db);
      Writer w2 = new Writer(2, db);
      Writer w3 = new Writer(3, db);
      Writer w4 = new Writer(4, db);
      Writer w5 = new Writer(5, db);

      t = new NachosThread("w1");
      t.fork(w1);
      t = new NachosThread("r1");
      t.fork(r1);
      t = new NachosThread("r2");
      t.fork(r2);
      t = new NachosThread("r3");
      t.fork(r3);
      t = new NachosThread("w2");
      t.fork(w2);
      t = new NachosThread("w3");
      t.fork(w3);
      t = new NachosThread("r4");
      t.fork(r4);
      t = new NachosThread("r5");
      t.fork(r5);
      t = new NachosThread("w4");
      t.fork(w4);
      t = new NachosThread("w5");
      t.fork(w5);
   }

   public static void
   startTest()
   {
      //tests Bounded buffer
      boundedBufferTest(); 

      //tests reader writer
      readerWriterTest();

   }
   
}

