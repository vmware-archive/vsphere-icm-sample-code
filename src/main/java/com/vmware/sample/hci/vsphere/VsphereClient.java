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

import java.util.Date;
import java.util.Map;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.connection.DisableSecurity;
import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

public class VsphereClient {
    private static final Logger logger =
            LoggerFactory.getLogger(VsphereClient.class);

    private VimService service;
    private VimPortType vimPort;
    private ServiceContent serviceContent;
    private UserSession userSession;

    public VsphereClient(String ipAddress, String userName, String password) {
        String url = String.format("https://%s/sdk/vimService/", ipAddress);

        this.service = new VimService();
        this.vimPort = service.getVimPort();

        Map<String, Object> ctxt =
                ((BindingProvider) vimPort).getRequestContext();
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        ManagedObjectReference serviceInstance = new ManagedObjectReference();
        serviceInstance.setType("ServiceInstance");
        serviceInstance.setValue("ServiceInstance");

        try {
            // Disable security check for development only
            // Need to change this part of code for production env
            DisableSecurity.trustEveryone();

            serviceContent = vimPort.retrieveServiceContent(serviceInstance);
            userSession = vimPort.login(serviceContent.getSessionManager(),
                    userName, password, null);
        } catch (Exception e) {
            logger.error("could not setup vsphere connection to {}", ipAddress);
            logger.error("connection error", e);
            throw new HciServerException("connection error", e);
        }
    }

    public VsphereClient(HostInfo esxInfo) {
        this(esxInfo.ipAddress, esxInfo.userName, esxInfo.password);
    }

    public VsphereClient(VcInfo vcInfo) {
        this(vcInfo.getIpAddress(), vcInfo.getUserName(), vcInfo.getPassword());
    }

    public VimService getService() {
        return service;
    }

    public VimPortType getVimPort() {
        return vimPort;
    }

    public ServiceContent getServiceContent() {
        return serviceContent;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public boolean isConnected() {
        if (userSession == null) {
            return false;
        }
        long startTime = userSession.getLastActiveTime().toGregorianCalendar()
                .getTime().getTime();

        return new Date().getTime() < startTime + 30 * 60 * 1000;
    }

    public VsphereClient disconnect() {
        if (this.isConnected()) {
            try {
                vimPort.logout(serviceContent.getSessionManager());
            } catch (RuntimeFaultFaultMsg e) {
                e.printStackTrace();
            } finally {
                service = null;
                vimPort = null;
                serviceContent = null;
                userSession = null;
            }
        }
        return this;
    }
}
