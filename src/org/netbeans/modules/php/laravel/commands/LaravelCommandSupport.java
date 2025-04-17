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
package org.netbeans.modules.php.laravel.commands;

import org.netbeans.modules.php.laravel.ArtisanScript;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.netbeans.modules.nativeexecution.api.util.WindowsSupport;
import org.netbeans.modules.nativeexecution.pty.NbStartUtility;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.executable.CustomProcessInfo;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommand;
import org.netbeans.modules.php.spi.framework.commands.FrameworkCommandSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author bhaidu
 */
public class LaravelCommandSupport extends FrameworkCommandSupport {

    ArtisanCommandSupport artisanSupport;

    public LaravelCommandSupport(PhpModule phpModule) {
        super(phpModule);
        artisanSupport = ArtisanCommandSupport.getInstance(phpModule);
    }

    @Override
    public String getFrameworkName() {
        return "Laravel";// NOI18N
    }

    @Override
    public void runCommand(CommandDescriptor commandDescriptor, Runnable postExecution) {

        String[] commands = commandDescriptor.getFrameworkCommand().getCommands();
        String[] commandParams = commandDescriptor.getCommandParams();
        List<String> params = new ArrayList<>(commands.length + commandParams.length);
        params.addAll(Arrays.asList(commands));
        params.addAll(Arrays.asList(commandParams));

        ArtisanScript artisan = ArtisanScript.forPhpModule(phpModule, true);

        if (artisan != null) {
            artisan.runCommand(phpModule, params, postExecution);
        }

        //ExecutableService.executeCommand(phpModule, artisanSupport, params);
    }

    @Override
    protected List<FrameworkCommand> getFrameworkCommandsInternal() {
        if (1==1) {
            return ArtisanScript.forPhpModule(phpModule, true).getCommands(phpModule);
        }
        List<FrameworkCommand> commands = new ArrayList<>();

        if (artisanSupport.getCommands().isEmpty()) {
            ExecutableService.extractArtisanCommands(phpModule, artisanSupport);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }

            if (artisanSupport.getCommands().isEmpty()) {
                return null;
            }
        }

        commands.addAll(artisanSupport.getCommands());
        return commands;
    }

    @Override
    protected String getOptionsPath() {
        return ArtisanScript.getOptionsPath();
    }

    @Override
    protected File getPluginsDirectory() {
        FileObject sourceDirectory = phpModule.getSourceDirectory();
        if (sourceDirectory == null) {
            // broken project
            return null;
        }
        FileObject vendor = sourceDirectory.getFileObject("vendor"); // NOI18N
        if (vendor != null && vendor.isFolder()) {
            return FileUtil.toFile(vendor);
        }
        return null;
    }

    private List<String> getComands(CustomProcessInfo info) {
        List<String> command = new ArrayList<>();
        String _nbStartPath = null;
        try {
            _nbStartPath = NbStartUtility.getInstance().getPath(info.getExecutionEnvironment());
        } catch (IOException ex) {
        } finally {
        }
        boolean isWindows = info.getExecutionEnvironment().isLocal() && Utilities.isWindows();
        if (isWindows) {
            String nbStartPath = WindowsSupport.getInstance().convertToShellPath(_nbStartPath);

            if (nbStartPath != null) {
                _nbStartPath = nbStartPath;
            }
        }
        command.add(_nbStartPath);
        return command;
    }

}
