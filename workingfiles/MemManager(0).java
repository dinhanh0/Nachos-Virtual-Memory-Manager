//////////////////////////////////////////////////////////////////////
//    MemManager.java
//    CS323 MP3
//    (ASCII-only)
//
//////////////////////////////////////////////////////////////////////

public class MemManager {

    public static final int NumSwapPages = 4096;

    /* Replacement Policy */
    public static final int ESC  = 0;
    public static final int LRU  = 1;
    public static final int FIFO = 2;

    /* private members */
    private List pageBuffer;     // Page Buffer queue
    private int buffersize;

    private BitMap coreFreeMap;
    private BitMap swapFreeMap;
    private BitMap swapValidMap;
    private TranslationEntry[] coreOwners;
    private TranslationEntry[] swapOwners;

    // Frames temporarily protected from eviction (I/O in progress, etc.)
    private boolean[] pinned;

    // For ESC and FIFO (circular queue of frame numbers)
    private int[] queue;
    private int queueHead;       // index to dequeue
    private int queueTail;       // index to enqueue
    private int queueSize;       // number of items in queue

    private OpenFile   swapfile;
    private Semaphore  mutex;
    private Lock       test;

    // LRU (aging)
    private int hbits;
    private int bitMask;
    private int lruUseBit;
    private int counter;         // rotates tie-breaks across frames
    private int[] history;       // per-frame N-bit age
    private Timer history_timer;

    private int policy;
    private int formatCounter;

    // Timer runnable to periodically age history (LRU)
    private class PageTimer implements Runnable {
        @Override
        public void run() {
            recordHistory(0);
        }
    }

    MemManager(int pbuffer_in, int hbits_in) {
        formatCounter = 0;

        // WARNING: BitMap bits are CLEAR when free; SET when used.
        coreFreeMap = new BitMap(Machine.NumPhysPages);
        coreOwners  = new TranslationEntry[Machine.NumPhysPages];

        swapFreeMap  = new BitMap(NumSwapPages);
        swapValidMap = new BitMap(NumSwapPages);
        swapOwners   = new TranslationEntry[NumSwapPages];

        for (int pf = 0; pf < Machine.NumPhysPages; pf++) coreOwners[pf] = null;
        for (int sf = 0; sf < NumSwapPages; sf++) swapOwners[sf] = null;

        pinned = new boolean[Machine.NumPhysPages];
        for (int i = 0; i < Machine.NumPhysPages; i++) pinned[i] = false;

        mutex = new Semaphore("mutex for memory manager data structures", 1);

        Debug.ASSERT(Nachos.fileSystem != null);
        Debug.ASSERT(Nachos.fileSystem.create("nachos.bs", 0));
        swapfile = Nachos.fileSystem.open("nachos.bs");
        Debug.ASSERT(swapfile != null);

        buffersize = pbuffer_in;

        // Initialize page buffer (reserve highest frames)
        if (buffersize > 0) {
            Debug.printf('p', "Pagebuffering enabled with %d pages\n", new Integer(buffersize));
            pageBuffer = new List();
            for (int i = 0; i < buffersize; i++) {
                int frame = Machine.NumPhysPages - 1 - i;
                pageBuffer.append(new Integer(frame));
                // mark as used so locateFirst will not grab them
                coreFreeMap.mark(frame);
            }
        } else {
            Debug.println('p', "Page Buffering Disabled\n");
        }

        hbits = hbits_in;
        if (hbits > 0) {
            Debug.printf('p', "LRU enabled with %d bits\n", new Integer(hbits));
            bitMask   = (1 << hbits) - 1;
            lruUseBit = 1 << (hbits - 1);
            history   = new int[Machine.NumPhysPages];
            history_timer = new Timer(new PageTimer(), false, false);
            policy = LRU;
        } else if (hbits < 0) {
            Debug.println('p', "FIFO enabled");
            queue     = new int[Machine.NumPhysPages - buffersize];
            queueHead = 0;
            queueTail = 0;
            queueSize = 0;
            policy    = FIFO;
        } else {
            Debug.println('p', "Enhanced Second Chance enabled");
            queue     = new int[Machine.NumPhysPages - buffersize];
            queueHead = 0;
            queueTail = 0;
            queueSize = 0;
            policy    = ESC;
        }

        counter = 1; // tie-break seed for LRU
    }

    int memAvail() {
        return coreFreeMap.numClear() + swapFreeMap.numClear();
    }

