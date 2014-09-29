package fr.toutatice.ecm.drive.automation;

import java.util.Map;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.drive.services.ToutaticeDriveService;


@Operation(
        id = CheckInDocument.ID,
        category = Constants.CAT_DOCUMENT,
        label = "Check in this document",
        description = "Copy the document from the user wallet to this directory")
public class CheckInDocument {

	public static final String ID = "ToutaticeDrive.CheckInDocument";
	
	@Context
    protected CoreSession session;
	
    @Param(name = "keepLocalCopy", required = false)
    protected boolean keepLocalCopy = false;
    

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) throws Exception {

    	// controls
    	ToutaticeDriveService drive = Framework.getService(ToutaticeDriveService.class);
    	
    	DocumentModel documentInUserWks = drive.getOpenableDocument(session, document);
    	DocumentModel copied = null;
    	
    	if(documentInUserWks != null) {
    		DocumentRef parentRef = document.getParentRef();
    		
    		session.removeDocument(document.getRef());
    		
    		if(keepLocalCopy) {
    			copied = session.copy(documentInUserWks.getRef(), parentRef, null);
    		}
    		else {
    			copied = session.move(documentInUserWks.getRef(), parentRef, null);
    		}
    		
    		copied.setProperty("toutatice_drive", "docRef", null);
    		copied.removeFacet("CheckedOutDocument");
    		session.saveDocument(copied);
    	}
    	else {
    		throw new OperationException("This file is not able to be checked in");
    	}
    	
    	
    	return copied;
    }
}
