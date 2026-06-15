//////////////////////////////////////////////////////////////////////
//    MemManager.java
//    CS323 MP3
/////////////////////////////////////////////////////////////////////

public class MemManager {

    public static final int NumSwapPages = 4096;

    /* Replacement Policy */
    public static final int ESC = 0;
    public static final int LRU = 1;
    public static final int FIFO = 2;

    /* private members */
    private List pageBuffer;     // Page Buffer queue
    private int buffersize;

    private BitMap coreFreeMap;
    private BitMap swapFreeMap;
    private BitMap swapValidMap;
    private TranslationEntry[] coreOwners;
    private TranslationEntry[] swapOwners;

    private int[] queue;      // for ESC and FIFO
    private int queueCounter; // head
    private int queueTail;    // tail
    private int queueSize;    // number of elements in queue

    private OpenFile swapfile;
    private Semaphore mutex;

    private int hbits;
    private int bitMask;
    private int lruUseBit;
    private int counter;          // tie-breaker start index for LRU

    private int[] history;        // for LRU
    private Timer history_timer;  // for LRU

    private int policy;
    private int formatCounter;

    /* periodic page history updater (runs in kernel thread) */
    private class PageTimer implements Runnable {
        public void run() {
            recordHistory(0);
        }
    }

    MemManager(int pbuffer_in, int hbits_in) {

        formatCounter = 0;

        coreFreeMap = new BitMap(Machine.NumPhysPages);
        coreOwners = new TranslationEntry[Machine.NumPhysPages];

        swapFreeMap = new BitMap(NumSwapPages);
        swapValidMap = new BitMap(NumSwapPages);
        swapOwners = new TranslationEntry[NumSwapPages];

        for (int i = 0; i < Machine.NumPhysPages; i++)
            coreOwners[i] = null;

        for (int i = 0; i < NumSwapPages; i++)
            swapOwners[i] = null;

        mutex = new Semaphore("mutex for mm", 1);

        Debug.ASSERT(Nachos.fileSystem != null);
        Debug.ASSERT(Nachos.fileSystem.create("nachos.bs", 0));
        swapfile = Nachos.fileSystem.open("nachos.bs");
        Debug.ASSERT(swapfile != null);

        buffersize = pbuffer_in;
        if (buffersize > 0) {
            pageBuffer = new List();
            for (int i = 0; i < buffersize; i++) {
                int frame = Machine.NumPhysPages - 1 - i;
                pageBuffer.append(new Integer(frame));
                coreFreeMap.mark(frame); // mark used
            }
        }

        hbits = hbits_in;

        if (hbits > 0) {
            policy = LRU;
            bitMask = (1 << hbits) - 1;
            lruUseBit = 1 << (hbits - 1);
            history = new int[Machine.NumPhysPages];

            PageTimer pt = new PageTimer();
            history_timer = new Timer(pt, false, false);
        } else if (hbits < 0) {
            policy = FIFO;
            queue = new int[Machine.NumPhysPages - buffersize];
            queueCounter = 0;
            queueTail = 0;
            queueSize = 0;
        } else {
            policy = ESC;
            queue = new int[Machine.NumPhysPages - buffersize];
            queueCounter = 0;
            queueTail = 0;
            queueSize = 0;
        }

        counter = 0;
    }

    int memAvail() {
        return coreFreeMap.numClear() + swapFreeMap.numClear();
    }

    void clear(TranslationEntry[] pageTable, int numPages) {
        for (int i = 0; i < numPages; i++) {
            if (pageTable[i].legal) {
                if (pageTable[i].valid) {
                    coreFreeMap.clear(pageTable[i].physicalPage);
                    coreOwners[pageTable[i].physicalPage] = null;
                } else {
                    int sf = swapSearch(pageTable[i]);
                    if (sf >= 0) {
                        swapFreeMap.clear(sf);
                        swapValidMap.clear(sf);
                        swapOwners[sf] = null;
                    }
                }
            }
        }
    }

