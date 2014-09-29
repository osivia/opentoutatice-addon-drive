package fr.toutatice.ecm.drive.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;


public class ToutaticeDriveServiceImpl implements ToutaticeDriveService {


    private static final Log log = LogFactory.getLog(ToutaticeDriveServiceImpl.class);

    protected static final String IS_UNDER_SYNCHRONIZATION_ROOT = "nuxeoDriveIsUnderSynchronizationRoot";

    protected static final String CURRENT_SYNCHRONIZATION_ROOT = "nuxeoDriveCurrentSynchronizationRoot";

    public static final String NXDRIVE_PROTOCOL = "nxdrive";

    public static final String PROTOCOL_COMMAND_EDIT = "edit";

    // protected FileSystemItem currentFileSystemItem;




    public Map<String, String> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        Map<String, String> synchronizationInfos = new TreeMap<String, String>();

        if (canSynchronizeCurrentDocument(coreSession, currentDocument)) {
            synchronizationInfos.put("canSynchronize", "true");
        } else if (canUnSynchronizeCurrentDocument(coreSession, currentDocument)) {
            synchronizationInfos.put("canUnsynchronize", "true");
        } else if (canNavigateToCurrentSynchronizationRoot(coreSession, currentDocument)) {

            synchronizationInfos.put("canNavigateToCurrentSynchronizationRoot", "true");
            synchronizationInfos.put("synchronizationRootPath", getCurrentSynchronizationRoot(coreSession, currentDocument).getPathAsString());

            if (canEditCurrentDocument(coreSession, currentDocument)) {

                synchronizationInfos.put("canEditCurrentDocument", "true");
                synchronizationInfos.put("driveEditURL", getDriveEditURL(coreSession, currentDocument));
            }
        } 
        else {
        	DocumentModel openableDocument = getOpenableDocument(coreSession, currentDocument);
        	
        	if(openableDocument != null) {
        		synchronizationInfos.put("canEditCurrentDocument", "true");
        		synchronizationInfos.put("canCheckIn", "true");
                synchronizationInfos.put("driveEditURL", getDriveEditURL(coreSession, openableDocument));
        	}
        	else if(canCheckOutCurrentDocument(coreSession, currentDocument)) {
            	synchronizationInfos.put("canCheckOut", "true");
            }
        }
        

