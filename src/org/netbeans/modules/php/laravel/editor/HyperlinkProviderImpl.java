package org.netbeans.modules.php.laravel.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.laravel.ConfigurationFiles;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;
import org.netbeans.modules.php.laravel.astnodes.ArrayFileVisitor.ConfigNamespace;
import org.netbeans.modules.php.laravel.project.LaravelAppSupport;
import org.netbeans.modules.php.laravel.editor.model.ConfigurationModel;
import org.netbeans.modules.php.laravel.editor.model.RoutesModel;
import org.netbeans.modules.php.laravel.utils.PathUtils;
import org.netbeans.modules.php.laravel.project.ProjectUtils;
import static org.netbeans.modules.php.laravel.utils.LaravelUtils.CONFIG_METHOD;
import static org.netbeans.modules.php.laravel.utils.LaravelUtils.VIEW_METHOD;
import static org.netbeans.modules.php.laravel.editor.plugins.inertia.InertiaPluginConstants.PAGES_RESOURCES_DIR_PATH;
import org.netbeans.modules.php.laravel.utils.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 * Similar to a declaration finder
 *
 * @author bhaidu
 */
@MimeRegistrations({
    @MimeRegistration(mimeType = FileUtils.PHP_MIME_TYPE, service = HyperlinkProviderExt.class),
    @MimeRegistration(mimeType = "text/x-blade", service = HyperlinkProviderExt.class)   
})
public class HyperlinkProviderImpl implements HyperlinkProviderExt {

    private static final int MIN_QUOTED_QUERY_TEXT_LENGTH = 5;

