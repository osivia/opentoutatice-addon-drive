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
package fr.toutatice.ecm.drive.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * impl of ToutaticeDriveService
 * @author lbillon
 *
 */
public class ToutaticeDriveServiceImpl implements ToutaticeDriveService {

	private static final Log log = LogFactory.getLog("Drive");

    protected static final String IS_UNDER_SYNCHRONIZATION_ROOT = "nuxeoDriveIsUnderSynchronizationRoot";

    protected static final String CURRENT_SYNCHRONIZATION_ROOT = "nuxeoDriveCurrentSynchronizationRoot";

    public static final String NXDRIVE_PROTOCOL = "nxdrive";

    public static final String PROTOCOL_COMMAND_EDIT = "edit";


    @Override
	public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        Map<String, Object> synchronizationInfos = new TreeMap<String, Object>();

        if (canSynchronizeCurrentDocument(coreSession, currentDocument)) {
            synchronizationInfos.put(CAN_SYNCHRONIZE, "true");
        } else if (canUnSynchronizeCurrentDocument(coreSession, currentDocument)) {
            synchronizationInfos.put(CAN_UNSYNCHRONIZE, "true");
        } else if (canNavigateToCurrentSynchronizationRoot(coreSession, currentDocument)) {

            synchronizationInfos.put(CAN_NAVIGATE_TO_CURRENT_SYNCHRONIZATION_ROOT, "true");
            synchronizationInfos.put(SYNCHRONIZATION_ROOT_PATH, getCurrentSynchronizationRoot(coreSession, currentDocument).getPathAsString());


        } 

		if (canEditCurrentDocument(coreSession, currentDocument)) {

			synchronizationInfos.put(CAN_EDIT_CURRENT_DOCUMENT, "true");
			synchronizationInfos.put(DRIVE_EDIT_URL, getDriveEditURL(coreSession, currentDocument));
		}
        
        return synchronizationInfos;
    }



	protected DocumentModelList getCurrentPath(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        DocumentModelList parentDocsList = new DocumentModelListImpl();

        List<DocumentModel> fromRoot = coreSession.getParentDocuments(currentDocument.getRef());
        // add in reverse order
        parentDocsList.addAll(fromRoot);
        Collections.reverse(parentDocsList);

        return parentDocsList;
    }


    public DocumentModel getCurrentSynchronizationRoot(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Set<IdRef> references = driveManager.getSynchronizationRootReferences(coreSession);
        DocumentModelList path = getCurrentPath(coreSession, currentDocument);
        DocumentModel root = null;
        // list is ordered such as closest synchronized ancestor is
        // considered the current synchronization root
        for (DocumentModel parent : path) {
            if (references.contains(parent.getRef())) {
                root = parent;
                break;
            }
        }

        return root;
    }

    public boolean canEditCurrentDocument(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {

        if (currentDocument == null) {
            return false;
        }
        if (currentDocument.isFolder()) {
            return false;
        }
        if (!coreSession.hasPermission(currentDocument.getRef(),
                SecurityConstants.WRITE)) {
            return false;
        }

        // Check if current document can be adapted as a FileSystemItem
        return getCurrentFileSystemItem(coreSession, currentDocument) != null;
    }

    /**
     * {@link #NXDRIVE_PROTOCOL} must be handled by a protocol handler
     * configured on the client side (either on the browser, or on the OS).
     * 
     * @return Drive edit URL in the form "{@link #NXDRIVE_PROTOCOL}:// {@link #PROTOCOL_COMMAND_EDIT} /protocol/server[:port]/webappName/nxdoc/repoName/docRef"
     * @throws ClientException
     * 
     */
    public String getDriveEditURL(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {

        FileSystemItem currentFileSystemItem = getCurrentFileSystemItem(coreSession, currentDocument);

        // Current document must be adaptable as a FileSystemItem
        if (currentFileSystemItem == null) {
            throw new ClientException(String.format(
                    "Document %s (%s) is not adaptable as a FileSystemItem thus not Drive editable, \"driveEdit\" action should not be displayed.",
                    currentDocument.getId(), currentDocument.getPathAsString()));
        }

        
        
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new ClientException(String.format("Document %s (%s) is not a BlobHolder, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            throw new ClientException(String.format("Document %s (%s) has no blob, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        String fileName = blob.getFilename();

        StringBuffer sb = new StringBuffer();
        sb.append(NXDRIVE_PROTOCOL).append("://");
        sb.append(PROTOCOL_COMMAND_EDIT).append("/");
        sb.append(Framework.getProperty("nuxeo.url").replaceFirst("://", "/"));
        sb.append("/repo/");
        sb.append(coreSession.getRepositoryName());
        sb.append("/nxdocid/");
        sb.append(currentDocument.getId());
        sb.append("/filename/");
        String escapedFilename = fileName.replaceAll("(/|\\\\|\\*|<|>|\\?|\"|:|\\|)", "-");
        sb.append(URIUtils.quoteURIPathComponent(escapedFilename, true));
        return sb.toString();
        
    }

    public boolean canSynchronizeCurrentDocument(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {

        if (currentDocument == null) {
            return false;
        }
        return isSyncRootCandidate(coreSession, currentDocument) && getCurrentSynchronizationRoot(coreSession, currentDocument) == null;
    }

    public boolean canUnSynchronizeCurrentDocument(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {

        if (currentDocument == null) {
            return false;
        }
        if (!isSyncRootCandidate(coreSession, currentDocument)) {
            return false;
        }
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getCurrentSynchronizationRoot(coreSession, currentDocument);
        if (currentSyncRoot == null) {
            return false;
        }
        return currentDocRef.equals(currentSyncRoot.getRef());
    }

    public boolean canNavigateToCurrentSynchronizationRoot(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {

        if (currentDocument == null) {
            return false;
        }
        if (LifeCycleConstants.DELETED_STATE.equals(currentDocument.getCurrentLifeCycleState())) {
            return false;
        }
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getCurrentSynchronizationRoot(coreSession, currentDocument);
        if (currentSyncRoot == null) {
            return false;
        }
        return !currentDocRef.equals(currentSyncRoot.getRef());
    }

    protected boolean isSyncRootCandidate(CoreSession coreSession, DocumentModel doc) throws ClientException {
        if (!doc.isFolder()) {
            return false;
        }
        if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            return false;
        }
        if (!coreSession.hasPermission(doc.getRef(), SecurityConstants.ADD_CHILDREN)) {
            return false;
        }
        return true;
    }

    protected FileSystemItem getCurrentFileSystemItem(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        // if (currentFileSystemItem == null) {
            // TODO: optim: add a new method to FileSystemItemAdapterService to
            // quickly compute the fsitem id from a doc (without having to
            // recursively adapt the parents)
            // DocumentModel currentDocument = navigationContext.getCurrentDocument();

        FileSystemItem currentFileSystemItem = null;

            try {
                currentFileSystemItem = Framework.getLocalService(FileSystemItemAdapterService.class).getFileSystemItem(currentDocument, null);
            } catch (RootlessItemException e) {
                log.debug(String.format("RootlessItemException thrown while trying to adapt document %s (%s) as a FileSystemItem.", currentDocument.getId(),
                        currentDocument.getPathAsString()));
            }
            if (currentFileSystemItem == null) {
                log.debug(String.format("Document %s (%s) is not adaptable as a FileSystemItem => currentFileSystemItem is null.", currentDocument.getId(),
                        currentDocument.getPathAsString()));
            }
        // }
        return currentFileSystemItem;
    }


}
