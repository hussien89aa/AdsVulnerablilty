/*
 * Decompiled with CFR 0_114.
 */
package com.squareup.picasso;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

final class MarkableInputStream
extends InputStream {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private final InputStream in;
    private long offset;
    private long reset;
    private long limit;
    private long defaultMark = -1;

    public MarkableInputStream(InputStream in) {
        this(in, 4096);
    }

    public MarkableInputStream(InputStream in, int size) {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in, size);
        }
        this.in = in;
    }

    @Override
    public void mark(int readLimit) {
        this.defaultMark = this.savePosition(readLimit);
    }

    public long savePosition(int readLimit) {
        long offsetLimit = this.offset + (long)readLimit;
        if (this.limit < offsetLimit) {
            this.setLimit(offsetLimit);
        }
        return this.offset;
    }

    private void setLimit(long limit) {
        try {
            if (this.reset < this.offset && this.offset <= this.limit) {
                this.in.reset();
                this.in.mark((int)(limit - this.reset));
                this.skip(this.reset, this.offset);
            } else {
                this.reset = this.offset;
                this.in.mark((int)(limit - this.offset));
            }
            this.limit = limit;
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to mark: " + e);
        }
    }

    @Override
    public void reset() throws IOException {
        this.reset(this.defaultMark);
    }

    public void reset(long token) throws IOException {
        if (this.offset > this.limit || token < this.reset) {
            throw new IOException("Cannot reset");
        }
        this.in.reset();
        this.skip(this.reset, token);
        this.offset = token;
    }

    private void skip(long current, long target) throws IOException {
        while (current < target) {
            long skipped = this.in.skip(target - current);
            if (skipped == 0) {
                if (this.read() == -1) break;
                skipped = 1;
            }
            current += skipped;
        }
    }

    @Override
    public int read() throws IOException {
        int result = this.in.read();
        if (result != -1) {
            ++this.offset;
        }
        return result;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int count = this.in.read(buffer);
        if (count != -1) {
            this.offset += (long)count;
        }
        return count;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int count = this.in.read(buffer, offset, length);
        if (count != -1) {
            this.offset += (long)count;
        }
        return count;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        long skipped = this.in.skip(byteCount);
        this.offset += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return this.in.available();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
    }

    @Override
    public boolean markSupported() {
        return this.in.markSupported();
    }
}

