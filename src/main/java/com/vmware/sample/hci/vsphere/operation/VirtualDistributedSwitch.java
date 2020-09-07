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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.TaskFailedException;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.vim25.AlreadyExistsFaultMsg;
import com.vmware.vim25.ConcurrentAccessFaultMsg;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSConfigInfo;
import com.vmware.vim25.DVSConfigSpec;
import com.vmware.vim25.DVSCreateSpec;
import com.vmware.vim25.DistributedVirtualSwitchHostMemberConfigSpec;
import com.vmware.vim25.DistributedVirtualSwitchHostMemberPnicBacking;
import com.vmware.vim25.DistributedVirtualSwitchHostMemberPnicSpec;
import com.vmware.vim25.DistributedVirtualSwitchProductSpec;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.DvsFaultFaultMsg;
import com.vmware.vim25.DvsNotAuthorizedFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.LimitExceededFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NotFoundFaultMsg;
import com.vmware.vim25.NumericRange;
import com.vmware.vim25.ResourceInUseFaultMsg;
import com.vmware.vim25.ResourceNotAvailableFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VmwareDistributedVirtualSwitchTrunkVlanSpec;

public class VirtualDistributedSwitch {
    public static final int DVPORTGROUPNUM = 1024;
    public static final String DVPORTGROUPTYPE_EARLYBIND = "earlyBinding";
    public static final String DVS_PRODUCTSPEC_NAME = "DVS";
    public static final String DVS_PRODUCTSPEC_VENDOR = "VMware, Inc.";
    public static final String DVS_PRODUCTSPEC_VER = "6.0.0";
    public static final String DVS_OPERATION_ADD = "add";
    public static final String DVS_PROPERTY_CFGVERSION = "config.configVersion";
    public static final String DVS_PROPERTY_CONFIG = "config";
    public static final String DVS_PROPERTY_KEY = "key";
    public static final String DVS_DVPORTGROUP_MOR_TYPE =
            "DistributedVirtual" + "Portgroup";
    private static final Logger logger =
            LoggerFactory.getLogger(VirtualDistributedSwitch.class);

