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
package fr.toutatice.ecm.drive;

/**
 * Constants
 * 
 * @author lbillon
 * 
 */
public abstract class DriveConstants {


	public static final String CAN_SYNCHRONIZE = "canSynchronize";
	public static final String CAN_UNSYNCHRONIZE = "canUnsynchronize";
	public static final String CAN_NAVIGATE_TO_CURRENT_SYNCHRONIZATION_ROOT = "canNavigateToCurrentSynchronizationRoot";
	public static final String SYNCHRONIZATION_ROOT_PATH = "synchronizationRootPath";
	public static final String CAN_EDIT_CURRENT_DOCUMENT = "canEditCurrentDocument";
	public static final String DRIVE_EDIT_URL = "driveEditURL";
    public static final String CAN_CHECK_IN = "canCheckIn";
	public static final String CAN_CHECK_OUT = "canCheckOut";

	public final static String FACET_CHECKED_OUT_DOC = "CheckedOutDocument";

	public static final String PROPERTY_DOC_REF = "ttcdr:docRef";
}
