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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.exception.HciClientException;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.TaskFailedException;
import com.vmware.sample.hci.vsphere.hwconfig.HardwareConfiguration;
import com.vmware.sample.hci.vsphere.operation.Cluster;
import com.vmware.sample.hci.vsphere.operation.HostNetwork;
import com.vmware.sample.hci.vsphere.operation.VirtualDistributedSwitch;
import com.vmware.sample.hci.vsphere.utils.ConfigProgress;
import com.vmware.sample.hci.vsphere.utils.ProgressCallback;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.sample.hci.vsphere.vcinstall.VcInstaller;
import com.vmware.sample.hci.vsphere.vcinstall.VcInstallerInfo;
import com.vmware.sample.hci.vsphere.vcinstall.VcJsonInput;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.DvsFaultFaultMsg;
import com.vmware.vim25.DvsNotAuthorizedFaultMsg;
import com.vmware.vim25.HostConfigFaultFaultMsg;
import com.vmware.vim25.HostService;
import com.vmware.vim25.HostVirtualNicManagerNicType;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.LicenseEntityNotFoundFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NotFoundFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VimFaultFaultMsg;

public class VsphereService {

    private static final Logger logger =
            LoggerFactory.getLogger(VsphereService.class);

    public static void verifyHost(HostInfo[] hostInfoList) {
        // verify all hosts in hostinfo list
        for (HostInfo hostInfo : hostInfoList) {
            VerifyHost verifyHost = new VerifyHost(new HostManager(hostInfo));
            verifyHost.verify();
        }
    }

