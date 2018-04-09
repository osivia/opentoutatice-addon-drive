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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;


/**
 * @author david
 *
 */
public class DriveHelper {
    
    /**
	 * Used to replace the host by current request host
	 */
	private static final String HOST_JOKER = "__HOST__";
	/** NxDrive protocol. */
    public static final String NXDRIVE_PROTOCOL = "nxdrive";
    /** NxDrive protocol edit command. */
    public static final String PROTOCOL_COMMAND_EDIT = "edit";
    
    /** NxDrive "volatile" tokens cache. */
    public static final String NX_DRIVE_VOLATILE_TOKEN_CAHE = "ottc-volatile-drive-token-cache";
    /** NxDrive token cache key prefix. */
    public static final String NX_DRIVE_TOKEN_CACHE_KEY = "Nx-Drive-T-";
    
    /** Token directory. */
    public static final String AUTH_TOKEN_DIRECTORY_NAME = "authTokens";
    /** Token directory schema. */
    public static final String AUTH_TOKEN_DIRECTORY_SCHEMA = "authtoken";
    /** Token userName field. */
    public static final String USERNAME_FIELD = "userName";
    /** Token token field. */
    public static final String TOKEN_FIELD = "token";
    /** Token applicationName field. */
    public static final String APPLICATION_NAME_FIELD = "applicationName";
    /** Token deviceId field. */
    public static final String DEVICE_ID_FIELD = "deviceId";
    
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
     * @throws NuxeoException
     */
    public static FileSystemItem getFileSystemItem(DocumentModel doc) throws NuxeoException {
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
     * @throws NuxeoException
     * 
     */
    public static String getDriveEditURL(CoreSession coreSession, DocumentModel currentDocument) throws NuxeoException {

        FileSystemItem currentFileSystemItem = getFileSystemItem(currentDocument);

        // Current document must be adaptable as a FileSystemItem
        if (currentFileSystemItem == null) {
            throw new NuxeoException(String.format(
                    "Document %s (%s) is not adaptable as a FileSystemItem thus not Drive editable, \"driveEdit\" action should not be displayed.",
                    currentDocument.getId(), currentDocument.getPathAsString()));
        }

        
        
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new NuxeoException(String.format("Document %s (%s) is not a BlobHolder, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            throw new NuxeoException(String.format("Document %s (%s) has no blob, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        String fileName = blob.getFilename();

        StringBuffer sb = new StringBuffer();
        sb.append(NXDRIVE_PROTOCOL).append("://");
        sb.append(PROTOCOL_COMMAND_EDIT).append("/");
        
        // #1421 - no host in nxdrive url (get the current portal request host)
        sb.append(HOST_JOKER);
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
