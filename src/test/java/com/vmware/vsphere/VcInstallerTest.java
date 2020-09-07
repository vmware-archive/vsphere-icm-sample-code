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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.HostInfo;
import com.vmware.sample.hci.vsphere.VsphereService;
import com.vmware.sample.hci.vsphere.vcinstall.VcJsonInput;

public class VcInstallerTest {

    private static final Logger logger = LoggerFactory.getLogger(VcInstallerTest.class);

    @Test
    public void test() {
        try {
            HostInfo hostInfo = new HostInfo("YOUR_HOST_IP", "YOUR_HOST_HOSTNAME", "YOUR_HOST_USERNAME", "YOUR_HOST_PASSWORD");
            VcJsonInput jsonInput = new VcJsonInput();
            jsonInput.setDnsServer("YOUR_DNS_1");
            jsonInput.setDnsAlternateServer("YOUR_DNS_2");
            jsonInput.setGateway("YOUR_GATEWAY_IP");
            jsonInput.setIp("YOUR_VC_IP");
            jsonInput.setIpFamily("ipv4");
            jsonInput.setMode("static");
            jsonInput.setPrefix("YOUR_VC_IP_PERFIX");
            jsonInput.setSystemName("YOUR_VC_FQDN");
            jsonInput.setPassword("YOUR_VC_PASSWORD");
            jsonInput.setVmName("YOUR_VC_VM_NAME");

            VsphereService.deployVcenter(hostInfo, "/root/Downloads/YOUR_VC_INSTALLATION_ISO_NAME",
                    "/mnt", "datastore1", jsonInput, 20, 60, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
}
