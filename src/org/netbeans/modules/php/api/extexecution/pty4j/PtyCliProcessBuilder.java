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
package org.netbeans.modules.php.api.extexecution.pty4j;

import com.pty4j.PtyProcess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.extexecution.base.WrapperProcess;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Parameters;

/**
 * used just for windows gitbash terminal through pty4j library
 *
 * @author bogdan
 */
public class PtyCliProcessBuilder implements Callable<Process> {

    private final List<String> arguments = new ArrayList<>();

    public PtyCliProcessBuilder() {

    }

    @Override
    public Process call() throws Exception {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm"); // NOI18N
        env.put("LANG", "C.UTF-8"); // NOI18N

        String[] cmd = arguments.toArray(new String[0]);
        try {
            PtyProcess process = PtyProcess.exec(cmd, env, System.getProperty("user.home"));
            String uuid = UUID.randomUUID().toString();
            WrapperProcess wp = new WrapperProcess(process, uuid);
            return wp;
        } catch (Exception ex) {
            NotifyDescriptor.Message message = new NotifyDescriptor.Message(
                    ex.getMessage(),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(message);
        }
        return null;
    }

    public void setArguments(@NonNull List<String> arguments) {
        Parameters.notNull("arguments", arguments);

        this.arguments.clear();
        this.arguments.addAll(arguments);
    }
}
