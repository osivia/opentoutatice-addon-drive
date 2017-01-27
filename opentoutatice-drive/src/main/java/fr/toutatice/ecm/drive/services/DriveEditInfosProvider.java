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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    /** Drive edit URL. */
    public static final String DRIVE_EDIT_URL = "driveEditURL";

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

        // Document can be used by Drive
        boolean isAvailableDoc = currentDocument != null && !currentDocument.isFolder() && DriveHelper.getFileSystemItem(currentDocument) != null;
        // Current user has Read permission
        boolean canWrite = coreSession.hasPermission(currentDocument.getRef(), SecurityConstants.WRITE);

        // Current user has token (identifying its device(s))
        // (Note: NDrive can not be turned on ...)
        long begin = System.currentTimeMillis();

        boolean hasToken = isDriveRunningOnClient(coreSession);

        if (log.isDebugEnabled()) {
            long time = System.currentTimeMillis() - begin;
            log.debug("[isDriveRunningOnClient]: " + time + " ms");
        }

        // If ok, return URL
        if (isAvailableDoc && canWrite && hasToken) {
            infos.put(DRIVE_EDIT_URL, DriveHelper.getDriveEditURL(coreSession, currentDocument));
        }

        return infos;
    }

    /**
     * Checks if user has at least one Drive token.
     * 
     * @param user
     * @return
     * @throws IOException
     */
    public boolean isDriveRunningOnClient(CoreSession session) {
        boolean running = false;

        // Current user
        String currentUserName = session.getPrincipal().getName();

        CacheService cs = (CacheService) Framework.getService(CacheService.class);
        CacheAttributesChecker tokensCache = cs.getCache(DriveHelper.NX_DRIVE_VOLATILE_TOKEN_CAHE);

        String keyCache = DriveHelper.NX_DRIVE_TOKEN_CACHE_KEY + session.getPrincipal().getName();

        try {
            String[] tokenInfos = (String[]) tokensCache.get(keyCache);

            if (tokenInfos != null) {
                // A Drive for current user is running on some client
                // Is it known by Nx server?
                String storedUserName = getTokenAuthService().getUserName(tokenInfos[0]);
                running = StringUtils.equals(storedUserName, currentUserName);
            }

        } catch (IOException e) {
            running = false;
        }


        return running;
    }

    /**
     * Gets userName stored in auth_tokens table with given token and devideId.
     * 
     * @param token
     * @param deviceId
     * @return userName
     */
    private String getUserNameWith(String token, String deviceId) {
        // User name
        String userName = null;

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
                Map<String, Serializable> filters = new HashMap<>(2);
                filters.put(DriveHelper.TOKEN_FIELD, token);
                filters.put(DriveHelper.DEVICE_ID_FIELD, deviceId);

                // Users with given token and deviceId
                List<String> userNames = session.getProjection(filters, DriveHelper.USERNAME_FIELD);

                if (userNames != null) {
                    if (userNames.size() == 0) {
                        log.debug(String.format("Found no user name bound to the token: '%s' and deviceId: '%s', returning null.", token, deviceId));
                    } else if (userNames.size() > 1) {
                        log.debug(String.format("Found more than one user bound to the token: '%s' and deviceId: '%s', returning null.", token, deviceId));
                    } else if (userNames.size() == 1) {
                        // User found
                        log.debug(String.format("Found a user name bound to the token: '%s' and deviceId: '%s', returning it.", token, deviceId));
                        userName = userNames.get(0);
                    }
                }

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

        return userName;
    }


}
