// Alarm.java
//This class allows the current thread to go to sleep for a set amount of time.
//It uses the simulated hardware Timer to sleep for a given number of ticks

public class Alarm implements Runnable
{
   private Timer t;
   private int soFar, ticks;
   private NachosThread thread;

   //Alarm
   //constructor for a new alarm.  
   //puts the current thread to sleep and starts a timer to wake of the thread
   //after ticks time.
   public Alarm(int ticks)
   {
      soFar = 0;
      this.ticks = ticks;
      t = new Timer(this, false, false);

      thread = NachosThread.thisThread();
      int oldLevel = Interrupt.setLevel(Interrupt.IntOff);
      thread.sleep();
      Interrupt.setLevel(Interrupt.IntOn);
      
   }

   //run
   //increments the number of ticks that have passed so far.  If the alarm
   //should go off then it wakes up the sleeping thread.
   public void run()
   {
      soFar += 10;
      if (soFar >= ticks)
      {
         Scheduler.readyToRun(thread);
	 t.cancel();
      }
      
   }
}
