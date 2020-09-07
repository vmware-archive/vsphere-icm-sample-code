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

package com.vmware.sample.hci.vsphere.utils;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.vmware.sample.hci.connection.helpers.GetMOREF;
import com.vmware.sample.hci.vsphere.HostInfo;
import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.exception.TaskFailedException;
import com.vmware.sample.hci.vsphere.operation.Task;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.NumericRange;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VimFaultFaultMsg;

public class VsphereUtil {

    private static final Logger logger = LoggerFactory.getLogger(VsphereUtil.class);
    public static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext
                    .newInstance(com.vmware.vim25.SSLVerifyFault.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static <T> Vector<T> arrayToVector(final T[] arrayArg) {
        Vector<T> v = new Vector<T>();
        if (arrayArg != null) {
            v = new Vector<T>();
            for (int i = 0; i < arrayArg.length; i++) {
                v.add(arrayArg[i]);
            }
        } else {
            logger.error("Input Array is null");
        }
        return v;
    }

    /**
     * Retrieve the datacenter Mor from datacenter name.
     *
     * @param vcClient host connection.
     * @param dcName   vds Name.
     * @return datacenter mor
     * null
     * Name of datacenter is unique in a VC instance.
     * datacenter should be found in root folder.
     */
    public static ManagedObjectReference getDatacenterMor(VsphereClient vcClient,
                                                          String dcName) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        logger.debug("getDatacenterMor, {}", dcName);
        ManagedObjectReference dcMor = null;
        GetMOREF getMOREFs = new GetMOREF(vcClient.getVimPort(), vcClient.getServiceContent());
        Map<String, ManagedObjectReference> dcResult =
                getMOREFs.inFolderByType(vcClient.getServiceContent().getRootFolder(),
                        VsphereConstants.DC_MOR_TYPE);
        dcMor = dcResult.get(dcName);
        if (null != dcMor) {
            logger.info("Got datacenter MOR with name {} . Type: {}, Value {}",
                    dcName, dcMor.getType(), dcMor.getValue());
        } else {
            logger.info("Could not find datacenter MOR with name {}", dcName);
        }
        return dcMor;
    }


    /**
     * Retrieve the cluster mor from a datacenter.
     *
     * @param vcClient    host connection.
     * @param dcName      datacenter name.
     * @param clusterName cluster name.
     * @return cluster mor
     * null
     * Name of cluster is unique in one datacenter.
     * Name of cluster could be duplicated in different datacenters.
     */
    public static ManagedObjectReference getClusterMor(VsphereClient vcClient,
                                                       String dcName, String clusterName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        logger.debug("getClusterMor, {}, {}", dcName, clusterName);
        ManagedObjectReference dcMor = VsphereUtil.getDatacenterMor(vcClient, dcName);
        if (null != dcMor) {
            return getClusterMor(vcClient, dcMor, clusterName);
        } else {
            logger.error("Could not find datacenter mor with name {}", dcName);
            return null;
        }
    }

    /**
     * Retrieve the host mor from the whole VC.
     *
     * @param vcClient host connection.
     * @param hostName host name.
     * @return host mor
     * null
     * Name of host is unique in a VC instance.
     * Host could be found either in root folder or in datacenter folder.
     */
    public static ManagedObjectReference getHostMor(VsphereClient vcClient,
                                                    String hostName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        logger.debug("getHostMor by hostname, {}", hostName);
        String hn = hostName.toLowerCase();
        GetMOREF getMOREFs = new GetMOREF(vcClient.getVimPort(), vcClient.getServiceContent());
        Map<String, ManagedObjectReference> hostMap = getMOREFs.inFolderByType(vcClient.getServiceContent().getRootFolder(),
                VsphereConstants.HOST_MOR_TYPE);

        ManagedObjectReference hostMor = hostMap.get(hn);
        if (null != hostMor) {
            logger.debug("Got host MOR with name {} on VC. Type: {}, Value {}",
                    hostName, hostMor.getType(), hostMor.getValue());
        } else {
            logger.info("Could not find host MOR with name {} on VC",
                    hostName);
        }
        return hostMor;
    }

    /**
     * Retrieve all the host mors from a cluster .
     *
     * @param vcClient   host connection.
     * @param clusterMor cluster object reference
     * @return host mor list
     * null
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     */
    public static List<ManagedObjectReference> getHostMorsInCluster(VsphereClient vcClient,
                                                                    ManagedObjectReference clusterMor) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        logger.debug("getHostMorsInCluster, {}", clusterMor.getValue());
        GetMOREF getMOREFs = new GetMOREF(vcClient.getVimPort(), vcClient.getServiceContent());
        Map<String, ManagedObjectReference> hostMap = getMOREFs.inFolderByType(clusterMor,
                VsphereConstants.HOST_MOR_TYPE);