    int locateFirst() {
        return coreFreeMap.find();
    }

    boolean InBuffer(int f) {
        if (pageBuffer == null) return false;
        for (int i = 0; i < buffersize; i++) {
            Integer p = (Integer) pageBuffer.viewElementAt(i);
            if (p.intValue() == f) return true;
        }
        return false;
    }

    private void enqueueFrame(int f) {
        if (buffersize > 0 && InBuffer(f)) return;
        if (queue == null) return;
        if (queueSize < queue.length) {
            queue[queueTail] = f;
            queueTail = (queueTail + 1) % queue.length;
            queueSize++;
        }
    }

    private int dequeueFrame() {
        if (queue == null || queueSize == 0) return -1;
        int f = queue[queueCounter];
        queueCounter = (queueCounter + 1) % queue.length;
        queueSize--;
        return f;
    }

    int makeFreeFrame() {

        if (policy == FIFO || policy == ESC) {
            int cand = dequeueFrame();
            while (buffersize > 0 && InBuffer(cand)) {
                cand = dequeueFrame();
            }
            return cand;
        }

        int best = -1;
        int bestVal = Integer.MAX_VALUE;

        int start = counter;
        for (int i = 0; i < Machine.NumPhysPages; i++) {
            int f = (start + i) % Machine.NumPhysPages;
            if (buffersize > 0 && InBuffer(f)) continue;
            TranslationEntry te = coreOwners[f];
            if (te != null && te.valid) {
                int h = history[f] & bitMask;
                if (h < bestVal) {
                    bestVal = h;
                    best = f;
                }
            }
        }

        if (best < 0) {
            for (int i = 0; i < Machine.NumPhysPages; i++) {
                int f = (start + i) % Machine.NumPhysPages;
                if (buffersize > 0 && InBuffer(f)) continue;
                TranslationEntry te = coreOwners[f];
                if (te != null && te.valid) {
                    best = f;
                    break;
                }
            }
        }

        if (best < 0) {
            best = start;
            while (buffersize > 0 && InBuffer(best))
                best = (best + 1) % Machine.NumPhysPages;
        }

        counter = (best + 1) % Machine.NumPhysPages;
        return best;
    }

    void faultIn(TranslationEntry te) {
        int pf = coreFreeMap.find();
        if (pf >= 0) {
            pageIn(te, pf);
            if (policy == FIFO || policy == ESC) enqueueFrame(pf);
            return;
        }

        pf = makeFreeFrame();

        if (coreOwners[pf].dirty) {
            pageOut(pf);
        } else {
            coreOwners[pf].valid = false;
        }

        pageIn(te, pf);

        if (policy == FIFO || policy == ESC) enqueueFrame(pf);
    }

    void recordHistory(int arg) {
        if (policy != LRU) return;

        for (int f = 0; f < Machine.NumPhysPages; f++) {
            if (buffersize > 0 && InBuffer(f)) continue;
            TranslationEntry te = coreOwners[f];
            if (te != null && te.valid) {
                int u = te.use ? lruUseBit : 0;
                history[f] = ((history[f] >> 1) | u) & bitMask;
                te.use = false;
            } else {
                history[f] = (history[f] >> 1) & bitMask;
            }
        }
    }

    void pageIn(TranslationEntry te, int pf) {
        byte[] buf = new byte[Machine.PageSize];
        int sf = swapSearch(te);

        if (sf >= 0) {
            swapfile.readAt(buf, 0, Machine.PageSize, Machine.PageSize * sf);
            swapFreeMap.mark(sf);
            if (swapValidMap != null) swapValidMap.mark(sf);
            swapOwners[sf] = te;
        } else {
            NachosThread.thisThread().space.readSourcePage(buf, te.virtualPage);
        }

        te.physicalPage = pf;
        te.valid = true;
        coreOwners[pf] = te;
        coreFreeMap.mark(pf);

        for (int i = 0; i < Machine.PageSize; i++)
            Machine.writeMem(te.virtualPage * Machine.PageSize + i, 1, (int) buf[i]);

        te.use = false;
        te.dirty = false;

        if (policy == LRU) history[pf] = 0;
    }

