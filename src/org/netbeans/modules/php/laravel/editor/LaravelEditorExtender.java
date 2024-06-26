/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.editor;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.netbeans.modules.php.api.editor.PhpBaseElement;
import org.netbeans.modules.php.spi.editor.EditorExtender;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * looks to be more compatible with components ??
 * 
 * @author bogdan
 */
public class LaravelEditorExtender extends EditorExtender {

    @Override
    public List<PhpBaseElement> getElementsForCodeCompletion(FileObject fo) {
        return Collections.emptyList();
    }

}