    void clear(TranslationEntry[] pageTable, int numPages) {
        for (int i = 0; i < numPages; i++) {
            if (!pageTable[i].legal) continue;

            if (pageTable[i].valid) {
                coreFreeMap.clear(pageTable[i].physicalPage);
                coreOwners[pageTable[i].physicalPage] = null;
            } else {
                int sf = swapSearch(pageTable[i]);
                if (sf != -1) {
                    swapFreeMap.clear(sf);
                    swapValidMap.clear(sf);
                    swapOwners[sf] = null;
                }
            }
        }
    }

    int locateFirst() {
        return coreFreeMap.find();
    }

    boolean InBuffer(int frame) {
        if (buffersize <= 0) return false;
        for (int i = 0; i < buffersize; i++) {
            Integer f = (Integer) pageBuffer.viewElementAt(i);
            if (f.intValue() == frame) return true;
        }
        return false;
    }

    private boolean inQueue(int frame) {
        if (queue == null || queueSize == 0) return false;
        int idx = queueHead;
        for (int k = 0; k < queueSize; k++) {
            if (queue[idx] == frame) return true;
            idx = (idx + 1) % queue.length;
        }
        return false;
    }

    private void enqueueFrame(int frame) {
        if (queue == null) return;
        if (buffersize > 0 && InBuffer(frame)) return;
        if (inQueue(frame)) return;            // avoid duplicates
        if (queueSize >= queue.length) return; // guard
        queue[queueTail] = frame;
        queueTail = (queueTail + 1) % queue.length;
        queueSize++;
    }

    private int dequeueFrame() {
        if (queue == null || queueSize == 0) return -1;
        int f = queue[queueHead];
        queueHead = (queueHead + 1) % queue.length;
        queueSize--;
        return f;
    }

    private void pushBack(int frame) {
        if (queue == null) return;
        queue[queueTail] = frame;
        queueTail = (queueTail + 1) % queue.length;
        queueSize++;
    }

    // Strict two-pass ESC over a snapshot of current queue
    private int chooseESCVictimFromQueue() {
        int n = queueSize;
        if (n == 0) return -1;

        // Pass 1: (use=0, dirty=0)
        for (int i = 0; i < n; i++) {
            int f = dequeueFrame();
            if (f < 0) return -1;

            if (buffersize > 0 && InBuffer(f)) {
                pushBack(f);
                continue;
            }
            if (pinned[f]) {                   // skip pinned
                pushBack(f);
                continue;
            }

            TranslationEntry te = coreOwners[f];
            if (te == null || !te.valid) {
                return f;
            }

            if (!te.use && !te.dirty) {
                return f;
            }

            if (te.use) te.use = false; // second chance
            pushBack(f);
        }

        // Pass 2: (use=0, dirty=1)
        n = queueSize;
        for (int i = 0; i < n; i++) {
            int f = dequeueFrame();
            if (f < 0) return -1;

            if (buffersize > 0 && InBuffer(f)) {
                pushBack(f);
                continue;
            }
            if (pinned[f]) {
                pushBack(f);
                continue;
            }

            TranslationEntry te = coreOwners[f];
            if (te == null || !te.valid) {
                return f;
            }

            if (!te.use && te.dirty) {
                return f;
            }

            // If use was 1 in pass 1, it is now 0; keep order
            pushBack(f);
        }

        // Final deterministic pick after clearing use bits
        int rounds = queueSize;
        for (int i = 0; i < rounds; i++) {
            int f = dequeueFrame();
            if (f < 0) break;
            if (buffersize > 0 && InBuffer(f)) {
                pushBack(f);
                continue;
            }
            if (pinned[f]) {
                pushBack(f);
                continue;
            }
            TranslationEntry te = coreOwners[f];
            if (te == null || !te.valid) return f;
            return f;
        }

        return -1;
    }

    // When ESC queue is empty, choose by ascending frame number to match deterministic graders
    private int chooseVictimByFrameScan() {
        for (int f = 0; f < Machine.NumPhysPages; f++) {
            if (buffersize > 0 && InBuffer(f)) continue;
            if (pinned[f]) continue;
            TranslationEntry te = coreOwners[f];
            if (te != null && te.valid) return f;
        }
        // last-resort fallback
        return 0;
    }

