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

package com.vmware.sample.hci.vsphere.vcinstall;

/**
 * The IP address that will be assigned to vCenter
 * ip.family
 * mode
 * ip
 * prefix
 * gateway
 * dns.servers
 * system.name
 */
public class VcJsonInput {

    private String ipFamily = "ipv4";
    private String mode = "static";
    private String ip;
    private String prefix;
    private String gateway;
    private String dnsServer;
    private String dnsAlternateServer;
    private String systemName;
    private String vmName;
    private String password;

    public String getIpFamily() {
        return ipFamily;
    }

    public void setIpFamily(String ipFamily) {
        this.ipFamily = ipFamily;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getDnsServer() {
        return dnsServer;
    }

    public void setDnsServer(String dnsServer) {
        this.dnsServer = dnsServer;
    }

    public String getDnsAlternateServer() {
        return dnsAlternateServer;
    }

    public void setDnsAlternateServer(String dnsAlternateServer) {
        this.dnsAlternateServer = dnsAlternateServer;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
