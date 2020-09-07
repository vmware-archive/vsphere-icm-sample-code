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
import java.util.Vector;
import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.HostInfo;
import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.TaskFailedException;
import com.vmware.sample.hci.vsphere.exception.VerificationFailedException;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.vim25.ClusterConfigSpec;
import com.vmware.vim25.ClusterConfigSpecEx;
import com.vmware.vim25.ClusterDasConfigInfo;
import com.vmware.vim25.ClusterDrsConfigInfo;
import com.vmware.vim25.ClusterFailoverLevelAdmissionControlPolicy;
import com.vmware.vim25.DrsBehavior;
import com.vmware.vim25.DuplicateNameFaultMsg;
import com.vmware.vim25.HostConnectFaultFaultMsg;
import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidNameFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SSLVerifyFault;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VsanClusterConfigInfo;
import com.vmware.vim25.VsanClusterConfigInfoHostDefaultInfo;

public class Cluster {

    protected static final int DEFAULT_FAILOVERLEVEL = 1;
    private static final Logger logger = LoggerFactory.getLogger(Cluster.class);
    private VsphereClient connection;

    public Cluster(VsphereClient vcConnection) {
        connection = vcConnection;
    }

    public static ManagedObjectReference createCluster(
            VsphereClient vcConnection, ManagedObjectReference dcMor,
            String clusterName) {

        ManagedObjectReference clusterMor = null;
        // Check the connection
        if (!vcConnection.isConnected()) {
            throw new VerificationFailedException(
                    "VC connection is not established.");
        }
        // Check the dcMor
        if (dcMor == null) {
            throw new IllegalArgumentException(
                    "Datacenter MOR is not specified.");
        }
        // Check the cluster name
        if (clusterName == null || clusterName.length() == 0) {
            throw new IllegalArgumentException("cluster Name is null or empty.");
        }

        ManagedObjectReference hostFolderMor;
        try {
            hostFolderMor = Datacenter.getHostFolder(vcConnection, dcMor);
        } catch (Exception e) {
            logger.error("Get DC's HostFolder Error: ", e);
            logger.error("Create Cluster failed.");
            throw new HciServerException("Create Cluster Failure", e);
        }

        logger.info("Start creating cluster " + clusterName + "...");

        ClusterConfigSpecEx ccs = new ClusterConfigSpecEx();

        ClusterDasConfigInfo haConfig = new ClusterDasConfigInfo();
        haConfig.setEnabled(false);
        ClusterFailoverLevelAdmissionControlPolicy failoverPolicy =
                new ClusterFailoverLevelAdmissionControlPolicy();
        failoverPolicy.setFailoverLevel(DEFAULT_FAILOVERLEVEL);
        haConfig.setAdmissionControlPolicy(failoverPolicy);
        ccs.setDasConfig(haConfig);

        ClusterDrsConfigInfo drsConfig = new ClusterDrsConfigInfo();
        drsConfig.setEnabled(false);
        drsConfig.setDefaultVmBehavior(
                DrsBehavior.fromValue(DrsBehavior.FULLY_AUTOMATED.value()));
        ccs.setDrsConfig(drsConfig);

        try {
            clusterMor = vcConnection.getVimPort()
                    .createClusterEx(hostFolderMor, clusterName, ccs);
        } catch (DuplicateNameFaultMsg | InvalidNameFaultMsg e) {
            logger.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (RuntimeFaultFaultMsg e) {

            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
        return clusterMor;
    }

    public static VsanClusterConfigInfo initializeVsanClusterConfigInfo() {
        VsanClusterConfigInfo vsanClusterConfigInfo =
                new VsanClusterConfigInfo();
        vsanClusterConfigInfo.setEnabled(true);

        VsanClusterConfigInfoHostDefaultInfo vsanHostDefaultInfo =
                new VsanClusterConfigInfoHostDefaultInfo();
        vsanHostDefaultInfo.setAutoClaimStorage(true);
        vsanHostDefaultInfo.setUuid(null);
        vsanClusterConfigInfo.setDefaultConfig(vsanHostDefaultInfo);

        return vsanClusterConfigInfo;
    }

    public ManagedObjectReference createCluster(ManagedObjectReference dcMor,
                                                String clusterName) {
        return createCluster(connection, dcMor, clusterName);
    }

    public ManagedObjectReference addHostIntoCluster(
            ManagedObjectReference dcMor, ManagedObjectReference clusterMor,
            HostInfo hostInfo) throws DuplicateNameFaultMsg,
            HostConnectFaultFaultMsg, InvalidLoginFaultMsg,
            RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        // Check the datacenter and the cluster MOR
        if (dcMor == null || clusterMor == null) {
            throw new IllegalArgumentException(
                    "Datacenter or Cluster MOR reference is null.");
        }

        HostConnectSpec hostSpec = genHostConnectSpec(dcMor, hostInfo);
        return addHost(clusterMor, hostSpec, true, null, null);
    }

    private HostConnectSpec genHostConnectSpec(ManagedObjectReference dcMor,
                                               HostInfo hostInfo) {
        HostConnectSpec hostSpec = new HostConnectSpec();

        hostSpec.setHostName(hostInfo.getHostName());
        hostSpec.setUserName(hostInfo.getUserName());
        hostSpec.setPassword(hostInfo.getPassword());
        hostSpec.setForce(true);

        // set the SSL Thumbprint, otherwise add host will fail
        try {
            connection.getVimPort().queryConnectionInfo(dcMor,
                    hostInfo.getHostName(), -1, hostInfo.getUserName(),
                    hostInfo.getPassword(), null);
        } catch (Exception e) {
            logger.error("queryConnectionInfo error: ", e);

            MethodFault methodFault = null;
            if (e instanceof SOAPFaultException) {
                methodFault =
                        VsphereUtil.getMethodFault((SOAPFaultException) e);
                if (methodFault instanceof SSLVerifyFault) {
                    hostSpec.setSslThumbprint(
                            ((SSLVerifyFault) methodFault).getThumbprint());
                    logger.info("Setup host's ssl thumbprint successfully.");
                } else {
                    throw new HciServerException(e.getMessage(), e);
                }
            } else {
                throw new HciServerException(e.getMessage(), e);
            }
        }

        return hostSpec;
    }

    private ManagedObjectReference addHost(ManagedObjectReference compResMor,
                                           HostConnectSpec hostCnxSpec, boolean asConnected,
                                           ManagedObjectReference resourcePool, String licenseKey)
            throws DuplicateNameFaultMsg, HostConnectFaultFaultMsg,
            InvalidLoginFaultMsg, RuntimeFaultFaultMsg,
            InvalidPropertyFaultMsg {

        boolean taskSuccess = false;
        ManagedObjectReference taskMor = null;

        Task t = new Task(connection);

        taskMor = connection.getVimPort().addHostTask(compResMor, hostCnxSpec,
                asConnected, resourcePool, licenseKey);
        taskSuccess = t.monitorTask(taskMor);
        TaskInfo taskInfo = t.getTaskInfo(taskMor);
        if (taskSuccess) {
            ManagedObjectReference hostMor =
                    (ManagedObjectReference) taskInfo.getResult();
            return hostMor;
        } else {
            TaskFailedException tfe = new TaskFailedException(
                    taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.ADDHOST_TASK_FAILURE, taskInfo.getName(),
                    hostCnxSpec.getHostName());
            throw tfe;
        }
    }

    public ManagedObjectReference setDRS(
            ManagedObjectReference compResMor, boolean toEnable) throws Exception {
        ClusterDrsConfigInfo cdrsci = new ClusterDrsConfigInfo();
        cdrsci.setEnabled(toEnable);
        ClusterConfigSpec ccs = new ClusterConfigSpec();
        ccs.setDrsConfig(cdrsci);
        Task t = new Task(connection);
        ManagedObjectReference taskMor = connection.getVimPort()
                .reconfigureClusterTask(compResMor, ccs, true);
        boolean taskSuccess = t.monitorTask(taskMor);
        TaskInfo taskInfo = t.getTaskInfo(taskMor);
        ManagedObjectReference clusterMor = null;
        if (taskSuccess) {
            clusterMor = (ManagedObjectReference) taskInfo.getResult();
        } else {
            TaskFailedException tfe = new TaskFailedException(
                    taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.SETDRS_TASK_FAILURE,
                    taskInfo.getName(), compResMor.getValue());
            throw tfe;
        }
        return clusterMor;
    }

    public ManagedObjectReference setDAS(
            ManagedObjectReference compResMor, boolean toEnable) throws Exception {
        ClusterDasConfigInfo cdasci = new ClusterDasConfigInfo();
        cdasci.setEnabled(toEnable);
        ClusterConfigSpec ccs = new ClusterConfigSpec();
        ccs.setDasConfig(cdasci);
        Task t = new Task(connection);
        ManagedObjectReference taskMor = connection.getVimPort()
                .reconfigureClusterTask(compResMor, ccs, true);
        boolean taskSuccess = t.monitorTask(taskMor);
        TaskInfo taskInfo = t.getTaskInfo(taskMor);
        ManagedObjectReference clusterMor = null;
        if (taskSuccess) {
            clusterMor = (ManagedObjectReference) taskInfo.getResult();
        } else {
            TaskFailedException tfe = new TaskFailedException(
                    taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.SETDAS_TASK_FAILURE,
                    taskInfo.getName(), compResMor.getValue());
            throw tfe;
        }
        return clusterMor;
    }

    public ManagedObjectReference enableVsan(ManagedObjectReference compResMor)
            throws Exception {
        ClusterConfigSpecEx ccs = new ClusterConfigSpecEx();
        VsanClusterConfigInfo vsanInfo = initializeVsanClusterConfigInfo();
        ccs.setVsanConfig(vsanInfo);

        Task t = new Task(connection);
        ManagedObjectReference taskMor = connection.getVimPort()
                .reconfigureComputeResourceTask(compResMor, ccs, true);
        boolean taskSuccess = t.monitorTask(taskMor);
        TaskInfo taskInfo = t.getTaskInfo(taskMor);
        ManagedObjectReference clusterMor = null;
        if (taskSuccess) {
            clusterMor = (ManagedObjectReference) taskInfo.getResult();
        } else {
            TaskFailedException tfe = new TaskFailedException(
                    taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.ENABLEVSAN_TASK_FAILURE,
                    taskInfo.getName(), compResMor.getValue());
            throw tfe;
        }
        return clusterMor;
    }

    /**
     * Wait for the vSAN update task to complete on multiple hosts.
     *
     * @param clusterMor Wait for vSan update task to complete on these hosts in the cluster.
     * @return boolean Returns true if vSan update succeeds on all hosts. false
     * otherwise
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     */
    public boolean waitForVsanUpdateTaskInCluster(
            ManagedObjectReference clusterMor) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        boolean success = true;
        List<ManagedObjectReference> hostList = VsphereUtil.getHostMorsInCluster(this.connection, clusterMor);
        if (hostList != null && hostList.size() == 0) {
            success = false;
            logger.error("No hosts are specified to wait for vSan update task");
        } else {
            for (ManagedObjectReference host : hostList) {
                if (!this.waitForVsanUpdateTask(host)) {
                    success = false;
                    logger.info("vSan update task failed for host {}",
                            host.getValue());
                }
            }
        }
        return success;
    }

