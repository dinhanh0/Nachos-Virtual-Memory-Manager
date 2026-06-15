// Debug.java
//
//	The debugging methods allow the user to turn on selected
//	debugging messages, controllable from the command line arguments
//	passed to Nachos (-d).  You are encouraged to add your own
//	debugging flags.  The pre-defined debugging flags are:
//
//	'+' -- turn on all debug messages
//   	't' -- thread system
//   	's' -- semaphores, locks, and conditions
//   	'i' -- interrupt emulation
//   	'm' -- machine emulation (USER_PROGRAM)
//   	'd' -- disk emulation (FILESYS)
//   	'c' -- console emulation 
//   	'f' -- file system (FILESYS)
//   	'a' -- address spaces (USER_PROGRAM)
//   	'n' -- network emulation (NETWORK)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

class Debug {

  static char Ch[] = {'0','1','2','3','4','5','6','7','8','9',
		      'a','b','c','d','e','f'};

  static String enableFlags = "+";

  // Equivalent of the C++ Nachos ASSERT macro.  Comes to a screeching
  // halt when the condition is false.  (Nifty thing about the JVM is
  // that it'll print out a full stack trace, automatically.  All we
  // have to do is throw an exception.)
  public static void ASSERT(boolean condition) {
    if (!condition)
      throw new AssertException();
  }
    
  // And an optional form of ASSERT which allows us to print our own
  // message.
  public static void ASSERT(boolean condition, String msg) {
    if (!condition)
      throw new AssertException(msg);
  }

  public static void init(String flagList) {
    enableFlags = flagList;
  }

  public static boolean isEnabled(char flag) {
    return (flag == '+' || enableFlags.indexOf(flag) >= 0 || 
	    enableFlags.indexOf('+') >= 0);
  }

  public static void print(char flag, String text) {
    if (isEnabled(flag))
      System.out.print(text);
  }

  public static void println(char flag, String text) {
    if (isEnabled(flag))
      System.out.println(text);
  }

  public static void printf(char flag, String format, Object o1) {
    Object objs[] = new Object[1];

    objs[0] = o1;
    printf(flag, format, objs);
  }

  public static void printf(char flag, String format, Object o1, Object o2) {
    Object objs[] = new Object[2];

    objs[0] = o1;
    objs[1] = o2;
    printf(flag, format, objs);
  }

  public static void printf(char flag, String format, 
			    Object o1, Object o2, Object o3) {
    Object objs[] = new Object[3];

    objs[0] = o1;
    objs[1] = o2;
    objs[2] = o3;
    printf(flag, format, objs);
  }

  public static void printf(char flag, String format, 
			    Object o1, Object o2, Object o3, Object o4) {
    Object objs[] = new Object[4];

    objs[0] = o1;
    objs[1] = o2;
    objs[2] = o3;
    objs[3] = o4;
    printf(flag, format, objs);
  }

  public static void printf(char flag, String format, Object o[]) {
    boolean pcFlag = false;
    boolean lFlag = false;
    int nextObj = 0;
    long l;
    char ch;

    if (!isEnabled(flag)) return;

    for (int i = 0; i < format.length(); i++) {
      ch = format.charAt(i);
      if (ch == '%') {
	pcFlag = true;
	continue;
      }
      if (!pcFlag) {
	System.out.print(ch);
        continue;
      }

      switch (ch) {

      case 'l':
	lFlag = true;
	continue;

      case 'c':
	byte tmp[] = new byte[1];
	tmp[0] = (byte)((Number) o[nextObj++]).intValue();
	System.out.print(new String(tmp));
	pcFlag = lFlag = false;
	continue;

      case 's':
	System.out.print((String)o[nextObj++]);
	pcFlag = lFlag = false;
	continue;

      case 'd':
	if (lFlag) l = ((Number) o[nextObj++]).longValue();
	else       l = ((Number) o[nextObj++]).longValue();
	if (l < 0) {
	  System.out.print('-');
	  l = -l;
	}
	kprintn(l, 10);
	pcFlag = lFlag = false;
	continue;

      case 'o':
	if (lFlag) l = ((Number) o[nextObj++]).longValue();
	else       l = ((Number) o[nextObj++]).longValue();
	kprintn(l, 8);
	pcFlag = lFlag = false;
	continue;

      case 'u':
	if (lFlag) l = ((Number) o[nextObj++]).longValue();
	else       l = ((Number) o[nextObj++]).longValue();
	kprintn(l, 10);
	pcFlag = lFlag = false;
	continue;

      case 'x':
	if (lFlag) l = ((Number) o[nextObj++]).longValue();
	else       l = ((Number) o[nextObj++]).longValue();
	kprintn(l, 16);
	pcFlag = lFlag = false;
	continue;

      default:
	System.out.print('%');
	if (lFlag) System.out.print('l');
	System.out.print(ch);
	pcFlag = lFlag = false;
      }
    }
  }

  static private void kprintn(long l, int base) {
    char cBuf[] = new char[32];
    int i = 0;
    
    do {
      cBuf[i++] = Ch[(int)(l % base)];
    } while ((l /= base) != 0);
    do {
      System.out.print(cBuf[--i]);
    } while (i > 0);

  }
  
}

