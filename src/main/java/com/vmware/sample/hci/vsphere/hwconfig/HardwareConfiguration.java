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

package com.vmware.sample.hci.vsphere.hwconfig;

import java.util.ArrayList;
import java.util.List;

public class HardwareConfiguration {
    private List<ProcessorConfiguration> lstProcessCfg;
    private MemoryConfiguration memCfg;
    private List<StorageConfiguration> lstStorageCfg;
    private List<NetworkAdapterConfiguration> lstNetAdapterCfg;

    public HardwareConfiguration() {
        this.lstProcessCfg = new ArrayList<ProcessorConfiguration>();
        this.lstStorageCfg = new ArrayList<StorageConfiguration>();
        this.lstNetAdapterCfg = new ArrayList<NetworkAdapterConfiguration>();
    }

    @Override
    public String toString() {
        String allAttributes = "";
        for (ProcessorConfiguration processCfg : this.lstProcessCfg) {
            allAttributes = allAttributes + processCfg;
        }
        allAttributes = allAttributes + this.memCfg;
        for (StorageConfiguration storageCfg : this.lstStorageCfg) {
            allAttributes = allAttributes + storageCfg;
        }
        for (NetworkAdapterConfiguration netAdapterCfg : this.lstNetAdapterCfg) {
            allAttributes = allAttributes + netAdapterCfg;
        }
        return allAttributes;
    }

    public List<ProcessorConfiguration> getLstProcessCfg() {
        return lstProcessCfg;
    }

    public List<StorageConfiguration> getLstStorageCfg() {
        return lstStorageCfg;
    }

    public List<NetworkAdapterConfiguration> getLstNetAdapterCfg() {
        return lstNetAdapterCfg;
    }

    public MemoryConfiguration getMemCfg() {
        return memCfg;
    }

    public void setMemCfg(MemoryConfiguration memCfg) {
        this.memCfg = memCfg;
    }
}
