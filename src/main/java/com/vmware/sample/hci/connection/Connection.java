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

package com.vmware.sample.hci.connection;

import java.net.URL;
import java.util.Map;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;


/**
 * Connection concept that connects to vCenter or ESXi.
 */
public interface Connection {

    // getters and setter
    String getUrl();

    void setUrl(String url);

    String getHost();

    Integer getPort();

    String getUsername();

    void setUsername(String username);

    String getPassword();

    void setPassword(String password);

    VimService getVimService();

    VimPortType getVimPort();

    ServiceContent getServiceContent();

    UserSession getUserSession();

    String getServiceInstanceName();

    Map getHeaders();

    URL getURL();

    @SuppressWarnings("rawtypes")

    ManagedObjectReference getServiceInstanceReference();

    Connection connect();

    boolean isConnected();

    Connection disconnect();
}
