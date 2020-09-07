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

package com.vmware.sample.hci.connection.helpers.builders;

import com.vmware.vim25.SelectionSpec;

/**
 * A simple builder that creates SelectionSpecBuilder instance.
 */
public class SelectionSpecBuilder extends SelectionSpec {
    public SelectionSpecBuilder name(final String name) {
        this.setName(name);
        return this;
    }
}
