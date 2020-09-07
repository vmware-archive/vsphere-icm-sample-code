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

package com.vmware.vsphere;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.HostInfo;
import com.vmware.sample.hci.vsphere.VcInfo;
import com.vmware.sample.hci.vsphere.VcManager;
import com.vmware.sample.hci.vsphere.operation.Cluster;
import com.vmware.sample.hci.vsphere.operation.HostNetwork;
import com.vmware.sample.hci.vsphere.operation.VirtualDistributedSwitch;
import com.vmware.vim25.HostVirtualNicManagerNicType;
import com.vmware.vim25.ManagedObjectReference;

public class VcManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(VcManagerTest.class);

    @Test
    public void vcManagerTest() {
        String dataCenterName = "Datacenter";
        String clusterName = "Cluster";
        String dvsName = "dvSwitch";
        String dvsPortGroupName = "dvPortGroup";
        String dvsPortGroupName_vMotion = "dvPortGroup_vMotion";
        String dvsPortGroupName_vSAN = "dvPortGroup_vSAN";
        logger.info("test starting");

        VcInfo myvc = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        HostInfo[] myhost = new HostInfo[3];
        ManagedObjectReference[] myhostMor = new ManagedObjectReference[3];
        myhost[0] = new HostInfo("YOUR_HOST1_IP", "YOUR_HOST1_HOSTNAME", "YOUR_HOST1_USERNAME", "YOUR_HOST1_PASSWORD");
        myhost[1] = new HostInfo("YOUR_HOST2_IP", "YOUR_HOST2_HOSTNAME", "YOUR_HOST2_USERNAME", "YOUR_HOST2_PASSWORD");
        myhost[2] = new HostInfo("YOUR_HOST3_IP", "YOUR_HOST3_HOSTNAME", "YOUR_HOST3_USERNAME", "YOUR_HOST3_PASSWORD");
        VcManager myVcMgr = new VcManager(myvc);
        try {
            ManagedObjectReference dcMor = myVcMgr.createDatacenter(dataCenterName);
            logger.info("created DC: {}", dcMor.getValue());
            ManagedObjectReference clusterMor = myVcMgr.createCluster(dcMor, clusterName);

            ManagedObjectReference dvsMor = VirtualDistributedSwitch
                    .createVds(myVcMgr.getVsphereClient(), dcMor, dvsName);
            try {
                logger.info("created VirtualDistributedSwitch: {}",
                        dvsMor.getValue());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            ManagedObjectReference dvpgMor = VirtualDistributedSwitch
                    .createDVPortGroup(myVcMgr.getVsphereClient(), dvsMor,
                            dvsPortGroupName, "YOUR_MANAGEMENT_VLAN_ID");
            ManagedObjectReference dvpgVmotionMor = VirtualDistributedSwitch
                    .createDVPortGroup(myVcMgr.getVsphereClient(), dvsMor,
                            dvsPortGroupName_vMotion, "YOUR_VMOTION_VLAN_ID");
            ManagedObjectReference dvpgVsanMor = VirtualDistributedSwitch
                    .createDVPortGroup(myVcMgr.getVsphereClient(), dvsMor,
                            dvsPortGroupName_vSAN, "YOUR_VSAN_VLAN_ID");

            HostInfo vcHostInfo =
                    new HostInfo(myvc.getIpAddress(), myvc.getIpAddress(),
                            myvc.getUserName(), myvc.getPassword());

            String vmotionIP = "YOUR_VMOTION_IP";
            String vSanIP = "YOUR_VSAN_IP";
            Integer index = 0;

            for (HostInfo hostInfo : myhost) {
                ManagedObjectReference hostMor =
                        myVcMgr.addHostToCluster(dcMor, clusterMor, hostInfo);
                myhostMor[index] = hostMor;
                VirtualDistributedSwitch.addHostToVds(
                        myVcMgr.getVsphereClient(), dcMor, hostMor, dvsMor,
                        dvpgMor);
                HostNetwork hostnwk = new HostNetwork(
                        myVcMgr.getVsphereClient(), hostMor, dvsMor, new ManagedObjectReference[]{dvpgMor, dvpgVmotionMor, dvpgVsanMor});

                index++;
                hostnwk.addVirtualNIC(false, vmotionIP + index.toString(),
                        "YOUR_VMOTION_NETMASK", HostVirtualNicManagerNicType.VMOTION);
                hostnwk.addVirtualNIC(false, vSanIP + index.toString(),
                        "YOUR_VSAN_NETMASK", HostVirtualNicManagerNicType.VSAN);
            }

            Cluster myCluster = new Cluster(myVcMgr.getVsphereClient());

            myCluster.setDRS(clusterMor, true);
            myCluster.setDAS(clusterMor, true);

            Thread.sleep(2 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myVcMgr.disconnect();
        logger.info("test completed.");
    }

    @Test
    public void testAddLicense() {
        VcInfo myvc = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        VcManager myVcMgr = null;
        try {
            myVcMgr = new VcManager(myvc);
            //Evaludation
            myVcMgr.addLicense("00000-00000-00000-00000-00000");
            System.out.println("Test done");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail(ex.getMessage());
        } finally {
            if (null != myVcMgr) {
                myVcMgr.disconnect();
            }
        }
    }

    @Test
    public void testRemoveLicense() {
        VcInfo myvc = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        VcManager myVcMgr = null;
        try {
            myVcMgr = new VcManager(myvc);
            //VC
            myVcMgr.removeLicense("00000-00000-00000-00000-00000");
            //ESXi
            myVcMgr.removeLicense("00000-00000-00000-00000-00000");
            //VSAN
            myVcMgr.removeLicense("00000-00000-00000-00000-00000");
            System.out.println("Test done");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail(ex.getMessage());
        } finally {
            if (null != myVcMgr) {
                myVcMgr.disconnect();
            }
        }
    }

    @Test
    public void testAssignESXiLicense() {
        VcInfo myvc = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        HostInfo hostInfo = new HostInfo("YOUR_HOST_IP", "YOUR_HOST_HOSTNAME", "YOUR_HOST_USERNAME", "YOUR_HOST_PASSWORD");
        String evaluationKey = "00000-00000-00000-00000-00000";
        VcManager myVcMgr = null;
        try {
            myVcMgr = new VcManager(myvc);
            myVcMgr.assignESXiLicense(hostInfo, evaluationKey);
            System.out.println("Test done");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail(ex.getMessage());
        } finally {
            if (null != myVcMgr) {
                myVcMgr.disconnect();
            }
        }
    }

    @Test
    public void testAssignVcLicense() {
        VcInfo myvc = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        String evaluationKey = "00000-00000-00000-00000-00000";
        VcManager myVcMgr = null;
        try {
            myVcMgr = new VcManager(myvc);
            myVcMgr.assignVcLicense(evaluationKey);
            System.out.println("Test done");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail(ex.getMessage());
        } finally {
            if (null != myVcMgr) {
                myVcMgr.disconnect();
            }
        }
    }

    @Ignore
    public void testAssignVsanLicense() {
        VcInfo myvc = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        String licenseKey = "00000-00000-00000-00000-00000";
        String dcName = "Datacenter";
        String clusterName = "New Cluster";
        VcManager myVcMgr = null;
        try {
            myVcMgr = new VcManager(myvc);
            myVcMgr.assignVsanLicense(dcName, clusterName, licenseKey);
            System.out.println("Test done");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            fail(ex.getMessage());
        } finally {
            if (null != myVcMgr) {
                myVcMgr.disconnect();
            }
        }
    }
}
