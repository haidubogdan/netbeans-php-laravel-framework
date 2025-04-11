package org.netbeans.modules.php.laravel.ui.options;

/**
 *
 * @author bogdan
 */
public class LaravelOptions {

    public static final String OPTIONS_ID = "org.netbeans.modules.php.laravel.editor.Options"; // NOI18N

    private static final LaravelOptions INSTANCE = new LaravelOptions();

    public static LaravelOptions getInstance() {
        return INSTANCE;
    }

}
