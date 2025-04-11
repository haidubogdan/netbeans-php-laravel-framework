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

import org.netbeans.modules.extexecution.InputOutputManager;
import org.netbeans.modules.extexecution.StopAction;
import org.netbeans.modules.extexecution.RerunAction;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.extexecution.base.BaseExecutionDescriptor;
import org.netbeans.api.extexecution.base.BaseExecutionService;
import org.netbeans.api.extexecution.base.ParametrizedRunnable;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.print.LineProcessors;
import org.netbeans.modules.php.api.extexecution.pty4j.PtyExecutionDescriptor.InputProcessorFactory2;
import org.netbeans.modules.php.api.extexecution.pty4j.PtyExecutionDescriptor.LineConvertorFactory;
import org.openide.util.Cancellable;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

/**
 * used just for windows gitbash terminal through pty4j library
 * 
 * @author bogdan
 */
public class PtyExecutionService {

    private final Callable<Process> processCreator;

    private final PtyExecutionDescriptor descriptor;

    private final String originalDisplayName;

    private PtyExecutionService(Callable<Process> processCreator, String displayName, PtyExecutionDescriptor descriptor) {
        this.processCreator = processCreator;
        this.originalDisplayName = displayName;
        this.descriptor = descriptor;
    }

    /**
     * Creates new execution service. Service will wrap up the processes created
     * by <code>processCreator</code> and will manage them.
     *
     * @param processCreator callable returning the process to wrap up
     * @param descriptor descriptor describing the configuration of service
     * @param displayName display name of this service
     * @return new execution service
     */
    @NonNull
    public static PtyExecutionService newService(@NonNull Callable<Process> processCreator,
            @NonNull PtyExecutionDescriptor descriptor, @NonNull String displayName) {
        return new PtyExecutionService(processCreator, displayName, descriptor);
    }
    
    @NonNull
    public Future<Integer> run() {
        return run(null);
    }

    private Future<Integer> run(InputOutput required) {
        final InputOutputManager.InputOutputData ioData = getInputOutput(required);

        final String displayName = ioData.getDisplayName();
        final ProgressCancellable cancellable = descriptor.isControllable() ? new ProgressCancellable() : null;
        final ProgressHandle handle = createProgressHandle(ioData.getInputOutput(), displayName, cancellable);
        final InputOutput io = ioData.getInputOutput();

        final OutputWriter out = io.getOut();
        final OutputWriter err = io.getErr();
        final Reader in = io.getIn();

        class ExecutedHolder {
            private boolean executed = false;
        }
        final ExecutedHolder executed = new ExecutedHolder();

        BaseExecutionDescriptor realDescriptor = new BaseExecutionDescriptor();
        realDescriptor = realDescriptor.charset(Charset.defaultCharset());
        realDescriptor = realDescriptor.inReaderFactory(new BaseExecutionDescriptor.ReaderFactory() {

            @Override
            public Reader newReader() {
                return in;
            }
        });
        realDescriptor = realDescriptor.preExecution(new Runnable() {

            @Override
            public void run() {
                executed.executed = true;
                Runnable orig = descriptor.getPreExecution();
                if (orig != null) {
                    orig.run();
                }
            }
        });
        realDescriptor = realDescriptor.postExecution(new ParametrizedRunnable<Integer>() {

            @Override
            public void run(Integer exitCode) {
                cleanup(handle, ioData, ioData.getInputOutput() != descriptor.getInputOutput(),
                        descriptor.isFrontWindowOnError() && exitCode != null && exitCode != 0);
                Consumer<? super Integer> orig = descriptor.getPostExecution();
                if (orig != null) {
                    orig.accept(exitCode);
                }
            }
        });
        realDescriptor = realDescriptor.outProcessorFactory(new BaseExecutionDescriptor.InputProcessorFactory() {

            @Override
            public InputProcessor newInputProcessor() {
                return createOutProcessor(out);
            }
        });
        realDescriptor = realDescriptor.errProcessorFactory(new BaseExecutionDescriptor.InputProcessorFactory() {

            @Override
            public InputProcessor newInputProcessor() {
                return createErrProcessor(err);
            }
        });
        
        BaseExecutionService service = BaseExecutionService.newService(processCreator, realDescriptor);
        final Future<Integer> delegate = service.run();
        
        final Future<Integer> current = new Future<Integer>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean ret = delegate.cancel(mayInterruptIfRunning);
                if (!executed.executed) {
                    // not executed at all - passing false to show
                    cleanup(handle, ioData, false);

                    synchronized (InputOutputManager.class) {
                        if (ioData.getInputOutput() != descriptor.getInputOutput()) {
                            InputOutputManager.addInputOutput(ioData);
                        }
                    }
                } else {
// uncomment this if state after cancel should be the same as when completed normally
//                    if (ret && mayInterruptIfRunning && executed.executed) {
//                        try {
//                            latch.await();
//                        } catch (InterruptedException ex) {
//                            Thread.currentThread().interrupt();
//                        }
//                    }
                }
                return ret;
            }

