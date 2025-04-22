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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author bogdan.haidu
 */
public class TerminalEm extends JFrame {

    private final JScrollPane scrollPane;
    private PrintWriter writer;

    public TerminalEm(JTextArea outputPane, JTextField inputPane) {

        scrollPane = new JScrollPane(outputPane);

        outputPane.setEditable(false);
        Map<TextAttribute, Object> attr = new HashMap<>();
        attr.put(TextAttribute.FAMILY, Font.MONOSPACED);
        outputPane.setFont(new Font(attr));


        add(scrollPane, BorderLayout.CENTER);
        add(inputPane, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                writer.close();
            }

        });
        setSize(600, 400);

        PipedOutputStream toIn = new PipedOutputStream();
        try {
            System.setIn(new PipedInputStream(toIn));
        } catch (IOException ex) {
            Logger.getLogger(TerminalEm.class.getName()).log(Level.SEVERE, null, ex);
        }
        writer = new PrintWriter(toIn, true);
    }

    public static void launchAndInstallTerminal(JTextArea outputPane, JTextField inputPane) {
        RequestProcessor Rp = new RequestProcessor("Terminald", 1, true);
        Rp.post(new Runnable() {
            @Override
            public void run() {
                TerminalEm te = new TerminalEm(outputPane, inputPane);
                te.setVisible(true);
            }
        });
    }
}