    /**
     * Deploy VCenter to ESXi0.
     *
     * @param hostInfo,      Ip, username and password of ESXi0
     * @param vcIsoPath,     location of Vcenter installer iso.
     * @param vcMntPath,     the path of virtual machine OS to mnt iso to.
     * @param datastoreName, on which the Vcenter to be installed. Required by
     *                       Vcenter installation script.
     * @param vcJsonInput,   information uesd to install Vcenter. Required by
     *                       Vcenter installation script.
     * @param startProgress, the start of progress.
     * @param endProgress,   the end of progress.
     * @param pc,            the callback function which can send the progress number to client
     * @throws HciClientException, be thrown if there is any user input error
     * @throws HciServerException, be thrown if there is any internal exception in server
     */
    public static void deployVcenter(HostInfo hostInfo, String vcIsoPath,
                                     String vcMntPath, String datastoreName, VcJsonInput vcJsonInput,
                                     int startProgress, int endProgress, ProgressCallback pc)
            throws HciClientException, HciServerException {
        StringBuilder errorMsg = new StringBuilder();

        String targetESX = "";
        // User has to provide either IP address or host name.
        // if both IP address is provided, use IP address.
        // else use host name.
        if (StringUtils.isBlank(hostInfo.getIpAddress())
                & (StringUtils.isBlank(hostInfo.getIpAddress()))) {
            errorMsg.append("IP address or host name is not provided.");
        } else if (StringUtils.isBlank(hostInfo.getIpAddress())) {
            targetESX = StringUtils.trim(hostInfo.getIpAddress());
        } else {
            targetESX = StringUtils.trim(hostInfo.getHostName());
        }
        if (StringUtils.isBlank(hostInfo.getUserName())) {
            errorMsg.append("Username of ESXi server is not provided.");
        }
        if (StringUtils.isBlank(hostInfo.getPassword())) {
            errorMsg.append("Password of ESXi server is not provided.");
        }
        String dsName = StringUtils.trim(datastoreName);
        if (StringUtils.isBlank(dsName)) {
            // datastoreName = "vsanDatastore"
            dsName = "datastore1";
            logger.info(
                    "Datastore name is not provided. Use 'datastore1' instead.");
        }
        String vcPath = vcIsoPath.trim();
        if (StringUtils.isBlank(vcPath)) {
            errorMsg.append("Location of VCSA installer is not provided.");
        }
        String mntPath = vcMntPath.trim();
        if (StringUtils.isBlank(mntPath)) {
            errorMsg.append("Mount point is not provided");
        }

        if (StringUtils.isBlank(vcJsonInput.getVmName())) {
            errorMsg.append("VCSA VM name is not provided.");
        }
        if (StringUtils.isBlank(vcJsonInput.getPassword())) {
            errorMsg.append("VCenter password  is not provided.");
        }
        String ipIpFamily = StringUtils.trim(vcJsonInput.getIpFamily());
        if (StringUtils.isBlank(ipIpFamily)) {
            ipIpFamily = "ipv4";
            logger.info("Ip family is not provided. Use 'ipv4' instead.");
        }
        String mode = StringUtils.trim(vcJsonInput.getMode());
        if (StringUtils.isBlank(mode)) {
            mode = "static";
            logger.info("Mode is not provided. Use 'static' instead.");
        }
        String prefix = StringUtils.trim(vcJsonInput.getPrefix());
        if (StringUtils.isBlank(prefix)) {
            prefix = "24";
            logger.info("Prefix is not provided. Use '24' instead.");
        } else {
            // if prefix is provide, make sure it is a integer
            // and between 0~32
            int prefixInt = 24;
            try {
                prefixInt = Integer.parseInt(prefix);
            } catch (NumberFormatException ex) {
                errorMsg.append(
                        String.format("Prefix %s is not a number", prefix));
                logger.info("Prefix %s is not a number {}", prefix);
            }
            if ((prefixInt < 0) && (prefixInt > 32)) {
                errorMsg.append(
                        String.format("Prefix %s must between 1~32", prefix));
                logger.info("Prefix {} must between 1~32", prefix);
            }
        }

        if (StringUtils.isBlank(vcJsonInput.getIp())) {
            errorMsg.append("VC ip is not provided.");
        }
        if (StringUtils.isBlank(vcJsonInput.getGateway())) {
            errorMsg.append("VC gateway is not provided.");
        }
        if (StringUtils.isBlank(vcJsonInput.getDnsServer())) {
            errorMsg.append("VC DNS server is not provided.");
        }
        if (startProgress < 0) {
            errorMsg.append("Start of progress should not less than 0");
        }
        if (endProgress > 100) {
            errorMsg.append("End of progress should not larger than 100");
        }
        if (startProgress > endProgress) {
            errorMsg.append("End of progress should larger than start");
        }

        if (errorMsg.length() > 0) {
            logger.error("User input with error: {}", errorMsg.toString());
            throw new HciClientException(errorMsg.toString());
        }

        VcInstallerInfo vcInstInfo = new VcInstallerInfo();

        vcInstInfo.setEsxHostname(targetESX);
        vcInstInfo.setEsxDatastore(dsName);
        vcInstInfo.setEsxUsername(hostInfo.getUserName());
        vcInstInfo.setEsxPassword(hostInfo.getPassword());

        vcInstInfo.setDeploymentOption("small");
        vcInstInfo.setDeploymentNetwork("VM Network");
        vcInstInfo.setApplianceName(vcJsonInput.getVmName());
        vcInstInfo.setApplianceThinDiskMode(true);
        vcInstInfo.setVcRootPassword(vcJsonInput.getPassword());
        vcInstInfo.setVcSshEnabled(true);
        vcInstInfo.setVcSsoPassword(vcJsonInput.getPassword());
        vcInstInfo.setVcSsoDomainName("vsphere.local");
        vcInstInfo.setVcSsoSiteName("Default-First-Site");

        vcInstInfo.setVcNWIpFamily(ipIpFamily);
        vcInstInfo.setVcNWMode(mode);
        vcInstInfo.setVcNWIp(vcJsonInput.getIp());
        vcInstInfo.setVcNWPrefix(prefix);
        vcInstInfo.setVcNWGateway(vcJsonInput.getGateway());
        vcInstInfo.setVcNWDnsServer(vcJsonInput.getDnsServer());
        vcInstInfo
                .setVcNWDnsAlternateServer(vcJsonInput.getDnsAlternateServer());
        vcInstInfo.setVcNWSystemName(vcJsonInput.getSystemName());

        vcInstInfo.setVcIsoPath(vcPath);
        vcInstInfo.setVcMntPath(mntPath);

        VcInstaller.installVc(vcInstInfo, true, startProgress, endProgress, pc);

    }

