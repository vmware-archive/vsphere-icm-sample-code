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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.base.InputBase;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;
import com.vmware.sample.hci.vsphere.utils.VsphereUtil;

public class InputPortgroupConfiguration extends InputBase {

    public static final int PORTGROUP_NUM = 4;
    private static final Logger logger =
            LoggerFactory.getLogger(InputPortgroupConfiguration.class);
    private String mgmtPortgroupName = VsphereConstants.DEFAULT_MGMT_PORTGROUPNAME;
    private String mgmtVlanTrunk;

    private String vMotionPortgroupName = VsphereConstants.DEFAULT_VMOTION_PORTGROUPNAME;
    private String vMotionVlanTrunk;

    private String vSanPortgroupName = VsphereConstants.DEFAULT_VSAN_PORTGROUPNAME;
    private String vSanVlanTrunk;

    private String vmPortgroupName = VsphereConstants.DEFAULT_VM_PORTGROUPNAME;
    private String vmVlanTrunk;

    public String getMgmtVlanTrunk() {
        return mgmtVlanTrunk;
    }

    public void setMgmtVlanTrunk(String mgmtVlanTrunk) {
        this.mgmtVlanTrunk = mgmtVlanTrunk;
    }

    public String getvMotionVlanTrunk() {
        return vMotionVlanTrunk;
    }

    public void setvMotionVlanTrunk(String vMotionVlanTrunk) {
        this.vMotionVlanTrunk = vMotionVlanTrunk;
    }

    public String getvSanVlanTrunk() {
        return vSanVlanTrunk;
    }

    public void setvSanVlanTrunk(String vSanVlanTrunk) {
        this.vSanVlanTrunk = vSanVlanTrunk;
    }

    public String getVmVlanTrunk() {
        return vmVlanTrunk;
    }

    public void setVmVlanTrunk(String vmVlanTrunk) {
        this.vmVlanTrunk = vmVlanTrunk;
    }

    @Override
    public List<String> checkValid() {
        try {
            VsphereUtil.parseVlanId(this.mgmtVlanTrunk);
        } catch (Exception ex) {
            errorInputList.add("Management vlan trunk string should be like '206, 207, 208-210'. "
                    + this.mgmtVlanTrunk + " is invalid");
        }
        try {
            VsphereUtil.parseVlanId(this.vMotionVlanTrunk);
        } catch (Exception ex) {
            errorInputList.add("vMotion vlan trunk string should be like '206, 207, 208-210'. "
                    + this.vMotionVlanTrunk + " is invalid");
        }
        try {
            VsphereUtil.parseVlanId(this.vSanVlanTrunk);
        } catch (Exception ex) {
            errorInputList.add("vSan vlan trunk string should be like '206, 207, 208-210'. "
                    + this.vSanVlanTrunk + " is invalid");
        }
        try {
            VsphereUtil.parseVlanId(this.vmVlanTrunk);
        } catch (Exception ex) {
            errorInputList.add("VM vlan trunk string should be like '206, 207, 208-210'. "
                    + this.vmVlanTrunk + " is invalid");
        }
        return this.errorInputList;
    }

    public String getMgmtPortgroupName() {
        return mgmtPortgroupName;
    }

    public String getvMotionPortgroupName() {
        return vMotionPortgroupName;
    }

    public String getvSanPortgroupName() {
        return vSanPortgroupName;
    }

    public String getVmPortgroupName() {
        return vmPortgroupName;
    }
}
