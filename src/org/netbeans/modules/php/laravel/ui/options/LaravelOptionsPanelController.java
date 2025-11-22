/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.ui.options;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@UiUtils.PhpOptionsPanelRegistration(
        id = LaravelOptionsPanelController.ID,
        displayName = "Laravel",
        position = 2200
)
public class LaravelOptionsPanelController extends OptionsPanelController {

    static final String ID = "Laravel"; // NOI18N
    public static final String OPTIONS_SUBPATH = UiUtils.FRAMEWORKS_AND_TOOLS_SUB_PATH + "/" + ID; // NOI18N

    private LaravelOptionsPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
 
    @Override
    public void update() {
        getPanel().load();
    }

    @Override
    public void applyChanges() {
        getPanel().store();
    }

    @Override
    public void cancel() {
        getPanel().cancel();
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return getPanel().changed();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(LaravelOptions.OPTIONS_ID); //NOI18N
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private LaravelOptionsPanel getPanel() {
        if (panel == null) {
            panel = new LaravelOptionsPanel(this);
        }
        return panel;
    }

    public static String getOptionsPath() {
        return UiUtils.OPTIONS_PATH + "/" + OPTIONS_SUBPATH; // NOI18N
    }
}