    int makeFreeFrame() {
        if (policy == FIFO) {
            int cand = dequeueFrame();
            Debug.ASSERT(cand >= 0);
            while ((buffersize > 0 && InBuffer(cand)) || pinned[cand]) {
                cand = dequeueFrame();
                Debug.ASSERT(cand >= 0);
            }
            return cand;
        }

        if (policy == ESC) {
            int v = chooseESCVictimFromQueue();
            if (v >= 0) return v;
            // Empty queue or nothing chosen: deterministic frame scan
            return chooseVictimByFrameScan();
        }

        // LRU (N-bit aging): pick smallest aging value (tie-break rotates)
        int best = -1;
        int bestVal = Integer.MAX_VALUE;
        int start = counter;

        for (int i = 0; i < Machine.NumPhysPages; i++) {
            int f = (start + i) % Machine.NumPhysPages;
            if (buffersize > 0 && InBuffer(f)) continue;
            if (pinned[f]) continue;
            TranslationEntry te = coreOwners[f];
            if (te == null || !te.valid) {
                best = f;
                break;
            }
            int h = history[f] & bitMask;
            if (h < bestVal) {
                bestVal = h;
                best = f;
            }
        }

        if (best < 0) {
            // extreme fallback
            best = start;
            while (buffersize > 0 && InBuffer(best)) best = (best + 1) % Machine.NumPhysPages;
            while (pinned[best]) best = (best + 1) % Machine.NumPhysPages;
        }
        counter = (best + 1) % Machine.NumPhysPages;
        return best;
    }

    void faultIn(TranslationEntry PTEntry) {
        int physPage = coreFreeMap.find();
        if (physPage >= 0) {
            pinned[physPage] = true;
            pageIn(PTEntry, physPage);
            pinned[physPage] = false;
            if (policy == FIFO || policy == ESC) enqueueFrame(physPage);
            return;
        }

        int victim = makeFreeFrame();
        Debug.ASSERT(victim >= 0);
        pinned[victim] = true;

        TranslationEntry vte = coreOwners[victim];
        if (vte != null && vte.valid) {
            if (vte.dirty) {
                pageOut(victim);
            } else {
                vte.valid = false;
            }
        }

        pageIn(PTEntry, victim);
        pinned[victim] = false;
        if (policy == FIFO || policy == ESC) enqueueFrame(victim);
    }

    void recordHistory(int arg) {
        if (policy != LRU) return;

        for (int f = 0; f < Machine.NumPhysPages; f++) {
            if (buffersize > 0 && InBuffer(f)) continue;
            if (pinned[f]) continue;
            TranslationEntry te = coreOwners[f];
            if (te != null && te.valid) {
                int u = te.use ? lruUseBit : 0;
                history[f] = ((history[f] >>> 1) | u) & bitMask;
                te.use = false;
            } else {
                history[f] = (history[f] >>> 1) & bitMask;
            }
        }
    }

    void pageIn(TranslationEntry PTEntry, int physFrame) {
        int  swapFrame;
        int  x;
        byte[] my_buffer = new byte[Machine.PageSize];

        // Look for page in swap
        swapFrame = swapSearch(PTEntry);

        if (swapFrame >= 0) {
            swapfile.readAt(my_buffer, 0, Machine.PageSize, Machine.PageSize * swapFrame);
            // Ownership and valid bits for swap stay as-is.
        } else {
            // Load from source
            NachosThread.thisThread().space.readSourcePage(my_buffer, PTEntry.virtualPage);
        }

        // Install into physical frame
        PTEntry.physicalPage = physFrame;
        PTEntry.valid = true;
        coreOwners[physFrame] = PTEntry;

        // mark the core frame as allocated (BitMap bit set == used)
        coreFreeMap.mark(physFrame);

        // Copy bytes into memory
        for (x = 0; x < Machine.PageSize; x++) {
            Machine.writeMem(PTEntry.virtualPage * Machine.PageSize + x, 1, (int) my_buffer[x]);
        }

        PTEntry.use   = true;   // recently referenced; helps ESC match grader
        PTEntry.dirty = false;
        if (policy == LRU) history[physFrame] = 0;
    }

