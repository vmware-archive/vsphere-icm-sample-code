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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.connection.helpers.GetMOREF;
import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.TaskFailedException;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ConcurrentAccessFaultMsg;
import com.vmware.vim25.DistributedVirtualSwitchHostMemberPnicBacking;
import com.vmware.vim25.DistributedVirtualSwitchHostMemberPnicSpec;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostNetworkConfig;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostProxySwitchConfig;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.HostVirtualNicConfig;
import com.vmware.vim25.HostVirtualNicManagerNicType;
import com.vmware.vim25.HostVirtualNicSpec;
import com.vmware.vim25.HostVirtualSwitch;
import com.vmware.vim25.HostVirtualSwitchBondBridge;
import com.vmware.vim25.HostVirtualSwitchConfig;
import com.vmware.vim25.InsufficientResourcesFaultFaultMsg;
import com.vmware.vim25.InvalidDatastoreFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.KeyValue;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PhysicalNic;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VmConfigFaultFaultMsg;
import com.vmware.vim25.VsanHostConfigInfo;
import com.vmware.vim25.VsanHostConfigInfoNetworkInfo;
import com.vmware.vim25.VsanHostConfigInfoNetworkInfoPortConfig;
import com.vmware.vim25.VsanHostConfigInfoStorageInfo;
import com.vmware.vim25.VsanHostDiskMapInfo;
import com.vmware.vim25.VsanHostDiskMapping;
import com.vmware.vim25.VsanHostFaultDomainInfo;
import com.vmware.vim25.VsanHostIpConfig;

public class HostNetwork {
    private static final Logger logger =
            LoggerFactory.getLogger(HostNetwork.class);
    private VsphereClient _connection;
    private ManagedObjectReference _hostMor;
    private ManagedObjectReference _dvsMor;
    private ManagedObjectReference _dvMgmtPortgroupMor;
    private ManagedObjectReference _dvVmotionPortgroupMor;
    private ManagedObjectReference _dvVsanPortgroupMor;

    public HostNetwork(VsphereClient vcConnection,
                       ManagedObjectReference hostMor, ManagedObjectReference dvsMor,
                       ManagedObjectReference[] dvpgMors) {
        _connection = vcConnection;
        _hostMor = hostMor;
        _dvsMor = dvsMor;
        _dvMgmtPortgroupMor = dvpgMors[0];
        _dvVmotionPortgroupMor = dvpgMors[1];
        _dvVsanPortgroupMor = dvpgMors[2];
    }

    public HostNetwork(VsphereClient vcConnection,
                       ManagedObjectReference hostMor, ManagedObjectReference dvsMor,
                       ManagedObjectReference dvpgMor) {
        _connection = vcConnection;
        _hostMor = hostMor;
        _dvsMor = dvsMor;
        _dvMgmtPortgroupMor = dvpgMor;
        _dvVmotionPortgroupMor = null;
        _dvVsanPortgroupMor = null;
    }

