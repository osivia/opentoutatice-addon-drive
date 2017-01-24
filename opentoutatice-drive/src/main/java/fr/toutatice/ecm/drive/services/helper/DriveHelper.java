/**
 * 
 */
package fr.toutatice.ecm.drive.services.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;


/**
 * @author david
 *
 */
public class DriveHelper {
    
    /** NxDrive protocol. */
    public static final String NXDRIVE_PROTOCOL = "nxdrive";
    /** NxDrive protocol edit command. */
    public static final String PROTOCOL_COMMAND_EDIT = "edit";
    
    /** Logger. */
    private static final Log log = LogFactory.getLog(DriveHelper.class);

    /**
     * Utility class.
     */
    private DriveHelper() {
        super();
    }
    
    /**
     * Checks if document can be adapt to (Drive) FileSystemItem.
     * 
     * @param doc
     * @return fileSystemItem
     * @throws ClientException
     */
    public static FileSystemItem getFileSystemItem(DocumentModel doc) throws ClientException {
        // Force parentItem to null to avoid computing ancestors
        // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
        FileSystemItem fileSystemItem = Framework.getLocalService(FileSystemItemAdapterService.class).getFileSystemItem(
                doc, null, false, false, false);
        if (fileSystemItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s (%s) is not adaptable as a FileSystemItem.",
                        doc.getPathAsString(), doc.getId()));
            }
        }
        return fileSystemItem;
    }
    
    /**
     * {@link #NXDRIVE_PROTOCOL} must be handled by a protocol handler
     * configured on the client side (either on the browser, or on the OS).
     * 
     * @return Drive edit URL in the form "{@link #NXDRIVE_PROTOCOL}:// {@link #PROTOCOL_COMMAND_EDIT} /protocol/server[:port]/webappName/nxdoc/repoName/docRef"
     * @throws ClientException
     * 
     */
    public static String getDriveEditURL(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {

        FileSystemItem currentFileSystemItem = getFileSystemItem(currentDocument);

        // Current document must be adaptable as a FileSystemItem
        if (currentFileSystemItem == null) {
            throw new ClientException(String.format(
                    "Document %s (%s) is not adaptable as a FileSystemItem thus not Drive editable, \"driveEdit\" action should not be displayed.",
                    currentDocument.getId(), currentDocument.getPathAsString()));
        }

        
        
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new ClientException(String.format("Document %s (%s) is not a BlobHolder, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            throw new ClientException(String.format("Document %s (%s) has no blob, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        String fileName = blob.getFilename();

        StringBuffer sb = new StringBuffer();
        sb.append(NXDRIVE_PROTOCOL).append("://");
        sb.append(PROTOCOL_COMMAND_EDIT).append("/");
        sb.append(Framework.getProperty("nuxeo.url").replaceFirst("://", "/"));
        sb.append("/repo/");
        sb.append(coreSession.getRepositoryName());
        sb.append("/nxdocid/");
        sb.append(currentDocument.getId());
        sb.append("/filename/");
        String escapedFilename = fileName.replaceAll("(/|\\\\|\\*|<|>|\\?|\"|:|\\|)", "-");
        sb.append(URIUtils.quoteURIPathComponent(escapedFilename, true));
        return sb.toString();
        
    }

}