        log.warn("[drive] " + currentDocument.getPathAsString() + 
        		" (" + currentDocument.getId() + ") " + synchronizationInfos);

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
        // DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        if (currentDocument.isFolder()) {
            return false;
        }
        if (getCurrentSynchronizationRoot(coreSession, currentDocument) == null) {
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


        String fsItemId = currentFileSystemItem.getId();
        // ServletRequest servletRequest = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        // String baseURL = VirtualHostHelper.getBaseURL(servletRequest);
        StringBuffer sb = new StringBuffer();
        sb.append(NXDRIVE_PROTOCOL).append("://");
        sb.append(PROTOCOL_COMMAND_EDIT).append("/");
        sb.append(Framework.getProperty("nuxeo.url").replaceFirst("://", "/"));
        sb.append("/fsitem/");
        sb.append(fsItemId);
        return sb.toString();
    }

    public boolean canSynchronizeCurrentDocument(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        // DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        return isSyncRootCandidate(coreSession, currentDocument) && getCurrentSynchronizationRoot(coreSession, currentDocument) == null;
    }

    public boolean canUnSynchronizeCurrentDocument(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        // DocumentModel currentDocument = navigationContext.getCurrentDocument();
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
        // DocumentModel currentDocument = navigationContext.getCurrentDocument();
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
    
    private static final String CHECKED_OUT_QUERY = "select * from Document Where ttcdr:docRef = '%s'"
            + " AND ecm:parentId = '%s'";
    
    
    public DocumentModel getOpenableDocument(CoreSession coreSession,
			DocumentModel currentDocument) throws ClientException {
    	
    	UserWorkspaceService userWorkspaceService;
		try {
			userWorkspaceService = Framework.getService(UserWorkspaceService.class);
		} catch (Exception e) {
			return null;
		}

		DocumentModel personalWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(coreSession, currentDocument);
		String mySyncDocumentsStr = personalWorkspace.getPathAsString();
		
		mySyncDocumentsStr += "/porte-document";
		
		PathRef path = new PathRef(mySyncDocumentsStr);
		DocumentModel mySyncDocuments = coreSession.getDocument(path);
		
		if(mySyncDocuments == null) {
			return null;
		}
		
		DocumentModelList alreadyChecked = coreSession.query(String.format(CHECKED_OUT_QUERY,currentDocument.getRef().toString(), mySyncDocuments.getId()));
		
		if(alreadyChecked.size() > 0) {
			return alreadyChecked.get(0);
		}
		else {
			return null;
		}
        
    }
    
    private boolean canCheckOutCurrentDocument(CoreSession coreSession,
			DocumentModel currentDocument) throws ClientException {
        if (currentDocument == null) {
            return false;
        }
        
        if (currentDocument.isFolder()) {
            return false;
        }
        
        	
		
		
        // TODO valider document pas déjà checkout
		return true;
	}

    // @Factory(value = "currentDocumentUserWorkspace", scope = ScopeType.PAGE)
    // public boolean isCurrentDocumentUserWorkspace() throws ClientException {
    // DocumentModel currentDocument = navigationContext.getCurrentDocument();
    // if (currentDocument == null) {
    // return false;
    // }
    // return UserWorkspaceHelper.isUserWorkspace(currentDocument);
    // }

    // public String synchronizeCurrentDocument(DocumentModel newSyncRoot) throws ClientException, SecurityException {
    // NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
    // Principal principal = documentManager.getPrincipal();
    // String userName = principal.getName();
    // //DocumentModel newSyncRoot = navigationContext.getCurrentDocument();
    // driveManager.registerSynchronizationRoot(principal, newSyncRoot, documentManager);
    // TokenAuthenticationService tokenService = Framework.getLocalService(TokenAuthenticationService.class);
    // boolean hasOneNuxeoDriveToken = false;
    // for (DocumentModel token : tokenService.getTokenBindings(userName)) {
    // if ("Nuxeo Drive".equals(token.getPropertyValue("authtoken:applicationName"))) {
    // hasOneNuxeoDriveToken = true;
    // break;
    // }
    // }
    // if (hasOneNuxeoDriveToken) {
    // return null;
    // } else {
    // // redirect to user center
    // userCenterViews.setCurrentViewId("userCenterNuxeoDrive");
    // return "view_home";
    // }
    // }

    // public void unsynchronizeCurrentDocument() throws ClientException {
    // NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
    // Principal principal = documentManager.getPrincipal();
    // DocumentModel syncRoot = navigationContext.getCurrentDocument();
    // driveManager.unregisterSynchronizationRoot(principal, syncRoot, documentManager);
    // }

    // public String navigateToCurrentSynchronizationRoot() throws ClientException {
    // DocumentModel currentRoot = getCurrentSynchronizationRoot();
    // if (currentRoot == null) {
    // return "";
    // }
    // return navigationContext.navigateToDocument(currentRoot);
    // }

    // public DocumentModelList getSynchronizationRoots() throws ClientException {
    // DocumentModelList syncRoots = new DocumentModelListImpl();
    // NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
    // Set<IdRef> syncRootRefs = driveManager.getSynchronizationRootReferences(documentManager);
    // for (IdRef syncRootRef : syncRootRefs) {
    // syncRoots.add(documentManager.getDocument(syncRootRef));
    // }
    // return syncRoots;
    // }

    // public void unsynchronizeRoot(DocumentModel syncRoot) throws ClientException {
    // NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
    // Principal principal = documentManager.getPrincipal();
    // driveManager.unregisterSynchronizationRoot(principal, syncRoot, documentManager);
    // }

    // @Factory(value = "nuxeoDriveClientPackages", scope = ScopeType.CONVERSATION)
    // public List<DesktopPackageDefinition> getClientPackages() {
    //
    // List<DesktopPackageDefinition> packages = new ArrayList<DesktopPackageDefinition>();
    // // Add packages from the client directory
    // File clientDir = new File(Environment.getDefault().getServerHome(), "client");
    // if (clientDir.isDirectory()) {
    // for (File file : clientDir.listFiles()) {
    // String fileName = file.getName();
    // boolean isDesktopPackage = false;
    // String platform = null;
    // if (fileName.endsWith(".msi")) {
    // isDesktopPackage = true;
    // platform = "windows";
    // } else if (fileName.endsWith(".dmg")) {
    // isDesktopPackage = true;
    // platform = "osx";
    // } else if (fileName.endsWith(".deb")) {
    // isDesktopPackage = true;
    // platform = "ubuntu";
    // }
    // if (isDesktopPackage) {
    // packages.add(new DesktopPackageDefinition(file, fileName, platform));
    // log.debug(String.format("Added %s to the list of desktop packages available for download.", fileName));
    // }
    // }
    // }
    // // Add external links
    // // TODO: remove when Debian package is available
    // packages.add(new DesktopPackageDefinition("https://github.com/nuxeo/nuxeo-drive/#ubuntudebian-and-other-linux-variants-client",
    // "user.center.nuxeoDrive.platform.ubuntu.docLinkTitle", "ubuntu"));
    // return packages;
    // }

    // public String downloadClientPackage(String name, File file) {
    // FacesContext facesCtx = FacesContext.getCurrentInstance();
    // return ComponentUtils.downloadFile(facesCtx, name, file);
    // }

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
                currentFileSystemItem = Framework.getLocalService(FileSystemItemAdapterService.class).getFileSystemItem(currentDocument);
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
