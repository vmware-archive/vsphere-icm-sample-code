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

import com.vmware.sample.hci.vsphere.exception.VerificationFailedException;
import com.vmware.vim25.HostSystemConnectionState;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.PhysicalNic;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VerifyHost {
    private static final Logger logger = LoggerFactory.getLogger(VerifyHost.class);

    //should change it to 6.0.0 for product env
    private final String hostVersion = "EXPECTED_HOST_VERSION";
    //should change it to 10000 for product env
    private final int linkSpeed = 1000;
    HostManager hostManager;

    public VerifyHost(HostManager hostManager) {
        this.hostManager = hostManager;
    }

    //Host connection check
    private void verifyHostConnection() {
        try {
            if (!isHostAccessible()) {
                throw new VerificationFailedException(
                        String.format("Host %s is unaccessible", hostManager.getHostInfo().getHostName()));
            }
        } catch (Exception e) {
            throw new VerificationFailedException(e.getMessage(), e);
        }
    }

    //Hosts runtime connection state is Connected
    private boolean isHostAccessible() throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return hostManager.getHostConnectionState() == HostSystemConnectionState.CONNECTED;
    }

    //ESXi Version check, should be 6.0
    private void verifyESXVersion() {
        String version = null;
        try {
            version = hostManager.getHostVersion();
            if (hostVersion.equals(version)) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new VerificationFailedException("Host version mismatch");
    }

    //Two 10G pnics, one is attached to standard switch, another one is available
    private void verifyHostNetwork() {
        try {
            List<PhysicalNic> pNicList = hostManager.getHostPhysicalNic();
            for (PhysicalNic pNic : pNicList) {
                if (pNic.getLinkSpeed() != null) {
                    if (pNic.getLinkSpeed().getSpeedMb() != linkSpeed) {
                        throw new VerificationFailedException("Host linkspeed is not 10Gbps ");
                    }
                    System.out.println(String.format("Port %s speed is %d Mbps",
                            pNic.getDevice(), pNic.getLinkSpeed().getSpeedMb()));
                } else {
                    System.out.println(String.format("Port %s is LinkDown",
                            pNic.getDevice()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new VerificationFailedException("Host network verification failed");
        }
    }

    private void verifyCleanUp() {
        hostManager.getVsphereClient().disconnect();
    }

    public void verify() {
        //Hosts connection check
        //Hosts runtime connection state is CONNECTED
        verifyHostConnection();

        verifyESXVersion();

        //Network check
        //Two 10G vmnics, one is attached to standard switch, another one is available
        verifyHostNetwork();

        //clean up connection
        verifyCleanUp();
    }
}
