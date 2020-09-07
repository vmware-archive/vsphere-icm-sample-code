/*
 *  ******************************************************
 *  Copyright VMware, Inc. 2019-2020.  All Rights Reserved.
 *  ******************************************************
 *
 * DISCLAIMER. THIS PROGRAM IS PROVIDED TO YOU "AS IS" WITHOUT
 * WARRANTIES OR CONDITIONS # OF ANY KIND, WHETHER ORAL OR WRITTEN,
 * EXPRESS OR IMPLIED. THE AUTHOR SPECIFICALLY # DISCLAIMS ANY IMPLIED
 * WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY # QUALITY,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package com.vmware.sample.hci.vsphere.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.TaskFailedException;
import com.vmware.sample.hci.vsphere.exception.VerificationFailedException;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VimFaultFaultMsg;

public class Datacenter {
    private static final Logger logger = LoggerFactory
            .getLogger(Datacenter.class);

    public static ManagedObjectReference createDatacenter(
            VsphereClient vcConnection, String dcName) {
        ManagedObjectReference dcMor = null;

        // Check the connection
        if (!vcConnection.isConnected()) {
            throw new VerificationFailedException("VC connection is not established");
        }

        // Check the datacenter name
        if (dcName == null || dcName.length() == 0) {
            throw new IllegalArgumentException("Datacenter name is null or empty.");
        }

        logger.info("Start creating datacenter " + dcName + "...");

        ManagedObjectReference rootFolderMor = vcConnection.getServiceContent()
                .getRootFolder();

        try {
            dcMor = vcConnection.getVimPort().createDatacenter(rootFolderMor,
                    dcName);
        } catch (DuplicateNameFaultMsg | InvalidNameFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }

        if (dcMor == null) {
            logger.error("Failed to create datacenter " + dcName);
            TaskFailedException tfe = new TaskFailedException("Failed to create datacenter " + dcName,
                    VsphereConstants.CREATEDATACENTER_TASK_FAILURE,
                    "createDatacenter", dcName);
            throw tfe;
        } else {
            logger.info("Create datacenter " + dcName + " successfully.");
        }

        return dcMor;
    }

    public static boolean isDatacenterExist(VsphereClient vcConnection,
                                            String dcName) {
        return false;
    }

    public static ManagedObjectReference getHostFolder(
            VsphereClient vcConnection, final ManagedObjectReference dcMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        PropertyCollector pCollector = new PropertyCollector(vcConnection);
        return (ManagedObjectReference) pCollector.getDynamicProperty(dcMor,
                PropertyCollector.DATACENTER_HOSTFOLDER_PROPERTYNAME);

    }

    public static ManagedObjectReference getNetworkFolder(
            VsphereClient vcConnection, final ManagedObjectReference dcMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        PropertyCollector pCollector = new PropertyCollector(vcConnection);
        return (ManagedObjectReference) pCollector.getDynamicProperty(dcMor,
                PropertyCollector.DATACENTER_NETWORKFOLDER_PROPERTYNAME);
    }


    public static void deleteDatacenter(VsphereClient vcConnection, ManagedObjectReference mor)
            throws RuntimeFaultFaultMsg, VimFaultFaultMsg, InvalidPropertyFaultMsg {
        logger.info("Deleting datacenter {}", mor.getValue());
        VsphereUtil.deleteObject(vcConnection, mor);
    }
}
