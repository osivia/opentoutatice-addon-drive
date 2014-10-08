package fr.toutatice.ecm.drive.services;


import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import fr.toutatice.ecm.platform.core.services.fetchinformation.FetchInformationProvider;


public interface ToutaticeDriveService extends FetchInformationProvider {
	
	DocumentModel getOpenableDocument(CoreSession coreSession,
			DocumentModel currentDocument) throws ClientException;

	DocumentModel checkOut(CoreSession coreSession,
			DocumentModel currentDocument) throws ClientException;


}
