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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.base.InputBase;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;

public class InputVcConfiguration extends InputBase {

    private static final Logger logger =
            LoggerFactory.getLogger(InputVcConfiguration.class);

    private VcInfo vcInfo;
    private String dataCenterName = VsphereConstants.DEFAULT_DATACENTER_NAME;
    private String clusterName = VsphereConstants.DEFAULT_CLUSTER_NAME;
    private String vdsName = VsphereConstants.DEFAULT_VDS_NAME;
    private InputPortgroupConfiguration portGroupInfo;

    private List<String> errorInputList = new ArrayList<String>();

    public VcInfo getVcInfo() {
        return vcInfo;
    }

    public void setVcInfo(VcInfo vcInfo) {
        this.vcInfo = vcInfo;
    }

    public String getDataCenterName() {
        return dataCenterName;
    }

    public void setDataCenterName(String dataCenterName) {
        this.dataCenterName = dataCenterName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getVdsName() {
        return vdsName;
    }

    public void setVdsName(String vdsName) {
        this.vdsName = vdsName;
    }

    public InputPortgroupConfiguration getPortGroupInfo() {
        return portGroupInfo;
    }

    public void setPortGroupInfo(InputPortgroupConfiguration portGroupInfo) {
        this.portGroupInfo = portGroupInfo;
    }

    @Override
    public List<String> checkValid() {
        try {
            if (null == this.vcInfo) {
                errorInputList.add("VC connection information is not provided.");
            } else {
                if (StringUtils.isBlank(this.vcInfo.getIpAddress())) {
                    errorInputList.add("VC ip is not provided");
                }
                if (StringUtils.isBlank(this.vcInfo.getUserName())) {
                    errorInputList.add("User name of VC is not provided");
                }
                if (StringUtils.isBlank(this.vcInfo.getPassword())) {
                    errorInputList.add("Password of VC is not provided");
                }
            }

            if (StringUtils.isBlank(this.dataCenterName)) {
                logger.warn("Datacenter name is not provided. '{}' will be used",
                        VsphereConstants.DEFAULT_DATACENTER_NAME);
                this.dataCenterName = VsphereConstants.DEFAULT_DATACENTER_NAME;
            }

            if (StringUtils.isBlank(this.clusterName)) {
                logger.warn("Cluster name is not provided. '{}' will be used",
                        VsphereConstants.DEFAULT_CLUSTER_NAME);
                this.clusterName = VsphereConstants.DEFAULT_CLUSTER_NAME;
            }

            if (StringUtils.isBlank(this.vdsName)) {
                logger.warn("Distributed switch name is not provided. '{}' will be used",
                        VsphereConstants.DEFAULT_VDS_NAME);
                this.vdsName = VsphereConstants.DEFAULT_VDS_NAME;
            }

            if (null == this.portGroupInfo) {
                logger.warn("Port group information is not provided. Default values"
                        + " will be used. No vlan will be configured");
            } else {
                this.errorInputList.addAll(this.portGroupInfo.checkValid());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            errorInputList.add("Internal error " + ex.getMessage() + " while checking valid");
        }
        return this.errorInputList;
    }
}
