package org.netbeans.modules.php.laravel.editor.completion;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.php.laravel.editor.ResourceUtilities;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/**
 *
 * @author bogdan
 */
public class LaravelCompletionItem implements CompletionItem {

    public static final String VIEWS_FOLDER_FRAGMENT = "/views/";
    protected static final int DEFAULT_SORT_PRIORITY = 20;
    protected int substitutionOffset;
    protected String text;
    protected boolean shift;

    @Override
    public void defaultAction(JTextComponent component) {
        if (component != null) {
            if (!shift) {
                Completion.get().hideDocumentation();
                Completion.get().hideCompletion();
            }
            int caretOffset = component.getSelectionEnd();
            int len = caretOffset - substitutionOffset;
            if (len >= 0) {
                substituteText(component, len);
            }
        }
    }

    @Override
    public void processKeyEvent(KeyEvent e) {
        shift = (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED && e.isShiftDown());
    }

    @Override
    public int getPreferredWidth(Graphics grphcs, Font font) {
        return CompletionUtilities.getPreferredWidth(getLeftHtmlText(), getRightHtmlText(), grphcs, font);
    }

    protected String getLeftHtmlText() {
        return text;
    }

    protected String getRightHtmlText() {
        return null;
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor, Color backgroundColor, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(getIcon(), getLeftHtmlText(), getRightHtmlText(), g, defaultFont, defaultColor, width, height, selected);
    }

    protected ImageIcon getIcon() {
        return null;
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return null;
    }

    @Override
    public CompletionTask createToolTipTask() {
        return null;
    }

    @Override
    public boolean instantSubstitution(JTextComponent component) {
        if (component != null) {
            try {
                int caretOffset = component.getSelectionEnd();
                if (caretOffset > substitutionOffset) {
                    String currentText = component.getDocument().getText(substitutionOffset, caretOffset - substitutionOffset);
                    if (!getSubstituteText().startsWith(currentText)) {
                        return false;
                    }
                }
            } catch (BadLocationException ble) {
            }
        }
        defaultAction(component);
        return true;
    }

    @Override
    public int getSortPriority() {
        return DEFAULT_SORT_PRIORITY;
    }

    @Override
    public CharSequence getSortText() {
        return getItemText();
    }

    @Override
    public CharSequence getInsertPrefix() {
        return getItemText();
    }

    protected String getSubstituteText() {
        return getItemText();
    }

    public String getItemText() {
        return text;
    }

    private boolean substituteText(JTextComponent component, int len) {
        return substituteText(component, getSubstituteText(), len, 0);
    }

    private boolean substituteText(JTextComponent c, final String substituteText, final int len, int moveBack) {
        final BaseDocument doc = (BaseDocument) c.getDocument();
        final boolean[] result = new boolean[1];
        result[0] = true;

        doc.runAtomic(new Runnable() {
            @Override
            public void run() {
                try {
                    //test whether we are trying to insert sg. what is already present in the text
                    String currentText = doc.getText(substitutionOffset, (doc.getLength() - substitutionOffset) < substituteText.length() ? (doc.getLength() - substitutionOffset) : substituteText.length());
                    if (!substituteText.equals(currentText)) {
                        //remove common part
                        doc.remove(substitutionOffset, len);
                        insertString(doc, substitutionOffset, substituteText, c);
                    } else {
                        c.setCaretPosition(c.getSelectionEnd() + substituteText.length() - len);
                    }
                } catch (BadLocationException ex) {
                    result[0] = false;
                }

            }
        });

        //format the inserted text
        reindent(c);

        if (moveBack != 0) {
            Caret caret = c.getCaret();
            int dot = caret.getDot();
            caret.setDot(dot - moveBack);
        }

        return result[0];
    }

    protected void insertString(BaseDocument doc, int substitutionOffset,
            String substituteText, JTextComponent c) throws BadLocationException {
        doc.insertString(substitutionOffset, substituteText, null);
    }

    protected void reindent(JTextComponent c) {

    }

    public static class ViewPath extends LaravelCompletionItem {

        protected boolean isFolder;
        protected String filePath;

        public ViewPath(String name, int substitutionOffset,
                boolean isFolder, String filePath) {
            this.text = name;
            this.substitutionOffset = substitutionOffset;
            this.isFolder = isFolder;
            this.filePath = filePath;
        }

        @Override
        protected ImageIcon getIcon() {
            return ResourceUtilities.loadFileResourceIcon(isFolder);
        }

        @Override
        protected String getRightHtmlText() {
            int viewsPos = filePath.indexOf(VIEWS_FOLDER_FRAGMENT);
            if (viewsPos >= 0) {
                return filePath.substring(viewsPos, filePath.length());
            }
            return filePath;
        }
    }

    public static class ConfigPath extends LaravelCompletionItem {

        protected boolean isParent;
        protected String filePath;

        public ConfigPath(String name, int substitutionOffset,
                boolean isFolder, String filePath) {
            this.text = name;
            this.substitutionOffset = substitutionOffset;
            this.isParent = isFolder;
            this.filePath = filePath;
        }

        @Override
        protected ImageIcon getIcon() {
            return ResourceUtilities.loadFileResourceIcon(isParent);
        }

        @Override
        protected String getRightHtmlText() {
            int viewsPos = filePath.indexOf(VIEWS_FOLDER_FRAGMENT);
            if (viewsPos >= 0) {
                return filePath.substring(viewsPos, filePath.length());
            }
            return filePath;
        }
    }
}
