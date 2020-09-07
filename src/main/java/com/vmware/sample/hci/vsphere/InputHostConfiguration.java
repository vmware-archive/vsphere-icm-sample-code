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

package com.vmware.sample.hci.vsphere;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.base.InputBase;

public class InputHostConfiguration extends InputBase {

    private static final Logger logger =
            LoggerFactory.getLogger(InputHostConfiguration.class);

    private HostInfo defaultHostInfo;
    private HostInfo customizedHostInfo;
    private boolean mgmtDHCP = false;
    private String mgmtIp;
    private String mgmtNetmask;
    private boolean vMotionDHCP = false;
    private String vMotionIp;
    private String vMotionNetmask;
    private boolean vSanDHCP = false;
    private String vSanIp;
    private String vSanNetmask;

    public HostInfo getDefaultHostInfo() {
        return defaultHostInfo;
    }

    public void setDefaultHostInfo(HostInfo defaultHostInfo) {
        this.defaultHostInfo = defaultHostInfo;
    }

    public HostInfo getCustomizedHostInfo() {
        return customizedHostInfo;
    }

    public void setCustomizedHostInfo(HostInfo customizedHostInfo) {
        this.customizedHostInfo = customizedHostInfo;
    }

    public boolean isMgmtDHCP() {
        return mgmtDHCP;
    }

    public void setMgmtDHCP(boolean mgmtDHCP) {
        this.mgmtDHCP = mgmtDHCP;
    }

    public String getMgmtIp() {
        return mgmtIp;
    }

    public void setMgmtIp(String mgmtIp) {
        this.mgmtIp = mgmtIp;
    }

    public String getMgmtNetmask() {
        return mgmtNetmask;
    }

    public void setMgmtNetmask(String mgmtNetmask) {
        this.mgmtNetmask = mgmtNetmask;
    }

    public boolean isvMotionDHCP() {
        return vMotionDHCP;
    }

    public void setvMotionDHCP(boolean vMotionDHCP) {
        this.vMotionDHCP = vMotionDHCP;
    }

    public String getvMotionIp() {
        return vMotionIp;
    }

    public void setvMotionIp(String vMotionIp) {
        this.vMotionIp = vMotionIp;
    }

    public String getvMotionNetmask() {
        return vMotionNetmask;
    }

    public void setvMotionNetmask(String vMotionNetmask) {
        this.vMotionNetmask = vMotionNetmask;
    }

    public boolean isvSanDHCP() {
        return vSanDHCP;
    }

    public void setvSanDHCP(boolean vSanDHCP) {
        this.vSanDHCP = vSanDHCP;
    }

    public String getvSanIp() {
        return vSanIp;
    }

    public void setvSanIp(String vSanIp) {
        this.vSanIp = vSanIp;
    }

    public String getvSanNetmask() {
        return vSanNetmask;
    }

    public void setvSanNetmask(String vSanNetmask) {
        this.vSanNetmask = vSanNetmask;
    }

    @Override
    public List<String> checkValid() {
        try {
            if (null == this.defaultHostInfo) {
                errorInputList.add("Default host information is not provided.");
            } else {
                HostManager hostMgr = null;
                try {
                    hostMgr = new HostManager(this.defaultHostInfo);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    errorInputList.add("Could not connect to host " + this.defaultHostInfo.getIpAddress());
                } finally {
                    if (null != hostMgr) {
                        try {
                            hostMgr.disconnect();
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                }
            }
            if (null == this.customizedHostInfo) {
                errorInputList.add("Customized host information is not provided.");
            } else {
                if (StringUtils.isBlank(this.customizedHostInfo.getIpAddress())) {
                    errorInputList.add("Customized host ip is not provided");
                }
                if (StringUtils.isBlank(this.customizedHostInfo.getHostName())) {
                    errorInputList.add("Customized host name is not provided");
                }
                if (StringUtils.isBlank(this.customizedHostInfo.getUserName())) {
                    errorInputList.add("User name of customized host is not provided");
                }
                if (StringUtils.isBlank(this.customizedHostInfo.getPassword())) {
                    errorInputList.add("Password of customized host is not provided");
                }
            }
            if (!this.mgmtDHCP) {
                if (StringUtils.isBlank(this.mgmtIp)) {
                    errorInputList.add("Management ip is not provided while DHCP is disabled");
                }
                if (StringUtils.isBlank(this.mgmtNetmask)) {
                    errorInputList.add("Management netmask is not provided while DHCP is disabled");
                }
            }
            if (!this.vMotionDHCP) {
                if (StringUtils.isBlank(this.vMotionIp)) {
                    errorInputList.add("vMotion ip is not provided while DHCP is disabled");
                }
                if (StringUtils.isBlank(this.vMotionNetmask)) {
                    errorInputList.add("vMotion netmask is not provided while DHCP is disabled");
                }
            }
            if (!this.vSanDHCP) {
                if (StringUtils.isBlank(this.vSanIp)) {
                    errorInputList.add("vSan ip is not provided while DHCP is disabled");
                }
                if (StringUtils.isBlank(this.vSanNetmask)) {
                    errorInputList.add("vSan netmask is not provided while DHCP is disabled");
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            errorInputList.add("Internal error " + ex.getMessage() + " while checking valid");
        }
        return this.errorInputList;
    }
}
