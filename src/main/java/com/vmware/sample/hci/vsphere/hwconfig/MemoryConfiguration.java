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

public class MemoryConfiguration {
    private long size;

    public String getSizeinMB() {
        float sizeInMB = this.size / (1024 * 1024);
        return String.format("%.1f", sizeInMB);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long memorySize) {
        this.size = memorySize;
    }

    @Override
    public String toString() {
        return String.format("Total Memory %s MB\n", this.getSizeinMB());
    }
}
