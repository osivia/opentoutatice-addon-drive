package fr.toutatice.ecm.drive.web;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.drive.services.DriveConstants;
import fr.toutatice.ecm.drive.services.ToutaticeDriveService;


@Name("driveBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.DEPLOYMENT)
public class ToutaticeDriveBean {

	private static final Log log = LogFactory.getLog(ToutaticeDriveBean.class);

	
	@In(create = true, required = true)
	protected transient CoreSession coreSession;
	
    @In(create = true)
    protected transient NavigationContext navigationContext;
	
	protected String driveLink;

	
	public String getDriveLink() throws ClientException {

		DocumentModel document = navigationContext.getCurrentDocument();
		
    	ToutaticeDriveService drive;
		try {
			drive = Framework.getService(ToutaticeDriveService.class);
		} catch (Exception e) {
			throw new ClientException(e);
		}
    	Map<String, String> fetchInfos = drive.fetchInfos(coreSession, document);
    	
    	String driveUrl = fetchInfos.get(DriveConstants.DRIVE_EDIT_URL);
    	
    	if(driveUrl == null) {
    		
    		String canCheckOut = fetchInfos.get(DriveConstants.CAN_CHECK_OUT);
    		
    		if(canCheckOut != null) {
    			drive.checkOut(coreSession, document);
    			fetchInfos = drive.fetchInfos(coreSession, document);
    			driveUrl = fetchInfos.get(DriveConstants.DRIVE_EDIT_URL);
    			
    		}
    	}
    	
    	
    	return driveUrl;
			

	}

}
