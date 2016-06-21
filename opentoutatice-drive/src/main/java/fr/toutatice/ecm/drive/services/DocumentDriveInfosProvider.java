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


import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;


/**
 * Extension for openToutatice and Drive
 * @author lbillon
 *
 */
public interface DocumentDriveInfosProvider extends DocumentInformationsProvider {

	// entries added to fetchpublicationinfos
	
	/** flag if a folder can be synchronized */ 
	public static final String CAN_SYNCHRONIZE = "canSynchronize";
	
	/** flag if a folder is synchronized and can be unsynchronized */
	public static final String CAN_UNSYNCHRONIZE = "canUnsynchronize";
	
	/** flag if a folder is children of a synchronized folder */
	public static final String CAN_NAVIGATE_TO_CURRENT_SYNCHRONIZATION_ROOT = "canNavigateToCurrentSynchronizationRoot";
	
	/** path of the parents which is synchronized */
	public static final String SYNCHRONIZATION_ROOT_PATH = "synchronizationRootPath";
	
	/** flag if a file is openable in desktop */
	public static final String CAN_EDIT_CURRENT_DOCUMENT = "canEditCurrentDocument";
	
	/** the driveEdit url (open a file) */
	public static final String DRIVE_EDIT_URL = "driveEditURL";

}
