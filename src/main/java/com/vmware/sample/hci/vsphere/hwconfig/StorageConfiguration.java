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

public class StorageConfiguration {
    private String canonicalName;
    private String deviceName;
    private String displayName;
    private long capacity;
    private boolean isSSD;
    private long block;
    private int blockSize;

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCapacity() {
        capacity = this.block * this.blockSize;
        float capacityInG = capacity / (1024 * 1024 * 1024);
        return String.format("%.2f", capacityInG);
    }

    public String getDriveType() {
        if (this.isSSD) {
            return "SSD";
        } else {
            return "HDD";
        }
    }

    public boolean isSSD() {
        return this.isSSD;
    }

    public void setSSD(boolean isSSD) {
        this.isSSD = isSSD;
    }

    public long getBlock() {
        return block;
    }

    public void setBlock(long block) {
        this.block = block;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public String toString() {
        String allAttributes = "";
        allAttributes = allAttributes + String.format("Storage DisplayName: %s\n", this.displayName);
        allAttributes = allAttributes + String.format("Storage CanonicalName: %s\n", this.canonicalName);
        allAttributes = allAttributes + String.format("Storage DeviceName: %s\n", this.deviceName);
        allAttributes = allAttributes + String.format("Drive Type: %s\n", this.getDriveType());
        allAttributes = allAttributes + String.format("Capacity: %s GB\n", this.getCapacity());
        return allAttributes;
    }

}
