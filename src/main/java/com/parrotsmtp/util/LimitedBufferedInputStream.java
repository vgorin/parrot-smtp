package com.parrotsmtp.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author vgorin (Vasiliy.Gorin)
 *         file created: 16.05.12 16:21
 */
public class LimitedBufferedInputStream extends BufferedInputStream {
    private AtomicLong counter = new AtomicLong();
    private long limit;

    public LimitedBufferedInputStream(InputStream in) {
        super(in);
    }

    public LimitedBufferedInputStream(InputStream in, int size) {
        super(in, size);
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public boolean limitOverflow() {
        try {
            return counter.get() >= limit && in.read() != -1;
        }
        catch(IOException e) {
            return false;
        }
    }

    @Override
    public synchronized int read() throws IOException {
        if(counter.get() < limit) {
            final int r = super.read();
            counter.addAndGet(Math.max(0, r));
            return r;
        }
        else {
            return -1;
        }
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        final long counterValue = counter.get();
        final int r;
        if(counterValue + len < limit) {
            r = super.read(b, off, len);
        }
        else if(counterValue < limit) {
            r = super.read(b, off, (int) (limit - counterValue));
        }
        else {
            r = -1;
        }
        counter.addAndGet(Math.max(0, r));
        return r;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        final long counterValue = counter.get();
        final long r;
        if(counterValue + n < limit) {
            r = super.skip(n);
        }
        else {
            r = super.skip(limit - counterValue);
        }
        counter.addAndGet(n);
        return r;
    }

    @Override
    public synchronized int available() throws IOException {
        final int available = super.available();
        final long counterValue = counter.get();
        if(counterValue + available < limit) {
            return available;
        }
        else {
            return (int) (limit - counterValue);
        }
    }
}