    //move to a handler
    public enum DeclarationType {
        VIEW_PATH,
        ROUTE_LABEL,
        CONFIG_PATH;
    }

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION, HyperlinkType.ALT_HYPERLINK);
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        if (!isInLaravelModule(doc)) {
            return false;
        }
        return getHyperlinkSpan(doc, offset, type) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        if (!isInLaravelModule(doc)) {
            return null;
        }
        if (!type.equals(HyperlinkType.GO_TO_DECLARATION)) {
            return null;
        }

        TokenSequence<PHPTokenId> tokensq = tokenSequenceForDeclFinder(doc, offset);

        if (tokensq == null) {
            return null;
        }

        BaseDocument baseDoc = (BaseDocument) doc;
        int offsetCorrection = 1; //allow new line formatting
        int lineStartOffset = LineDocumentUtils.getLineStart(baseDoc, offset);
        int startOffset = tokensq.offset();
        Token<PHPTokenId> currentToken = tokensq.token();
        PHPTokenId openParenToken = null;

        while (tokensq.movePrevious() && tokensq.offset() >= (lineStartOffset - offsetCorrection)) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }
            String text = token.text().toString();

            PHPTokenId id = token.id();

            //skip whitespace tokens but allow searching for the prev tokens
            if (id.equals(PHPTokenId.WHITESPACE)) {
                offsetCorrection += text.length();
                continue;
            }

            if (openParenToken != null && id.equals(PHPTokenId.PHP_STRING)) {
                String methodName = text;

                switch (methodName) {
                    case "route": // NOI18N
                    case CONFIG_METHOD:
                    case VIEW_METHOD:
                    case "make": // NOI18N
                    case "render": // NOI18N
                    case "send": // NOI18N
                    case "loadView": // NOI18N
                    //plugins
                    case "inertia": // NOI18N 
                        return new int[]{startOffset, startOffset + currentToken.length()};
                    default:
                        return null;
                }
            }

            if (EditorUtils.isOpenParenToken(id, text)) {  // NOI18N
                openParenToken = id;
            }
        }
        return null;
    }

    @Override
    public void performClickAction(Document doc, int offset, HyperlinkType type) {
        switch (type) {
            case GO_TO_DECLARATION:
                PhpModule module = ProjectUtils.getPhpModule(doc);
                FileObject sourceDir = module.getSourceDirectory();
                if (sourceDir == null) {
                    return;
                }
                HyperlinkInfo info = getContextHyperlinkInfo(doc, offset, sourceDir);

                if (info != null) {
                    openDocument(info.getGoToFile(), info.getOffset());
                }

                break;
            case ALT_HYPERLINK:
                JTextComponent focused = EditorRegistry.focusedComponent();
                if (focused != null && focused.getDocument() == doc) {
                    focused.setCaretPosition(offset);
                }
                break;
        }
    }

    private void openDocument(FileObject f, int offset) {
        try {
            DataObject dob = DataObject.find(f);
            NbDocument.openDocument(dob, offset, Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private boolean isInLaravelModule(Document doc) {
        return ProjectUtils.isInLaravelModule(doc);
    }

    @Override
    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        PhpModule module = ProjectUtils.getPhpModule(doc);
        FileObject sourceDir = module.getSourceDirectory();
        String tooltipText = "";
        if (sourceDir != null) {
            HyperlinkInfo info = getContextHyperlinkInfo(doc, offset, sourceDir);
            if (info != null) {
                tooltipText = info.generateTooltipText();
            }
        }

        return "<html><body>" + tooltipText + "</body></html>";
    }

    private HyperlinkInfo getContextHyperlinkInfo(Document doc, int offset, FileObject sourceDir) {
        TokenSequence<PHPTokenId> tokensq = tokenSequenceForDeclFinder(doc, offset);

        if (tokensq == null) {
            return null;
        }

        BaseDocument baseDoc = (BaseDocument) doc;
        int offsetCorrection = 1; //allow new line formatting
        int lineStart = LineDocumentUtils.getLineStart(baseDoc, offset);

        Token<PHPTokenId> currentToken = tokensq.token();
        PHPTokenId openParenToken = null;
        boolean lookForClass = false;

        String currentTokenText = currentToken.text().toString();
        String referenceText = currentTokenText.substring(1, currentTokenText.length() - 1);
        int counter = 0;

        while (tokensq.movePrevious() && tokensq.offset() >= (lineStart - offsetCorrection)) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }
            String text = token.text().toString();
            PHPTokenId id = token.id();

            //skip whitespace tokens but allow searching for the prev tokens
            if (id.equals(PHPTokenId.WHITESPACE)) {
                offsetCorrection += text.length();
                continue;
            }

            if (!lookForClass && openParenToken != null && id.equals(PHPTokenId.PHP_STRING)) {
                String methodName = token.text().toString();

                switch (methodName) {
                    case VIEW_METHOD: // NOI18N
                    case "make": // NOI18N
                    case "send": // NOI18N
                    case "loadView": { // NOI18N
                        String viewPath = PathUtils.LARAVEL_VIEW_PATH + "/" + referenceText.replace(".", "/") + PathUtils.BLADE_EXT;
                        FileObject goToFile = sourceDir.getFileObject(viewPath);
                        if (goToFile == null || !goToFile.isValid()) {
                            return null;
                        }
                        return new HyperlinkInfo(referenceText, goToFile, 0, DeclarationType.VIEW_PATH);
                    }
                    case "render": // NOI18N
                        lookForClass = true;
                        break;
                    case "inertia": {
                        String vuePath = PAGES_RESOURCES_DIR_PATH + "/" + referenceText + ".vue";
                        FileObject goToVueFile = sourceDir.getFileObject(vuePath);
                        if (goToVueFile == null || !goToVueFile.isValid()) {
                            return null;
                        }
                        return new HyperlinkInfo(referenceText, goToVueFile, 0, DeclarationType.VIEW_PATH);
                    }
                    case CONFIG_METHOD:
                        return configurationFileHyperlink(referenceText, sourceDir);
                    case "route":  // NOI18N
                        return routeLabelHyperlink(doc, referenceText);
                    default:
                        return null;
                }
            } else if (lookForClass && id.equals(PHPTokenId.PHP_STRING)) {
                //TO DO implement a handler
                if (text.equals("Inertia")) {
                    String vuePath = PAGES_RESOURCES_DIR_PATH + "/" + referenceText + ".vue";
                    FileObject goToVueFile = sourceDir.getFileObject(vuePath);
                    if (goToVueFile == null || !goToVueFile.isValid()) {
                        return null;
                    }
                    return new HyperlinkInfo(referenceText, goToVueFile, 0, DeclarationType.VIEW_PATH);
                } else {
                    String viewPath = PathUtils.LARAVEL_VIEW_PATH + "/" + referenceText.replace(".", "/") + PathUtils.BLADE_EXT;
                    FileObject goToFile = sourceDir.getFileObject(viewPath);
                    if (goToFile == null || !goToFile.isValid()) {
                        return null;
                    }
                    return new HyperlinkInfo(referenceText, goToFile, 0, DeclarationType.VIEW_PATH);
                }
            }

            if (EditorUtils.isOpenParenToken(id, text)) {
                openParenToken = id;
            }

            counter++;
        }

        return null;
    }

    private TokenSequence<PHPTokenId> tokenSequenceForDeclFinder(Document doc, int offset) {
        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null) {
            return null;
        }

        Token<PHPTokenId> token = tokensq.token();

        if (token == null) {
            return null;
        }

        String focusedText = token.text().toString();

        //2 char config are not that relevant
        if (focusedText.length() < MIN_QUOTED_QUERY_TEXT_LENGTH
                || !StringUtils.isQuotedString(focusedText)) {
            return null;
        }
        return tokensq;
    }

    private HyperlinkInfo configurationFileHyperlink(String referenceText, FileObject sourceDir) {
        String[] queryConfigNamespace = referenceText.split("\\.");

        ConfigurationFiles confFiles = LaravelPhpFrameworkProvider.getInstance().getConfigurationFiles2(sourceDir);

        if (confFiles == null) {
            return null;
        }

        FileObject configDirectory = confFiles.getConfigDirectory();

        if (configDirectory == null) {
            return null;
        }

        ConfigurationModel model = ConfigurationModel.getModel(configDirectory);
        Map<FileObject, ConfigNamespace> confFileNamespace = model.getConfigurationFileNamespace();

        ArrayList<ConfigNamespace> queue = new ArrayList<>();
        String configNamespaceConcat;

        for (Map.Entry<FileObject, ConfigNamespace> entry : confFileNamespace.entrySet()) {
            ConfigNamespace root = entry.getValue();
            if (!root.namespace.equals(queryConfigNamespace[0])) {
                continue;
            }
            queue.addAll(root.children);
            configNamespaceConcat = root.namespace;
            int treeDepth = 2;
            while (queue.size() > 0) {
                ConfigNamespace children = queue.get(0);
                queue.remove(0);//remove from queue
                if (queryConfigNamespace.length < treeDepth) {
                    continue;
                }
                String configPart = queryConfigNamespace[treeDepth - 1];

                if (configPart.equals(children.namespace)) {
                    configNamespaceConcat += "." + children.namespace;
                    treeDepth++;
                    queue.addAll(children.children);
                    if (configNamespaceConcat.equals(referenceText)) {
                        //match
                        return new HyperlinkInfo(referenceText, entry.getKey(), children.offset, DeclarationType.CONFIG_PATH);
                    }
                }
            }
        }

        return null;
    }

    private HyperlinkInfo routeLabelHyperlink(Document doc, String referenceText) {
        LaravelAppSupport support = LaravelAppSupport.getInstance(doc);
        if (support == null) {
            return null;
        }

        Map<FileObject, RoutesModel> collection = support.getRoutesConfigParser().getRoutesCollection();
        
        for (Map.Entry<FileObject, RoutesModel> entry : collection.entrySet()) {
            for (Map.Entry<String, OffsetRange> route : entry.getValue().getRouteNames().entrySet()) {
                if (route.getKey().equals(referenceText)) {
                    return new HyperlinkInfo(referenceText, entry.getKey(), route.getValue().getStart(), DeclarationType.ROUTE_LABEL);
                }
            }
        }
        return null;
    }

    private static class HyperlinkInfo {

        private final String queryText;
        private final FileObject goToFile;
        private final int offset;
        private final DeclarationType type;

        public HyperlinkInfo(String queryText, FileObject goToFile, int offset, DeclarationType type) {
            this.queryText = queryText;
            this.goToFile = goToFile;
            this.offset = offset;
            this.type = type;
        }

        public String getQueryText() {
            return queryText;
        }

        public FileObject getGoToFile() {
            return goToFile;
        }

        public int getOffset() {
            return offset;
        }

        public String generateTooltipText() {
            StringBuilder builder = new StringBuilder();

            String prefixText = "";
            //could be a risc
            switch (type) {
                case VIEW_PATH:
                    prefixText = "Blade Template File :";
                    break;
                case CONFIG_PATH:
                    prefixText = "Config File :";
                    break;
                case ROUTE_LABEL:
                    prefixText = "Route File :";
                    break;
            }

            builder.append(prefixText);
            builder.append(getGoToFile().getPath());
            builder.append("<b></b><br><br><i style='margin-left:20px;'>");
            builder.append(getQueryText());
            builder.append("</i>");
            return builder.toString();
        }
    }
}