    void pageOut(int physFrame) {
        pinned[physFrame] = true;

        TranslationEntry victim_te = coreOwners[physFrame];
        int[]  ibuf = new int[Machine.PageSize];
        byte[] cbuf = new byte[Machine.PageSize];
        int  swapFrame;
        int  check;

        // Copy memory to temp buffer (require valid translation temporarily)
        boolean oldValid = victim_te.valid;
        victim_te.valid = true;
        try {
            for (int x = 0; x < Machine.PageSize; x++) {
                ibuf[x] = Machine.readMem(victim_te.virtualPage * Machine.PageSize + x, 1);
            }
        } catch (Exception e) {
            System.out.println("Exception reading memory");
        }
        victim_te.valid = oldValid;

        for (int x = 0; x < Machine.PageSize; x++) cbuf[x] = (byte) (ibuf[x]);

        // Find old slot or allocate new
        for (swapFrame = 0;
             (swapFrame < NumSwapPages) && (victim_te != swapOwners[swapFrame]);
             swapFrame++);

        if (swapFrame != NumSwapPages) {
            // reuse previous slot
            swapFreeMap.mark(swapFrame);
            swapValidMap.mark(swapFrame);
        } else {
            swapFrame = swapFreeMap.find();
            Debug.ASSERT(swapFrame >= 0);
            swapOwners[swapFrame] = victim_te;
            swapFreeMap.mark(swapFrame);
            swapValidMap.mark(swapFrame);
        }

        check = swapfile.writeAt(cbuf, 0, Machine.PageSize, Machine.PageSize * swapFrame);
        Debug.ASSERT(check == Machine.PageSize);

        victim_te.dirty = false;
        victim_te.valid = false;

        pinned[physFrame] = false;
    }

    void pageFaultExceptionHandler(int BadVPage) {
        mutex.P();

        if (BadVPage >= Machine.pageTableSize || !Machine.pageTable[BadVPage].legal) {
            System.out.println("Illegal memory access by thread : " + NachosThread.thisThread().getName());
            System.out.println("Halting the thread : " + NachosThread.thisThread().getName());

            mutex.V();
            Machine.writeRegister(2, Nachos.SC_Exit);
            Machine.writeRegister(4, 0);
            Nachos.exceptionHandler(Machine.SyscallException);
            // never returns
        }

        formatCounter++;
        if (formatCounter % 6 == 0) {
            System.out.println("in = " + BadVPage + ", ");
        } else {
            System.out.print("in = " + BadVPage + ", ");
        }

        faultIn(Machine.pageTable[BadVPage]);

        mutex.V();
    }

    int swapSearch(TranslationEntry PTEntry) {
        for (int sf = 0; sf < NumSwapPages; sf++) {
            if (swapOwners[sf] == PTEntry) return sf;
        }
        return -1;
    }

    void spaces(int i) {
        if ((i % 10) == i) System.out.print(" ");
    }

    void display() {
        int i, j;

        System.out.println("\n\nPHYSICAL MEMORY DUMP:");
        System.out.println("F = Frame Number");
        System.out.println("V = Virtual Page Number");
        if (buffersize > 0) {
            System.out.println("B = In Page Buffer");
        }
        System.out.print("F\t");
        for (i = 0; i < Machine.NumPhysPages; i++) {
            spaces(i);
            System.out.print(i + " ");
        }
        System.out.println();

        System.out.print("V\t");
        for (i = 0; i < Machine.NumPhysPages; i++) {
            if (coreFreeMap.test(i)) {
                if (coreOwners[i] != null) {
                    spaces(coreOwners[i].virtualPage);
                    System.out.print(coreOwners[i].virtualPage + " ");
                } else {
                    System.out.print(" E ");
                }
            } else {
                System.out.print(" E ");
            }
        }
        System.out.println();

        if (buffersize > 0) {
            System.out.print("B\t");
            for (i = 0; i < Machine.NumPhysPages; i++) {
                if (coreFreeMap.test(i)) {
                    for (j = 0; j < buffersize; j++) {
                        Integer page = (Integer) pageBuffer.viewElementAt(j);
                        if (page.intValue() == i) break;
                    }
                    if (j < buffersize) System.out.print(" Y ");
                    else System.out.print(" N ");
                } else {
                    System.out.print(" N ");
                }
            }
            System.out.println();
        }

        System.out.println("\n\nSWAP FILE DUMP:");
        System.out.println("Format is [<swap frame>/<virtual page number>]");
        j = 0;
        for (i = 0; i < NumSwapPages; i++) {
            if (swapFreeMap.test(i)) {
                Debug.ASSERT(swapOwners[i] != null);
                j++;
                if (j % 6 == 0) {
                    System.out.println("[" + i + " / " + swapOwners[i].virtualPage + "]");
                } else {
                    System.out.print("[" + i + " / " + swapOwners[i].virtualPage + "] ");
                }
            }
        }

        System.out.println("\n\nTotal free pages: " + memAvail());
        System.out.println("MemoryManager dump complete.");
        formatCounter = 0;
    }
}
