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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.HostInfo;
import com.vmware.sample.hci.vsphere.HostManager;
import com.vmware.vim25.HostServicePolicy;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class HostManagerTest {
    private static final Logger logger = LoggerFactory.getLogger(HostManagerTest.class);

    private HostManager hostManager;

    @Before
    public void connect() {
        HostInfo hostInfo = new HostInfo("YOUR_HOST_IP_ADDRESS", "YOUR_HOST_HOSTNAME", "YOUR_USER_NAME", "YOUR_PASS_WORD");
        hostManager = new HostManager(hostInfo);
    }

    @After
    public void disconnect() {
        if (hostManager != null) {
            hostManager.disconnect();
        }
    }

    @Test
    public void testGetHostVersion() {
        try {
            String version = hostManager.getHostVersion();
            assertEquals("6.7.0", version);
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testAddManagementIPAddress()
            throws RuntimeFaultFaultMsg, InvalidStateFaultMsg, InvalidPropertyFaultMsg {
        String ipAddress = "YOUR_HOST_IP_ADDRESS";
        String mask = "YOUR_NET_MASK";
        String pgName = "BackUp";

        String vswName = hostManager.getValidVirtualSwitch(pgName);
        if (vswName != null) {
            hostManager.addVirtualSwitchPortGroup(pgName, vswName);
            try {
                hostManager.addManagementIPAddress(ipAddress, mask, pgName);
            } catch (IllegalArgumentException e) {
                fail(e.getMessage());
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Test
    public void testUpdateNtp() {
        String ntpAddrA = "YOUR_NTP_A";
        String ntpAddrB = "YOUR_NTP_B";
        String[] ntp = {ntpAddrA, ntpAddrB};

        try {
            hostManager.updateNtp(ntp);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg | IllegalArgumentException e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testUpdateDns() {
        String dnsAddrA = "YOUR_DNS_A";
        String dnsAddrB = "YOUR_DNS_B";
        String[] dns = {dnsAddrA, dnsAddrB};

        try {
            hostManager.updateDns(dns);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg | IllegalArgumentException e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    /* Host name and password should be tested at the same time,
     * or the test will fail to get ESX connection.
     * because the hostname or password has been changed by tests.
     */
    @Test
    public void testUpdateHostNameAndPassword() {
        String domainName = "YOUR_DOMAIN_NAME";
        String hostName = "YOUR_HOST_NAME";
        String userName = "YOUR_USER_NAME";
        String password = "YOUR_PASS_WORD";

        try {
            hostManager.updateHostName(hostName, domainName);
            hostManager.updateUserPassword(userName, password);
        } catch (InvalidStateFaultMsg | RuntimeFaultFaultMsg | IllegalArgumentException e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testUpdateDefaultGateway() {
        String gateway = "YOUR_GATEWAY_IP_ADDRESS";
        try {
            hostManager.updateDefaultGateway(gateway);
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testUpdateHostService() {
        try {
            hostManager.updateHostService("TSM-SSH", true, HostServicePolicy.ON.value());
            logger.info("Success");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testBackup() {
        try {
            hostManager.runBackupCmd();
            logger.info("Success");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testRestore() {
        try {
            hostManager.runRestoreCmd();
            logger.info("Success");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testGetHardwareConfiguration() {
        try {
            hostManager.getHardwareConfiguration();
            logger.info("Success");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    public void testExitMaintenanceMode() {
        try {
            hostManager.exitMaintenanceMode();
            logger.info("Success");
        } catch (Exception e) {
            fail(e.getMessage());
            logger.error(e.getMessage(), e);
        }
    }
}