    void pageOut(int pf) {
        TranslationEntry te = coreOwners[pf];

        int[] temp = new int[Machine.PageSize];
        byte[] buf = new byte[Machine.PageSize];

        te.valid = true;
        try {
            for (int i = 0; i < Machine.PageSize; i++)
                temp[i] = Machine.readMem(te.virtualPage * Machine.PageSize + i, 1);
        } catch (Exception e) {
            System.out.println("Exception reading memory");
        }
        te.valid = false;

        for (int i = 0; i < Machine.PageSize; i++)
            buf[i] = (byte) temp[i];

        int sf;
        for (sf = 0; sf < NumSwapPages; sf++)
            if (swapOwners[sf] == te) break;

        if (sf < NumSwapPages) {
            swapFreeMap.mark(sf);
            if (swapValidMap != null) swapValidMap.mark(sf);
        } else {
            sf = swapFreeMap.find();
            swapOwners[sf] = te;
            swapFreeMap.mark(sf);
            if (swapValidMap != null) swapValidMap.mark(sf);
        }

        int check = swapfile.writeAt(buf, 0, Machine.PageSize, Machine.PageSize * sf);
        Debug.ASSERT(check == Machine.PageSize);

        te.dirty = false;
        te.valid = false;
    }

    void pageFaultExceptionHandler(int vpage) {
        mutex.P();

        if (vpage >= Machine.pageTableSize || !Machine.pageTable[vpage].legal) {
            System.out.println("Illegal memory access by thread");
            Machine.writeRegister(2, Nachos.SC_Exit);
            Machine.writeRegister(4, 0);
            Nachos.exceptionHandler(Machine.SyscallException);
        }

        if (policy == LRU) recordHistory(0);

        formatCounter++;
        if (formatCounter % 6 == 0)
            System.out.println("in = " + vpage + ", ");
        else
            System.out.print("in = " + vpage + ", ");

        faultIn(Machine.pageTable[vpage]);

        mutex.V();
    }

    int swapSearch(TranslationEntry te) {
        for (int i = 0; i < NumSwapPages; i++)
            if (swapOwners[i] == te) return i;
        return -1;
    }

    void spaces(int i) {
        if ((i % 10) == i) System.out.print(" ");
    }

    void display() {

        System.out.println("\n\nPHYSICAL MEMORY DUMP:");
        System.out.println("F = Frame Number");
        System.out.println("V = Virtual Page Number");
        System.out.print("F\t");

        for (int i = 0; i < Machine.NumPhysPages; i++) {
            spaces(i);
            System.out.print(i + " ");
        }
        System.out.println();

        System.out.print("V\t");
        for (int i = 0; i < Machine.NumPhysPages; i++) {
            if (coreFreeMap.test(i) && coreOwners[i] != null) {
                spaces(coreOwners[i].virtualPage);
                System.out.print(coreOwners[i].virtualPage + " ");
            } else {
                System.out.print(" E ");
            }
        }
        System.out.println();

        System.out.println("\n\nSWAP FILE DUMP:");
        System.out.println("Format is [<swap frame>/<virtual page>]");

        int c = 0;
        for (int i = 0; i < NumSwapPages; i++) {
            if (swapFreeMap.test(i)) {
                c++;
                if (c % 6 == 0)
                    System.out.println("[" + i + " / " + swapOwners[i].virtualPage + "]");
                else
                    System.out.print("[" + i + " / " + swapOwners[i].virtualPage + "] ");
            }
        }

        System.out.println("\n\nTotal free pages: " + memAvail());
        System.out.println("MemoryManager dump complete.");
        formatCounter = 0;
    }

}
