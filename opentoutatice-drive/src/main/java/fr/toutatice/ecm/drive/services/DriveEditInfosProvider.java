/**
 * 
 */
package fr.toutatice.ecm.drive.services;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.cache.CacheAttributesChecker;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
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

    /** Drive edit URL (filled if Drive is running). */
    public static final String DRIVE_EDIT_URL = "driveEditURL";
    /**
     * Drive(s) installed (but not running).
     * Number of Drives can be more than one for a given user.
     * In this case, many devices of user have a Drive client.
     */
    public static final String DRIVE_ENABLED = "driveEnabled";

    /** TokenAuthenticationService. */
    private static TokenAuthenticationService tokenAuthService;

    /**
     * Getter for TokenAuthenticationService.
     */
    protected static TokenAuthenticationService getTokenAuthService() {
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
        
        // Current user
        String userName = coreSession.getPrincipal().getName();

        // Document can be used by Drive
        boolean isAvailableDoc = currentDocument != null && !currentDocument.isFolder() && DriveHelper.getFileSystemItem(currentDocument) != null;
        
        // Current user has token (identifying its device(s))
        // (Note: NDrive can not be turned on ...)
        long begin = System.currentTimeMillis();
        
        if(isAvailableDoc){
            // Current user has Read permission
            boolean canWrite = coreSession.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE);
    
            // Cached token
            String[] tokenInfos = getTokenInfos(userName);
            
            // Drive is running
            boolean driveRunning = tokenInfos != null;
    
            // If ok, return URL
            if (canWrite && driveRunning) {
                infos.put(DRIVE_EDIT_URL, DriveHelper.getDriveEditURL(coreSession, currentDocument));
            }
    
            // Not running, check if user has been connected once
            if (!driveRunning) {
                DocumentModelList tokensOfUser = getTokenAuthService().getTokenBindings(userName);
                boolean driveEnabled = tokensOfUser != null && tokensOfUser.size() > 0;
                
                infos.put(DRIVE_ENABLED, driveEnabled);
            }
        }

        if (log.isTraceEnabled()) {
            long time = System.currentTimeMillis() - begin;
            log.trace(": " + time + " ms");
        }

        return infos;
    }
    
    /**
     * Get token informations of user.
     * 
     * @param userName
     * @return tokenInfos
     */ 
    public String[] getTokenInfos(String userName){
        // Token infos
        String[] tokenInfos = null;
        
        CacheService cs = (CacheService) Framework.getService(CacheService.class);
        CacheAttributesChecker tokensCache = cs.getCache(DriveHelper.NX_DRIVE_VOLATILE_TOKEN_CAHE);

        String keyCache = DriveHelper.NX_DRIVE_TOKEN_CACHE_KEY + userName;

        try {
            tokenInfos = (String[]) tokensCache.get(keyCache);

        } catch (IOException e) {
            // Nothing
        }
        
        return tokenInfos;
    }

    /**
     * Look if user has at least one Drive installed on some device.
     * 
     * @param infos
     * @param userName
     * @return infos
     */
    private Map<String, Object> getDriveInfos(Map<String, Object> infos, String userName) {
        // Get userNames with given tokens and devices
        List<String> tokens = getTokensForUser(userName);

        // Number of Drive for user
        int nbTokens = tokens != null ? tokens.size() : 0;
        infos.put(DRIVE_ENABLED, nbTokens);


        if (nbTokens == 0) {
            // No Drive installed
            log.debug(String.format("No Drive client found for user: '%s'", userName));
        } else if (nbTokens == 1) {
            // One Drive
            log.debug(String.format("One Drive client found for user: '%s'", userName));
        } else if (nbTokens > 1) {
            // More than one Drive
            log.debug(String.format("More than one Drive client found for user: '%s'", userName));
        }

        return infos;
    }

    /**
     * Gets userName stored in auth_tokens table with given token and devideId.
     * 
     * @param token
     * @param deviceId
     * @return userName
     */
    private List<String> getTokensForUser(String userName) {
        // User name
        List<String> tokens = null;

        // Log in as system user
        LoginContext lc;
        try {
            lc = Framework.login();
        } catch (LoginException e) {
            throw new ClientException("Cannot log in as system user", e);
        }
        try {
            final Session session = Framework.getService(DirectoryService.class).open(DriveHelper.AUTH_TOKEN_DIRECTORY_NAME);
            try {
                // Filters
                Map<String, Serializable> filters = new HashMap<>(1);
                filters.put(DriveHelper.USERNAME_FIELD, userName);

                // Users with given token and deviceId
                tokens = session.getProjection(filters, DriveHelper.TOKEN_FIELD);

            } finally {
                session.close();
            }
        } finally {
            try {
                // Login context may be null in tests
                if (lc != null) {
                    lc.logout();
                }
            } catch (LoginException e) {
                throw new ClientException("Cannot log out system user", e);
            }
        }

        return tokens;
    }


}
