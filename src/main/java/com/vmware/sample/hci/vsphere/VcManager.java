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

package com.vmware.sample.hci.vsphere;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.connection.helpers.GetMOREF;
import com.vmware.sample.hci.vsphere.exception.HciClientException;
import com.vmware.sample.hci.vsphere.operation.Cluster;
import com.vmware.sample.hci.vsphere.operation.Datacenter;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.HostConnectFaultFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LicenseEntityNotFoundFaultMsg;
import com.vmware.vim25.LicenseManagerLicenseInfo;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VcManager extends VsphereInventory {
    public static final String DVSTYPE = "DistributedVirtualSwitch";

    private static final Logger logger = LoggerFactory.getLogger(VcManager.class);

    private final VcInfo vcInfo;

    public VcManager(VcInfo vcInfo) {
        super(new VsphereClient(vcInfo));
        this.vcInfo = vcInfo;
    }

    public VcInfo getVcInfo() {
        return vcInfo;
    }

    public void disconnect() {
        vsphereClient.disconnect();
    }

    public Map<String, ManagedObjectReference> getDVSwitch()
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        return getMOREFs.inFolderByType(
                vsphereClient.getServiceContent().getRootFolder(), DVSTYPE);
    }

    public ManagedObjectReference createDatacenter(String dcName)
            throws RuntimeException {
        return Datacenter.createDatacenter(getVsphereClient(), dcName);
    }

    public ManagedObjectReference createCluster(String datacenterName,
                                                String clusterName)
            throws RuntimeException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ManagedObjectReference dcMor = VsphereUtil.getDatacenterMor(this.vsphereClient, datacenterName);
        return Cluster.createCluster(getVsphereClient(), dcMor, clusterName);
    }

    public ManagedObjectReference createCluster(ManagedObjectReference dcMor,
                                                String clusterName) throws RuntimeException {
        return Cluster.createCluster(getVsphereClient(), dcMor, clusterName);
    }

    public ManagedObjectReference addHostToCluster(ManagedObjectReference dcMor,
                                                   ManagedObjectReference clusterMor, HostInfo host)
            throws DuplicateNameFaultMsg, HostConnectFaultFaultMsg,
            InvalidLoginFaultMsg, RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        Cluster c = new Cluster(getVsphereClient());
        return c.addHostIntoCluster(dcMor, clusterMor, host);
    }


    public ManagedObjectReference addHostToCluster(String dcName,
                                                   String clusterName, HostInfo host) throws Exception {

        ManagedObjectReference dcMor = VsphereUtil.getDatacenterMor(this.vsphereClient, dcName);
        ManagedObjectReference clusterMor = VsphereUtil.getClusterMor(this.vsphereClient, dcName, clusterName);
        Cluster c = new Cluster(getVsphereClient());
        return c.addHostIntoCluster(dcMor, clusterMor, host);
    }

    public void addLicense(String licenseKey) throws RuntimeFaultFaultMsg {
        ManagedObjectReference licenseMgrMor = this.vsphereClient
                .getServiceContent().getLicenseManager();
        LicenseManagerLicenseInfo licenseInfo = this.vsphereClient.
                getVimPort().decodeLicense(licenseMgrMor, licenseKey);
        if (StringUtils.isBlank(licenseInfo.getEditionKey()) && (StringUtils.isBlank(licenseInfo.getLicenseKey()))) {
            logger.error("Invalid license: {}", licenseKey);
            throw new HciClientException("Invalid license: " + licenseKey);
        }
        this.vsphereClient.getVimPort().addLicense(licenseMgrMor, licenseKey, null);
    }

    public void removeLicense(String licenseKey) throws RuntimeFaultFaultMsg {
        ManagedObjectReference licenseMgrMor = this.vsphereClient
                .getServiceContent().getLicenseManager();
        logger.debug("Remove license {} from VC", licenseKey);
        LicenseManagerLicenseInfo licenseInfo = this.vsphereClient.
                getVimPort().decodeLicense(licenseMgrMor, licenseKey);
        if (null == licenseInfo.getUsed() || (0 == licenseInfo.getUsed().intValue())) {
            this.vsphereClient.getVimPort().removeLicense(licenseMgrMor, licenseKey);
        } else {
            logger.error("License {} is in use and cannot be removed", licenseKey);
            throw new HciClientException("License " + licenseKey + " is used");
        }
    }

    public void assignESXiLicense(HostInfo hostInfo, String licenseKey)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg {

        ManagedObjectReference hostMor = VsphereUtil.getHostMor(this.vsphereClient,
                hostInfo.getHostName());
        if (null == hostMor) {
            throw new HciClientException("Host " + hostInfo.getHostName() + " not found");
        }
        this.assignLicense(hostMor.getValue(), licenseKey);
    }


    public void assignVcLicense(String licenseKey)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg {
        String vcId = this.vsphereClient.getServiceContent().getAbout().getInstanceUuid();
        this.assignLicense(vcId, licenseKey);
    }


    public void assignVsanLicense(String dcName, String clusterName, String licenseKey)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg {

        ManagedObjectReference clusterMor = VsphereUtil.getClusterMor(this.vsphereClient,
                dcName, clusterName);
        this.assignLicense(clusterMor.getValue(), licenseKey);
    }

    private void assignLicense(String entityId, String licenseKey)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg {

        GetMOREF getMOREFs = new GetMOREF(this.vsphereClient.getVimPort(),
                this.vsphereClient.getServiceContent());
        ManagedObjectReference licenseMgrMor = this.vsphereClient
                .getServiceContent().getLicenseManager();
        ManagedObjectReference LicenseAssignmentManager = (ManagedObjectReference) getMOREFs.
                entityProps(licenseMgrMor, new String[]{"licenseAssignmentManager"})
                .get("licenseAssignmentManager");
        this.vsphereClient.getVimPort().updateAssignedLicense(LicenseAssignmentManager
                , entityId, licenseKey, null);
    }
}
