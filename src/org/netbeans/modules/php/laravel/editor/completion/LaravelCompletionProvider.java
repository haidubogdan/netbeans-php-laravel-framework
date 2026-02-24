/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor.completion;

import org.netbeans.modules.php.laravel.project.LaravelAppSupport;
import org.netbeans.modules.php.laravel.utils.PathUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.laravel.ConfigurationFiles;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;
import org.netbeans.modules.php.laravel.editor.EditorUtils;
import org.netbeans.modules.php.laravel.editor.ResourceUtilities;
import org.netbeans.modules.php.laravel.editor.model.ConfigurationModel;
import org.netbeans.modules.php.laravel.project.ProjectUtils;
import org.netbeans.modules.php.laravel.utils.LaravelUtils;
import org.netbeans.modules.php.laravel.utils.StringUtils;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.filesystems.FileObject;

@MimeRegistrations({
    @MimeRegistration(mimeType = FileUtils.PHP_MIME_TYPE, service = CompletionProvider.class),
    @MimeRegistration(mimeType = "text/x-blade", service = CompletionProvider.class)   
})
public class LaravelCompletionProvider implements CompletionProvider {

    private String methodName;

    public static String[] QUERY_METHODS = new String[]{
        "config",
        "view",
        "make",
        "render",
        "send",
        "loadView",
        "get",
        "put",
        "delete",
        "post",
        "group",
        "resource",
        "only",
        "route"
    }; // NOI18N 

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        FileObject currentFile = NbEditorUtilities.getFileObject(component.getDocument());

        if (currentFile == null) {
            return null;
        }

        PhpModule module = ProjectUtils.getPhpModule(currentFile);

        if (module == null) {
            return null;
        }

        if (!ProjectUtils.isInLaravelModule(module)) {
            return null;
        }

        String reference = getQueryString(component.getDocument(), component.getCaretPosition());
        //

