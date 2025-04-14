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
package org.netbeans.modules.php.laravel.executable;

import java.nio.charset.Charset;
import org.netbeans.api.extexecution.ExecutionDescriptor.LineConvertorFactory;
import org.netbeans.modules.nativeexecution.api.execution.PostMessageDisplayer;
import org.openide.windows.InputOutput;

/**
 *
 * @author bogdan
 */
public class CustomExecutionDescriptor {

    boolean controllable;
    boolean frontWindow;
    boolean requestFocus;
    boolean inputVisible;
    InputOutput inputOutput;
    boolean outLineBased;
    boolean showProgress;
    Runnable postExecution;
    LineConvertorFactory errConvertorFactory;
    LineConvertorFactory outConvertorFactory;
    boolean resetInputOutputOnFinish = true;
    boolean closeInputOutputOnFinish = true;
    Charset charset;
    PostMessageDisplayer postMessageDisplayer;

    public CustomExecutionDescriptor inputOutput(InputOutput inputOutput) {
        this.inputOutput = inputOutput;
        return this;
    }

    public CustomExecutionDescriptor controllable(boolean controllable) {
        this.controllable = controllable;
        return this;
    }

    public CustomExecutionDescriptor frontWindow(boolean frontWindow) {
        this.frontWindow = frontWindow;
        return this;
    }

    public CustomExecutionDescriptor inputVisible(boolean inputVisible) {
        this.inputVisible = inputVisible;
        return this;
    }

    /**
     * Passed Runnable will be executed after process is finished and all I/O is
     * done. Also it is guaranteed that executed process's exitValue() is
     * available at this point...
     *
     * @param postExecution
     * @return
     */
    public CustomExecutionDescriptor postExecution(Runnable postExecution) {
        this.postExecution = postExecution;
        return this;
    }

    public CustomExecutionDescriptor showProgress(boolean showProgress) {
        this.showProgress = showProgress;
        return this;
    }
}
