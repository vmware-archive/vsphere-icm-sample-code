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

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.SelectionSpec;

/**
 * A simple builder that creates ObjectSpec instance.
 */
public class ObjectSpecBuilder extends ObjectSpec {
    private void init() {
        if (selectSet == null) {
            selectSet = new ArrayList<>();
        }
    }

    public ObjectSpecBuilder obj(final ManagedObjectReference objectReference) {
        this.setObj(objectReference);
        return this;
    }

    public ObjectSpecBuilder skip(final Boolean skip) {
        this.setSkip(skip);
        return this;
    }

    public ObjectSpecBuilder selectSet(final SelectionSpec... selectionSpecs) {
        init();
        this.selectSet.addAll(Arrays.asList(selectionSpecs));
        return this;
    }
}
