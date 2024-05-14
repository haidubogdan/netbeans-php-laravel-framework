package org.netbeans.modules.php.laravel.editor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.Document;
import org.netbeans.api.lsp.HyperlinkLocation;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.modules.csl.api.*;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.editor.hyperlink.GoToSupport;
/**
 *
 * @author bhaidu
 */
public class GoToSupportLaravel {

    public static CompletableFuture<HyperlinkLocation> getGoToLocation(Document doc, int offset, boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public static void goTo(Document doc, int offset) {
        GoToSupport.performGoTo(doc,offset);
    }


}
