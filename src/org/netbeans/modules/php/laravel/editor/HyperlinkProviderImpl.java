package org.netbeans.modules.php.laravel.editor;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.*;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.DataEditorSupport;
import org.openide.util.Exceptions;

/**
 *
 * @author bhaidu
 */
@MimeRegistration(mimeType = "text/x-php5", service = HyperlinkProviderExt.class)
public class HyperlinkProviderImpl implements HyperlinkProviderExt {

    String methodName;
    String bladePath;

    public enum DeclarationType {
        VIEW_PATH;
    }

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        if (!hasLaravelProvider()){
            return null;
        }
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION, HyperlinkType.ALT_HYPERLINK);
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        return getHyperlinkSpan(doc, offset, type) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        if (!hasLaravelProvider()){
            return null;
        }
        if (!type.equals(HyperlinkType.GO_TO_DECLARATION)) {
            //not handled by a LSP handler
            return null;
        }

        BaseDocument baseDoc = (BaseDocument) doc;
        int lineStart = LineDocumentUtils.getLineStart(baseDoc, offset);
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null){
            return null;
        }
        
        Token<PHPTokenId> currentToken = tokensq.token();
        int startOffset = tokensq.offset();

        if (currentToken == null) {
            return null;
        }
        
        List<String> methodsWithViewArg =  Arrays.asList(LaravelUtils.methodsWithViewArg());

        PHPTokenId prevTokenId = null;

        while (tokensq.movePrevious() && tokensq.offset() >= lineStart) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }
            String text = token.text().toString();
            PHPTokenId id = token.id();

            if (prevTokenId != null && id.equals(PHPTokenId.PHP_STRING)
                    && methodsWithViewArg.contains(text)) {
                methodName = text;
                String quotedBladePath = currentToken.text().toString();
                bladePath = quotedBladePath.substring(1, quotedBladePath.length() - 1);
                return new int[]{startOffset, startOffset + currentToken.length()};
            }

            if (id.equals(PHPTokenId.PHP_TOKEN) && text.equals("(")) {
                prevTokenId = id;
            }
        }
        return null;
    }

    @Override
    public void performClickAction(Document doc, int offset, HyperlinkType type) {
        switch (type) {
            case GO_TO_DECLARATION:
                FileObject dir = LaravelPhpFrameworkProvider.getInstance().getSourceDirectory();
                if (dir != null) {
                    String viewPath = "resources/views/" + bladePath.replace(".", "/") + ".blade.php";
                    //FileObject views = dir.getFileObject("resources/views");
                    FileObject viewFile = dir.getFileObject(viewPath);
                    if (viewFile == null){
                        return;
                    }
                    openDocument(viewFile);
                }
                break;
            case ALT_HYPERLINK:
                JTextComponent focused = EditorRegistry.focusedComponent();
                if (focused != null && focused.getDocument() == doc) {
                    focused.setCaretPosition(offset);
                    //GoToImplementation.goToImplementation(focused);
                }
                break;
        }
    }

    private void openDocument(FileObject f) {
        try {
            DataObject dob = DataObject.find(f);
            DataEditorSupport ed = dob.getLookup().lookup(DataEditorSupport.class);
            //boolean isLoaded = ed.isDocumentLoaded();
            ed.open();
        }  catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }
    
    private boolean hasLaravelProvider(){
        //??not sure how
        return true;
    }

    @Override
    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        return bladePath;
    }

}
