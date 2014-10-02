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
        id = CheckOutDocument.ID,
        category = Constants.CAT_DOCUMENT,
        label = "Check out this document",
        description = "Copy this document to the user's wallet, make it possible to synchronize with Drive. Add a facet to identify the outcome")
public class CheckOutDocument {

	public static final String ID = "ToutaticeDrive.CheckOutDocument";
	
	@Context
    protected CoreSession session;
	

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel document) throws Exception {

    	ToutaticeDriveService drive = Framework.getService(ToutaticeDriveService.class);
    	
    	return drive.checkOut(session, document);
    }
}
