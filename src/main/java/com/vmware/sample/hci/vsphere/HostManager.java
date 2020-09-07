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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.VerificationFailedException;
import com.vmware.sample.hci.vsphere.hwconfig.HardwareConfiguration;
import com.vmware.sample.hci.vsphere.hwconfig.MemoryConfiguration;
import com.vmware.sample.hci.vsphere.hwconfig.NetworkAdapterConfiguration;
import com.vmware.sample.hci.vsphere.hwconfig.ProcessorConfiguration;
import com.vmware.sample.hci.vsphere.hwconfig.StorageConfiguration;
import com.vmware.sample.hci.vsphere.operation.Task;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.vim25.AlreadyExistsFaultMsg;
import com.vmware.vim25.HostAccountSpec;
import com.vmware.vim25.HostConfigFaultFaultMsg;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostCpuPackage;
import com.vmware.vim25.HostDateTimeConfig;
import com.vmware.vim25.HostDnsConfig;
import com.vmware.vim25.HostHardwareInfo;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostIpRouteConfig;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostNetworkPolicy;
import com.vmware.vim25.HostNtpConfig;
import com.vmware.vim25.HostPciDevice;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostScsiDisk;
import com.vmware.vim25.HostService;
import com.vmware.vim25.HostServiceInfo;
import com.vmware.vim25.HostStorageDeviceInfo;
import com.vmware.vim25.HostSystemConnectionState;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.HostVirtualSwitch;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NotFoundFaultMsg;
import com.vmware.vim25.PhysicalNic;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ScsiLun;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.UserNotFoundFaultMsg;


public class HostManager extends VsphereInventory {
    private static final Logger logger = LoggerFactory.getLogger(HostManager.class);

    private HostInfo hostInfo;
    private ManagedObjectReference hostMor;

    public HostManager(HostInfo hostInfo) {
        super(new VsphereClient(hostInfo));
        this.hostInfo = hostInfo;
    }

    public HostManager(HostInfo hostInfo, VsphereClient hostClient) {
        super(hostClient);
        this.hostInfo = hostInfo;
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    public ManagedObjectReference getHostMor() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        Map<String, ManagedObjectReference> hostMap = getMOREFs.inFolderByType(vsphereClient.getServiceContent().getRootFolder(), "HostSystem");
        String keyStr = "";
        for (Iterator<String> it = hostMap.keySet().iterator(); it.hasNext(); ) {
            keyStr = it.next();
            logger.debug("Host Mor key {}", keyStr);
            break;
        }
        this.hostMor = hostMap.get(keyStr);
        if (this.hostMor == null) {
            throw new VerificationFailedException(String.format("Host %s is unaccessible", hostInfo.getIpAddress()));
        }
        return hostMor;
    }

    public void disconnect() {
        vsphereClient.disconnect();
    }

    HostConfigManager getConfigManager() {
        HostConfigManager hostConfigManager = null;
        try {
            hostConfigManager = (HostConfigManager) getMOREFs
                    .entityProps(getHostMor(), new String[]{"configManager"}).get("configManager");
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
            logger.error("Fail to get ConfigManager", e);
        }
        return hostConfigManager;
    }

    HostConfigInfo getHostConfigInfo() {
        HostConfigInfo hostConfigInfo = null;
        try {
            hostConfigInfo = (HostConfigInfo) getMOREFs.entityProps(getHostMor(), new String[]{"config"})
                    .get("config");
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
            logger.error("Fail to get HostConfigInfo", e);
        }
        return hostConfigInfo;
    }