            @Override
            public boolean isCancelled() {
                return delegate.isCancelled();
            }

            @Override
            public boolean isDone() {
                return delegate.isDone();
            }

            @Override
            public Integer get() throws InterruptedException, ExecutionException {
                return delegate.get();
            }

            @Override
            public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return delegate.get(timeout, unit);
            }
        };

        final StopAction workingStopAction = ioData.getStopAction();
        final RerunAction workingRerunAction = ioData.getRerunAction();

        Mutex.EVENT.readAccess(new Runnable() {
            @Override
            public void run() {
                if (workingStopAction != null) {
                    synchronized (workingStopAction) {
                        workingStopAction.setTask(current);
                        workingStopAction.setEnabled(true);
                    }
                }

                if (workingRerunAction != null) {
                    synchronized (workingRerunAction) {
//                        workingRerunAction.setExecutionService(PtyExecutionService.this);
//                        workingRerunAction.setRerunCondition(descriptor.getRerunCondition());
//                        workingRerunAction.setRerunCallback(descriptor.getRerunCallback());
//                        workingRerunAction.setEnabled(false);
                    }
                }
            }
        });

        if (cancellable != null) {
            cancellable.setTask(current);
        }

        return current;
    }

    /**
     * Retrieves or creates the output window usable for the current run.
     *
     * @param required output window required by rerun or <code>null</code>
     * @return the output window usable for the current run
     */
    private InputOutputManager.InputOutputData getInputOutput(InputOutput required) {
        InputOutputManager.InputOutputData result = null;

        synchronized (InputOutputManager.class) {
            InputOutput io = descriptor.getInputOutput();
            if (io != null) {
                result = new InputOutputManager.InputOutputData(io,
                        originalDisplayName, null, null, null);
            }

            // try to acquire required one (rerun action)
            // this will always succeed if this method is called from EDT
            if (result == null) {
                result = InputOutputManager.getInputOutput(required);
            }

            // try to find free output windows
            if (result == null) {
                result = InputOutputManager.getInputOutput(
                        originalDisplayName, descriptor.isControllable(), descriptor.getOptionsPath());
            }

            // free IO was not found, create new one
            if (result == null) {
                result = InputOutputManager.createInputOutput(
                        originalDisplayName, descriptor.isControllable(), descriptor.getOptionsPath());
            }

            configureInputOutput(result.getInputOutput());
        }

        return result;
    }

    /**
     * Configures the output window before usage.
     *
     * @param inputOutput output window to configure
     */
    private void configureInputOutput(InputOutput inputOutput) {
        if (inputOutput == InputOutput.NULL) {
            return;
        }

        if (descriptor.getInputOutput() == null || !descriptor.noReset()) {
            try {
                inputOutput.getOut().reset();
            } catch (IOException exc) {
//                LOGGER.log(Level.INFO, null, exc);
            }

            // Note - do this AFTER the reset() call above; if not, weird bugs occur
            inputOutput.setErrSeparated(false);
        }

        // Open I/O window now. This should probably be configurable.
        if (descriptor.isFrontWindow()) {
            inputOutput.select();
        }

        inputOutput.setInputVisible(descriptor.isInputVisible());
    }

    private ProgressHandle createProgressHandle(InputOutput inputOutput,
            String displayName, Cancellable cancellable) {

        if (!descriptor.showProgress() && !descriptor.showSuspended()) {
            return null;
        }

        ProgressHandle handle = ProgressHandle.createHandle(displayName,
                cancellable, new ProgressAction(inputOutput));

        handle.setInitialDelay(0);
        handle.start();
        handle.switchToIndeterminate();

        if (descriptor.showSuspended()) {
            handle.suspend(NbBundle.getMessage(PtyExecutionService.class, "Running"));
        }

        return handle;
    }

    private void cleanup(final ProgressHandle progressHandle,
            final InputOutputManager.InputOutputData inputOutputData,
            final boolean managed, final boolean show) {

        cleanup(progressHandle, inputOutputData, show);

        synchronized (InputOutputManager.class) {
            if (managed) {
                InputOutputManager.addInputOutput(inputOutputData);
            }
        }
    }

    private void cleanup(final ProgressHandle progressHandle,
            final InputOutputManager.InputOutputData inputOutputData, final boolean show) {

        Runnable ui = new Runnable() {
            @Override
            public void run() {
                if (show) {
                    inputOutputData.getInputOutput().select();
                }
                if (inputOutputData.getStopAction() != null) {
                    inputOutputData.getStopAction().setEnabled(false);
                }
                if (inputOutputData.getRerunAction() != null) {
                    inputOutputData.getRerunAction().setEnabled(true);
                }
                if (progressHandle != null) {
                    progressHandle.finish();
                }
            }
        };

        Mutex.EVENT.readAccess(ui);
    }

    private InputProcessor createOutProcessor(OutputWriter writer) {
        LineConvertorFactory convertorFactory = descriptor.getOutConvertorFactory();
        InputProcessor outProcessor = null;
        if (descriptor.isOutLineBased()) {
            outProcessor = InputProcessors.bridge(LineProcessors.printing(writer,
                    convertorFactory != null ? convertorFactory.newLineConvertor() : null, true));
        } else {
            outProcessor = org.netbeans.api.extexecution.print.InputProcessors.printing(writer,
                    convertorFactory != null ? convertorFactory.newLineConvertor() : null, true);
        }

        InputProcessorFactory2 descriptorOutFactory2 = descriptor.getOutProcessorFactory2();
        if (descriptorOutFactory2 != null) {
            outProcessor = descriptorOutFactory2.newInputProcessor(outProcessor);
        }


        return outProcessor;
    }

    private InputProcessor createErrProcessor(OutputWriter writer) {
        LineConvertorFactory convertorFactory = descriptor.getErrConvertorFactory();
        InputProcessor errProcessor = null;
        if (descriptor.isErrLineBased()) {
            errProcessor = InputProcessors.bridge(LineProcessors.printing(writer,
                    convertorFactory != null ? convertorFactory.newLineConvertor() : null, false));
        } else {
            errProcessor = org.netbeans.api.extexecution.print.InputProcessors.printing(writer,
                    convertorFactory != null ? convertorFactory.newLineConvertor() : null, false);
        }

        InputProcessorFactory2 descriptorErrFactory2 = descriptor.getErrProcessorFactory2();
        if (descriptorErrFactory2 != null) {
            errProcessor = descriptorErrFactory2.newInputProcessor(errProcessor);
        }

        return errProcessor;
    }

    private static class ProgressCancellable implements Cancellable {

        private Future<Integer> task;

        public ProgressCancellable() {
            super();
        }

        public synchronized void setTask(Future<Integer> task) {
            this.task = task;
        }

        @Override
        public synchronized boolean cancel() {
            if (task != null) {
                task.cancel(true);
            }
            return true;
        }
    }

    private static class ProgressAction extends AbstractAction {

        private final InputOutput io;

        public ProgressAction(InputOutput io) {
            this.io = io;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            io.select();
        }
    }
}