    public static Object deepCopyObject(final Object actualObject)
            throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bas = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bas);
        oos.writeObject(actualObject);
        final ByteArrayInputStream bais =
                new ByteArrayInputStream(bas.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    public void migrateVmkNicToVds()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        // generate the HostVirtualNicConfig
        // use DistributedVirtualSwitchPortConnection for vnic
        logger.info("creating the VirtualNicConfig...");
        List<HostVirtualNicConfig> vnicConfigList = buildHostVirtualNicConfig();

        HostNetworkConfig updatedNwkConfig = new HostNetworkConfig();
        updatedNwkConfig.getVnic().clear();
        updatedNwkConfig.getVnic().addAll(vnicConfigList);

        logger.info("start the migration vmknic...");
        try {
            ManagedObjectReference hostNwkmor = getNetworkSystem();
            _connection.getVimPort().updateNetworkConfig(hostNwkmor,
                    updatedNwkConfig, "modify");
            _connection.getVimPort().refreshNetworkSystem(hostNwkmor);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TaskFailedException(e.getMessage(),
                    VsphereConstants.MIGRATEVMTOVDS_TASK_FAILURE,
                    "getNetworkSystem", this._hostMor.getValue());
        }

        // generate the HostProxySwitchConfig
        // find out all the pnics used by vss
        // add them into the vds pnic backing spec
        logger.info("creating the HostProxySwitchConfig...");
        HostProxySwitchConfig dvsConfig = buildHostProxySwitchConfig();

        // generate the HostVirtualSwitchConfig
        // remove the pnics from vss spec.
        logger.info("creating the VirtualSwitchConfig...");
        HostVirtualSwitchConfig vssConfig = buildHostVirtualSwitchConfig();
        updatedNwkConfig = new HostNetworkConfig();
        updatedNwkConfig.getProxySwitch().clear();
        updatedNwkConfig.getProxySwitch().add(dvsConfig);
        updatedNwkConfig.getVswitch().clear();
        updatedNwkConfig.getVswitch().add(vssConfig);

        logger.info("start the migration pnic...");
        try {
            ManagedObjectReference hostNwkmor = getNetworkSystem();
            _connection.getVimPort().updateNetworkConfig(hostNwkmor,
                    updatedNwkConfig, "modify");
            _connection.getVimPort().refreshNetworkSystem(hostNwkmor);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TaskFailedException(e.getMessage(),
                    VsphereConstants.MIGRATEVMTOVDS_TASK_FAILURE,
                    "refreshNetworkSystem", this._hostMor.getValue());
        }
    }

    public boolean migrateVmToVds(ManagedObjectReference vmMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg,
            ConcurrentAccessFaultMsg, DuplicateNameFaultMsg, FileFaultFaultMsg,
            InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg,
            InvalidNameFaultMsg, InvalidStateFaultMsg, TaskInProgressFaultMsg,
            VmConfigFaultFaultMsg {
        if (vmMor == null) {
            logger.debug("vmMor is null, skipping the migration");
            return false;
        }
        VirtualMachineConfigSpec updatedDeltaConfigSpec = null;
        VirtualMachineConfigSpec originalDeltaConfigSpec = null;

        VirtualMachineConfigInfo vmConfigInfo = null;
        List<VirtualDevice> vds = null;
        VirtualEthernetCardDistributedVirtualPortBackingInfo backingInfo = null;
        VirtualDeviceConfigSpec updatedDeviceConfigSpec = null;
        VirtualDeviceConfigSpec originalDeviceConfigSpec = null;
        List<VirtualDeviceConfigSpec> updatedDeviceChange = new ArrayList<VirtualDeviceConfigSpec>();
        List<VirtualDeviceConfigSpec> originalDeviceChange = new ArrayList<VirtualDeviceConfigSpec>();

        vmConfigInfo = getVMConfigInfo(vmMor);
        updatedDeltaConfigSpec = new VirtualMachineConfigSpec();
        originalDeltaConfigSpec = new VirtualMachineConfigSpec();
        if (vmConfigInfo != null && vmConfigInfo.getHardware() != null) {
            vds = vmConfigInfo.getHardware().getDevice();
            for (VirtualDevice vd : vds) {
                if (vd != null && vd instanceof VirtualEthernetCard) {
                    logger.debug("found a virtual device as ethernet card: {}",
                            vd);
                    updatedDeviceConfigSpec = new VirtualDeviceConfigSpec();
                    originalDeviceConfigSpec = new VirtualDeviceConfigSpec();
                    originalDeviceConfigSpec.setOperation(
                            VirtualDeviceConfigSpecOperation.EDIT);

                    originalDeviceConfigSpec.setDevice(vd);

                    originalDeviceChange.add(originalDeviceConfigSpec);
                    backingInfo =
                            new VirtualEthernetCardDistributedVirtualPortBackingInfo();
                    DistributedVirtualSwitchPortConnection portConn =
                            new DistributedVirtualSwitchPortConnection();
                    portConn.setSwitchUuid(
                            getHostProxySwitchConfig().getUuid());
                    portConn.setPortgroupKey(VirtualDistributedSwitch
                            .getPortGroupKey(_connection, _dvMgmtPortgroupMor));

                    backingInfo.setPort(portConn);
                    vd.setBacking(backingInfo);
                    if (vd.getConnectable() != null) {
                        vd.getConnectable().setStartConnected(true);
                    }
                    updatedDeviceConfigSpec.setOperation(
                            VirtualDeviceConfigSpecOperation.EDIT);
                    updatedDeviceConfigSpec.setDevice(vd);
                    updatedDeviceChange.add(updatedDeviceConfigSpec);
                }
            }
            updatedDeltaConfigSpec.getDeviceChange().clear();
            updatedDeltaConfigSpec.getDeviceChange().addAll(updatedDeviceChange);
        }
        /*
         * Reconfigure the virtual machine with the new settings
         */
        Task t = new Task(_connection);
        ManagedObjectReference taskMor = _connection.getVimPort().reconfigVMTask(vmMor, updatedDeltaConfigSpec);
        boolean taskSuccess = t.monitorTask(taskMor);
        if (taskSuccess) {
            return taskSuccess;
        } else {
            TaskInfo taskInfo = t.getTaskInfo(taskMor);
            TaskFailedException tfe = new TaskFailedException(
                    taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.MIGRATEVMTOVDS_TASK_FAILURE,
                    taskInfo.getName(), vmMor.getValue());
            throw tfe;
        }
    }

    public ManagedObjectReference[] getVM()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        final PropertyCollector mPropCollector =
                new PropertyCollector(_connection);
        Object obj = mPropCollector.getDynamicProperty(_hostMor, "vm");

        ManagedObjectReference[] vmMor = null;
        if ((obj != null) && (obj instanceof ArrayOfManagedObjectReference)) {
            ArrayOfManagedObjectReference aom =
                    (ArrayOfManagedObjectReference) obj;
            if (aom != null) {
                vmMor = aom.getManagedObjectReference()
                        .toArray(new ManagedObjectReference[aom
                                .getManagedObjectReference().size()]);
            }
        } else {
            vmMor = (ManagedObjectReference[]) obj;
        }

        return vmMor;
    }

    public VirtualMachineConfigInfo getVMConfigInfo(
            final ManagedObjectReference vmMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        final PropertyCollector mPropCollector =
                new PropertyCollector(_connection);
        final VirtualMachineConfigInfo vmConfigInfo =
                (VirtualMachineConfigInfo) mPropCollector
                        .getDynamicProperty(vmMor, "config");
        return vmConfigInfo;
    }

    private ManagedObjectReference getNetworkSystem()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        PropertyCollector pc = new PropertyCollector(_connection);
        HostConfigManager cfgMgr = (HostConfigManager) pc
                .getDynamicProperty(_hostMor, "configManager");
        return cfgMgr.getNetworkSystem();
    }

    private HostNetworkInfo getHostNetworkInfo() {
        PropertyCollector pc = new PropertyCollector(_connection);
        HostConfigInfo hostConfigInfo = null;
        try {
            hostConfigInfo =
                    (HostConfigInfo) pc.getDynamicProperty(_hostMor, "config");
            return hostConfigInfo.getNetwork();
        } catch (Exception e) {

            throw new HciServerException("Could not get HostNetworkInfo", e);
        }
    }

    private List<HostVirtualNicConfig> buildHostVirtualNicConfig()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        DistributedVirtualSwitchPortConnection dvsPortConnection =
                new DistributedVirtualSwitchPortConnection();
        dvsPortConnection.setPortgroupKey(VirtualDistributedSwitch
                .getPortGroupKey(_connection, _dvMgmtPortgroupMor));
        dvsPortConnection.setPortKey(null);
        dvsPortConnection.setSwitchUuid(getHostProxySwitchConfig().getUuid());

        HostVirtualNicSpec updatedVnicSpec = new HostVirtualNicSpec();
        updatedVnicSpec.setDistributedVirtualPort(dvsPortConnection);
        updatedVnicSpec.setPortgroup(null);

        List<HostVirtualNicConfig> vNicConfigList =
                new ArrayList<HostVirtualNicConfig>();

        List<HostVirtualNic> vnicListOnVss = getHostNetworkInfo().getVnic();

        for (HostVirtualNic vnic : vnicListOnVss) {
            HostVirtualNicConfig vNicConfig = new HostVirtualNicConfig();
            logger.debug("adding {} to the migration list", vnic.getDevice());
            vNicConfig.setSpec(updatedVnicSpec);
            vNicConfig.setDevice(vnic.getDevice());
            vNicConfig.setPortgroup("");
            vNicConfig.setChangeOperation("edit");
            vNicConfigList.add(vNicConfig);
        }
        return vNicConfigList;
    }

    private HostVirtualSwitchConfig buildHostVirtualSwitchConfig() {
        HostVirtualSwitch vSwitch = getHostNetworkInfo().getVswitch().get(0);
        vSwitch.getSpec().getPolicy().getNicTeaming().getNicOrder()
                .getStandbyNic().clear();
        vSwitch.getSpec().getPolicy().getNicTeaming().getNicOrder()
                .getStandbyNic().clear();
        vSwitch.getSpec().getPolicy().getNicTeaming().getNicOrder()
                .getActiveNic().clear();
        vSwitch.getSpec().setBridge(null);

        HostVirtualSwitchConfig hvs = new HostVirtualSwitchConfig();
        hvs.setChangeOperation("edit");
        hvs.setName(vSwitch.getName());
        hvs.setSpec(vSwitch.getSpec());
        return hvs;
    }

    private List<DistributedVirtualSwitchHostMemberPnicSpec> buildPnicSpec()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        // pnic in VSS and in Idle
        // mapping to free uplinkPortKey
        List<DistributedVirtualSwitchHostMemberPnicSpec> specList;
        specList = new ArrayList<>();
        List<String> vssPnicList = getPnicFromVSS();
        List<String> uplinkPortList = getIdleVdsUplinkPortKey();

        int loopNum = uplinkPortList.size();
        if (vssPnicList.size() < uplinkPortList.size()) {
            loopNum = vssPnicList.size();
        }
        for (int i = 0; i < loopNum; i++) {
            DistributedVirtualSwitchHostMemberPnicSpec spec =
                    new DistributedVirtualSwitchHostMemberPnicSpec();
            spec.setUplinkPortKey(uplinkPortList.get(i));
            spec.setPnicDevice(vssPnicList.get(i));
            logger.debug("adding pnic {} into uplink {}", vssPnicList.get(i),
                    uplinkPortList.get(i));
            specList.add(spec);
        }
        return specList;
    }

    private List<PhysicalNic> getAllPnic() {
        HostNetworkInfo info = getHostNetworkInfo();
        // the string likes this: key-vim.host.PhysicalNic-vmnic0
        return info.getPnic();
    }

    private List<String> getPnicFromVSS() {
        HostNetworkInfo info = getHostNetworkInfo();
        HostVirtualSwitch vss = info.getVswitch().get(0);
        return ((HostVirtualSwitchBondBridge) vss.getSpec().getBridge())
                .getNicDevice();
    }

    private List<String> getIdleVdsUplinkPortKey()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostProxySwitchConfig nwk = getHostProxySwitchConfig();

        DistributedVirtualSwitchHostMemberPnicBacking pnicBacking = null;
        pnicBacking = (DistributedVirtualSwitchHostMemberPnicBacking) nwk
                .getSpec().getBacking();

        List<DistributedVirtualSwitchHostMemberPnicSpec> pnicSpecList;
        pnicSpecList = pnicBacking.getPnicSpec();

        HostNetworkInfo hostNwk = getHostNetworkInfo();

        for (DistributedVirtualSwitchHostMemberPnicSpec pnic : pnicSpecList) {
            logger.debug("pnic {} is banding to uplinkport {}",
                    pnic.getPnicDevice(), pnic.getUplinkPortKey());
        }
        ArrayList<String> idlePortList = new ArrayList<String>();
        List<KeyValue> uplinkList =
                hostNwk.getProxySwitch().get(0).getUplinkPort();
        for (KeyValue uplink : uplinkList) {
            logger.debug("checking if uplink {} is idle ...", uplink.getKey());
            boolean isIdle = true;
            for (DistributedVirtualSwitchHostMemberPnicSpec pnic : pnicSpecList) {
                if (uplink.getKey().equals(pnic.getUplinkPortKey())) {
                    logger.debug("uplink {} is used by {}", uplink.getKey(),
                            pnic.getPnicDevice());
                    isIdle = false;
                    break;
                }
            }
            if (isIdle) {
                logger.debug("uplink {} is idle", uplink.getKey());
                idlePortList.add(uplink.getKey());
            }
        }
        return idlePortList;
    }

    private HostProxySwitchConfig buildHostProxySwitchConfig()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        HostProxySwitchConfig original = null;
        HostProxySwitchConfig update = null;
        original = getHostProxySwitchConfig();
        update = original;
        update.setChangeOperation("edit");

        // add the new pnic based to the current backing list.
        DistributedVirtualSwitchHostMemberPnicBacking pnicBacking
                = (DistributedVirtualSwitchHostMemberPnicBacking) update.getSpec().getBacking();

        pnicBacking.getPnicSpec().addAll(buildPnicSpec());
        update.getSpec().setBacking(pnicBacking);
        return update;
    }

    private HostProxySwitchConfig getHostProxySwitchConfig()
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ManagedObjectReference nwkMor = getNetworkSystem();

        PropertyCollector pc = new PropertyCollector(_connection);
        HostNetworkConfig hostNetworkConfig = (HostNetworkConfig) pc
                .getDynamicProperty(nwkMor, "networkConfig");
        // for now, we only have one dvs, need to enhance this in future.
        return hostNetworkConfig.getProxySwitch().get(0);
    }

    public List<String> getIdlePNic(VsphereClient vcConnection,
                                    ManagedObjectReference hostMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        GetMOREF getMOREFs = new GetMOREF(vcConnection.getVimPort(),
                vcConnection.getServiceContent());

        HostConfigManager configManager = (HostConfigManager) getMOREFs
                .entityProps(hostMor,
                        new String[]{
                                VsphereConstants.HOST_CONFIGMANAGER_PROPERTYNAME})
                .get(VsphereConstants.HOST_CONFIGMANAGER_PROPERTYNAME);
        ManagedObjectReference networkMor = configManager.getNetworkSystem();
        HostNetworkInfo networkInfo =
                (HostNetworkInfo) getMOREFs
                        .entityProps(networkMor,
                                new String[]{
                                        VsphereConstants.NWSYSTEM_NWINFO})
                        .get(VsphereConstants.NWSYSTEM_NWINFO);

        // remove the pNics that are connected to standard vSwitch
        List<HostVirtualSwitch> vSwitchList = networkInfo.getVswitch();
        List<String> pNicOnvSwitchKeyList = new ArrayList<String>();
        for (HostVirtualSwitch vSwitch : vSwitchList) {
            logger.debug(vSwitch.getName());
            pNicOnvSwitchKeyList.addAll(vSwitch.getPnic());
        }

        logger.debug(pNicOnvSwitchKeyList.toString());

        List<PhysicalNic> pNicList = networkInfo.getPnic();
        String nicKey = "";
        List<String> nicNameList = new ArrayList<String>();
        for (PhysicalNic pnic : pNicList) {
            nicKey = pnic.getKey();
            logger.debug(nicKey);
            if (pNicOnvSwitchKeyList.contains(nicKey)) {
                continue;
            } else {
                nicNameList.add(pnic.getDevice());
            }
        }
        if (nicNameList.size() > 0) {
            // get available uplink port that not bound to a pNic
            logger.info(
                    "Get physical nic {} already on vSwitch and remove from available list");
            if (networkInfo.getProxySwitch().size() > 0) {
                // if there is a proxySwitch, the host was added to a dvs
                // should removed the pNic in use from the available list
                DistributedVirtualSwitchHostMemberPnicBacking pnicBacking =
                        (DistributedVirtualSwitchHostMemberPnicBacking) networkInfo
                                .getProxySwitch().get(0).getSpec().getBacking();
                List<DistributedVirtualSwitchHostMemberPnicSpec> dvsmpsList =
                        pnicBacking.getPnicSpec();
                for (DistributedVirtualSwitchHostMemberPnicSpec dvsmps : dvsmpsList) {
                    nicNameList.remove(dvsmps.getPnicDevice());
                }
            } else {
                logger.info("No proxySwitch at present");
            }
        }
        if (nicNameList.size() > 0) {
            logger.debug("Get available pnic {}", nicNameList);
            return nicNameList;
        } else {
            InvalidProperty ip = new InvalidProperty();
            ip.setName("No idle physical found");
            logger.error("No idle physical found", ip);
            InvalidPropertyFaultMsg ex =
                    new InvalidPropertyFaultMsg(ip.getName(), ip);
            throw ex;
        }
    }

    public boolean addVirtualNIC(boolean enableDHCP, String sIpAddress, String sSubnetMask,
                                 HostVirtualNicManagerNicType vnicType) {

        HostVirtualNicSpec vNicSpec = new HostVirtualNicSpec();
        HostIpConfig hostipconfig = new HostIpConfig();
        if (enableDHCP) {
            hostipconfig.setDhcp(Boolean.TRUE);
        } else {
            hostipconfig.setDhcp(Boolean.FALSE);
            hostipconfig.setIpAddress(sIpAddress);
            hostipconfig.setSubnetMask(sSubnetMask);
        }
        vNicSpec.setIp(hostipconfig);

        ManagedObjectReference nwSystem;
        try {
            nwSystem = getNetworkSystem();
            DistributedVirtualSwitchPortConnection dvsPortConnect =
                    new DistributedVirtualSwitchPortConnection();
            if (vnicType.equals(HostVirtualNicManagerNicType.VSAN)) {
                logger.info("VSAN portgroup: {}", this._dvVsanPortgroupMor);
                dvsPortConnect.setPortgroupKey(VirtualDistributedSwitch
                        .getPortGroupKey(_connection, this._dvVsanPortgroupMor));

            } else if (vnicType.equals(HostVirtualNicManagerNicType.VMOTION)) {
                logger.info("vMOTION portgroup: {}", this._dvVmotionPortgroupMor);
                dvsPortConnect.setPortgroupKey(VirtualDistributedSwitch
                        .getPortGroupKey(_connection, this._dvVmotionPortgroupMor));
            } else {
                logger.info("Mgmt portgroup: {}", this._dvMgmtPortgroupMor);
                dvsPortConnect.setPortgroupKey(VirtualDistributedSwitch
                        .getPortGroupKey(_connection, this._dvMgmtPortgroupMor));
            }
            dvsPortConnect.setSwitchUuid(getHostProxySwitchConfig().getUuid());
            vNicSpec.setDistributedVirtualPort(dvsPortConnect);
            String vnicName = _connection.getVimPort().addVirtualNic(nwSystem,
                    "", vNicSpec);
            selectTrafficOnVnic(vnicName, vnicType);
            return true;
        } catch (Exception e) {

            logger.error("create vNIC error:", e);
        }
        return false;
    }

    private void selectVsanOnVnic(String nicName) {
        final String downstreamIpAddress = "224.2.3.4";
        final String upstreamIpAddress = "224.1.2.3";

        try {
            PropertyCollector pc = new PropertyCollector(_connection);
            HostConfigManager cfgMgr = (HostConfigManager) pc
                    .getDynamicProperty(_hostMor, "configManager");
            // Should use VsanSystem.update to modify vsan settings.
            ManagedObjectReference vSanSystemMor = cfgMgr.getVsanSystem();
            VsanHostConfigInfo oldConfig =
                    (VsanHostConfigInfo) pc.getDynamicProperty(vSanSystemMor,
                            VsphereConstants.VSAN_HOST_CONFIGINFO_PROPERTYNAME);

            VsanHostIpConfig vsanHostIpCfg = new VsanHostIpConfig();
            vsanHostIpCfg.setDownstreamIpAddress(downstreamIpAddress);
            vsanHostIpCfg.setUpstreamIpAddress(upstreamIpAddress);

            VsanHostConfigInfoNetworkInfoPortConfig vhcinipc =
                    new VsanHostConfigInfoNetworkInfoPortConfig();
            vhcinipc.setDevice(nicName);
            vhcinipc.setIpConfig(vsanHostIpCfg);

            VsanHostConfigInfoNetworkInfo vhcini =
                    new VsanHostConfigInfoNetworkInfo();
            vhcini.getPort().add(vhcinipc);

            List<VsanHostConfigInfoNetworkInfoPortConfig> oldPortList =
                    oldConfig.getNetworkInfo().getPort();
            if ((null != oldPortList) && (oldPortList.size()) > 0) {
                vhcini.getPort().addAll(oldPortList);
            }

            VsanHostConfigInfo newConfig = new VsanHostConfigInfo();
            newConfig.setNetworkInfo(vhcini);
            newConfig.setEnabled(oldConfig.isEnabled());
            newConfig.setHostSystem(oldConfig.getHostSystem());

            VsanHostFaultDomainInfo vhfdi = new VsanHostFaultDomainInfo();
            vhfdi.setName(oldConfig.getFaultDomainInfo().getName());
            newConfig.setFaultDomainInfo(vhfdi);

            VsanHostConfigInfoStorageInfo vhcifi =
                    new VsanHostConfigInfoStorageInfo();
            vhcifi.setAutoClaimStorage(
                    oldConfig.getStorageInfo().isAutoClaimStorage());
            vhcifi.setChecksumEnabled(
                    oldConfig.getStorageInfo().isChecksumEnabled());
            List<VsanHostDiskMapInfo> oldMapInfo =
                    new ArrayList<VsanHostDiskMapInfo>();
            if ((null != oldMapInfo) && (oldMapInfo.size() > 0)) {
                vhcifi.getDiskMapInfo().addAll(oldMapInfo);
            }
            List<VsanHostDiskMapping> oldMapping =
                    new ArrayList<VsanHostDiskMapping>();
            if ((null != oldMapping) && (oldMapping.size() > 0)) {
                vhcifi.getDiskMapping().addAll(oldMapping);
            }
            newConfig.setStorageInfo(vhcifi);
            /*
             * Do NOT set VsanHostConfigInfoClusterInfo It is read-only If set,
             * the vsan will not be selected.
             */
            ManagedObjectReference taskMor = _connection.getVimPort()
                    .updateVsanTask(vSanSystemMor, newConfig);
            Task t = new Task(_connection);
            if (t.monitorTask(taskMor)) {
                logger.debug(
                        "create vNIC for vSAN traffic successfully on host {}",
                        _hostMor.getValue());
            } else {
                TaskInfo taskInfo = t.getTaskInfo(taskMor);
                logger.debug("create vNIC for vSAN traffic failed on host {}",
                        _hostMor.getValue());
                logger.debug("error message: {}",
                        taskInfo.getError().getLocalizedMessage());
            }
        } catch (Exception e) {
            logger.error("select vSAN traffic error.", e);
            throw new HciServerException("select vSAN traffic error on vNic", e);
        }

    }

    private void selectTrafficOnVnic(String nicName,
                                     HostVirtualNicManagerNicType vnicType) {
        logger.debug("trying to select traffic {} on vNic {}",
                vnicType.toString(), nicName);
        if (vnicType.equals(HostVirtualNicManagerNicType.VSAN)) {
            selectVsanOnVnic(nicName);
            return;
        } else {
            try {
                PropertyCollector pc = new PropertyCollector(_connection);
                HostConfigManager cfgMgr = (HostConfigManager) pc
                        .getDynamicProperty(_hostMor, "configManager");
                ManagedObjectReference vNicMgrMor =
                        cfgMgr.getVirtualNicManager();
                _connection.getVimPort().selectVnicForNicType(vNicMgrMor,
                        vnicType.value(), nicName);
            } catch (Exception e) {
                logger.error("select traffic error on vNic", e);
                throw new HciServerException("select traffic error on vNic", e);
            }
        }
        logger.debug("completed to select traffic {} on vNic {}",
                vnicType.toString(), nicName);
    }
}
