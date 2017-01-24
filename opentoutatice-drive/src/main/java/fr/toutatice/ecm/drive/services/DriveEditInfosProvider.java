/**
 * 
 */
package fr.toutatice.ecm.drive.services;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.drive.services.helper.DriveHelper;
import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;


/**
 * @author david
 *
 */
public class DriveEditInfosProvider implements DocumentInformationsProvider {
    
    /** Logger. */
    private static final Log log = LogFactory.getLog(DriveEditInfosProvider.class);

    /** CanDriveEdit property. */
    public static final String CAN_DRIVE_EDIT = "isDriveEditable";

    /** TokenAuthenticationService. */
    private static TokenAuthenticationService tokenAuthService;

    /**
     * Getter for TokenAuthenticationService.
     */
    protected static TokenAuthenticationService getTokenAuthServcie() {
        if (tokenAuthService == null) {
            tokenAuthService = (TokenAuthenticationService) Framework.getService(TokenAuthenticationService.class);
        }
        return tokenAuthService;
    }

    /**
     * Checks if document can be drive editable.
     */
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        // Drive editable infos
        Map<String, Object> infos = new HashMap<String, Object>(1);

        // Document can be used by Drive
        boolean isAvailableDoc = currentDocument!= null && !currentDocument.isFolder() 
                && DriveHelper.getFileSystemItem(currentDocument) != null;
        // Current user has Read permission
        boolean canWrite = coreSession.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE);

        // Current user has token (identifying its device(s))
        // (Note: NDrive can not be turned on ...)
        long begin = System.currentTimeMillis();
        
        boolean hasToken = hasOneDriveToken(coreSession.getPrincipal());
        
        if(log.isDebugEnabled()){
            long time = System.currentTimeMillis() - begin;
            log.debug("[hasOneDriveToken]: " + time + " ms");
        }

        // Result
        infos.put(CAN_DRIVE_EDIT, isAvailableDoc && canWrite && hasToken);

        return infos;
    }

    /**
     * Checks if user has at least one Drive token.
     * 
     * @param user
     * @return
     */
    public boolean hasOneDriveToken(Principal user) {
        TokenAuthenticationService tokenService = getTokenAuthServcie();
        for (DocumentModel token : tokenService.getTokenBindings(user.getName())) {
            if ("Nuxeo Drive".equals(token.getPropertyValue("authtoken:applicationName"))) {
                return true;
            }
        }
        return false;
    }

}
