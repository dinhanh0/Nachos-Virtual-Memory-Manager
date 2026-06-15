//class Database
//This class implements the synchronization methods to be used in 
//the readers writers problem
public class Database
{
   //MP2 create any variables that you need for implementation of the methods
   //of this class
   int readerCount;
   boolean dbReading;
   boolean dbWriting;
   private Semaphore db;
   private Semaphore mutex;
   private Lock writerLock;
   private Semaphore writerMutex;
   private Condition writerCondition;
   private int writerWaiting;

   //Database
   //Initializes Database variables
   public Database()
   {
     //MP2
     readerCount = 0;
     writerWaiting = 0;
     db = new Semaphore("data semaphore",1);
     mutex = new Semaphore("mutex",1);
     writerCondition = new Condition("writer condition");
     writerLock = new Lock("writer lock");
     writerMutex = new Semaphore("writer mutex", 1);
   }

   //napping()
   //this is called when a reader or writer wants to go to sleep and when 
   //a reader or writer is doing its work.
   //Do not change for MP2
   public static void napping()
   {
      Alarm ac = new Alarm(20);  
   }

   //startRead
   //this function should block any reader that wants to read if there 
   //is a writer that is currently writing.
   //it returns the number of readers currently reading including the
   //new reader.
   public int startRead()
   {
      //MP2
      int c;
      int l;

      writerMutex.P();
      l = writerWaiting;
      writerMutex.V();
      if (l > 0)
      {
         writerLock.acquire();
	 writerCondition.wait(writerLock);
	 writerLock.release();
      }
     
      mutex.P();
      
      ++readerCount;
      c = readerCount;

      if (readerCount == 1)
        db.P();

      mutex.V();
      
      return c;
   }

   //endRead()
   //This function is called by a reader that has finished reading from the 
   //database.  It returns the current number of readers excluding the one who
   //just finished.
   public int endRead()
   {
      //MP2
      int c;
      mutex.P();

      --readerCount;
      c = readerCount;

      if (readerCount == 0)
	db.V();

      mutex.V();
      
      return c;
   }

   //startWrite()
   //This function should allow only one writer at a time into the Database
   //and block the writer if anyone else is accessing the database for read 
   //or write.
   public void startWrite()
   {
      int l;
      writerMutex.P();
      writerWaiting++;
      writerMutex.V();

      //MP2
      db.P();

      writerMutex.P();
      writerWaiting--; 
      l = writerWaiting;
      writerMutex.V();

      if (l == 0)
      {
	 writerLock.acquire();
         writerCondition.broadcast(writerLock);
	 writerLock.release();
      }

   }
   
   //endWrite()
   //signal that a writer is done writing and the database can now be accessed
   //by someone who is waiting to read or write.
   public void endWrite()
   {
      //MP2
      db.V();
   }
}
