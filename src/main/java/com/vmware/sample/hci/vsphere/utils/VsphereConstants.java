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

package com.vmware.sample.hci.vsphere.utils;

public class VsphereConstants {

    public static final String DC_MOR_TYPE = "Datacenter";
    public static final String FOLDER_MOR = "Folder";
    public static final String MANAGEDENTITY_TYPE = "ManagedEntity";
    public static final String COMPRES_MOR_TYPE = "ComputeResource";
    public static final String RESPOOL_MOR_TYPE = "ResourcePool";
    public static final String CLUSTER_COMPRES_MOR_TYPE = "ClusterComputeResource";
    public static final String STORAGE_POD_MOR_TYPE = "StoragePod";
    public static final String HOST_MOR_TYPE = "HostSystem";
    public static final String VIRTUAL_APP_MOR_TYPE = "VirtualApp";
    public static final String DVS_MOR_TYPE = "VmwareDistributedVirtualSwitch";
    public static final String DV_PORT_GROUP_MOR_TYPE = "DistributedVirtualPortgroup";

    public static final String DATACENTER_HOSTFOLDER_PROPERTYNAME = "hostFolder";
    public static final String DATACENTER_VMFOLDER_PROPERTYNAME = "vmFolder";
    public static final String DATACENTER_DATASTOREFOLDER_PROPERTYNAME = "datastoreFolder";
    public static final String DATACENTER_NETWORKFOLDER_PROPERTYNAME = "networkFolder";
    public static final String MANAGEDENTITY_CHILDENTITY_PROPERTYNAME = "childEntity";
    public static final String COMPRES_HOST_PROPERTYNAME = "host";
    public static final String RESPOOL_RESOURCEPOOL_PROPERTYNAME = "resourcePool";
    public static final String COMPRES_RESOURCEPOOL_PROPERTYNAME = "resourcePool";
    public static final String RESPOOL_VM_PROPERTYNAME = "vm";
    public static final String HOST_VM_PROPERTYNAME = "vm";
    public static final String VAPP_VM_PROPERTYNAME = "vm";
    public static final String VAPP_VIRTUALAPP_PROPERTYNAME = "resourcePool";
    public static final String MANAGEDENTITY_NAME_PROPERTYNAME = "name";
    public static final String MANAGEDENTITY_PARENT_PROPERTYNAME =
            "parent";
    public static final String HOST_CONFIGMANAGER_PROPERTYNAME = "configManager";
    public static final String DSWITCH_UUID = "uuid";
    public static final String PORTGROUP_KEY = "key";
    public static final String VSAN_HOST_CONFIGINFO_PROPERTYNAME = "config";
    public static final String VDVS_CONFIGINFO_PROPERTYNAME = "config";
    public static final String VIRTUAL_NIC_MANAGER_INFO = "info";
    public static final String NWSYSTEM_NWINFO = "networkInfo";

    public static final String DEFAULT_VDS_VERSTION_2015 = "6.0.0";
    public static final int MAX_RETRY = 3;

    //error code
    public static final int ADDHOST_TASK_FAILURE = 1;
    public static final int CREATEPORTGROUP_TASK_FAILURE = 2;
    public static final int CREATEVDS_TASK_FAILURE = 3;
    public static final int DELETEDCOJBECT_TASK_FAILURE = 4;
    public static final int SETDRS_TASK_FAILURE = 5;
    public static final int MIGRATEVMTOVDS_TASK_FAILURE = 6;
    public static final int VDSRECONFIG_TASK_FAILURE = 7;
    public static final int CREATEVIRTUALNIC_TASK_FAILURE = 8;
    public static final int SELECTSERVICE_TASK_FAILURE = 9;
    public static final int CREATEDATACENTER_TASK_FAILURE = 10;
    public static final int CREATECLUSTER_TASK_FAILURE = 11;
    public static final int UPDATEVSAN_TASK_FAILURE = 12;
    public static final int SELECTMANAGEMENTSERVICE_TASK_FAILURE = 13;
    public static final int SELECTVMOTIONSERVICE_TASK_FAILURE = 14;
    public static final int ENABLEVSAN_TASK_FAILURE = 15;
    public static final int SETDAS_TASK_FAILURE = 16;

    //
    public static final int LOG_DEFAULT = 0;
    public static final int LOG_INFO = 1;
    public static final int LOG_ERROR = 2;

    public static final String EVALUATION_LICENSE = "00000-00000-00000-00000-00000";

    public static final String DEFAULT_DATACENTER_NAME = "Datacenter1";
    public static final String DEFAULT_CLUSTER_NAME = "Cluster1";
    public static final String DEFAULT_VDS_NAME = "DVSwitch1";
    public static final String DEFAULT_MGMT_PORTGROUPNAME = "mgmtPortgroup";
    public static final String DEFAULT_VMOTION_PORTGROUPNAME = "vmotionPortgroup";
    public static final String DEFAULT_VSAN_PORTGROUPNAME = "vsanPortgroup";
    public static final String DEFAULT_VM_PORTGROUPNAME = "vmPortgroup";
}