    public String getHostVersion() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostConfigInfo hostConfigInfo = getHostConfigInfo();
        if (hostConfigInfo == null) {
            throw new IllegalArgumentException("HostConfigInfo is null");
        }
        return hostConfigInfo.getProduct().getVersion();
    }

    public HostSystemConnectionState getHostConnectionState() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostRuntimeInfo connectionState = (HostRuntimeInfo) getMOREFs
                .entityProps(getHostMor(), new String[]{"runtime"}).get("runtime");
        if (connectionState == null) {
            throw new IllegalArgumentException("HostRuntimeInfo is null");
        }
        return connectionState.getConnectionState();
    }

    public HostDnsConfig getHostDnsConfig(ManagedObjectReference nwSystem) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostDnsConfig hostDnsConfig = (HostDnsConfig) getMOREFs
                .entityProps(nwSystem, new String[]{"dnsConfig"}).get("dnsConfig");
        return hostDnsConfig;
    }

    public List<HostVirtualNic> getHostVirtualNic() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostConfigInfo hostConfigInfo = getHostConfigInfo();
        if (hostConfigInfo == null) {
            throw new IllegalArgumentException("HostConfigInfo is null");
        }

        HostNetworkInfo networkInfo = hostConfigInfo.getNetwork();
        if (networkInfo == null) {
            throw new IllegalArgumentException("HostNetworkInfo is null");
        }

        return networkInfo.getVnic();
    }

    public List<PhysicalNic> getHostPhysicalNic() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostConfigInfo hostConfigInfo = getHostConfigInfo();
        if (hostConfigInfo == null) {
            throw new IllegalArgumentException("HostConfigInfo is null");
        }

        HostNetworkInfo networkInfo = hostConfigInfo.getNetwork();
        if (networkInfo == null) {
            throw new IllegalArgumentException("HostNetworkInfo is null");
        }

        return networkInfo.getPnic();
    }

    public List<HostVirtualSwitch> getHostVirtualSwitch() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostConfigInfo hostConfigInfo = getHostConfigInfo();
        if (hostConfigInfo == null) {
            throw new IllegalArgumentException("HostConfigInfo is null");
        }

        HostNetworkInfo networkInfo = hostConfigInfo.getNetwork();
        if (networkInfo == null) {
            throw new IllegalArgumentException("HostNetworkInfo is null");
        }

        return networkInfo.getVswitch();
    }

    public void updateDns(String[] dnsServers)
            throws InvalidStateFaultMsg, RuntimeFaultFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        if (hostConfigManager == null) {
            throw new IllegalArgumentException("HostConfigManager is null");
        }

        ManagedObjectReference nwSystem = hostConfigManager.getNetworkSystem();
        if (nwSystem == null) {
            throw new IllegalArgumentException("HostNetworkSystem if null");
        }

        if (dnsServers == null || dnsServers.length == 0) {
            throw new IllegalArgumentException("DNS server list is empty");
        }

        HostDnsConfig orgDnsConfig = null;
        try {
            orgDnsConfig = getHostDnsConfig(nwSystem);
        } catch (InvalidPropertyFaultMsg e1) {
            throw new IllegalArgumentException("Fail to get HostDnsConfig");
        }
        if (orgDnsConfig == null) {
            throw new IllegalArgumentException("HostDnsConfig if null");
        }

        String hostName = orgDnsConfig.getHostName();
        if (hostName == null) {
            throw new IllegalArgumentException("HostName if null");
        }

        String domainName = orgDnsConfig.getDomainName();
        if (domainName == null) {
            throw new IllegalArgumentException("Domain name if null");
        }

        HostDnsConfig dnsConfig = new HostDnsConfig();
        dnsConfig.setDhcp(Boolean.FALSE);
        dnsConfig.setHostName(hostName);
        dnsConfig.setDomainName(domainName);
        dnsConfig.getSearchDomain().add(domainName);
        dnsConfig.getAddress().clear();
        dnsConfig.getAddress().addAll(VsphereUtil.arrayToVector(dnsServers));

        logger.info("Update host DNS and HostName");
        try {
            vsphereClient.getVimPort().updateDnsConfig(nwSystem, dnsConfig);
        } catch (HostConfigFaultFaultMsg | NotFoundFaultMsg e) {
            logger.error("Configuration parameter is invalid");
            throw new IllegalArgumentException("Configuration parameter is invalid");
        }
        logger.info("Update host DNS and HostName successfully");
    }

    public void updateHostName(String hostName, String domainName) throws InvalidStateFaultMsg, RuntimeFaultFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        if (hostConfigManager == null) {
            throw new IllegalArgumentException("HostConfigManager is null");
        }

        ManagedObjectReference nwSystem = hostConfigManager.getNetworkSystem();
        if (nwSystem == null) {
            throw new IllegalArgumentException("HostNetworkSystem if null");
        }

        if (hostName == null || hostName.length() == 0) {
            throw new IllegalArgumentException("HostName is empty");
        }

        if (domainName == null || domainName.length() == 0) {
            throw new IllegalArgumentException("DomainName is empty");
        }

        HostDnsConfig orgDnsConfig = null;
        try {
            orgDnsConfig = getHostDnsConfig(nwSystem);
        } catch (InvalidPropertyFaultMsg e1) {
            throw new IllegalArgumentException("Fail to get HostDnsConfig");
        }
        if (orgDnsConfig == null) {
            throw new IllegalArgumentException("HostDnsConfig if null");
        }

        HostDnsConfig dnsConfig = new HostDnsConfig();
        dnsConfig.setDhcp(Boolean.FALSE);
        dnsConfig.setDomainName(domainName);
        dnsConfig.setHostName(hostName);
        dnsConfig.getSearchDomain().add(domainName);
        if ((null != orgDnsConfig.getAddress()) && (orgDnsConfig.getAddress().size() > 0)) {
            dnsConfig.getAddress().clear();
            dnsConfig.getAddress().addAll(orgDnsConfig.getAddress());
        }

        logger.info("Update host with new hostname {}, domain {}", hostName, domainName);
        try {
            vsphereClient.getVimPort().updateDnsConfig(nwSystem, dnsConfig);
        } catch (HostConfigFaultFaultMsg | NotFoundFaultMsg e) {
            logger.error("Configuration parameter is invalid");
            throw new IllegalArgumentException("Configuration parameter is invalid");
        }
        logger.info("Update host name successfully");
    }

    public void updateNtp(String[] ntpServers) throws InvalidStateFaultMsg, RuntimeFaultFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        if (hostConfigManager == null) {
            throw new IllegalArgumentException("HostConfigManager is null");
        }

        ManagedObjectReference dtSystem = hostConfigManager.getDateTimeSystem();
        if (dtSystem == null) {
            throw new IllegalArgumentException("DateTimeSystem if null");
        }

        if (ntpServers == null || ntpServers.length == 0) {
            throw new IllegalArgumentException("NTP server list is empty");
        }

        HostDateTimeConfig dateTimeConfig = new HostDateTimeConfig();
        HostNtpConfig ntpConfig = new HostNtpConfig();
        ntpConfig.getServer().clear();
        ntpConfig.getServer().addAll(VsphereUtil.arrayToVector(ntpServers));
        dateTimeConfig.setNtpConfig(ntpConfig);

        logger.info("Update host NTP server");
        try {
            vsphereClient.getVimPort().updateDateTimeConfig(dtSystem, dateTimeConfig);
        } catch (HostConfigFaultFaultMsg e) {
            logger.error("Configuration parameter is invalid");
            throw new IllegalArgumentException("Configuration parameter is invalid");
        }
        logger.info("Update hostNTP server successfully");
    }

    public void updateUserPassword(String userName, String password) throws RuntimeFaultFaultMsg {
        ManagedObjectReference accountManager = vsphereClient.getServiceContent().getAccountManager();

        if (accountManager == null) {
            throw new IllegalArgumentException("Account Manager MOR is null");
        }

        if (userName == null || userName.length() == 0) {
            throw new IllegalArgumentException("User Name is empty");
        }

        if (password == null || password.length() == 0) {
            throw new IllegalArgumentException("Password is empty");
        }

        logger.info("Update User {} with new password {}", userName, password);

        HostAccountSpec account = new HostAccountSpec();
        account.setId(userName);
        account.setPassword(password);

        try {
            vsphereClient.getVimPort().updateUser(accountManager, account);
            logger.info("Update user password successfully");
        } catch (UserNotFoundFaultMsg e) {
            logger.error("User {} not found", userName);
            throw new IllegalArgumentException(String.format("User %s not found", userName));
        } catch (AlreadyExistsFaultMsg e) {
            logger.error("Password is same as the old one");
            throw new IllegalArgumentException("Password is same as the old one");
        }
    }

    /*
     * Find a valid vswitch on which there's no portgroup named by pgName
     * existing so we can create a new portgroup on this vSwitch
     */
    public String getValidVirtualSwitch(String pgName) {
        HostConfigInfo hostConfigInfo = getHostConfigInfo();

        if (hostConfigInfo == null) {
            throw new IllegalArgumentException("HostConfigInfo is null");
        }

        if (pgName == null || pgName.length() == 0) {
            throw new IllegalArgumentException("PortGroup name is empty");
        }

        List<HostVirtualSwitch> list = hostConfigInfo.getNetwork().getVswitch();
        if (list != null) {
            for (HostVirtualSwitch vSwitch : list) {
                if (vSwitch.getPortgroup().contains(pgName) == true)
                    continue;
                return vSwitch.getName();
            }
        } else {
            throw new IllegalArgumentException("Virtual switch list is empty");
        }
        throw new IllegalArgumentException("No valid virtual switch");
    }

    public void addVirtualSwitchPortGroup(String pgName, String vswName)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        if (hostConfigManager == null) {
            throw new IllegalArgumentException("HostConfigManager is null");
        }

        ManagedObjectReference nwSystem = hostConfigManager.getNetworkSystem();
        if (nwSystem == null) {
            throw new IllegalArgumentException("HostNetworkSystem if null");
        }

        if (pgName == null || pgName.length() == 0) {
            throw new IllegalArgumentException("pgName is empty");
        }

        if (vswName == null || vswName.length() == 0) {
            throw new IllegalArgumentException("vswName is empty");
        }

        HostPortGroupSpec portGroup = new HostPortGroupSpec();
        portGroup.setName(pgName);
        portGroup.setVswitchName(vswName);
        portGroup.setPolicy(new HostNetworkPolicy());

        logger.info("Add portgroup {} onto virtual switch {}", pgName, vswName);

        try {
            vsphereClient.getVimPort().addPortGroup(nwSystem, portGroup);
            logger.info("Add port group successfully");
        } catch (NotFoundFaultMsg e) {
            logger.error("vSwitch {} not found", vswName);
            throw new IllegalArgumentException(String.format("vSwitch %s not found", vswName));
        } catch (AlreadyExistsFaultMsg e) {
            logger.error("Portgroup {} already existed", pgName);
            throw new IllegalArgumentException(String.format("Portgroup %s already existed", pgName));
        } catch (HostConfigFaultFaultMsg e) {
            logger.error("Host is invalid");
            throw new IllegalArgumentException("Host is invalid");
        }
    }

    HostVirtualNicSpec createVirtualNicSpec(String ipAddress, String mask) {
        HostIpConfig ipConfig = new HostIpConfig();
        ipConfig.setDhcp(Boolean.FALSE);
        ipConfig.setIpAddress(ipAddress);
        ipConfig.setSubnetMask(mask);
        HostVirtualNicSpec nicSpec = new HostVirtualNicSpec();
        nicSpec.setIp(ipConfig);
        return nicSpec;
    }

    public void addManagementIPAddress(String ipAddress, String mask, String pgName)
            throws InvalidStateFaultMsg, RuntimeFaultFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        if (hostConfigManager == null) {
            throw new IllegalArgumentException("HostConfigManager is null");
        }

        ManagedObjectReference nwSystem = hostConfigManager.getNetworkSystem();
        if (nwSystem == null) {
            throw new IllegalArgumentException("HostNetworkSystem is null");
        }

        if (ipAddress == null || ipAddress.length() == 0) {
            throw new IllegalArgumentException("ipAddress is empty");
        }

        if (mask == null || mask.length() == 0) {
            throw new IllegalArgumentException("mask is empty");
        }

        if (pgName == null || pgName.length() == 0) {
            throw new IllegalArgumentException("pgName is empty");
        }

        HostVirtualNicSpec nicSpec = createVirtualNicSpec(ipAddress, mask);

        logger.info("Add a management ip address {} on portgroup {}", ipAddress, pgName);

        try {
            vsphereClient.getVimPort().addVirtualNic(nwSystem, pgName, nicSpec);
            logger.info("Add management ip address successfully");
        } catch (AlreadyExistsFaultMsg e) {
            logger.error("Portgroup {} already has a ip address", pgName);
            throw new IllegalArgumentException(String.format("Portgroup %s already has a ip address", pgName));
        } catch (HostConfigFaultFaultMsg e) {
            logger.error("Host configuration parameter is invalid");
            throw new IllegalArgumentException("Host configuration parameter is invalid");
        }
    }

    public void updateDefaultGateway(String gateway)
            throws HostConfigFaultFaultMsg, InvalidStateFaultMsg,
            RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        if (StringUtils.isBlank(gateway)) {
            throw new IllegalArgumentException("gateway is empty");
        }
        HostConfigManager hostConfigManager = getConfigManager();
        ManagedObjectReference nwSystem = hostConfigManager.getNetworkSystem();
        //Get legacy settings and preserve.
        HostIpRouteConfig oldHirc = (HostIpRouteConfig) this.getMOREFs.entityProps(
                nwSystem, new String[]{"ipRouteConfig"}).get("ipRouteConfig");
        HostIpRouteConfig newHirc = new HostIpRouteConfig();
        if (null != oldHirc) {
            logger.debug("default gateway: {}", oldHirc.getDefaultGateway());
            newHirc.setDefaultGateway(gateway);
            logger.debug("default gateway device: {}", oldHirc.getGatewayDevice());
            newHirc.setGatewayDevice(oldHirc.getGatewayDevice());
            logger.debug("default IPV6 gateway: {}", oldHirc.getIpV6DefaultGateway());
            newHirc.setIpV6DefaultGateway(oldHirc.getIpV6DefaultGateway());
            logger.debug("default IPV6 gateway device: {}", oldHirc.getIpV6GatewayDevice());
            newHirc.setIpV6GatewayDevice(oldHirc.getIpV6GatewayDevice());
        } else {
            newHirc.setDefaultGateway(gateway);
        }
        vsphereClient.getVimPort().updateIpRouteConfig(nwSystem, newHirc);
    }

    /**
     * Start/stop service
     * Invoke start/stop API and then updateServicePolicy
     *
     * @param serviceKey   the key of service. Returned from HostService.
     * @param startOrStop  true for start and false for stop.
     * @param policyString the policy of service. Returned from HostService.
     */
    public void updateHostService(String serviceKey, boolean startOrStop,
                                  String policyString) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg, HostConfigFaultFaultMsg, InvalidStateFaultMsg,
            NotFoundFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        ManagedObjectReference hostServiceMor = hostConfigManager.getServiceSystem();
        if (startOrStop) {
            vsphereClient.getVimPort().startService(hostServiceMor, serviceKey);
        } else {
            vsphereClient.getVimPort().stopService(hostServiceMor, serviceKey);
        }
        vsphereClient.getVimPort().updateServicePolicy(hostServiceMor, serviceKey, policyString);
        logger.info("Update service {} to {} with policy {} successfully.",
                serviceKey, startOrStop, policyString);
    }

    /**
     * Retrieve the information of service with serviceName.
     *
     * @param serviceName the service name.
     * @return HostService the service information
     */
    public HostService getServiceInfo(String serviceName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostConfigManager hostConfigManager = getConfigManager();
        ManagedObjectReference hostServiceMor = hostConfigManager.getServiceSystem();
        HostServiceInfo hostServices = (HostServiceInfo)
                getMOREFs.entityProps(hostServiceMor, new String[]{"serviceInfo"}).get("serviceInfo");
        for (Iterator<HostService> it = hostServices.getService().iterator(); it.hasNext(); ) {
            HostService service = it.next();
            if (StringUtils.equalsIgnoreCase(serviceName, service.getKey())) {
                logger.info("Get running status of {}", serviceName);
                return service;
            }
        }
        logger.error("Cannot find running status of {}", serviceName);
        throw new HciServerException("Cannot find running status of " + serviceName);
    }

    /**
     * To run backup commands through SSH channel to backup host.
     * mkdir -p /bootbank/reset
     * /sbin/firmwareConfig.sh --backup /bootbank/reset
     *
     */
    public void runBackupCmd() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        String commands = "mkdir -p /bootbank/reset;"
                + "/sbin/firmwareConfig.sh --backup /bootbank/reset";
        if (VsphereUtil.executeSSHCommand(this.hostInfo, commands) == 0) {
            logger.info("Backup host {} successfully", this.hostInfo.getIpAddress());
        } else {
            logger.error("Failed to backup host {}", this.hostInfo.getIpAddress());
            throw new HciServerException("Failed to backup host " + this.hostInfo.getIpAddress());
        }
    }

    /**
     * To run restore commands through SSH channel to backup host.
     * vim-cmd hostsvc/maintenance_mode_enter
     * cp /bootbank/reset/*.tgz /tmp/configBundle.tgz
     * vim-cmd hostsvc/firmware/restore_config /tmp/configBundle.tgz
     *
     */
    public void runRestoreCmd() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        String commands = "vim-cmd hostsvc/maintenance_mode_enter;"
                + " cp /bootbank/reset/*.tgz /tmp/configBundle.tgz;"
                + "vim-cmd hostsvc/firmware/restore_config /tmp/configBundle.tgz";
        if (VsphereUtil.executeSSHCommand(this.hostInfo, commands) == 0) {
            logger.info("Restore host {} successfully", this.hostInfo.getIpAddress());
        } else {
            logger.error("Failed to restore host {}", this.hostInfo.getIpAddress());
            throw new HciServerException("Failed to restore host " + this.hostInfo.getIpAddress());
        }
    }

    private HostHardwareInfo getHostHardwareInfo() {
        try {
            return (HostHardwareInfo) getMOREFs.entityProps(getHostMor(), new String[]{"hardware"})
                    .get("hardware");
        } catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
            logger.error("Fail to get HostConfigInfo", e);
            throw new HciServerException("Fail to get HostConfigInfo");
        }
    }

    public HardwareConfiguration getHardwareConfiguration() {
        HardwareConfiguration hc = new HardwareConfiguration();
        HostHardwareInfo hhi = this.getHostHardwareInfo();

        //Query process information
        List<HostCpuPackage> lstCpuPkgs = hhi.getCpuPkg();
        for (HostCpuPackage hcp : lstCpuPkgs) {
            try {
                ProcessorConfiguration pc = new ProcessorConfiguration();
                pc.setDescription(hcp.getDescription());
                hc.getLstProcessCfg().add(pc);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }

        //Query memory information
        MemoryConfiguration mc = new MemoryConfiguration();
        try {
            mc.setSize(hhi.getMemorySize());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        hc.setMemCfg(mc);

        //Query Storage information
        HostConfigInfo hci = this.getHostConfigInfo();
        HostStorageDeviceInfo hsdi = hci.getStorageDevice();
        List<ScsiLun> lstScsiLun = hsdi.getScsiLun();
        for (ScsiLun scsiLun : lstScsiLun) {
            try {
                if (!(scsiLun instanceof HostScsiDisk)) {
                    logger.warn("SCSI LUN is not HostScsiDisk: {}", scsiLun.getClass().getName());
                    continue;
                }
                HostScsiDisk hsd = (HostScsiDisk) scsiLun;
                StorageConfiguration sc = new StorageConfiguration();
                sc.setBlock(hsd.getCapacity().getBlock());
                sc.setBlockSize(hsd.getCapacity().getBlockSize());
                sc.setCanonicalName(hsd.getCanonicalName());
                sc.setDeviceName(hsd.getDeviceName());
                sc.setDisplayName(hsd.getDisplayName());
                sc.setSSD(hsd.isSsd());
                hc.getLstStorageCfg().add(sc);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        //Query Network adapter information
        HostNetworkInfo hwi = hci.getNetwork();
        List<PhysicalNic> lstPhysicalNic = hwi.getPnic();
        List<HostPciDevice> lstHostPciDevice = hhi.getPciDevice();
        for (PhysicalNic nic : lstPhysicalNic) {
            try {
                NetworkAdapterConfiguration nac = new NetworkAdapterConfiguration();
                nac.setNicName(nic.getDevice());
                nac.setDriverName(nic.getDriver());
                nac.setDuplex(nic.getLinkSpeed().isDuplex());
                nac.setSpeedMb(nic.getLinkSpeed().getSpeedMb());
                String pciId = nic.getPci();
                for (HostPciDevice hpc : lstHostPciDevice) {
                    if (StringUtils.equals(hpc.getId(), pciId)) {
                        nac.setDeviceName(hpc.getDeviceName());
                        nac.setVendorName(hpc.getVendorName());
                        break;
                    }
                }
                hc.getLstNetAdapterCfg().add(nac);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return hc;
    }

    public void exitMaintenanceMode() {
        try {
            ManagedObjectReference hostMor = this.getHostMor();
            ManagedObjectReference taskMor = vsphereClient.getVimPort().exitMaintenanceModeTask(hostMor, 120000);
            Task t = new Task(this.vsphereClient);
            boolean taskSuccess = t.monitorTask(taskMor);
            if (!taskSuccess) {
                TaskInfo taskInfo = t.getTaskInfo(taskMor);
                logger.error(taskInfo.getError().getLocalizedMessage());
            }
        } catch (InvalidStateFaultMsg e) {
            logger.error(e.getMessage(), e);
            logger.error("Host {} is not in maintenance mode", this.hostInfo.getIpAddress());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new HciServerException("Failed to exit maintenance mode on " + this.hostInfo.getIpAddress());
        }
    }
}
