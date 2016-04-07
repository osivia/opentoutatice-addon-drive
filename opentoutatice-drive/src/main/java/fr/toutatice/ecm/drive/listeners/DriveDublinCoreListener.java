/**
 * 
 */
package fr.toutatice.ecm.drive.listeners;

import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;

/**
 * @author david
 *
 */
public class DriveDublinCoreListener extends DublinCoreListener {
	
	/**
	 * To manage correct lastContributor on synchronization.
	 */
	@Override
    public void handleEvent(Event event) throws ClientException {
		String eventName = event.getName();
		DocumentModel sourceDocument = null;
		if (event.getContext() instanceof DocumentEventContext) {
			sourceDocument = ((DocumentEventContext) event.getContext()).getSourceDocument();
        } else {
            return;
        }
		if(sourceDocument != null){
			if(DocumentEventTypes.BEFORE_DOC_UPDATE.equals(eventName) 
					&& sourceDocument.hasFacet(NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET)){
				Property driveSubscription = sourceDocument.getProperty(NuxeoDriveManagerImpl.DRIVE_SUBSCRIPTIONS_PROPERTY);
				if(driveSubscription.isDirty()){
					return;
				}
			}
		}
		
		super.handleEvent(event);
	}

}