    public static ManagedObjectReference createVds(VsphereClient vcConnection,
                                                   final ManagedObjectReference dcMor, String vdsName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, DuplicateNameFaultMsg,
            DvsFaultFaultMsg, DvsNotAuthorizedFaultMsg, InvalidNameFaultMsg,
            NotFoundFaultMsg {
        DVSConfigSpec dvsConfigSpec = new DVSConfigSpec();
        dvsConfigSpec.setName(vdsName);
        dvsConfigSpec.setConfigVersion(DVS_PRODUCTSPEC_VER);

        DistributedVirtualSwitchProductSpec dvsProductSpec =
                new DistributedVirtualSwitchProductSpec();
        dvsProductSpec.setName(DVS_PRODUCTSPEC_NAME);
        dvsProductSpec.setVendor(DVS_PRODUCTSPEC_VENDOR);
        dvsProductSpec.setVersion(DVS_PRODUCTSPEC_VER);

        DVSCreateSpec dvsCreateSpec = new DVSCreateSpec();
        dvsCreateSpec.setProductInfo(dvsProductSpec);
        dvsCreateSpec.setConfigSpec(dvsConfigSpec);

        ManagedObjectReference ntwkFolderMor =
                Datacenter.getNetworkFolder(vcConnection, dcMor);
        Task t = new Task(vcConnection);
        ManagedObjectReference taskMor = vcConnection.getVimPort().createDVSTask(ntwkFolderMor,
                dvsCreateSpec);
        t.monitorTask(taskMor);
        TaskInfo taskInfo = t.getTaskInfo(taskMor);

        return (ManagedObjectReference) taskInfo.getResult();
    }

    public static ManagedObjectReference createDVPortGroup(
            VsphereClient vcConnection, ManagedObjectReference dvsMor,
            String pgName, String vlanIdStr) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg,
            DuplicateNameFaultMsg, DvsFaultFaultMsg, InvalidNameFaultMsg {
        DVPortgroupConfigSpec dvpgSpec = new DVPortgroupConfigSpec();
        dvpgSpec.setName(pgName);
        dvpgSpec.setNumPorts(DVPORTGROUPNUM);
        dvpgSpec.setType(DVPORTGROUPTYPE_EARLYBIND);

        if ((StringUtils.isNotBlank(vlanIdStr)) && (!StringUtils.equals(vlanIdStr, "0"))) {
            VmwareDistributedVirtualSwitchTrunkVlanSpec vmdvstvs =
                    new VmwareDistributedVirtualSwitchTrunkVlanSpec();
            vmdvstvs.setInherited(false);
            List<NumericRange> lstNR = vmdvstvs.getVlanId();
            List<NumericRange> ranges = VsphereUtil.parseVlanId(vlanIdStr);
            lstNR.addAll(ranges);
            VMwareDVSPortSetting vmdvsps = new VMwareDVSPortSetting();
            vmdvsps.setVlan(vmdvstvs);
            dvpgSpec.setDefaultPortConfig(vmdvsps);
        }

        try {
            Task t = new Task(vcConnection);
            ManagedObjectReference taskMor = vcConnection.getVimPort()
                    .createDVPortgroupTask(dvsMor, dvpgSpec);
            if (t.monitorTask(taskMor)) {
                logger.info("create DV port group succussfully.");
                ManagedObjectReference dvPortgroupMor = (ManagedObjectReference) t.getTaskInfo(taskMor)
                        .getResult();
                logger.info("DV Portgroup created: {}: {}", dvPortgroupMor.getType(), dvPortgroupMor.getValue());
                return dvPortgroupMor;
            }
        } catch (Exception e) {
            logger.info("Create DV port group failed.", e);
            throw new HciServerException("Create DV port group failed", e);
        }

        logger.info("Create DV port group failed.");
        throw new HciServerException("Create DV port group failed");
    }

    public static String getConfigVersion(VsphereClient vcConnection,
                                          final ManagedObjectReference dvsMor) {
        try {
            PropertyCollector pc = new PropertyCollector(vcConnection);
            String ver = (String) pc.getDynamicProperty(dvsMor,
                    DVS_PROPERTY_CFGVERSION);
            logger.debug("the dvs {}'s config.configversion is {}",
                    dvsMor.getValue(), ver);
            return ver;
        } catch (Exception e) {
            logger.error("Get DVS configuration version error", e);
            throw new HciServerException("Create DV port group error", e);
        }
    }

    public static boolean addHostToVds(VsphereClient vcConnection,
                                       ManagedObjectReference dcMor, ManagedObjectReference hostMor,
                                       ManagedObjectReference dvsMor, ManagedObjectReference pgMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg,
            AlreadyExistsFaultMsg, ConcurrentAccessFaultMsg, DuplicateNameFaultMsg,
            DvsFaultFaultMsg, DvsNotAuthorizedFaultMsg, InvalidNameFaultMsg,
            InvalidStateFaultMsg, LimitExceededFaultMsg, NotFoundFaultMsg,
            ResourceInUseFaultMsg, ResourceNotAvailableFaultMsg {

        DVSConfigSpec cs = new DVSConfigSpec();
        cs.setConfigVersion(getConfigVersion(vcConnection, dvsMor));

        DistributedVirtualSwitchHostMemberConfigSpec hostSpec =
                new DistributedVirtualSwitchHostMemberConfigSpec();
        hostSpec.setHost(hostMor);
        hostSpec.setOperation(DVS_OPERATION_ADD);

        DistributedVirtualSwitchHostMemberPnicBacking hostPincBacking =
                new DistributedVirtualSwitchHostMemberPnicBacking();
        HostNetwork hnw = new HostNetwork(vcConnection, hostMor,
                dvsMor, pgMor);
        // select the available pNic and uplinks pair from the host.

        List<String> pNicForUplinks = hnw.getIdlePNic(vcConnection, hostMor);
        ManagedObjectReference uplinkMor =
                getUplinkPortgroups(vcConnection, dvsMor).get(0);
        String uplinkPortgroupkey = uplinkMor.getValue();
        for (String pNicName : pNicForUplinks) {
            DistributedVirtualSwitchHostMemberPnicSpec hostPnicSpec =
                    new DistributedVirtualSwitchHostMemberPnicSpec();
            hostPnicSpec.setPnicDevice(pNicName);
            hostPnicSpec.setUplinkPortgroupKey(uplinkPortgroupkey);
            hostPincBacking.getPnicSpec().add(hostPnicSpec);
        }
        hostSpec.setBacking(hostPincBacking);
        cs.getHost().add(hostSpec);
        return vdsReconfig(vcConnection, dvsMor, cs);
    }

    public static DVSConfigInfo getDVSConfig(VsphereClient vcConnection,
                                             final ManagedObjectReference dvsMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        DVSConfigInfo configInfo = null;
        Object object = null;
        final PropertyCollector iPropertyCollector =
                new PropertyCollector(vcConnection);

        if (dvsMor != null && dvsMor.getType() != null) {
            object = iPropertyCollector.getDynamicProperty(dvsMor,
                    DVS_PROPERTY_CONFIG);
        }
        if (object != null && object instanceof DVSConfigInfo) {
            configInfo = (DVSConfigInfo) object;
        }
        return configInfo;
    }

    public static List<ManagedObjectReference> getUplinkPortgroups(
            VsphereClient vcConnection, final ManagedObjectReference dvsMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<ManagedObjectReference> uplinkPortgroups = null;
        DVSConfigInfo configInfo = null;

        if (dvsMor != null) {
            configInfo = getDVSConfig(vcConnection, dvsMor);
            if (configInfo != null && configInfo.getUplinkPortgroup() != null) {
                uplinkPortgroups = configInfo.getUplinkPortgroup();
                logger.info("DVS uplink portgroup: {}, item0 is {}",
                        uplinkPortgroups.size(), uplinkPortgroups.get(0).getValue());
            } else {
                logger.warn("The dvs config info object is null");
            }
        } else {
            logger.warn("The DVS object is null");
        }
        return uplinkPortgroups;
    }

    public static String getPortGroupKey(VsphereClient vcConnection,
                                         ManagedObjectReference dvPortgroupMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        String portgroupKey = null;
        Object object = null;
        PropertyCollector pc = new PropertyCollector(vcConnection);
        if (dvPortgroupMor != null && dvPortgroupMor.getType() != null
                && dvPortgroupMor.getType().equals(DVS_DVPORTGROUP_MOR_TYPE)) {
            object = pc.getDynamicProperty(dvPortgroupMor, DVS_PROPERTY_KEY);
            if (object != null && object.getClass().equals(String.class)) {
                portgroupKey = (String) object;
            }
            logger.info("uplink portgroup type {} value {}, key is {}",
                    dvPortgroupMor.getType(), dvPortgroupMor.getValue(),
                    portgroupKey);
        }
        return portgroupKey;
    }

    public static boolean vdsReconfig(VsphereClient vcConnection,
                                      ManagedObjectReference dvsMor, DVSConfigSpec dvsConfigSpec)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        ManagedObjectReference taskMor;
        try {
            taskMor = vcConnection.getVimPort().reconfigureDvsTask(dvsMor, dvsConfigSpec);

            Task t = new Task(vcConnection);
            if (t.monitorTask(taskMor)) {
                return true;
            }

            TaskInfo taskInfo = t.getTaskInfo(taskMor);
            TaskFailedException tfe = new TaskFailedException(taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.VDSRECONFIG_TASK_FAILURE, taskInfo.getName(), dvsMor.getValue());
            throw tfe;
        } catch (AlreadyExistsFaultMsg | ConcurrentAccessFaultMsg | DuplicateNameFaultMsg | DvsFaultFaultMsg
                | DvsNotAuthorizedFaultMsg | InvalidNameFaultMsg | InvalidStateFaultMsg | LimitExceededFaultMsg
                | NotFoundFaultMsg | ResourceInUseFaultMsg | ResourceNotAvailableFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }
}
