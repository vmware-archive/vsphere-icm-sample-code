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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.VcInfo;
import com.vmware.sample.hci.vsphere.VcManager;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;
import com.vmware.vim25.ManagedObjectReference;

public class VsphereUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(HostManagerTest.class);

    private VcManager vcManager;

    @Before
    public void connect() {
        VcInfo vcInfo = new VcInfo("YOUR_VC_IP", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
        this.vcManager = new VcManager(vcInfo);
    }

    @After
    public void disconnect() {
        if (this.vcManager != null) {
            this.vcManager.disconnect();
        }
    }

    @Ignore
    public void testGetDatacenterMor() {
        try {
            String dcName = "Datacenter";
            ManagedObjectReference dcMor = VsphereUtil.getDatacenterMor(this.vcManager.getVsphereClient(), dcName);
            assertNotNull(dcMor);
            dcName = "Datacenter";
            dcMor = VsphereUtil.getDatacenterMor(this.vcManager.getVsphereClient(), dcName);
            assertNull(dcMor);
            logger.info("Test completed");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Ignore
    public void testGetVdsMor() {
        try {
            String vdsName = "DSwitch";
            ManagedObjectReference vdsMor = VsphereUtil.getVdsMor(this.vcManager.getVsphereClient(), vdsName);
            assertNotNull(vdsMor);
            vdsName = "DSwitch";
            vdsMor = VsphereUtil.getVdsMor(this.vcManager.getVsphereClient(), vdsName);
            assertNull(vdsMor);
            logger.info("Test completed");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Ignore
    public void testGetPorggroupMor() {
        try {
            String dcName = "Datacenter1";
            String pgName = "pg1";
            ManagedObjectReference pgMor = VsphereUtil.getPorggroupMor(
                    this.vcManager.getVsphereClient(), dcName, pgName);
            assertNotNull(pgMor);
            dcName = "Datacenter2";
            pgName = "pg2";
            pgMor = VsphereUtil.getPorggroupMor(this.vcManager.getVsphereClient(),
                    dcName, pgName);
            assertNull(pgMor);
            logger.info("Test completed");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Ignore
    public void testGetClusterMor() {
        try {
            String dcName = "Datacenter";
            String clusterName = "Cluster1";
            ManagedObjectReference clusterMor = VsphereUtil.getClusterMor(this.vcManager.getVsphereClient(), dcName, clusterName);
            assertNotNull(clusterMor);
            clusterName = "Cluster2";
            clusterMor = VsphereUtil.getClusterMor(this.vcManager.getVsphereClient(), dcName, clusterName);
            assertNull(clusterMor);
            logger.info("Test completed");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Ignore
    public void testGetHostMorsInCluster() {
        try {
            String dcName = "Datacenter";
            String clusterName = "Cluster1";
            ManagedObjectReference clusterMor = VsphereUtil.getClusterMor(this.vcManager.getVsphereClient(), dcName, clusterName);
            List<ManagedObjectReference> LstHostMor = VsphereUtil.getHostMorsInCluster(this.vcManager.getVsphereClient(), clusterMor);
            for (ManagedObjectReference mor : LstHostMor) {
                logger.info("Type: {}", mor.getType());
                logger.info("Value: {}", mor.getValue());
            }
            logger.info("Test completed");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testParseVlanId() {
        try {
            String[] vlanIdStrs = new String[]{"0", "105, 115, 205-210", "106, 116", "asdfasdf", "-1--2"};
            for (String vlanIdStr : vlanIdStrs) {
                VsphereUtil.parseVlanId(vlanIdStr);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
}
