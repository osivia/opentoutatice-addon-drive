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

import java.io.IOException;

import javax.mail.internet.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.operations.NuxeoDriveUpdateFile;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

import fr.toutatice.ecm.drive.DriveConstants;

/**
 * Redefinition of the NuxeoDrive.updateFile operation to handle the
 * modification of local documents and synchronize their blobs with the document
 * synchronized
 * 
 * @author lbillon
 * 
 */
@Operation(id = NuxeoDriveUpdateFile.ID, category = Constants.CAT_SERVICES, label = "Toutatice Drive: Update file and synchronize it if it is a user wks document")
public class ToutaticeDriveUpdateFile extends NuxeoDriveUpdateFile {

	private static final Log log = LogFactory.getLog("Drive");

	@Context
	protected OperationContext ctx;

	@Param(name = "id")
	protected String id;

	@Override
	@OperationMethod
	public Blob run(Blob blob) throws ClientException, ParseException,
			IOException {

		// ============ default operation process
		super.id = id;
		super.ctx = ctx;

		super.run(blob);

		// ============ specific operation process for the local files.
		// FileSystemItemManager fileSystemItemManager =
		// Framework.getLocalService(FileSystemItemManager.class);
		// FileSystemItem fsItem =
		// fileSystemItemManager.getFileSystemItemById(id, ctx.getPrincipal());

		// if(fsItem instanceof DocumentBackedFileItem) {

		// DocumentBackedFileItem file = (DocumentBackedFileItem) fsItem;

		// TODO comment obtenir cet id ?
		// String id2 = file.getId();
		String id3 = id.split("#")[2];

		// get the user document
		IdRef pathRef = new IdRef(id3);
		DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);

		// if the document is a checkedout document
		if (doc.hasFacet(DriveConstants.FACET_CHECKED_OUT_DOC)) {
			String refId = doc
					.getPropertyValue(DriveConstants.PROPERTY_DOC_REF)
					.toString();

			// get the targeted document (in a common workspace)
			IdRef idDocSrc = new IdRef(refId);
			DocumentModel docSrc = ctx.getCoreSession().getDocument(idDocSrc);

			DocumentHelper.addBlob(docSrc.getProperty("file:content"), blob);
			// report the blob in the targeted document
			ctx.getCoreSession().saveDocument(docSrc);
			log.warn("file updated : " + docSrc.getPathAsString());

		}

		// }

		return blob;

	}
}