    /**
     * Wait for the vSAN update task to complete on host.
     *
     * @param hostMor Wait for vSan update task to complete on the host.
     * @return boolean Returns true if vSan update succeeds on the host. false
     * otherwise
     */
    public boolean waitForVsanUpdateTask(ManagedObjectReference hostMor) {
        boolean success = true;
        PropertyCollector pc = new PropertyCollector(connection);
        Task t = new Task(connection);

        Vector<ManagedObjectReference> activeTaskList =
                pc.getRecentActiveTask(hostMor);
        String hostname = pc.getName(hostMor);
        ManagedObjectReference vsanUpdateTask = null;

        if (activeTaskList != null) {
            for (int i = 0; i < activeTaskList.size(); i++) {
                vsanUpdateTask = activeTaskList.get(i);
                if (vsanUpdateTask != null) {
                    logger.info(hostname + " Active task found = "
                            + vsanUpdateTask.getValue());
                    logger.info(hostname + " Active task type = "
                            + vsanUpdateTask.getType());
                    TaskInfo taskInfo = null;
                    try {
                        taskInfo = t.getTaskInfo(vsanUpdateTask);
                    } catch (Exception ex) {
                        /*
                         * Ignoring this exception as it comes from obsolete
                         * tasks intermittently
                         */
                        logger.error("Could not get TaskInfo "
                                + vsanUpdateTask.getValue(), ex);
                        continue;
                    }
                    if (taskInfo != null) {
                        logger.info("Active task name = " + taskInfo.getName());
                        logger.info("Active task description = "
                                + taskInfo.getDescriptionId() + " State = "
                                + taskInfo.getState());
                        if ("host.VsanSystem.update".equalsIgnoreCase(
                                taskInfo.getDescriptionId())) {
                            try {
                                success &= t.monitorTask(vsanUpdateTask);
                            } catch (Exception e) {
                                logger.error("monitor task error.", e);
                                success = false;
                            }
                        }
                    } else {
                        logger.info("There is no task information for task "
                                + vsanUpdateTask.getValue());
                    }
                }
            }
        } else {
            logger.info(hostname + " no active Task found.");
        }
        return success;
    }
}