    /**
     * Initialize VCenter by creating datacenter, cluster, distributed switch,
     * port group, adding hosts into cluster, enabling vsan, ha and dr.
     *
     * @param vcConfig,    VC configuration.
     * @param hostConfigs, host configuration.
     * @param start,       start progress.
     * @param end,         end start progress.
     * @param pc,          progress callback function
     * @return list of host IP address on which error occurs
     */
    public static List<String> initializeVcenter(InputVcConfiguration vcConfig,
                                                 InputHostConfiguration[] hostConfigs, int start, int end, ProgressCallback pc) {
        ConfigProgress.setProgress(start, pc);
        int share = (end - start) / (3 + hostConfigs.length);
        logger.debug("Start is {}, end is {}, Share is {}", start, end, share);
        //check input
        checkAddHostsInput(vcConfig, hostConfigs);

        VcManager vcManager = null;
        ManagedObjectReference dcMor = null;
        ManagedObjectReference clusterMor = null;
        ManagedObjectReference vdsMor = null;
        ManagedObjectReference[] pgMors = new ManagedObjectReference[InputPortgroupConfiguration.PORTGROUP_NUM];
        try {
            // create DataCenter
            logger.info("__Start creating datacenter");
            vcManager = new VcManager(vcConfig.getVcInfo());
            dcMor = vcManager.createDatacenter(vcConfig.getDataCenterName());
            logger.info("__Datacenter created");
            // create cluster
            logger.info("__Start creating cluster");
            clusterMor = vcManager.createCluster(dcMor, vcConfig.getClusterName());
            logger.info("__Cluster created");
            logger.info("__Creating dvs");
            // create vds
            vdsMor = VirtualDistributedSwitch
                    .createVds(vcManager.getVsphereClient(), dcMor, vcConfig.getVdsName());
            logger.info("__Creating dvs completed");
            // create portgroup
            logger.info("__Creating portgroup");
            pgMors[0] = VirtualDistributedSwitch.createDVPortGroup(
                    vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                            .getMgmtPortgroupName(), null);
            pgMors[1] = VirtualDistributedSwitch.createDVPortGroup(
                    vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                            .getvMotionPortgroupName(), vcConfig.getPortGroupInfo().getvMotionVlanTrunk());
            pgMors[2] = VirtualDistributedSwitch.createDVPortGroup(
                    vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                            .getvSanPortgroupName(), vcConfig.getPortGroupInfo().getvSanVlanTrunk());
            pgMors[3] = VirtualDistributedSwitch.createDVPortGroup(
                    vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                            .getVmPortgroupName(), vcConfig.getPortGroupInfo().getVmVlanTrunk());

            logger.info("__Creating portgroup completed");
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (TaskFailedException e) {
            logger.error("Falied to performe {} on {} with error code {}. {} ",
                    e.getTaskName(), e.getTargetName(), e.getErrorCode(),
                    e.getMessage());
            throw new HciServerException(e.getMessage(), e);
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg
                | DuplicateNameFaultMsg | DvsFaultFaultMsg
                | DvsNotAuthorizedFaultMsg | InvalidNameFaultMsg
                | NotFoundFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
        ConfigProgress.increaseProgressBy(share, pc);
        Cluster myCluster = new Cluster(vcManager.getVsphereClient());
        // enabled vSan first. Otherwise node0 could not be added
        try {
            myCluster.enableVsan(clusterMor);
            myCluster.waitForVsanUpdateTaskInCluster(clusterMor);
        } catch (Exception e) {
            logger.error("Error when enabling Vsan", e);
            throw new HciServerException(e.getMessage(), e);
        }
        ConfigProgress.increaseProgressBy(share, pc);
        List<String> errorHostList =
                addHostToVc(vcManager, dcMor, clusterMor, vdsMor, pgMors, hostConfigs, share, pc);
        logger.info("Re-enable HA on cluster");
        try {
            myCluster.setDRS(clusterMor, true);
            myCluster.setDAS(clusterMor, true);
            myCluster.waitForVsanUpdateTaskInCluster(clusterMor);
        } catch (Exception e) {
            logger.error("Error when enabling DRS and DAS", e);
            throw new HciServerException(e.getMessage(), e);
        }
        ConfigProgress.setProgress(end, pc);
        return errorHostList;
    }

    public static void initializeCluster() {
    }

    public static void initializeVsan() {
    }

    /**
     * Update username and password of an ESXi host.
     *
     * @param hostInfo, Ip, hostname, username and password of ESXi host
     * @param userName, new username of ESXi host.
     * @param password, new password of ESXi host.
     */
    public static void updateUserPassword(HostInfo hostInfo, String userName,
                                          String password) {
        HostManager hostManager = new HostManager(hostInfo);
        try {
            hostManager.updateUserPassword(userName, password);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (RuntimeFaultFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Update hostname and domain name of an ESXi host.
     *
     * @param hostInfo, Ip, hostname, username and password of ESXi host
     * @param hostName, new name of ESXi host.
     * @param domain,   new domain name of ESXi host.
     */
    public static void updateHostName(HostInfo hostInfo, String hostName,
                                      String domain) {
        HostManager hostManager = new HostManager(hostInfo);
        try {
            hostManager.updateHostName(hostName, domain);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (RuntimeFaultFaultMsg | InvalidStateFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Update DNS of an ESXi host.
     *
     * @param hostInfo,  Ip, hostname, username and password of ESXi host
     * @param dnsServer, array of DNS server ip.
     */
    public static void updateDns(HostInfo hostInfo, String[] dnsServer) {
        HostManager hostManager = new HostManager(hostInfo);
        try {
            hostManager.updateDns(dnsServer);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Update NTP of an ESXi host.
     *
     * @param hostInfo,  Ip, hostname, username and password of ESXi host
     * @param ntpServer, array of NTP server ip.
     */
    public static void updateNtp(HostInfo hostInfo, String[] ntpServer) {
        HostManager hostManager = new HostManager(hostInfo);
        try {
            hostManager.updateNtp(ntpServer);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Add a new management Ip to an ESXi host.
     *
     * @param hostInfo,  Ip, hostname, username and password of ESXi host
     * @param ipAddress, new ip address.
     * @param mask,      subnet mask
     * @param pgName,    name of port group to be created
     */
    public static void addManagementIpAddress(HostInfo hostInfo,
                                              String ipAddress, String mask, String pgName) {
        HostManager hostManager = new HostManager(hostInfo);
        try {
            String vswName = hostManager.getValidVirtualSwitch(pgName);
            hostManager.addVirtualSwitchPortGroup(pgName, vswName);
            hostManager.addManagementIPAddress(ipAddress, mask, pgName);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg
                | InvalidPropertyFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    public static void createCluster(VcInfo vcInfo, String dcName,
                                     String clusterName) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.createCluster(dcName, clusterName);
        } catch (InvalidPropertyFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage());
        } catch (RuntimeException | RuntimeFaultFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * update gateway of an ESXi host.
     *
     * @param hostInfo, Ip, hostname, username and password of ESXi host
     * @param gateway,  new gateway ip address.
     */
    public static void updateDefaultGateway(HostInfo hostInfo, String gateway) {
        HostManager hostManager = new HostManager(hostInfo);
        try {
            hostManager.updateDefaultGateway(gateway);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg
                | InvalidPropertyFaultMsg | HostConfigFaultFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Backup host information of ESXi host.
     * The bundled packege is saved to /bootbank/reset
     *
     * @param hostInfo, Ip, hostname, username and password of ESXi host
     */
    public static void backupHost(HostInfo hostInfo) {
        HostManager hostManager = new HostManager(hostInfo);
        boolean isSSHRunning = true;
        String policyString = "";
        try {
            HostService service = hostManager.getServiceInfo("TSM-SSH");
            isSSHRunning = service.isRunning();
            policyString = service.getPolicy();
            // if SSH is not running, enable it first
            if (!isSSHRunning) {
                hostManager.updateHostService("TSM-SSH", true, policyString);
            }
            hostManager.runBackupCmd();
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg
                | HostConfigFaultFaultMsg | InvalidStateFaultMsg
                | NotFoundFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        } finally {
            if (!isSSHRunning) {
                // disable SSH service if necessary
                try {
                    hostManager.updateHostService("TSM-SSH", false,
                            policyString);
                } catch (HostConfigFaultFaultMsg | InvalidStateFaultMsg
                        | NotFoundFaultMsg | RuntimeFaultFaultMsg
                        | InvalidPropertyFaultMsg e) {
                    logger.error(e.getMessage(), e);
                    throw new HciServerException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * To run commands through SSH channel to restore host.
     *
     * @param hostInfo, Ip, hostname, username and password of ESXi host
     */
    public static void restoreHost(HostInfo hostInfo) {
        HostManager hostManager = new HostManager(hostInfo);
        boolean isSSHRunning = true;
        String policyString = "";
        try {
            // get service information.
            // the service should be disabled if it was not running at the
            // beginning.
            // keep the policy during update process.
            HostService service = hostManager.getServiceInfo("TSM-SSH");
            isSSHRunning = service.isRunning();
            policyString = service.getPolicy();
            // if SSH is not running, enable it first
            if (!isSSHRunning) {
                hostManager.updateHostService("TSM-SSH", true, policyString);
            }
            hostManager.runRestoreCmd();
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg
                | HostConfigFaultFaultMsg | InvalidStateFaultMsg
                | NotFoundFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        } finally {
            if (!isSSHRunning) {
                // disable SSH service if necessary
                try {
                    int retry = 0;
                    HostManager newHostManager = null;
                    while (retry < VsphereConstants.MAX_RETRY) {
                        logger.info(
                                "Wait for host restoring and reconnect to host.-- {}",
                                (retry + 1));
                        try {
                            Thread.sleep(120000);
                        } catch (InterruptedException ie) {
                            logger.error(ie.getMessage(), ie);
                        }
                        try {
                            newHostManager = new HostManager(hostInfo);
                            break;
                        } catch (Exception ex) {
                            logger.debug("Failed to connect to host. Retrying",
                                    ex);
                        }
                        retry++;
                    }
                    if (retry == VsphereConstants.MAX_RETRY) {
                        logger.error("Retry times exceeds.");
                        throw new HciServerException("Retry times exceeds.");
                    }
                    newHostManager.updateHostService("TSM-SSH", false, policyString);
                } catch (HostConfigFaultFaultMsg | InvalidStateFaultMsg
                        | NotFoundFaultMsg | RuntimeFaultFaultMsg
                        | InvalidPropertyFaultMsg e) {
                    logger.error(e.getMessage(), e);
                    throw new HciServerException(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * After host restoring, remove it from inventory on VCenter.
     *
     * @param vcInfo,        the VCenter information.
     * @param hostConfigs    the information of multiple ESXi hosts.
     * @param waitForRestore if true, then wait 120 seconds before acting.
     * @return List<String>, host name of failure hosts.
     */
    public static List<String> RemoveHostsFromInventory(VcInfo vcInfo,
                                                        InputHostConfiguration[] hostConfigs, boolean waitForRestore) {

        if (waitForRestore) {
            logger.info("Wait 2 minutes for all host from restoring");
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        VcManager vcManager = new VcManager(vcInfo);
        List<String> errorHostList = new ArrayList<>();
        for (InputHostConfiguration hostCfg : hostConfigs) {
            try {
                ManagedObjectReference hostMor =
                        VsphereUtil.getHostMor(vcManager.getVsphereClient(),
                                hostCfg.getCustomizedHostInfo().getHostName());
                if (null != hostMor) {
                    VsphereUtil.deleteObject(vcManager.getVsphereClient(), hostMor);
                } else {
                    logger.info("{} is not in the inventory", hostCfg.getCustomizedHostInfo().getHostName());
                }
            } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg
                    | VimFaultFaultMsg e) {
                errorHostList.add(hostCfg.getCustomizedHostInfo().getHostName());
            }
        }
        if (errorHostList.size() > 0) {
            logger.error(
                    "Failed to remove hosts {} from inventory,"
                            + "please see more detail in log",
                    errorHostList.toString());
        }
        return errorHostList;
    }

    public static void writeInfoLog(String msg) {
        logger.info(msg);
    }

    /**
     * Check input information.
     *
     * @param vcConfig,   the VCenter information.
     * @param hostConfigs the information of multiple ESXi hosts.
     * @throws HciClientException if there is any invalid input.
     */
    public static void checkAddHostsInput(InputVcConfiguration vcConfig,
                                          InputHostConfiguration[] hostConfigs) {
        List<String> errorInputList = new ArrayList<>();
        errorInputList.addAll(vcConfig.checkValid());
        for (InputHostConfiguration hostCfg : hostConfigs) {
            errorInputList.addAll(hostCfg.checkValid());
        }
        if (errorInputList.size() > 0) {
            logger.error(
                    "There are input errors: {}",
                    errorInputList.toString());
            throw new HciClientException("There are input errors."
                    + " See more detail in log");
        }
    }

    /**
     * Add hosts to existing datacenter/cluster/distributed switch, port group
     *
     * @param vcConfig,    VC configuration.
     * @param hostConfigs, host configuration.
     * @param start,       start progress.
     * @param end,         end start progress.
     * @param pc,          progress callback function
     * @return list of host IP address on which error occurs
     */
    public static List<String> addHosts(InputVcConfiguration vcConfig,
                                        InputHostConfiguration[] hostConfigs, int start, int end,
                                        ProgressCallback pc) {
        //check input
        ConfigProgress.setProgress(start, pc);
        int share = ((end - start) / (3 + hostConfigs.length));
        logger.debug("Start is {}, end is {}, share is {}", start, end, share);

        checkAddHostsInput(vcConfig, hostConfigs);

        VcManager vcManager = null;
        ManagedObjectReference dcMor = null;
        ManagedObjectReference clusterMor = null;
        ManagedObjectReference vdsMor = null;
        ManagedObjectReference[] pgMors = new ManagedObjectReference[InputPortgroupConfiguration.PORTGROUP_NUM];
        try {
            vcManager = new VcManager(vcConfig.getVcInfo());
            //Get datacenter mor
            dcMor = VsphereUtil.getDatacenterMor(vcManager.getVsphereClient(), vcConfig.getDataCenterName());
            if (null == dcMor) {
                dcMor = vcManager.createDatacenter(vcConfig.getDataCenterName());
            }
            //Get cluster mor
            clusterMor = VsphereUtil.getClusterMor(vcManager.getVsphereClient(), dcMor, vcConfig.getClusterName());
            if (null == clusterMor) {
                clusterMor = vcManager.createCluster(dcMor, vcConfig.getClusterName());
            }
            //Get vds mor
            vdsMor = VsphereUtil.getVdsMor(vcManager.getVsphereClient(), vcConfig.getVdsName());
            if (null == vdsMor) {
                vdsMor = VirtualDistributedSwitch
                        .createVds(vcManager.getVsphereClient(), dcMor, vcConfig.getVdsName());
            }

            pgMors[0] = VsphereUtil.getPorggroupMor(vcManager.getVsphereClient(), dcMor,
                    vcConfig.getPortGroupInfo().getMgmtPortgroupName());
            if (null == pgMors[0]) {
                pgMors[0] = VirtualDistributedSwitch.createDVPortGroup(
                        vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                                .getMgmtPortgroupName(), null);
            }
            pgMors[1] = VsphereUtil.getPorggroupMor(vcManager.getVsphereClient(), dcMor,
                    vcConfig.getPortGroupInfo().getvMotionPortgroupName());
            if (null == pgMors[1]) {
                pgMors[1] = VirtualDistributedSwitch.createDVPortGroup(
                        vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                                .getvMotionPortgroupName(), vcConfig.getPortGroupInfo().getvMotionVlanTrunk());
            }

            pgMors[2] = VsphereUtil.getPorggroupMor(vcManager.getVsphereClient(), dcMor,
                    vcConfig.getPortGroupInfo().getvSanPortgroupName());
            if (null == pgMors[2]) {
                pgMors[2] = VirtualDistributedSwitch.createDVPortGroup(
                        vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                                .getvSanPortgroupName(), vcConfig.getPortGroupInfo().getvSanVlanTrunk());
            }

            pgMors[3] = VsphereUtil.getPorggroupMor(vcManager.getVsphereClient(), dcMor,
                    vcConfig.getPortGroupInfo().getVmPortgroupName());
            if (null == pgMors[3]) {
                pgMors[3] = VirtualDistributedSwitch.createDVPortGroup(
                        vcManager.getVsphereClient(), vdsMor, vcConfig.getPortGroupInfo()
                                .getVmPortgroupName(), vcConfig.getPortGroupInfo().getVmVlanTrunk());
            }

        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        } catch (TaskFailedException e) {
            logger.error("Falied to performe {} on {} with error code {}. {} ",
                    e.getTaskName(), e.getTargetName(), e.getErrorCode(),
                    e.getMessage());
            throw new HciServerException(e.getMessage(), e);
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg
                | DuplicateNameFaultMsg | DvsFaultFaultMsg
                | DvsNotAuthorizedFaultMsg | InvalidNameFaultMsg
                | NotFoundFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
        if ((null == hostConfigs) || (hostConfigs.length == 0)) {
            logger.info("No ESXi hosts information provided. Quiting");
            return new ArrayList<String>();
        }
        ConfigProgress.increaseProgressBy(share, pc);
        //Disable HA first
        logger.info("Disable HA on cluster to avoid alert when adding new hosts");
        Cluster myCluster = new Cluster(vcManager.getVsphereClient());
        try {
            myCluster.setDAS(clusterMor, false);
        } catch (Exception e) {
            logger.error("Error when enabling DAS", e);
            throw new HciServerException(e.getMessage(), e);
        }
        ConfigProgress.increaseProgressBy(share, pc);
        List<String> errorHostList = addHostToVc(vcManager, dcMor, clusterMor, vdsMor,
                pgMors, hostConfigs, share, pc);
        // enabled vSan, HA and DR..
        logger.info("Re-enable HA on cluster");
        try {
            myCluster.setDAS(clusterMor, true);
            myCluster.waitForVsanUpdateTaskInCluster(clusterMor);
        } catch (Exception e) {
            logger.error("Error when enabling DAS", e);
            throw new HciServerException(e.getMessage(), e);
        }
        ConfigProgress.setProgress(end, pc);
        return errorHostList;
    }

    private static List<String> addHostToVc(
            VcManager vcManager, ManagedObjectReference dcMor,
            ManagedObjectReference clusterMor, ManagedObjectReference vdsMor,
            ManagedObjectReference[] pgMors, InputHostConfiguration[] hostConfigs,
            int share, ProgressCallback pc
    ) {

        List<String> errorHostList = new ArrayList<String>();
        logger.info(
                "__Adding hosts to dvs, migrating VMs, Migrating vNics, Adding vNics");
        for (InputHostConfiguration hostConfig : hostConfigs) {
            try {
                HostManager hostMgr = new HostManager(hostConfig.getCustomizedHostInfo());
                hostMgr.exitMaintenanceMode();
                logger.info("__Adding {} to cluster",
                        hostConfig.getCustomizedHostInfo().getIpAddress());
                ManagedObjectReference hostMor = vcManager
                        .addHostToCluster(dcMor, clusterMor, hostConfig.getCustomizedHostInfo());
                logger.info("__Adding {} to cluster completed",
                        hostConfig.getCustomizedHostInfo().getIpAddress());

                logger.info("__Adding {} to vds", hostConfig.getCustomizedHostInfo().getIpAddress());
                VirtualDistributedSwitch.addHostToVds(vcManager.getVsphereClient(),
                        dcMor, hostMor, vdsMor, pgMors[0]);
                logger.info("__Adding {} to vds completed",
                        hostConfig.getCustomizedHostInfo().getIpAddress());
                HostNetwork hostnwk = new HostNetwork(vcManager.getVsphereClient(),
                        hostMor, vdsMor, pgMors);
                ManagedObjectReference[] vmmor = hostnwk.getVM();
                int vmlen = vmmor.length;
                for (int j = 0; j < vmlen; j++) {
                    logger.info("__Migrating vm {}", vmmor[j].getValue());
                    hostnwk.migrateVmToVds(vmmor[j]);
                    logger.info("__Migrating vm {} complete",
                            vmmor[j].getValue());
                }
                logger.info("__Migrating vNic on {}",
                        hostConfig.getCustomizedHostInfo().getIpAddress());
                hostnwk.migrateVmkNicToVds();
                logger.info("__Migrating vNic on {} completed",
                        hostConfig.getCustomizedHostInfo().getIpAddress());

                logger.info("__Adding vMotion vNic on {}",
                        hostConfig.getCustomizedHostInfo().getIpAddress());

                hostnwk.addVirtualNIC(hostConfig.isvMotionDHCP(),
                        hostConfig.getvMotionIp(), hostConfig.getvMotionNetmask(),
                        HostVirtualNicManagerNicType.VMOTION);
                logger.info("__Adding vMotion vNic on {} completed",
                        hostConfig.getCustomizedHostInfo().getIpAddress());

                logger.info("__Adding vSan vNic on {}",
                        hostConfig.getCustomizedHostInfo().getIpAddress());

                hostnwk.addVirtualNIC(hostConfig.isvSanDHCP(),
                        hostConfig.getvSanIp(), hostConfig.getvSanNetmask(),
                        HostVirtualNicManagerNicType.VSAN);
                logger.info("__Adding vSan vNic on {} completed",
                        hostConfig.getCustomizedHostInfo().getIpAddress());
                ConfigProgress.increaseProgressBy(share, pc);
            } catch (IllegalArgumentException e) {
                errorHostList.add(hostConfig.getCustomizedHostInfo().getIpAddress());
                logger.error(e.getMessage(), e);
                throw new HciClientException(e.getMessage(), e);
            } catch (Exception e) {
                errorHostList.add(hostConfig.getCustomizedHostInfo().getIpAddress());
                if (e instanceof TaskFailedException) {
                    TaskFailedException ex = (TaskFailedException) e;
                    logger.error(
                            "Falied to performe {} on {} with error code {}. {} ",
                            ex.getTaskName(), ex.getTargetName(),
                            ex.getErrorCode(), ex.getMessage());
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (errorHostList.size() > 0) {
            logger.error(
                    "Failed to operate on host {}, please see more detail in log",
                    errorHostList.toString());
        }
        return errorHostList;
    }

    private static void enableALLOnCluster(VcManager vcManager, ManagedObjectReference clusterMor) {
        Cluster myCluster = new Cluster(vcManager.getVsphereClient());
        try {
            myCluster.enableVsan(clusterMor);
            myCluster.waitForVsanUpdateTaskInCluster(clusterMor);
        } catch (Exception e) {
            logger.error("Error when enabling Vsan", e);
            throw new HciServerException(e.getMessage(), e);
        }
        try {
            myCluster.setDRS(clusterMor, true);
            myCluster.waitForVsanUpdateTaskInCluster(clusterMor);
        } catch (Exception e) {
            logger.error("Error when enabling DRS", e);
            throw new HciServerException(e.getMessage(), e);
        }
        try {
            myCluster.setDAS(clusterMor, true);
            myCluster.waitForVsanUpdateTaskInCluster(clusterMor);
        } catch (Exception e) {
            logger.error("Error when enabling DAS", e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Add license through VC. Only valid license could be added. Otherwise
     * HciClientException will be thrown.
     *
     * @param vcInfo,  VC connection info.
     * @param license, license key. Could be VC, ESXi and VSAN license
     * @throws HciServerException if any internal error
     * @throws HciClientException if the license is invalid
     */
    public static void addLicense(VcInfo vcInfo, String license) {
        VcManager vcManager = new VcManager(vcInfo);
        logger.debug("Adding license {}", license);
        try {
            vcManager.addLicense(license);
        } catch (RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Remove license through VC. Only license that is not used could be removed.
     * Otherwise HciClientException will be thrown.
     *
     * @param vcInfo,  VC connection info.
     * @param license, license key. Could be VC, ESXi and VSAN license
     * @throws HciClientException if the license is assigned
     * @throws HciServerException if any internal error
     */
    public static void removeLicense(VcInfo vcInfo, String license) {
        VcManager vcManager = new VcManager(vcInfo);
        logger.debug("Removing license {}", license);
        try {
            vcManager.removeLicense(license);
        } catch (RuntimeFaultFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Assign VC license to VC.
     *
     * @param vcInfo,  VC connection info.
     * @param license, VC license key.
     * @throws HciClientException if no VC entity found
     * @throws HciServerException if any internal error
     */
    public static void assignVcLicense(VcInfo vcInfo, String license) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.assignVcLicense(license);
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        } catch (LicenseEntityNotFoundFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        }
    }

    /**
     * Remove assigned VC license.
     *
     * @param vcInfo, VC connection info.
     * @throws HciClientException if no VC entity found
     * @throws HciServerException if any internal error
     */
    public static void removeVcAssignedLicense(VcInfo vcInfo) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.assignVcLicense(VsphereConstants.EVALUATION_LICENSE);
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        } catch (LicenseEntityNotFoundFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        }
    }

    /**
     * Assign VSAN license to a cluster. Only cluster enabled with VSAN can be
     * assigned a VSAN license
     *
     * @param vcInfo,      VC connection info.
     * @param dcName,      Datacenter name.
     * @param clusterName, cluster name.
     * @param license,     VSAN license key.
     * @throws HciClientException if no cluster entity found
     * @throws HciServerException if any internal error
     */
    public static void assignVsanLicense(VcInfo vcInfo, String dcName,
                                         String clusterName, String license) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.assignVsanLicense(dcName, clusterName, license);
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        } catch (LicenseEntityNotFoundFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        }
    }

    /**
     * Remove VSAN license from a cluster.
     *
     * @param vcInfo,      VC connection info.
     * @param dcName,      Datacenter name.
     * @param clusterName, cluster name.
     * @throws HciClientException if no cluster entity found
     * @throws HciServerException if any internal error
     */
    public static void removeVsanAssignedLicense(VcInfo vcInfo, String dcName,
                                                 String clusterName) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.assignVsanLicense(dcName, clusterName, VsphereConstants.EVALUATION_LICENSE);
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        } catch (LicenseEntityNotFoundFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciClientException(e.getMessage(), e);
        }
    }

    /**
     * Assign ESXi license to some ESXi hosts
     *
     * @param vcInfo,   VC connection info.
     * @param hostInfo, host information.
     * @param license,  ESXi license key.
     * @throws HciClientException if ESXi entity found
     * @throws HciServerException if any internal error
     */
    public static void assignESXiLicense(VcInfo vcInfo, HostInfo hostInfo, String license) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.assignESXiLicense(hostInfo, license);
        } catch (HciClientException e) {

            throw e;
        } catch (LicenseEntityNotFoundFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciClientException("license " + license + " can"
                    + " not be assigned to host " + hostInfo.getHostName());
        } catch (Exception e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException("Error occurs when assgining license "
                    + license + " to host " + hostInfo.getHostName());
        }
    }

    /**
     * Remove ESXi license from some ESXi hosts
     *
     * @param vcInfo,   VC connection info.
     * @param hostInfo, host information.
     * @throws HciClientException if ESXi entity found
     * @throws HciServerException if any internal error
     */
    public static void removeESXiAssignedLicense(VcInfo vcInfo, HostInfo hostInfo) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            vcManager.assignESXiLicense(hostInfo, VsphereConstants.EVALUATION_LICENSE);
        } catch (HciClientException e) {

            throw e;
        } catch (LicenseEntityNotFoundFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciClientException("Can not remove assigned "
                    + "license from host " + hostInfo.getHostName());
        } catch (Exception e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException("Error occurs when removing assigned license "
                    + " from host " + hostInfo.getHostName());
        }
    }

    /**
     * Remove datacenter
     *
     * @param vcInfo, VC connection info.
     * @param dcName, name of datacenter
     * @throws HciServerException if any internal error
     */
    public static void removeDatacenter(VcInfo vcInfo, String dcName) {
        VcManager vcManager = new VcManager(vcInfo);
        try {
            VsphereClient vcConnection = vcManager.getVsphereClient();
            ManagedObjectReference dcMor = VsphereUtil.getDatacenterMor(vcConnection, dcName);
            if (null != dcMor) {
                VsphereUtil.deleteObject(vcConnection, dcMor);
            } else {
                logger.info("Datacenter {} is not in the vCenter", dcName);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new HciServerException("Error occurs when removing datacenter."
                    + " Maybe you should remove all the hosts first."
                    + " Causion: Node 0 should be removed manually."
                    + " Please find more detail in logs");

        }
    }

    public static void hostExitMaintenanceMode(HostInfo hostInfo) {
        HostManager hostManager = new HostManager(hostInfo);
        hostManager.exitMaintenanceMode();
    }

    /**
     * Get hardware configuration
     *
     * @param hostInfo, host connection information.
     */
    public static HardwareConfiguration getHostHardwareConfiguration(HostInfo hostInfo) {
        HostManager hostManager = new HostManager(hostInfo);
        return hostManager.getHardwareConfiguration();
    }
}
