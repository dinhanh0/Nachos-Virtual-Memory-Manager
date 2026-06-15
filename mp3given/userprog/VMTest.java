//////////////////////////////////////////////////////////////////////
//    MemManager.java
//    CS323 MP3
/////////////////////////////////////////////////////////////////////
public class VMTest {
   public static void TestCase(int which_case)
   {
      int i;

     System.out.println("\n\nOUTPUT FOR TESTCASE "+ which_case);
     switch (which_case) {
     case 0:
       // Display the state of main memory and swap file
       Machine.memManager.display();
/*
       System.out.print ("\n\nPROGRAM PAGE TABLE CONTENTS:\n");
       for (i=0; i<NachosThread.thisThread().space.numPages; i++) {
	 TranslationEntry te=NachosThread.thisThread().space.pageTable[i];
	 System.out.println ("entry "+ i + ", phys "+te.physicalPage+", virt "+te.virtualPage+", "+(te.legal?"legal":"illegal")+", "+ (te.valid?"valid":"invalid"));
       }
*/
       break;

     case 1:
       // Dump the page table:
       Machine.memManager.display();
       System.out.print ("\n\nPROGRAM PAGE TABLE CONTENTS:\n");
       for (i=0; i<NachosThread.thisThread().space.numPages; i++) {
	 TranslationEntry te=NachosThread.thisThread().space.pageTable[i];
	 System.out.println ("entry "+ i + ", phys "+te.physicalPage+", virt "+te.virtualPage+", "+(te.legal?"legal":"illegal")+", "+ (te.valid?"valid":"invalid"));
       }
       break;

     case 2:
       // Display the state of main memory and swap file
       // Dump the page table.
       Machine.memManager.display();

       System.out.print ("\n\nPROGRAM PAGE TABLE CONTENTS:\n");
       for (i=0; i<NachosThread.thisThread().space.numPages; i++) {
	 TranslationEntry te=NachosThread.thisThread().space.pageTable[i];
	 System.out.println ("entry "+ i + ", phys "+te.physicalPage+", virt "+te.virtualPage+", "+(te.legal?"legal":"illegal")+", "+ (te.valid?"valid":"invalid"));
       }
       break;

     default:
       Debug.printf ('p',"Thread \"%s\" called for unimplemented test case %d\n",
	       NachosThread.thisThread().getName(),new Integer(which_case));
     }
     
   }
}
