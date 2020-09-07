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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.HostInfo;
import com.vmware.sample.hci.vsphere.InputHostConfiguration;
import com.vmware.sample.hci.vsphere.InputPortgroupConfiguration;
import com.vmware.sample.hci.vsphere.InputVcConfiguration;
import com.vmware.sample.hci.vsphere.VcInfo;
import com.vmware.sample.hci.vsphere.VsphereService;
import com.vmware.sample.hci.vsphere.utils.ConfigProgress;
import com.vmware.sample.hci.vsphere.utils.ProgressCallback;
import com.vmware.sample.hci.vsphere.vcinstall.VcJsonInput;

public class IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
    final int LEN = 2;

    final VcInfo vcInfo = new VcInfo("YOUR_VC_HOSTNAME", "YOUR_VC_USERNAME", "YOUR_VC_PASSWORD");
    final InputVcConfiguration vcConfig = new InputVcConfiguration();
    final InputHostConfiguration[] hostConfigs = new InputHostConfiguration[LEN];
    final String[] ntpServers = new String[]{"YOUR_NTP_SERVER"};
    final String[] dnsServers = new String[]{"YOUR_DNS_SERVER"};

    public IntegrationTest() {
        this.vcConfig.setVcInfo(this.vcInfo);
        InputPortgroupConfiguration portGroupInfo = new InputPortgroupConfiguration();
        portGroupInfo.setMgmtVlanTrunk(null);
        portGroupInfo.setvMotionVlanTrunk("YOUR_VMOTION_VLAN_ID");
        portGroupInfo.setvSanVlanTrunk("YOUR_VSAN_VLAN_ID");
        portGroupInfo.setVmVlanTrunk("YOUR_VM_VLAN_ID");
        this.vcConfig.setPortGroupInfo(portGroupInfo);

        HostInfo esxHost = new HostInfo("YOUR_HOST1_HOST_IP", "YOUR_HOST1_HOSTNAME", "YOUR_HOST1_USERNAME", "YOUR_HOST1_PASSWORD");
        hostConfigs[0] = new InputHostConfiguration();
        hostConfigs[0].setDefaultHostInfo(esxHost);
        HostInfo newEsxHost = new HostInfo("YOUR_HOST1_NEW_IP", "YOUR_HOST1_NEW_NAME", "YOUR_HOST1_NEW_USERNAME", "YOUR_HOST1_NEW_PASSWORD");
        hostConfigs[0].setCustomizedHostInfo(newEsxHost);

        hostConfigs[0].setMgmtDHCP(false);
        hostConfigs[0].setMgmtIp("YOUR_HOST1_MANAGEMENT_IP");
        hostConfigs[0].setMgmtNetmask("YOUR_HOST1_MANAGEMENT_NETMASK");

        hostConfigs[0].setvMotionDHCP(true);
        hostConfigs[0].setvMotionIp("YOUR_HOST1_VMOTION_IP");
        hostConfigs[0].setvMotionNetmask("YOUR_HOST1_VMOTION_NETMASK");

        hostConfigs[0].setvSanDHCP(true);
        hostConfigs[0].setvSanIp("YOUR_HOST1_VSAN_IP");
        hostConfigs[0].setvSanNetmask("YOUR_HOST1_VSAN_IP");

        esxHost = new HostInfo("YOUR_HOST2_HOST_IP", "YOUR_HOST2_HOSTNAME", "YOUR_HOST2_USERNAME", "YOUR_HOST2_PASSWORD");
        hostConfigs[1] = new InputHostConfiguration();
        hostConfigs[1].setDefaultHostInfo(esxHost);
        newEsxHost = new HostInfo("YOUR_HOST2_NEW_IP", "YOUR_HOST2_NEW_NAME", "YOUR_HOST2_NEW_USERNAME", "YOUR_HOST2_NEW_PASSWORD");
        hostConfigs[1].setCustomizedHostInfo(newEsxHost);

        hostConfigs[1].setMgmtDHCP(false);
        hostConfigs[1].setMgmtIp("YOUR_HOST2_MANAGEMENT_IP");
        hostConfigs[1].setMgmtNetmask("YOUR_HOST2_MANAGEMENT_NETMASK");

        hostConfigs[1].setvMotionDHCP(true);
        hostConfigs[1].setvMotionIp("YOUR_HOST2_VMOTION_IP");
        hostConfigs[1].setvMotionNetmask("YOUR_HOST2_VMOTION_NETMASK");

        hostConfigs[1].setvSanDHCP(true);
        hostConfigs[1].setvSanIp("YOUR_HOST2_VSAN_IP");
        hostConfigs[1].setvSanNetmask("YOUR_HOST2_VSAN_NETMASK");

    }

    public static void main(String[] args) {
        logger.info("test begin");
        String domainName = "YOUR_DOMAIN_NAME";
        try {
            IntegrationTest it = new IntegrationTest();
            if (StringUtils.equals(args[0], "Test")) {
                logger.info("Test");
            } else if (StringUtils.equals(args[0], "BackupHosts")) {
                logger.info("testBackupHosts");
                it.testBackupHosts();
            } else if (StringUtils.equals(args[0], "RestoreHosts")) {
                logger.info("testRestoreHosts");
                for (int i = 0; i < it.LEN; i++) {
                    it.hostConfigs[i].getCustomizedHostInfo().setHostName(
                            it.hostConfigs[i].getCustomizedHostInfo().getHostName() + "." + domainName);
                }
                it.testRestoreHosts();
                logger.info("Wait 3 minutes for host to restart...");
                try {
                    Thread.sleep(180000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                it.testRemoveHostsFromInventory();
            } else if (StringUtils.equals(args[0], "PrepareHosts")) {
                it.testPrepareHosts();
            } else if (StringUtils.equals(args[0], "InstallVcWithProgress")) {
                it.testInstallVcWithProgress();
            } else if (StringUtils.equals(args[0], "InitializeVcenter")) {
                for (int i = 0; i < it.LEN; i++) {
                    it.hostConfigs[i].getCustomizedHostInfo().setHostName(it.hostConfigs[i].getCustomizedHostInfo().getHostName() + "." + domainName);
                }
                it.testInitializeVcenter();
            } else if (StringUtils.equals(args[0], "AddHosts")) {
                it.testAddHosts();
            } else if (StringUtils.equals(args[0], "All")) {
                it.testAll();
            } else if (StringUtils.equals(args[0], "RemoveDC")) {
                it.testRemoveDatacenter();
            } else {
                logger.error("Invalid");
            }
            it.testBackupHosts();
            it.testInstallVcWithProgress();
            it.testInitializeVcenter();
            it.testAddHosts();
            it.testAddLicenses();
            it.testRemoveLicenses();
            it.testAssignESXiLicense();
            System.out.println("InitializeVcenter done");
            logger.info("Wait 3 minutes ...");
            try {
                Thread.sleep(180000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            it.testRestoreHosts();
            System.out.println("testRestoreHosts done");
            logger.info("Wait 3 minutes ...");
            try {
                Thread.sleep(180000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            it.testRemoveHostsFromInventory();
            logger.info("Test completed");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void testInitializeVcenter() {
        UserLog ul = new UserLog();
        VsphereService.initializeVcenter(
                this.vcConfig, this.hostConfigs, 0, 100, ul);
    }

    private void testPrepareHosts() {
        try {
            String domainName = "YOUR_DOMAIN_NAME";
            String newGateway = "YOUR_GATEWAY_IP";
            String pgName = "backupPort2";
            for (int i = 0; i < LEN; i++) {
                //update NTP server on hosts
                VsphereService.updateNtp(hostConfigs[i].getDefaultHostInfo(), ntpServers);
                //update DNS server on hosts
                VsphereService.updateDns(hostConfigs[i].getDefaultHostInfo(), dnsServers);
                VsphereService.updateHostName(hostConfigs[i].getDefaultHostInfo(), hostConfigs[i].getCustomizedHostInfo().getHostName(), domainName);
                hostConfigs[i].getCustomizedHostInfo().setHostName(hostConfigs[i].getCustomizedHostInfo().getHostName() + "." + domainName);
                VsphereService.addManagementIpAddress(hostConfigs[i].getDefaultHostInfo(), hostConfigs[i].getMgmtIp(),
                        hostConfigs[i].getMgmtNetmask(), pgName);
                VsphereService.updateDefaultGateway(hostConfigs[i].getDefaultHostInfo(), newGateway);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void testAll() {
        try {
            UserLog ul = new UserLog();
            ConfigProgress.setProgress(0, ul);
            this.testBackupHosts();
            ConfigProgress.increaseProgressBy(5, ul);
            this.testPrepareHosts();
            ConfigProgress.increaseProgressBy(10, ul);
            this.testInstallVcWithProgress();

            logger.info("Installing VC to ESXi");
            VcJsonInput jsonInput = new VcJsonInput();
            jsonInput.setDnsServer(dnsServers[0]);
            jsonInput.setDnsAlternateServer(dnsServers[1]);
            jsonInput.setGateway("YOUR_GATEWAY_IP");
            String vcIp = "YOUR_VC_IP_ADDRESS";
            jsonInput.setIp("YOUR_VC_IP_ADDRESS");
            jsonInput.setIpFamily("ipv4");
            jsonInput.setMode("static");
            jsonInput.setPrefix("YOUR_VC_IP_PREFIX");
            String vcSystemName = "YOUR_VC_FQDN";
            jsonInput.setSystemName(vcSystemName);
            String vcPassword = "YOUR_VC_PASSWORD";
            jsonInput.setPassword(vcPassword);
            jsonInput.setVmName("YOUR_VC_VM_NAME");

            VsphereService.deployVcenter(hostConfigs[0].getDefaultHostInfo(), "/root/Downloads/VC_INSTALLATION_ISO_NAME", "/mnt", "datastore1", jsonInput, 20, 60, null);


            VsphereService.initializeVcenter(this.vcConfig,
                    this.hostConfigs, ConfigProgress.getProgress() + 2, 99, ul);

            ConfigProgress.setProgress(100, ul);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void testBackupHosts() {
        for (int i = 0; i < this.LEN; i++) {
            try {
                VsphereService.backupHost(hostConfigs[i].getDefaultHostInfo());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void testRestoreHosts() {
        InputHostConfiguration[] restoreHostConfigs = new InputHostConfiguration[2];

        HostInfo esxHost = new HostInfo("YOUR_HOST1_HOST_IP", "YOUR_HOST1_HOSTNAME", "YOUR_HOST1_USERNAME", "YOUR_HOST1_PASSWORD");
        restoreHostConfigs[0] = new InputHostConfiguration();
        restoreHostConfigs[0].setDefaultHostInfo(esxHost);
        HostInfo newEsxHost = new HostInfo("YOUR_HOST1_NEW_IP", "YOUR_HOST1_NEW_NAME", "YOUR_HOST1_NEW_USERNAME", "YOUR_HOST1_NEW_PASSWORD");
        restoreHostConfigs[0].setCustomizedHostInfo(newEsxHost);

        restoreHostConfigs[0].setMgmtDHCP(false);
        restoreHostConfigs[0].setMgmtIp("YOUR_HOST1_MANAGEMENT_IP");
        restoreHostConfigs[0].setMgmtNetmask("YOUR_HOST1_MANAGEMENT_NETMASK");

        restoreHostConfigs[0].setvMotionDHCP(true);
        restoreHostConfigs[0].setvMotionIp("YOUR_HOST1_VMOTION_IP");
        restoreHostConfigs[0].setvMotionNetmask("YOUR_HOST1_VMOTION_NETMASK");

        restoreHostConfigs[0].setvSanDHCP(true);
        restoreHostConfigs[0].setvSanIp("YOUR_HOST1_VSAN_IP");
        restoreHostConfigs[0].setvSanNetmask("YOUR_HOST1_VSAN_NETMASK");

        esxHost = new HostInfo("YOUR_HOST2_HOST_IP", "YOUR_HOST2_HOSTNAME", "YOUR_HOST2_USERNAME", "YOUR_HOST2_PASSWORD");
        restoreHostConfigs[1] = new InputHostConfiguration();
        restoreHostConfigs[1].setDefaultHostInfo(esxHost);
        newEsxHost = new HostInfo("YOUR_HOST2_NEW_IP", "YOUR_HOST2_NEW_NAME", "YOUR_HOST2_NEW_USERNAME", "YOUR_HOST2_NEW_PASSWORD");
        restoreHostConfigs[1].setCustomizedHostInfo(newEsxHost);

        restoreHostConfigs[1].setMgmtDHCP(false);
        restoreHostConfigs[1].setMgmtIp("YOUR_HOST2_MANAGEMENT_IP");
        restoreHostConfigs[1].setMgmtNetmask("YOUR_HOST2_MANAGEMENT_NETMASK");

        restoreHostConfigs[1].setvMotionDHCP(true);
        restoreHostConfigs[1].setvMotionIp("YOUR_HOST2_VMOTION_IP");
        restoreHostConfigs[1].setvMotionNetmask("YOUR_HOST2_VMOTION_NETMASK");

        restoreHostConfigs[1].setvSanDHCP(true);
        restoreHostConfigs[1].setvSanIp("YOUR_HOST2_VSAN_IP");
        restoreHostConfigs[1].setvSanNetmask("YOUR_HOST2_VSAN_NETMASK");


        for (int i = 0; i < 2; i++) {
            try {
                VsphereService.restoreHost(restoreHostConfigs[i].getDefaultHostInfo());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        logger.info("Wait 5 minutes for all host from restoring");
        try {
            Thread.sleep(300000);
        } catch (InterruptedException e) {

            logger.error(e.getMessage(), e);
        }
        try {
            VsphereService.RemoveHostsFromInventory(this.vcConfig.getVcInfo(), restoreHostConfigs, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void testRemoveHostsFromInventory() {
        try {
            VsphereService.RemoveHostsFromInventory(this.vcConfig.getVcInfo(), this.hostConfigs, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void testInstallVcWithProgress() {
        try {
            UserLog ul = new UserLog();
            ConfigProgress.increaseProgressBy(1, ul);
            Thread t = new InstallThread(ul);
            t.start();
            t.join();
            ConfigProgress.increaseProgressBy(2, ul);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            System.exit(-1);
        }
    }

    private void testAddHosts() {
        final int newHostLen = 1;
        try {
            String domainName = "YOUR_DOMAIN_NAME";
            String newGateway = "YOUR_GATEWAY_IP";
            String pgName = "backupPort2";

            final InputHostConfiguration[] newHostConfigs = new InputHostConfiguration[newHostLen];

            HostInfo esxHost = new HostInfo("YOUR_HOST3_HOST_IP", "YOUR_HOST3_HOSTNAME", "YOUR_HOST3_USERNAME", "YOUR_HOST3_PASSWORD");
            newHostConfigs[0] = new InputHostConfiguration();
            newHostConfigs[0].setDefaultHostInfo(esxHost);
            HostInfo newEsxHost = new HostInfo("YOUR_HOST3_NEW_IP", "YOUR_HOST3_NEW_HOSTNAME", "YOUR_HOST3_NEW_USERNAME", "YOUR_HOST3_NEW_PASSWORD");
            newHostConfigs[0].setCustomizedHostInfo(newEsxHost);

            newHostConfigs[0].setMgmtDHCP(false);
            newHostConfigs[0].setMgmtIp("YOUR_HOST3_MANAGEMENT_IP");
            newHostConfigs[0].setMgmtNetmask("YOUR_HOST3_MANAGEMENT_NETMASK");

            newHostConfigs[0].setvMotionDHCP(true);
            newHostConfigs[0].setvMotionIp("YOUR_HOST3_VMOTION_IP");
            newHostConfigs[0].setvMotionNetmask("YOUR_HOST3_VMOTION_NETMASK");

            newHostConfigs[0].setvSanDHCP(true);
            newHostConfigs[0].setvSanIp("YOUR_HOST3_VSAN_IP");
            newHostConfigs[0].setvSanNetmask("YOUR_HOST3_VSAN_NETMASK");

            VsphereService.backupHost(newHostConfigs[0].getDefaultHostInfo());
            //update NTP server on hosts
            VsphereService.updateNtp(newHostConfigs[0].getDefaultHostInfo(), ntpServers);
            //update DNS server on hosts
            VsphereService.updateDns(newHostConfigs[0].getDefaultHostInfo(), dnsServers);
            VsphereService.updateHostName(newHostConfigs[0].getDefaultHostInfo(), newHostConfigs[0].getCustomizedHostInfo().getHostName(), domainName);
            newHostConfigs[0].getCustomizedHostInfo().setHostName(newHostConfigs[0].getCustomizedHostInfo().getHostName() + "." + domainName);
            VsphereService.addManagementIpAddress(newHostConfigs[0].getDefaultHostInfo(), newHostConfigs[0].getMgmtIp(),
                    newHostConfigs[0].getMgmtNetmask(), pgName);
            VsphereService.updateDefaultGateway(newHostConfigs[0].getDefaultHostInfo(), newGateway);

            UserLog ul = new UserLog();
            VsphereService.addHosts(this.vcConfig, newHostConfigs, 0, 100, ul);

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void testAddLicenses() {
        final String[] licenses = new String[]{
                "00000-00000-00000-00000-00000",
        };
        try {
            for (String license : licenses) {
                VsphereService.addLicense(this.vcInfo, license);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void testRemoveLicenses() {
        final String[] licenses = new String[]{
                "00000-00000-00000-00000-00000",
        };
        try {
            for (String license : licenses) {
                VsphereService.removeLicense(vcInfo, license);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public void testAssignESXiLicense() {
        final String ESXiStandardLicense = "00000-00000-00000-00000-00000";

        final int newHostLen = 3;
        HostInfo[] newEsxHosts = new HostInfo[newHostLen];
        newEsxHosts[0] = new HostInfo("YOUR_HOST1_IP", "YOUR_HOST1_HOSTNAME", "YOUR_HOST1_USERNAME", "YOUR_HOST1_PASSWORD");
        newEsxHosts[1] = new HostInfo("YOUR_HOST2_IP", "YOUR_HOST2_HOSTNAME", "YOUR_HOST2_USERNAME", "YOUR_HOST2_PASSWORD");
        newEsxHosts[2] = new HostInfo("YOUR_HOST3_IP", "YOUR_HOST3_HOSTNAME", "YOUR_HOST3_USERNAME", "YOUR_HOST3_PASSWORD");
        for (HostInfo hostInfo : newEsxHosts) {
            try {
                VsphereService.assignESXiLicense(vcInfo, hostInfo, ESXiStandardLicense);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public void testRemoveDatacenter() {
        VsphereService.removeDatacenter(this.vcInfo, this.vcConfig.getDataCenterName());
    }

    public void testGetHostHardwareConfiguration() {
        try {
            for (InputHostConfiguration hostCfg : this.hostConfigs) {
                logger.info("{}", VsphereService.getHostHardwareConfiguration(hostCfg.getDefaultHostInfo()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class UserLog implements ProgressCallback {

        @Override
        public void printProgress(int progress) {
            logger.info("__Printed from callback: {}", progress);
        }
    }

    class InstallThread extends Thread {
        private ProgressCallback pc;

        public InstallThread(ProgressCallback pc) {
            super();
            this.pc = pc;
        }

        @Override
        public void run() {
            HostInfo targetEsxHost = new HostInfo("YOUR_HOST1_IP", "YOUR_HOST1_HOSTNAME", "YOUR_HOST1_USERNAME", "YOUR_HOST1_PASSWORD");
            logger.info("Installing VC to ESXi...");
            VcJsonInput jsonInput = new VcJsonInput();
            jsonInput.setDnsServer("YOUR_DNS_1");
            jsonInput.setDnsAlternateServer("YOUR_DNS_2");
            jsonInput.setGateway("YOUR_GATEWAY_IP");
            String vcIp = "YOUR_VC_IP";
            jsonInput.setIp(vcIp);
            jsonInput.setIpFamily("ipv4");
            jsonInput.setMode("static");
            jsonInput.setPrefix("YOUR_VC_IP_PREFIX");
            String vcSystemName = "YOUR_VC_FQDN";
            jsonInput.setSystemName(vcSystemName);
            String vcPassword = "YOUR_VC_PASSWORD";
            jsonInput.setPassword(vcPassword);
            jsonInput.setVmName("YOUR_VC_VM_NAME");
            VsphereService.deployVcenter(targetEsxHost, "/root/Downloads/VC_INSTALLATION_ISO_NAME", "/mnt", "datastore1", jsonInput, 16, 50, this.pc);
        }
    }
}
