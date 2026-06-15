//class BoundedBuffer
//This class implements the synchronization methods to be used in 
//the bounded buffer problem 

public class BoundedBuffer
{
   //MP2 create any variables you need
   Lock lock;
   Condition okToRead;
   Condition okToWrite;
   int length;
   char queue[];
   int count, out, in;

   //BoundedBuffer
   //constructor:  initialize any variables that are needed for a bounded 
   //buffer of size "size"
   public BoundedBuffer(int size)
   {
     length = size;
     queue = new char[size];
     lock = new Lock("buffer lock");
     okToRead = new Condition("Ok to read");
     okToWrite = new Condition("Ok to write");
     count = out = in = 0;
   }

   //produce()
   //produces a character c.  If the buffer is full, wait for an empty
   //slot
   public void produce(char c)
   {
     //MP2
     lock.acquire();
     while (count >= length)
     {
	okToWrite.wait(lock);
     }

     count++;
     queue[in] = c;
     in = (in + 1) % length;
     okToRead.signal(lock);
     lock.release();
     
   }

   //consume()
   //consumes a character.  If the buffer is empty, wait for a producer.
   //use method SynchTest.addToOutputString(c) upon consuming a character. 
   //This is used to test your implementation.
   public void consume()
   {
     //MP2
     char c;

     lock.acquire();
     while (count < 1) {
        okToRead.wait(lock);
     }
     count--;
     c = queue[out];
     out = (out + 1) % length;
     SynchTest.addToOutputString(c);
     okToWrite.signal(lock);
     lock.release();

   }

}
