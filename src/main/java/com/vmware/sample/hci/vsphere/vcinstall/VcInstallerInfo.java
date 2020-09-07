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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.exception.HciServerException;

/**
 * Author: yingjisun
 * Collect ESX information, VC information and Network information
 * for generating json file
 */
public class VcInstallerInfo {

    private static final Logger logger = LoggerFactory.getLogger(VcInstallerInfo.class);

    private String esxHostname;
    private String esxDatastore;
    private String esxUsername;
    private String esxPassword;
    private String deploymentOption;
    private String deploymentNetwork;
    private String applianceName;
    private boolean applianceThinDiskMode;
    private String vcIsoPath;
    private String vcMntPath;

    private String vcRootPassword;
    private boolean vcSshEnabled;

    private String vcSsoPassword;
    private String vcSsoDomainName;
    private String vcSsoSiteName;

    private String vcNWIpFamily;
    private String vcNWMode;
    private String vcNWIp;
    private String vcNWPrefix;
    private String vcNWGateway;
    private String vcNWDnsServer;
    private String vcNWDnsAlternateServer;
    private String vcNWHostName;

    public String getEsxHostname() {
        return esxHostname;
    }

    public void setEsxHostname(String esxHostname) {
        this.esxHostname = esxHostname;
    }

    public String getEsxDatastore() {
        return esxDatastore;
    }

    public void setEsxDatastore(String esxDatastore) {
        this.esxDatastore = esxDatastore;
    }

    public String getEsxUsername() {
        return esxUsername;
    }

    public void setEsxUsername(String esxUsername) {
        this.esxUsername = esxUsername;
    }

    public String getEsxPassword() {
        return esxPassword;
    }

    public void setEsxPassword(String esxPassword) {
        this.esxPassword = esxPassword;
    }

    public String getDeploymentOption() {
        return deploymentOption;
    }

    public void setDeploymentOption(String deploymentOption) {
        this.deploymentOption = deploymentOption;
    }

    public String getDeploymentNetwork() {
        return deploymentNetwork;
    }

    public void setDeploymentNetwork(String deploymentNetwork) {
        this.deploymentNetwork = deploymentNetwork;
    }