        List<ManagedObjectReference> lstHostMor = new ArrayList<ManagedObjectReference>();
        for (ManagedObjectReference mor : hostMap.values()) {
            lstHostMor.add(mor);
        }
        logger.debug("Get {} hosts in cluster", lstHostMor.size(), clusterMor.getValue());
        return lstHostMor;
    }

    /**
     * Retrieve the cluster mor from a datacenter.
     *
     * @param vcClient    host connection.
     * @param dcMor       datacenter mor.
     * @param clusterName cluster name.
     * @return cluster mor
     * null
     * Name of cluster is unique in one datacenter.
     * Name of cluster could be duplicated in different datacenters.
     */
    public static ManagedObjectReference getClusterMor(VsphereClient vcClient,
                                                       ManagedObjectReference dcMor, String clusterName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        if (null != dcMor) {
            logger.debug("getClusterMor by dcMor, {}, {}", dcMor.getValue(), clusterName);
            GetMOREF getMOREFs = new GetMOREF(vcClient.getVimPort(), vcClient.getServiceContent());
            Map<String, ManagedObjectReference> clusterResult = getMOREFs.inFolderByType(dcMor,
                    VsphereConstants.CLUSTER_COMPRES_MOR_TYPE);
            ManagedObjectReference clusterMor = clusterResult.get(clusterName);
            if (null != clusterMor) {
                logger.debug("Got cluster MOR with name {} on datacenter {}. Type: {}, Value {}",
                        clusterName, dcMor.getValue(), clusterMor.getType(), clusterMor.getValue());
            } else {
                logger.info("Could not find cluster MOR with name {} on datacenter {}",
                        clusterName, dcMor.getValue());
            }
            return clusterMor;
        } else {
            logger.error("Datacenter mor should not be null");
            return null;
        }
    }

    /**
     * Retrieve the portgroup mor from dc mor and portgroup name.
     *
     * @param vcClient host connection.
     * @param vdsName  vds name.
     * @return vds mor
     * null
     * Name of vds is unique in a VC instance.
     * vds should be found in root folder.
     */
    public static ManagedObjectReference getVdsMor(VsphereClient vcClient,
                                                   String vdsName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        logger.debug("getVdsMor, {}", vdsName);
        ManagedObjectReference vdsMor = null;
        GetMOREF getMOREFs = new GetMOREF(vcClient.getVimPort(), vcClient.getServiceContent());
        Map<String, ManagedObjectReference> vdsMap =
                getMOREFs.inFolderByType(vcClient.getServiceContent().getRootFolder(),
                        VsphereConstants.DVS_MOR_TYPE);
        vdsMor = vdsMap.get(vdsName);
        if (null != vdsMor) {
            logger.info("Got vds MOR with name {}. Type: {}, Value {}",
                    vdsName, vdsMor.getType(), vdsMor.getValue());
        } else {
            logger.info("Could not find vds MOR with name {}", vdsName);
        }
        return vdsMor;
    }

    /**
     * Retrieve the portgroup mor from dc mor and portgroup name.
     *
     * @param vcClient      host connection.
     * @param dcName        datacenter name.
     * @param portgroupName portgroup name.
     * @return portgroup mor
     * null
     * Name of portgroup is unique in one datacenter.
     * Name of portgroup could be duplicated in different datacenters.
     * portgroup should be found in datacenter folder. No need to provide vds info.
     */
    public static ManagedObjectReference getPorggroupMor(VsphereClient vcClient,
                                                         String dcName, String portgroupName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        logger.debug("getPorggroupMor, {}, {}, {}", dcName, portgroupName);
        ManagedObjectReference dcMor = VsphereUtil.getDatacenterMor(vcClient, dcName);
        if (null != dcMor) {
            return getPorggroupMor(vcClient, dcMor, portgroupName);
        } else {
            logger.error("Vds mor should not be null");
            return null;
        }
    }

    /**
     * Retrieve the portgroup mor from dc mor and portgroup name.
     *
     * @param vcClient      host connection.
     * @param dcMor         datacenter mor.
     * @param portgroupName portgroup name.
     * @return portgroup mor
     * null
     * Name of portgroup is unique in one datacenter.
     * Name of portgroup could be duplicated in different datacenters.
     */
    public static ManagedObjectReference getPorggroupMor(VsphereClient vcClient,
                                                         ManagedObjectReference dcMor, String portgroupName) throws
            InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        if (null != dcMor) {
            logger.debug("getPorggroupMor by dcMor, {}, {}", dcMor.getValue(), portgroupName);
            GetMOREF getMOREFs = new GetMOREF(vcClient.getVimPort(), vcClient.getServiceContent());
            Map<String, ManagedObjectReference> portgroupMap = getMOREFs.inFolderByType(dcMor,
                    VsphereConstants.DV_PORT_GROUP_MOR_TYPE);
            ManagedObjectReference pgMor = portgroupMap.get(portgroupName);
            if (null != pgMor) {
                logger.debug("Got portgroup MOR with name {} on datacenter {}. Type: {}, Value {}",
                        portgroupName, dcMor.getValue(), pgMor.getType(), pgMor.getValue());
            } else {
                logger.info("Could not find portgroup MOR with name {} on vds {}",
                        portgroupName, dcMor.getValue());
            }
            return pgMor;
        } else {
            logger.error("Vds mor should not be null");
            return null;
        }
    }

    public static MethodFault getMethodFault(SOAPFaultException excep) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement xx = (JAXBElement) unmarshaller
                    .unmarshal(excep.getFault().getDetail().getFirstChild());
            Object obj = xx.getValue();
            MethodFault result = null;
            if (obj instanceof MethodFault) {
                result = (MethodFault) obj;
            } else {
                logger.warn("No MethodFault Found in SOAPFaultException");
            }
            return result;
        } catch (JAXBException e) {
            logger.error(e.getMessage(), e);
            throw new HciServerException(e.getMessage(), e);
        }
    }

    /**
     * Execute SSH command through exec channel on ESXi host.
     *
     * @param host     the ESXi host information.
     * @param commands the command to run. ";" as separator between commands.
     * @return The result of execution. if successful, 0 returned.
     */
    public static int executeSSHCommand(HostInfo host, String commands) {
        logger.info("Running commands {}", commands);
        int retVal = -1;
        String user = host.getUserName();
        String password = host.getPassword();
        String hostIp = host.getIpAddress();
        int port = 22;
        Session session = null;
        Channel channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, hostIp, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing connection...");
            session.connect();
            logger.info("Connection established.");
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(commands);

            channel.setInputStream(null);
            InputStream in = channel.getInputStream();
            InputStream err = ((ChannelExec) channel).getErrStream();
            channel.connect();

            Thread inthread = new InputStreamToLog(in, VsphereConstants.LOG_INFO);
            inthread.start();
            inthread.join();

            Thread errthread = new InputStreamToLog(err, VsphereConstants.LOG_ERROR);
            errthread.start();
            errthread.join();

            retVal = channel.getExitStatus();

            channel.disconnect();
            session.disconnect();

        } catch (Exception ex) {
            logger.error("error when executing ssh commands", ex);
        } finally {
            if (channel.isConnected()) {
                try {
                    channel.disconnect();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                } finally {
                    channel = null;
                }
            }
            if (session.isConnected()) {
                try {
                    session.disconnect();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                } finally {
                    session = null;
                }
            }
        }
        return retVal;
    }

    public static void deleteObject(VsphereClient vcConnection, ManagedObjectReference mor)
            throws RuntimeFaultFaultMsg, VimFaultFaultMsg, InvalidPropertyFaultMsg {

        ManagedObjectReference taskMor = vcConnection.getVimPort().destroyTask(mor);
        Task t = new Task(vcConnection);
        if (t.monitorTask(taskMor)) {
            logger.info("destroy object done.");
        } else {
            TaskInfo taskInfo = t.getTaskInfo(taskMor);
            TaskFailedException tfe = new TaskFailedException(taskInfo.getError().getLocalizedMessage(),
                    VsphereConstants.DELETEDCOJBECT_TASK_FAILURE, taskInfo.getName(), mor.getValue());
            throw tfe;
        }
    }

    /**
     * Parse vlan string to valid vlan value.
     *
     * @param vlanIdStr, vlan trunking string, such as "105, 115, 205-210".
     *                   ',' is used to separate different vlan id
     *                   '-' is used for vlan trunking range
     */
    public static List<NumericRange> parseVlanId(String vlanIdStr) {
        logger.debug("parseVlanId: {}", vlanIdStr);
        if (StringUtils.isNotBlank(vlanIdStr)) {
            String[] IdStrs = vlanIdStr.split(",");
            int len = IdStrs.length;
            List<NumericRange> lstNumericRange = new ArrayList<NumericRange>();
            for (int i = 0; i < len; i++) {
                String[] ids = IdStrs[i].split("-");
                NumericRange nr = new NumericRange();
                if (ids.length == 1) {
                    nr.setStart(Integer.parseInt(StringUtils.trim(ids[0])));
                    nr.setEnd(Integer.parseInt(StringUtils.trim(ids[0])));
                } else if (ids.length >= 2) {
                    nr.setStart(Integer.parseInt(StringUtils.trim(ids[0])));
                    nr.setEnd(Integer.parseInt(StringUtils.trim(ids[1])));
                }
                lstNumericRange.add(nr);
            }
            return lstNumericRange;
        }
        return null;
    }

}


