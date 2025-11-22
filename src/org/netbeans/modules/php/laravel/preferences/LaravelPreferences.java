/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.preferences;

import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;
import org.netbeans.modules.php.laravel.commands.DlightTerminalEnvironment;

/**
 *
 * @author bogdan
 */
public final class LaravelPreferences {

    private static final String APP_DIR = "appDir-path"; // NOI18N
    private static final String ENABLED = "laravel_enabled"; // NOI18N
    private static final String DEFAULT_APP_DIR = "app"; // NOI18N
    private static final String USE_REMOTE_CONNECTION = "use_remote_connection"; // NOI18N
    private static final String USE_DOCKER = "use_docker"; // NOI18N
    private static final String DOCKER_CONTAINER_NAME = "docker_container_name"; // NOI18N
    private static final String DOCKER_BASH_PATH = "docker_bash_path"; // NOI18N
    private static final String DOCKER_WORKDIR = "docker_workdir"; // NOI18N
    private static final String DOCKER_USE_TTY = "docker_use_tty"; // NOI18N
    private static final String DOCKER_USE_INTERACTIVE = "docker_use_interactive"; // NOI18N
    private static final String DOCKER_USER = "docker_user"; // NOI18N
    
    private static final boolean DEFAULT_DOCKER_TTY = true;
    private static final boolean DEFAULT_DOCKER_INTERACTIVE = true;

    private final Preferences modulePreferences;
    
    private LaravelPreferences(PhpModule module) {
        this.modulePreferences = getModulePreferences(module);
    }

    public static void setEnabled(PhpModule module, boolean useDocker) {
        getModulePreferences(module).putBoolean(ENABLED, useDocker);
    }
    
    public static void setUseDocker(PhpModule module, boolean useDocker) {
        getModulePreferences(module).putBoolean(USE_DOCKER, useDocker);
    }
    
    public static void setDockerContainerName(PhpModule module, String dockerContainerName) {
        getModulePreferences(module).put(DOCKER_CONTAINER_NAME, dockerContainerName);
    }

    public static void setDockerBashPath(PhpModule module, String bashPath) {
        getModulePreferences(module).put(DOCKER_BASH_PATH, bashPath);
    }

    public static void setDockerWorkdir(PhpModule module, String text) {
        getModulePreferences(module).put(DOCKER_WORKDIR, text);
    }

    public static void setRemoteConnectionFlag(PhpModule module, boolean remoteConnFlag) {
        getModulePreferences(module).putBoolean(USE_REMOTE_CONNECTION, remoteConnFlag);
    }
    
    public static void setDockerUseTty(PhpModule module, boolean bool) {
        getModulePreferences(module).putBoolean(DOCKER_USE_TTY, bool);
    }
    
    public static void setDockerUseInteractive(PhpModule module, boolean bool) {
        getModulePreferences(module).putBoolean(DOCKER_USE_INTERACTIVE, bool);
    }
    
    public static void setDockerUser(PhpModule module, String text) {
        getModulePreferences(module).put(DOCKER_USER, text);
    }
    
    public static Preferences getModulePreferences(PhpModule module) {
        return module.getPreferences(LaravelPhpFrameworkProvider.class, true);
    }

    public String getAppDir() {
        return modulePreferences.get(APP_DIR, DEFAULT_APP_DIR);
    }

    public boolean hasEnabledConfigured() {
        return modulePreferences.get(ENABLED, null) != null;
    }

    public boolean isEnabled() {
        return modulePreferences.getBoolean(ENABLED, false);
    }

    public String getDockerContainerName() {
        return modulePreferences.get(DOCKER_CONTAINER_NAME, null);
    }

    public String getDockerBashPath() {
        return modulePreferences.get(DOCKER_BASH_PATH, null);
    }

    public boolean getRemoteConnectionFlag() {
        return modulePreferences.getBoolean(USE_REMOTE_CONNECTION, false);
    }

    public boolean getUseDocker() {
        return modulePreferences.getBoolean(USE_DOCKER, false);
    }

    public String geDockerWorkdir() {
        return modulePreferences.get(DOCKER_WORKDIR, null);
    }
    
    public boolean getDockerUseTTy() {
        return modulePreferences.getBoolean(DOCKER_USE_TTY, DEFAULT_DOCKER_TTY);
    }
        
    public boolean getDockerUseInteractive() {
        return modulePreferences.getBoolean(DOCKER_USE_INTERACTIVE, DEFAULT_DOCKER_INTERACTIVE);
    }
    
    public String getDockerUser() {
        return modulePreferences.get(DOCKER_USER, null);
    }
    
    public static LaravelPreferences fromPhpModule(PhpModule module) {
        return new LaravelPreferences(module);
    }
}
