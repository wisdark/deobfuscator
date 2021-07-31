package com.javadeobfuscator.deobfuscator.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.javadeobfuscator.deobfuscator.executor.values.JavaValue;

public class Monitor {
    private final Map<Object, Thread> locks = new HashMap<>();
    private final Map<Thread, AtomicInteger> counters = new HashMap<>();

    public void enter(Object obj) {
    	if (((JavaValue)obj).value() == null) throw new NullPointerException();

        while (true) {
            synchronized (locks) {
                Thread current = Thread.currentThread();
                if (!locks.containsKey(obj)) {
                    locks.put(obj, current);
                    counters.put(current, new AtomicInteger(1));
                    break;
                } else if (locks.get(obj) == current) {
                    counters.get(current).getAndIncrement();
                    break;
                }
            }

            synchronized (obj) {
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void exit(Object obj) {
    	if (((JavaValue)obj).value() == null) throw new NullPointerException();

        boolean notify = false;
        Thread current = Thread.currentThread();
        synchronized (locks) {
            if (!locks.containsKey(obj)) throw new IllegalMonitorStateException("Object not locked");
            else if (locks.get(obj) != current)
                throw new IllegalMonitorStateException("This object not owned by " + current);

            AtomicInteger counter = counters.get(current);
            if (counter.get() == 0) throw new IllegalMonitorStateException("Counter should not be zero");
            else if (counter.decrementAndGet() == 0) {
                locks.remove(obj);
                counters.remove(current);
                notify = true;
            }
        }

        if (notify) {
            synchronized (obj) {
                obj.notifyAll();
            }
        }
    }
}

