/**
 * 
 */
package fr.toutatice.ecm.drive.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.cache.CacheAttributesChecker;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.drive.services.helper.DriveHelper;


/**
 * @author david
 *
 */
public class DriveTokenFilter implements Filter {

    /** Logger. */
    private static final Log log = LogFactory.getLog(DriveTokenFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing
    }

    /**
     * Takes deviceId of Drive client.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Continue if not HttpServletRequest
        if (request instanceof HttpServletRequest == false) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        // TokenLoginName
        String tokenLoginName = null;
        
        // Token cache
        CacheService cs = (CacheService) Framework.getService(CacheService.class);
        CacheAttributesChecker tokensCache = cs.getCache(DriveHelper.NX_DRIVE_VOLATILE_TOKEN_CAHE);

        // Token authentification
        if (StringUtils.isNotBlank(httpRequest.getHeader("x-authentication-token"))) {
            // From Drive
            String app = httpRequest.getHeader("x-application-name");
            if ("Nuxeo Drive".equals(app)) {
                // Drive user
                tokenLoginName = httpRequest.getHeader("x-user-id");
                if (StringUtils.isNotBlank(tokenLoginName)) {
                    // Set token and deviceId in cache for user
                    String keyCache = DriveHelper.NX_DRIVE_TOKEN_CACHE_KEY + tokenLoginName;
                    
                    String[] tokenInfos = {httpRequest.getHeader("x-authentication-token"), httpRequest.getHeader("x-device-id")};
                    tokensCache.put(keyCache, tokenInfos);
                    
                }
            }

        }

        // Continue
        chain.doFilter(request, response);
        
    }

    @Override
    public void destroy() {
        // Nothing
    }

}
