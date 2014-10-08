package fr.toutatice.ecm.drive.web;

import java.io.Serializable;
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
import org.nuxeo.runtime.transaction.TransactionHelper;

import fr.toutatice.ecm.drive.DriveConstants;
import fr.toutatice.ecm.drive.services.ToutaticeDriveService;


@Name("driveBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.DEPLOYMENT)
public class ToutaticeDriveBean implements Serializable {

    private static final long serialVersionUID = 5464835083155875914L;

    private static final Log log = LogFactory.getLog(ToutaticeDriveBean.class);

	
	@In(create = true, required = true)
	protected transient CoreSession documentManager;
	
    @In(create = true)
    protected transient NavigationContext navigationContext;
	
	protected String driveLink;

	
	public String getDriveLink() throws ClientException {

		if(driveLink == null) {
			DocumentModel document = navigationContext.getCurrentDocument();
			
	    	ToutaticeDriveService drive;
			try {
				drive = Framework.getService(ToutaticeDriveService.class);
			} catch (Exception e) {
				throw new ClientException(e);
			}
	    	Map<String, String> fetchInfos = drive.fetchInfos(documentManager, document);
	    	
	    	driveLink = fetchInfos.get(DriveConstants.DRIVE_EDIT_URL);
	    	
	    	if(driveLink == null) {
	    		
	    		String canCheckOut = fetchInfos.get(DriveConstants.CAN_CHECK_OUT);
	    		
	    		if(canCheckOut != null) {
	    			drive.checkOut(documentManager, document);
	    			TransactionHelper.commitOrRollbackTransaction();
	    			
	    			fetchInfos = drive.fetchInfos(documentManager, document);
	    			driveLink = fetchInfos.get(DriveConstants.DRIVE_EDIT_URL);
	    			
	    		}
	    	}
    	
		}
    	return driveLink;
			

	}

}
