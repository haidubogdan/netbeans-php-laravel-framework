package org.netbeans.modules.php.laravel.options;

import org.openide.util.ChangeSupport;

/**
 *
 * @author bogdan
 */
public class LaravelOptions {

    private static final String PREFERENCES_PATH = "laravel"; // NOI18N

    private static final LaravelOptions INSTANCE = new LaravelOptions();

    final ChangeSupport changeSupport = new ChangeSupport(this);

    private volatile boolean installerSearched = false;

    public static LaravelOptions getInstance() {
        return INSTANCE;
    }

}
