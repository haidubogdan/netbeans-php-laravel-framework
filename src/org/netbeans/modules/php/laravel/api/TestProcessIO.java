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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.openide.util.Exceptions;

public class TestProcessIO {

    public static boolean isAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public static void main(JTextArea outputPane, JTextField inputPane) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("ssh-keygen", "-i");
        builder.redirectErrorStream(true); // so we can ignore the error stream
        Process process = builder.start();
        InputStream out = process.getInputStream();
        OutputStream in = process.getOutputStream();

        inputPane.addActionListener((ae) -> {
            Document doc = inputPane.getDocument();
            try {
                try {
                    in.write(doc.getText(0, doc.getLength()).getBytes());
                    in.flush();
                    System.out.println("Sent " + doc.getText(0, doc.getLength()));
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                doc.remove(0, doc.getLength());
            } catch (BadLocationException ex) {
                Logger.getLogger(TerminalEm.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        byte[] buffer = new byte[4000];
        while (isAlive(process)) {
            int no = out.available();
            if (no > 0) {
                int n = out.read(buffer, 0, Math.min(no, buffer.length));
                String output = new String(buffer, 0, n);
                System.err.println(output);
                outputPane.setText(output);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                int x = 1;
            }
        }

        System.out.println(process.exitValue());
    }
}