    public String getApplianceName() {
        return applianceName;
    }

    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }

    public boolean getApplianceThinDiskMode() {
        return applianceThinDiskMode;
    }

    public void setApplianceThinDiskMode(boolean applianceThinDiskMode) {
        this.applianceThinDiskMode = applianceThinDiskMode;
    }

    public String getVcRootPassword() {
        return vcRootPassword;
    }

    public void setVcRootPassword(String vcRootPassword) {
        this.vcRootPassword = vcRootPassword;
    }

    public boolean getVcSshEnabled() {
        return vcSshEnabled;
    }

    public void setVcSshEnabled(boolean vcSshEnabled) {
        this.vcSshEnabled = vcSshEnabled;
    }

    public String getVcSsoPassword() {
        return vcSsoPassword;
    }

    public void setVcSsoPassword(String vcSsoPassword) {
        this.vcSsoPassword = vcSsoPassword;
    }

    public String getVcSsoDomainName() {
        return vcSsoDomainName;
    }

    public void setVcSsoDomainName(String vcSsoDomainName) {
        this.vcSsoDomainName = vcSsoDomainName;
    }

    public String getVcSsoSiteName() {
        return vcSsoSiteName;
    }

    public void setVcSsoSiteName(String vcSsoSiteName) {
        this.vcSsoSiteName = vcSsoSiteName;
    }

    public String getVcNWIpFamily() {
        return vcNWIpFamily;
    }

    public void setVcNWIpFamily(String vcNWIpFamily) {
        this.vcNWIpFamily = vcNWIpFamily;
    }

    public String getVcNWMode() {
        return vcNWMode;
    }

    public void setVcNWMode(String vcNWMode) {
        this.vcNWMode = vcNWMode;
    }

    public String getVcNWIp() {
        return vcNWIp;
    }

    public void setVcNWIp(String vcNWIp) {
        this.vcNWIp = vcNWIp;
    }

    public String getVcNWPrefix() {
        return vcNWPrefix;
    }

    public void setVcNWPrefix(String vcNWPrefix) {
        this.vcNWPrefix = vcNWPrefix;
    }

    public String getVcNWGateway() {
        return vcNWGateway;
    }

    public void setVcNWGateway(String vcNWGateway) {
        this.vcNWGateway = vcNWGateway;
    }

    public String getVcNWDnsServer() {
        return vcNWDnsServer;
    }

    public void setVcNWDnsServer(String vcNWDnsServer) {
        this.vcNWDnsServer = vcNWDnsServer;
    }

    public String getVcNWHostName() {
        return vcNWHostName;
    }

    public void setVcNWSystemName(String vcNWHostName) {
        this.vcNWHostName = vcNWHostName;
    }

    public String getVcIsoPath() {
        return vcIsoPath;
    }

    public void setVcIsoPath(String vcIsoPath) {
        this.vcIsoPath = vcIsoPath;
    }

    public String getVcMntPath() {
        return vcMntPath;
    }

    public void setVcMntPath(String vcMntPath) {
        this.vcMntPath = vcMntPath;
    }

    public String getVcNWDnsAlternateServer() {
        return vcNWDnsAlternateServer;
    }

    public void setVcNWDnsAlternateServer(String vcNWDnsAlternateServer) {
        this.vcNWDnsAlternateServer = vcNWDnsAlternateServer;
    }

    public void saveToTemplate() {
        InputStream in = null;
        BufferedReader reader = null;
        FileWriter fw = null;
        try {
            in = new FileInputStream("/embedded_vCSA_on_ESXi.json");
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while (null != (line = reader.readLine())) {
                sb.append(line);
            }
            logger.debug(sb.toString());

            JSONObject jsonTemplate = new JSONObject(sb.toString());

            JSONObject applianceJson = jsonTemplate.getJSONObject("target.vcsa").getJSONObject("appliance");
            applianceJson.put("deployment.network", this.getDeploymentNetwork());
            applianceJson.put("name", this.getApplianceName());
            applianceJson.put("deployment.option", this.getDeploymentOption());
            applianceJson.put("thin.disk.mode", this.getApplianceThinDiskMode());

            JSONObject esxJson = jsonTemplate.getJSONObject("target.vcsa").getJSONObject("esxi");
            esxJson.put("hostname", this.getEsxHostname());
            esxJson.put("password", this.getEsxPassword());
            esxJson.put("datastore", this.getEsxDatastore());

            JSONObject networkJson = jsonTemplate.getJSONObject("target.vcsa").getJSONObject("network");
            networkJson.put("hostname", this.getVcNWHostName());
            networkJson.put("dns.servers", new String[]{this.getVcNWDnsServer(), this.getVcNWDnsAlternateServer()});
            networkJson.put("gateway", this.getVcNWGateway());
            networkJson.put("ip", this.getVcNWIp());
            networkJson.put("ip.family", this.getVcNWIpFamily());
            networkJson.put("mode", this.getVcNWMode());
            networkJson.put("prefix", this.getVcNWPrefix());

            JSONObject osJson = jsonTemplate.getJSONObject("target.vcsa").getJSONObject("os");
            osJson.put("password", this.getVcRootPassword());
            osJson.put("ssh.enable", this.getVcSshEnabled());

            JSONObject ssoJson = jsonTemplate.getJSONObject("target.vcsa").getJSONObject("sso");
            ssoJson.put("password", this.getVcSsoPassword());
            ssoJson.put("domain-name", this.getVcSsoDomainName());
            ssoJson.put("site-name", this.getVcSsoSiteName());

            logger.debug(jsonTemplate.toString());

            fw = new FileWriter("/tmp/vc.json");
            jsonTemplate.write(fw);
            fw.flush();

        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            throw new HciServerException("unable to read data from template", ex);
        } catch (JSONException ex) {
            logger.error(ex.getMessage(), ex);
            throw new HciServerException("Fail to read or write template file", ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new HciServerException("Fail to read or write template file", ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
            try {
                in.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
            try {
                fw.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
}
