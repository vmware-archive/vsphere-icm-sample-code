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

import com.vmware.sample.hci.connection.helpers.GetMOREF;

public abstract class VsphereInventory {
    protected final VsphereClient vsphereClient;
    protected final GetMOREF getMOREFs;

    VsphereInventory(VsphereClient hostClient) {
        this.vsphereClient = hostClient;
        getMOREFs = new GetMOREF(hostClient.getVimPort(), hostClient.getServiceContent());
    }

    public VsphereClient getVsphereClient() {
        return vsphereClient;
    }

    public GetMOREF getGetMOREFs() {
        return getMOREFs;
    }
}
