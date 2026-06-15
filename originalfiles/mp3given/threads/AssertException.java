/************************
 **
 ** AssertException:
 **
 ** This is an exception which (presumably) nobody will catch, and
 ** will therefore halt the program completely.  It's used largely for
 ** ASSERT purposes.
 **
 ** Richard Cobbe
 ** Comp421 Staff, Spring 1998
 **
 ** Copyright (c) 1998 Rice University.
 *************************/

public class AssertException extends RuntimeException {

  public AssertException() {
    super("Assert failed");
  }

  public AssertException(String msg) {
    super(msg);
  }
}
