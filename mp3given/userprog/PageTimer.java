// Actual interrupt handler.  Uses divider to lower overhead.
public class PageTimer implements Runnable {
    // arg is a dummy variable
    int arg;
    static int count; 
    PageTimer(){
	arg = 0;
    }
    public void run()
    {
        int divider = 64;
	if ( (count = (++count % divider )) == 0) {
  	    Machine.memManager.recordHistory( arg );
	}
	return;
    }
}

