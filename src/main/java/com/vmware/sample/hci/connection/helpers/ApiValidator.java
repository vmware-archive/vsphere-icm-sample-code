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

package com.vmware.sample.hci.connection.helpers;

import com.vmware.sample.hci.connection.Connection;

/**
 * Some samples make use of API only found on vCenter. Other samples
 * make use of API that only make sense when used with a Host. This
 * utility helps with determining if the proper connection has been made.
 */
public class ApiValidator extends BaseHelper {

    public static final String VCENTER_API_TYPE = "VirtualCenter";
    public static final String HOST_API_TYPE = "HostAgent";

    public ApiValidator(final Connection connection) {
        super(connection);
    }

    public static boolean assertVCenter(final Connection connection) {
        return new ApiValidator(connection).assertVCenter();
    }

    public static boolean assertHost(final Connection connection) {
        return new ApiValidator(connection).assertHost();
    }

    public String getApiType() {
        return connection.connect().getServiceContent().getAbout().getApiType();
    }

    public boolean assertVCenter() {
        return isOfType(getApiType(), VCENTER_API_TYPE);
    }

    public boolean assertHost() {
        return isOfType(getApiType(), HOST_API_TYPE);
    }

    private boolean isOfType(final String apiType, final String vcenterApiType) {
        final boolean same = vcenterApiType.equals(apiType);
        if (!same) {
            System.out.printf("This sample is currently connected to %s %n", apiType);
            System.out.printf("This sample should be used with %s %n", vcenterApiType);

        }
        return same;
    }
}
