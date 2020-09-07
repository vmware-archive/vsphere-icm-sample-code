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

public abstract class BaseHelper {
    final Connection connection;

    public BaseHelper(final Connection connection) {
        try {
            this.connection = connection.connect();
        } catch (Throwable t) {
            throw new HelperException(t);
        }
    }

    public class HelperException extends RuntimeException {
        public HelperException(Throwable cause) {
            super(cause);
        }
    }
}
