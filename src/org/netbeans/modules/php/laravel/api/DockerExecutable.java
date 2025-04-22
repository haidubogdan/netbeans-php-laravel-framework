/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.php.laravel.api;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.pty.Pty;
import org.netbeans.modules.nativeexecution.api.pty.PtySupport;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author bogdan.haidu
 */
public class DockerExecutable {

    public void run() {
        JTextArea outputPane = new JTextArea();
        JTextField inputPane = new JTextField();
        TerminalEm.launchAndInstallTerminal(outputPane, inputPane);
        RequestProcessor Rp = new RequestProcessor("TerminalOut", 1, true);
        Rp.post(new Runnable() {
            @Override
            public void run() {
                try {
                    TestProcessIO.main(outputPane, inputPane);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }

    public Process startProcess(Process process) throws IOException {
        Pty pty = PtySupport.allocate(ExecutionEnvironmentFactory.getLocal());
        AtomicInteger exitCode = new AtomicInteger();
        return new Process() {

            private InputStreamWithCloseDetection std;
            private InputStreamWithCloseDetection err;

            @Override
            public OutputStream getOutputStream() {
                return pty.getOutputStream();
            }

            @Override
            public synchronized InputStream getInputStream() {
                if (std == null) {
                    std = new InputStreamWithCloseDetection(pty.getInputStream());
                }
                return std;
            }

            @Override
            public synchronized InputStream getErrorStream() {
                if (err == null) {
                    err = new InputStreamWithCloseDetection(pty.getErrorStream());
                }
                return err;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public int waitFor() throws InterruptedException {
                process.waitFor();
                // We do not plan to write to PTY any more, close its input,
                // PTY will close its output then.
                try {
                    pty.getOutputStream().close();
                } catch (IOException ex) {
                }
                //debugger.spawnFinishWhenClosed(pty, std, err);
                return exitCode.get();
            }

            @Override
            public int exitValue() {
                int debugExit = process.exitValue();
                int programExit = exitCode.get();
                if (programExit != 0) {
                    return programExit;
                } else {
                    return debugExit;
                }
            }

            @Override
            public void destroy() {
                process.destroy();
            }
        };

    }

    public static final class InputStreamWithCloseDetection extends FilterInputStream {

        private final CountDownLatch closed = new CountDownLatch(1);

        public InputStreamWithCloseDetection(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int r;
            try {
                r = super.read();
            } catch (IOException ex) {
                notifyClosed();
                throw ex;
            }
            if (r == -1) {
                notifyClosed();
            }
            return r;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int l;
            try {
                l = super.read(b);
            } catch (IOException ex) {
                notifyClosed();
                throw ex;
            }
            if (l == -1) {
                notifyClosed();
            }
            return l;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int l;
            try {
                l = super.read(b, off, len);
            } catch (IOException ex) {
                notifyClosed();
                throw ex;
            }
            if (l == -1) {
                notifyClosed();
            }
            return l;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                notifyClosed();
            }
        }

        private void notifyClosed() {
            closed.countDown();
        }

        /**
         * Wait till this stream is closed, or at EOF.
         */
        public void waitForClose() throws InterruptedException {
            closed.await();
        }
    }
}