        if (reference != null) {
            AsyncCompletionQuery completionQuery;

            switch (methodName) {
                case "config": // NOI18N
                    completionQuery = new ConfigurationCompletionQuery(module);
                    break;
                case "view": // NOI18N
                case "make": // NOI18N
                case "render": // NOI18N
                case "send": // NOI18N
                case "loadView": // NOI18N 
                    completionQuery = new ViewCompletionQuery(module);
                    break;
                case "route":
                    completionQuery = new RouteLabelCompletionQuery();
                    break;
                //ROUTING info completion   
                case "get": // NOI18N 
                case "post": // NOI18N 
                case "group": // NOI18N 
                case "delete": // NOI18N 
                case "put": // NOI18N 
                case "resource": // NOI18N 
                case "only": // NOI18N 
                    if (reference.startsWith("/") || !ProjectUtils.isRouteFile(currentFile)) { // NOI18N 
                        return null;
                    } else {
                        completionQuery = new RouteCompletionQuery();
                    }
                    break;
                default:
                    return null;
            }

            return new AsyncCompletionTask(completionQuery, component);
        }
        return null;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        if (component.getDocument() == null) {
            return 0;
        }
        if (!ProjectUtils.isInLaravelModule(component.getDocument())) {
            return 0;
        }
        return COMPLETION_QUERY_TYPE;
    }

    private String getQueryString(Document doc, int offset) {

        TokenSequence<PHPTokenId> tokensq = EditorUtils.getTokenSequence(doc, offset);

        if (tokensq == null) {
            return null;
        }

        Token<PHPTokenId> currentToken = tokensq.token();

        if (currentToken == null) {
            return null;
        }

        PHPTokenId openParenToken = null;

        String quotedReference = ""; // NOI18N
        int tokenCount = 0;
        while (tokensq.movePrevious() && tokenCount <= 6) {
            Token<PHPTokenId> token = tokensq.token();
            if (token == null) {
                break;
            }

            String text = token.text().toString();
            PHPTokenId id = token.id();

            if (id.equals(PHPTokenId.WHITESPACE) || text.equals("[") || text.equals(",")) {
                continue;
            }

            tokenCount++;
            if (openParenToken != null && id.equals(PHPTokenId.PHP_STRING)) {
                if (Arrays.asList(QUERY_METHODS).indexOf(text) > -1) {
                    methodName = text;
                    quotedReference = currentToken.text().toString();
                }
                break;
            }

            if (EditorUtils.isOpenParenToken(id, text)) {
                openParenToken = id;
            }
        }

        if (quotedReference.length() < 2 || LaravelUtils.isVariable(quotedReference)) {
            return null;
        }
        if (StringUtils.isQuotedString(quotedReference)) {
            String reference = quotedReference.substring(1, quotedReference.length() - 1);
            return reference;
        }
        return null;
    }

    private class ConfigurationCompletionQuery extends AsyncCompletionQuery {

        PhpModule module;

        public ConfigurationCompletionQuery(PhpModule module) {
            this.module = module;
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            long startTime = System.currentTimeMillis();

            try {
                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }
                ConfigurationFiles confFiles = (ConfigurationFiles) LaravelPhpFrameworkProvider.getInstance().getConfigurationFiles2(module);
                if (confFiles != null && confFiles.getConfigDirectory() != null) {
                    FileObject configDirectory = confFiles.getConfigDirectory();
                    ConfigurationModel model = ConfigurationModel.getModel(configDirectory);
                    String[] queryConfigNamespace = query.split("\\.");
                    Map<String, Map<String, List<String>>> confFileList = model.getConfigurationMapping();
                    Map<String, FileObject> confFileAlias = model.getConfigurationFilesAlias();
                    String filterQuery = query;
                    if (query.endsWith(".")) {
                        filterQuery = query.substring(0, query.length() - 1);
                    }
                    for (Map.Entry<String, Map<String, List<String>>> entry : confFileList.entrySet()) {
                        String fileKey = entry.getKey();
                        if (!fileKey.startsWith(queryConfigNamespace[0])) {
                            continue;
                        }
                        FileObject configFile = confFileAlias.get(entry.getKey());
                        String configPath = "";
                        if (configFile != null) {
                            configPath = LaravelUtils.DIR_CONFIG + "/" + configFile.getNameExt();
                        }
                        if (queryConfigNamespace.length == 1) {
                            if (query.endsWith(".") && !entry.getValue().isEmpty()) {
                                for (Map.Entry<String, List<String>> namespace : entry.getValue().entrySet()) {
                                    String rootKey = entry.getKey() + "." + namespace.getKey();
                                    addConfigCompletionItem(query, rootKey, configPath, caretOffset, resultSet);
                                }
                                //should be only a unique namespace
                            } else {
                                addConfigCompletionItem(query, fileKey, configPath, caretOffset, resultSet);
                            }
                            break;
                        }
                        if (queryConfigNamespace.length < 2) {
                            continue;
                        }
                        for (Map.Entry<String, List<String>> namespace : entry.getValue().entrySet()) {
                            String rootKey = entry.getKey() + "." + namespace.getKey();

                            if (!rootKey.startsWith(queryConfigNamespace[0] + "." + queryConfigNamespace[1])) {
                                continue;
                            }

                            if (queryConfigNamespace.length == 2 && !query.endsWith(".")) {
                                addConfigCompletionItem(query, rootKey, configPath, caretOffset, resultSet);
                            }

                            if (query.endsWith(".") && !namespace.getValue().isEmpty()) {

                                for (String configNamespace : namespace.getValue()) {
                                    String fullConfigNamespace = entry.getKey() + "." + configNamespace;
                                    addConfigCompletionItem(query, fullConfigNamespace, configPath, caretOffset, resultSet);
                                }
                                //should be only a unique namespace
                                break;
                            }

                            for (String configNamespace : namespace.getValue()) {
                                String fullConfigNamespace = entry.getKey() + "." + configNamespace;
                                if (fullConfigNamespace.startsWith(filterQuery)) {

                                    addConfigCompletionItem(query, fullConfigNamespace, configPath, caretOffset, resultSet);
                                }
                            }
                        }
                    }
                }
            } finally {
//                long time = System.currentTimeMillis() - startTime;
                resultSet.finish();
            }
        }

        private void addConfigCompletionItem(String prefix, String value, String filePath, int caretOffset, CompletionResultSet resultSet) {
            String completionValue = value;
            int insertOffset = caretOffset;
            String previewValue = value;
            if (prefix.contains(".")) {
                completionValue = value.replace(prefix, "");
                int lastSeparatorPos = value.lastIndexOf(".");
                previewValue = value.substring(lastSeparatorPos);
            } else {
                insertOffset = caretOffset - prefix.length();
            }
            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(completionValue)
                    .iconResource(ResourceUtilities.ICON_BASE + "icons/config.png")
                    .startOffset(insertOffset)
                    .leftHtmlText(previewValue)
                    .rightHtmlText(filePath)
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }

    private class ViewCompletionQuery extends AsyncCompletionQuery {

        PhpModule module;

        public ViewCompletionQuery(PhpModule module) {
            this.module = module;
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                FileObject sourceDir = module.getSourceDirectory();
                if (sourceDir == null) {
                    return;
                }
                int lastDotPos;

                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }

                if (query.endsWith(".")) {
                    lastDotPos = query.length();
                } else {
                    lastDotPos = query.lastIndexOf(".");
                }
                int pathOffset;

                if (lastDotPos > 0) {
                    int dotFix = query.endsWith(".") ? 0 : 1;
                    pathOffset = caretOffset - query.length() + lastDotPos + dotFix;
                } else {
                    pathOffset = caretOffset - query.length();
                }
                FileObject viewFolder = sourceDir.getFileObject(PathUtils.LARAVEL_VIEW_PATH);
                List<FileObject> childrenFiles = PathUtils.getParentChildrenFromPrefixPath(viewFolder, query);
                for (FileObject file : childrenFiles) {
                    String pathFileName = file.getName();
                    if (!file.isFolder()) {
                        pathFileName = pathFileName.replace(".blade", ""); //NOI18N
                    }
                    addViewCompletionItem(pathFileName, file, sourceDir, pathOffset, resultSet);
                }

            } finally {
//                long time = System.currentTimeMillis() - startTime;
                resultSet.finish();
            }
        }

        private void addViewCompletionItem(String completion, FileObject file, FileObject sourceDir,
                int caretOffset, CompletionResultSet resultSet) {
            int insertOffset = caretOffset;
            String imagePath = ResourceUtilities.ICON_BASE + "icons/file.png";//NOI18N
            if (file.isFolder()) {
                imagePath = "org/openide/loaders/defaultFolder.gif";//NOI18N
            }

            String filePath = file.getPath().replace(sourceDir.getPath(), "");

            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(completion)
                    .iconResource(imagePath)
                    .startOffset(insertOffset)
                    .leftHtmlText(completion)
                    .rightHtmlText(filePath)
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }

    private class RouteLabelCompletionQuery extends AsyncCompletionQuery {

        public RouteLabelCompletionQuery() {

        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {

                LaravelAppSupport support = LaravelAppSupport.getInstance(doc);

                if (support == null) {
                    return;
                }

                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }

                Set<String> routeLabels = support.getRoutesConfigParser().getRoutesLabel();
                int insertOffset = caretOffset - query.length();
                for (String routelabel : routeLabels) {
                    
                    if (routelabel.startsWith(query)) {
                        addMethodCompletionItem(routelabel, " - route label", resultSet, insertOffset);
                    }

                }

            } finally {
                resultSet.finish();
            }
        }

        private void addMethodCompletionItem(String method, String rightInfo,
                CompletionResultSet resultSet, int insertOffset) {
            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(method)
                    .iconResource("org/netbeans/modules/websvc/saas/ui/resources/method.png")
                    .startOffset(insertOffset)
                    .leftHtmlText(method)
                    .rightHtmlText(rightInfo)
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }

    private class RouteCompletionQuery extends AsyncCompletionQuery {

        public RouteCompletionQuery() {

        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {

                LaravelAppSupport support = LaravelAppSupport.getInstance(doc);

                if (support == null) {
                    return;
                }

                String query = getQueryString(doc, caretOffset);
                if (query == null) {
                    return;
                }

                Map<String, Set<String>> methods = support.getControllerClassParser().findClassReferences(query);
                int insertOffset = caretOffset - query.length();

                for (Map.Entry<String, Set<String>> methodEntry : methods.entrySet()) {
                    String methodName = methodEntry.getKey();
                    
                    for (String classes : methodEntry.getValue()) {
                        addMethodCompletionItem(methodName, classes, resultSet, insertOffset);
                    }
                }

            } finally {
                resultSet.finish();
            }
        }

        private void addMethodCompletionItem(String method, String className,
                CompletionResultSet resultSet, int insertOffset) {
            CompletionItem item = CompletionUtilities.newCompletionItemBuilder(method)
                    .iconResource("org/netbeans/modules/websvc/saas/ui/resources/method.png")
                    .startOffset(insertOffset)
                    .leftHtmlText(method)
                    .rightHtmlText(className)
                    .sortPriority(1)
                    .build();
            resultSet.addItem(item);
        }
    }
}
