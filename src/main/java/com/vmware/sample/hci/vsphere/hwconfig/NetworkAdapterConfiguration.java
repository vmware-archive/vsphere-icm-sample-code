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

public class NetworkAdapterConfiguration {

    private String deviceName;
    private String nicName;
    private String driverName;
    private boolean isDuplex;
    private int speedMb;
    private String vendorName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getNicName() {
        return nicName;
    }

    public void setNicName(String nicName) {
        this.nicName = nicName;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDuplexStr() {
        if (this.isDuplex) {
            return "Yes";
        } else {
            return "No";
        }
    }

    public boolean isDuplex() {
        return this.isDuplex;
    }

    public void setDuplex(boolean isDuplex) {
        this.isDuplex = isDuplex;
    }

    public int getSpeedMb() {
        return speedMb;
    }

    public void setSpeedMb(int speedMb) {
        this.speedMb = speedMb;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String toString() {
        String allAttributes = "";
        allAttributes = allAttributes + String.format("Network NicName: %s\n", this.nicName);
        allAttributes = allAttributes + String.format("Network Speed: %d Mb\n", this.speedMb);
        allAttributes = allAttributes + String.format("Network Duplex: %s\n", this.getDuplexStr());
        allAttributes = allAttributes + String.format("Network DeviceName: %s\n", this.deviceName);
        allAttributes = allAttributes + String.format("Network DriverName: %s\n", this.driverName);
        allAttributes = allAttributes + String.format("Network VendorName: %s\n", this.vendorName);
        return allAttributes;
    }

}
