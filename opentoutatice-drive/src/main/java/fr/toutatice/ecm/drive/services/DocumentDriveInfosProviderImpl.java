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
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * impl of ToutaticeDriveService
 * @author lbillon
 *
 */
public class DocumentDriveInfosProviderImpl implements DocumentDriveInfosProvider {

	private static final Log log = LogFactory.getLog("Drive");

    protected static final String IS_UNDER_SYNCHRONIZATION_ROOT = "nuxeoDriveIsUnderSynchronizationRoot";

    protected static final String CURRENT_SYNCHRONIZATION_ROOT = "nuxeoDriveCurrentSynchronizationRoot";


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

}
