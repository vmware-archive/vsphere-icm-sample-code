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

import java.util.ArrayList;
import java.util.Arrays;

import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;

/**
 * A simple builder that creates PropertyFilterSpec instance.
 */
public class PropertyFilterSpecBuilder extends PropertyFilterSpec {
    private void init() {
        if (propSet == null) {
            propSet = new ArrayList<>();
        }
        if (objectSet == null) {
            objectSet = new ArrayList<>();
        }
    }

    public PropertyFilterSpecBuilder reportMissingObjectsInResults(final Boolean value) {
        this.setReportMissingObjectsInResults(value);
        return this;
    }

    public PropertyFilterSpecBuilder propSet(final PropertySpec... propertySpecs) {
        init();
        this.propSet.addAll(Arrays.asList(propertySpecs));
        return this;
    }

    public PropertyFilterSpecBuilder objectSet(final ObjectSpec... objectSpecs) {
        init();
        this.objectSet.addAll(Arrays.asList(objectSpecs));
        return this;
    }
}
