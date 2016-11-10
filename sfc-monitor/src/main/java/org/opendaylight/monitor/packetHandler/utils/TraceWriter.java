/*
 * Copyright (c) 2016 Rafael Eichelberger, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.monitor.packetHandler.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class TraceWriter implements Runnable {

        private final File file;
        private final Writer out;
        private final BlockingQueue<Item> queue = new LinkedBlockingQueue<Item>();
        private volatile boolean started = false;
        private volatile boolean stopped = false;

    private static final Logger LOG = LoggerFactory.getLogger(TraceWriter.class);

    public TraceWriter(File file) throws IOException {
            this.file = file;
            this.out = new BufferedWriter(new java.io.FileWriter(file));
        }

        public TraceWriter append(CharSequence seq) {
            if (!started) {
                throw new IllegalStateException("open() call expected before append()");
            }
            try {
                queue.put(new CharSeqItem(seq));
            } catch (InterruptedException ignored) {
                LOG.error("Error on put");
            }
            return this;
        }

        public TraceWriter indent(int indent) {
            if (!started) {
                throw new IllegalStateException("open() call expected before append()");
            }
            try {
                queue.put(new IndentItem(indent));
            } catch (InterruptedException ignored) {
                LOG.error("Error on put");
            }
            return this;
        }

        public void open() {
            this.started = true;
            new Thread(this).start();
        }

        public void run() {
            while (!stopped) {
                try {
                    Item item = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (item != null) {
                        try {
                            item.write(out);
                        } catch (IOException logme) {
                            LOG.error("Error on write");
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.error("Error on pool");
                }

                try {
                    out.flush();
                } catch (IOException e) {
                    LOG.error("Error on flush");
                }

            }
            try {
                out.close();
            } catch (IOException ignore) {
                LOG.error("Error on close");
            }
        }

        public void close() {
            this.stopped = true;
        }

        private interface Item {
            void write(Writer out) throws IOException;
        }

        private static class CharSeqItem implements Item {
            private final CharSequence sequence;

            public CharSeqItem(CharSequence sequence) {
                this.sequence = sequence;
            }

            public void write(Writer out) throws IOException {
                out.append(sequence);
            }
        }

        private static class IndentItem implements Item {
            private final int indent;

            public IndentItem(int indent) {
                this.indent = indent;
            }

            public void write(Writer out) throws IOException {
                for (int i = 0; i < indent; i++) {
                    out.append(" ");
                }
            }
        }
    }

