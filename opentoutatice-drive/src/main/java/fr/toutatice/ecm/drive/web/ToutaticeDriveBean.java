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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import fr.toutatice.ecm.drive.DriveConstants;
import fr.toutatice.ecm.drive.services.ToutaticeDriveService;

/**
 * bean for the view to open a document
 * 
 * @author lbillon
 * 
 */
@Name("driveBean")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.DEPLOYMENT)
public class ToutaticeDriveBean implements Serializable {

	private static final int DOWNLOAD_RATE_PER_SECOND = 2000000;

	private static final int INITIAL_DELAY = 3;

	private static final long serialVersionUID = 5464835083155875914L;

	private static final Log log = LogFactory.getLog(ToutaticeDriveBean.class);

	@In(create = true, required = true)
	protected transient CoreSession documentManager;

	@In(create = true)
	protected transient NavigationContext navigationContext;

	/** the url with ndrive:// protocal */
	protected String driveLink;

	/** blob attached with the document */
	protected Blob blob;

	/**
	 * delay to invoke the opening of the document, depending of the size of the
	 * file to download (in secs)
	 */
	protected long openDelay = 0;

	public String getDriveLink() throws ClientException {

		if (driveLink == null) {
			DocumentModel document = navigationContext.getCurrentDocument();

			ToutaticeDriveService drive;
			try {
				drive = Framework.getService(ToutaticeDriveService.class);
			} catch (Exception e) {
				throw new ClientException(e);
			}
			Map<String, String> fetchInfos = drive.fetchInfos(documentManager,
					document);

			driveLink = fetchInfos.get(DriveConstants.DRIVE_EDIT_URL);

			boolean openImmediatly = false;
			if (driveLink == null) {

				String canCheckOut = fetchInfos
						.get(DriveConstants.CAN_CHECK_OUT);

				if (canCheckOut != null) {
					drive.checkOut(documentManager, document);
					TransactionHelper.commitOrRollbackTransaction();

					fetchInfos = drive.fetchInfos(documentManager, document);
					driveLink = fetchInfos.get(DriveConstants.DRIVE_EDIT_URL);

				}
			} else {
				openImmediatly = true;
			}

			Serializable fileContent = document
					.getPropertyValue("file:content");
			blob = (Blob) fileContent;

			if (blob.getLength() > 0 && !openImmediatly) {
				openDelay = INITIAL_DELAY
						+ (blob.getLength() / DOWNLOAD_RATE_PER_SECOND);
			}

		}
		return driveLink;

	}

	public Blob getBlob() throws ClientException {
		if (driveLink == null) {
			getDriveLink();
		}
		return blob;
	}

	public long getOpenDelay() throws ClientException {
		if (driveLink == null) {
			getDriveLink();
		}

		return openDelay;
	}

}
