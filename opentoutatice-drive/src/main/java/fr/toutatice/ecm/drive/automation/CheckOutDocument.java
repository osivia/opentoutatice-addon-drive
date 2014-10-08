/*
 * (C) Copyright 2014 Académie de Rennes (http://www.ac-rennes.fr/), OSIVIA (http://www.osivia.com) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *
 * Contributors:
 *   lbillon
 *    
 */
package fr.toutatice.ecm.drive.automation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.drive.services.ToutaticeDriveService;

/**
 * 
 * Operation called by the portal to check out a document
 * 
 * @author lbillon
 * 
 */
@Operation(
        id = CheckOutDocument.ID,
        category = Constants.CAT_DOCUMENT,
        label = "Check out this document",
        description = "Copy this document to the user's wallet, make it possible to synchronize with Drive. Add a facet to identify the outcome")
public class CheckOutDocument {

	public static final String ID = "ToutaticeDrive.CheckOutDocument";
	
	@Context
    protected CoreSession session;
	

	/**
	 * Main op
	 * 
	 * @param document
	 *            the document to check out in the local folder
	 * @return the local document
	 * @throws ClientException
	 * @throws Exception
	 */
    @OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(DocumentModel document) throws ClientException {

		ToutaticeDriveService drive;
		try {
			drive = Framework.getService(ToutaticeDriveService.class);
		} catch (Exception e) {
			throw new ClientException(e);
		}
    	
    	return drive.checkOut(session, document);
    }
}
