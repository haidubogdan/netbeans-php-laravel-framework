package org.netbeans.modules.php.laravel.executable;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.project.ComposerPackages;
import org.openide.windows.IOContainer;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.RetainLocation;
import org.openide.windows.TopComponent;

/**
 *
 * @author bhaidu
 */
@TopComponent.Description(
        preferredID = "LogIOTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@RetainLocation(value = "output")
public class TerminalComponent extends TopComponent {

    private static final Map<String, TerminalComponent> INSTANCES = new WeakHashMap<>();
    private final String laravelVersion;
    private InputOutput io;

    private TerminalComponent(String laravelVersion) {
        this.laravelVersion = laravelVersion;
        initComponents();
    }
    
    public static TerminalComponent getInstance(PhpModule phpModule)
    {
        String projectPath = phpModule.getProjectDirectory().getPath();

        synchronized (INSTANCES) {
            TerminalComponent terminalCompoment = INSTANCES.get(projectPath);
            if (terminalCompoment == null) {
                String laravelVersion = ComposerPackages.getInstance(phpModule).getLaravelVersion();
                terminalCompoment = new TerminalComponent(laravelVersion);
                INSTANCES.put(projectPath, terminalCompoment);
            }
            return terminalCompoment;
        }
    }

    private void initComponents() {
        setName("Output - Laravel " + laravelVersion);

        setLayout(new BorderLayout());
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane();
        JPanel infoPanelContent = new JPanel(new FlowLayout(FlowLayout.LEADING));
        scrollPane.setViewportView(infoPanelContent);
        infoPanel.add(scrollPane, BorderLayout.CENTER);

        add(infoPanel);
//        actionBar = new JToolBar();
//        actionBar.setFloatable(false);
//        actionBar.setOrientation(JToolBar.VERTICAL);
//        infoPanel.add(actionBar, BorderLayout.WEST);
        LogIOProvider  ioProvider = new LogIOProvider(infoPanelContent);
        IOContainer container = IOContainer.create(ioProvider);
        List<Action> actions = new ArrayList<>();

        io = IOProvider.getDefault().getIO(getName(), actions.toArray(new Action[0]), container);
        infoPanel.validate();
        infoPanel.repaint();
    }

    public InputOutput getIo() {
        return io;
    }
    

    public static String colorize(String msg) {
        return "\033[1;30m" + msg + "\033[0m"; // NOI18N
    }
        
    private static class LogIOProvider implements IOContainer.Provider {

        private JComponent parent;

        public LogIOProvider(JComponent parent) {
            this.parent = parent;
        }

        @Override
        public void open() {
        }

        @Override
        public void requestActive() {
        }

        @Override
        public void requestVisible() {
        }

        @Override
        public boolean isActivated() {
            return true;
        }

        @Override
        public void add(JComponent comp, IOContainer.CallBacks cb) {
            assert parent != null;
            parent.setLayout(new BorderLayout());
            parent.add(comp, BorderLayout.CENTER);
        }

        @Override
        public void remove(JComponent comp) {
            assert parent != null;
            parent.remove(comp);
        }

        @Override
        public void select(JComponent comp) {
        }

        @Override
        public JComponent getSelected() {
            return parent;
        }

        @Override
        public void setTitle(JComponent comp, String name) {
        }

        @Override
        public void setToolTipText(JComponent comp, String text) {
        }

        @Override
        public void setIcon(JComponent comp, Icon icon) {
        }

        @Override
        public void setToolbarActions(JComponent comp, Action[] toolbarActions) {
            //setButtons(toolbarActions);
        }

        @Override
        public boolean isCloseable(JComponent comp) {
            return false;
        }

        private void close() {
            parent = null;
        }

    }
}
