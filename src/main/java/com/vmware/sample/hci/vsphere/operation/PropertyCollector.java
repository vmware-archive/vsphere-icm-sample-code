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

package com.vmware.sample.hci.vsphere.operation;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.WaitOptions;

public class PropertyCollector {
    public static final String TASK_INFO_PROPERTYNAME = "info";
    public static final String DATACENTER_HOSTFOLDER_PROPERTYNAME = "hostFolder";
    public static final String DATACENTER_NETWORKFOLDER_PROPERTYNAME = "networkFolder";
    public static final String MANAGEDENTITY_NAME_PROPERTYNAME = "name";
    private static final Logger logger = LoggerFactory.getLogger(PropertyCollector.class);
    private VsphereClient connection;
    private ManagedObjectReference propertyCollectorMor;

    /**
     * Construction
     *
     * @param vcConnection
     */
    public PropertyCollector(VsphereClient vcConnection) {
        connection = vcConnection;
        propertyCollectorMor =
                connection.getServiceContent().getPropertyCollector();
    }

    /**
     * Get DynamicProperty Value of ManagedObjectReference object matching the
     * given property name
     *
     * @param mor          ManagedObjectReference object
     * @param propertyName Property name of ManagedObjectReference object
     * @return Property content value
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     * @throws MethodFault,            Exception
     */
    public static Object getDynamicProperty(VsphereClient vcConnection,
                                            ManagedObjectReference propertyCollectorMor,
                                            ManagedObjectReference mor, String propertyName)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        if (mor == null) {
            logger.error("Specified MOR object is null", propertyName);
            throw new RuntimeException(
                    "Could not get dynamic property for null mor.");
        }

        Object propertyValue = null;

        PropertySpec propSpec = new PropertySpec();
        propSpec.setAll(Boolean.FALSE);
        propSpec.getPathSet().add(propertyName);
        propSpec.setType(mor.getType());

        ObjectSpec objSpec = new ObjectSpec();
        objSpec.setObj(mor);
        objSpec.setSkip(Boolean.FALSE);

        PropertyFilterSpec[] pfs = new PropertyFilterSpec[1];
        pfs[0] = new PropertyFilterSpec();
        pfs[0].getObjectSet().add(objSpec);
        pfs[0].getPropSet().add(propSpec);

        ObjectContent[] objContentArr =
                retrieveProperties(vcConnection, propertyCollectorMor, pfs);

        if (objContentArr != null) {
            for (ObjectContent objContent : objContentArr) {
                List<DynamicProperty> dynamicProperty = objContent.getPropSet();
                if (dynamicProperty != null && dynamicProperty.size() > 0) {
                    if (dynamicProperty.get(0).getName() == null) {
                        throw new RuntimeException("The name in propSet of the "
                                + "ObjectContent found null when queried for '"
                                + propertyName + "'");
                    }
                    propertyValue = dynamicProperty.get(0).getVal();
                } else {
                    if (propertyName != null) {
                        logger.debug("The propSet of the ObjectContent "
                                + "found null when queried for '" + propertyName
                                + "'");
                    }
                }
            }
        }

        // If the property is an array, convert to array and return
        if ((propertyValue != null)
                && (propertyValue instanceof ArrayOfManagedObjectReference)) {
            logger.debug(
                    "get dynamicProperty as ArrayOfManagedObjectReference, convert to array");
            ArrayOfManagedObjectReference aom =
                    (ArrayOfManagedObjectReference) propertyValue;
            return aom.getManagedObjectReference()
                    .toArray(new ManagedObjectReference[aom
                            .getManagedObjectReference().size()]);
        }

        // Enums directly from PropertyCollector has issues in JAX-WS as they
        // come directly as Element.
        // This is the workaround for getting enum values. returning as String
        // values for now.
        if (propertyValue instanceof Element) {
            Element element = (Element) propertyValue;
            propertyValue = element.getFirstChild().getTextContent();
        }

        return propertyValue;
    }

    /**
     * Retrieve child MOR Objects from given MOR node and filter using given
     * property specification
     *
     * @param mor ManagedObjectReference object
     * @param pfs Array of PropertyFilter Spec
     * @return MOR objects for matching property spec null, for no match
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     * @throws MethodFault,            Exception
     */
    public static ObjectContent[] retrieveProperties(VsphereClient vcConnection,
                                                     ManagedObjectReference mor, PropertyFilterSpec pfs[])
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        List<ObjectContent> ObjectContentList =
                vcConnection.getVimPort().retrieveProperties(mor,
                        new Vector<PropertyFilterSpec>(Arrays.asList(pfs)));

        ObjectContent[] properties =
                ObjectContentList.toArray(new ObjectContent[]{});

        return properties;
    }

    public Object getDynamicProperty(ManagedObjectReference mor,
                                     String propertyName)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return getDynamicProperty(connection, propertyCollectorMor, mor,
                propertyName);
    }

    /**
     * Create ObjectFilter to receive updates.
     *
     * @param mor          MOR object on which updates will be received
     * @param propertyName List of property interested to receive updates
     * @return Return Filter MOR Object
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     * @throws MethodFault,            Exception
     */
    public ManagedObjectReference createFilter(ManagedObjectReference mor,
                                               String propertyName)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Vector<String> property = new Vector<String>();
        property.add(propertyName);

        PropertySpec[] propSpec = new PropertySpec[1];
        propSpec[0] = new PropertySpec();

        propSpec[0].setAll(Boolean.FALSE);

        propSpec[0].getPathSet().clear();
        propSpec[0].getPathSet().addAll(property);
        propSpec[0].setType(mor.getType());

        Vector<ObjectSpec> objSpecList = new Vector<ObjectSpec>();
        ObjectSpec objSpec = new ObjectSpec();
        objSpec.setObj(mor);
        objSpec.setSkip(Boolean.FALSE);
        objSpecList.add(objSpec);

        PropertyFilterSpec pfs = new PropertyFilterSpec();
        pfs.getObjectSet().clear();
        pfs.getObjectSet().addAll(objSpecList);

        pfs.getPropSet().clear();
        pfs.getPropSet()
                .addAll(new Vector<PropertySpec>(Arrays.asList(propSpec)));

        boolean partialUpdates = false;
        return connection.getVimPort().createFilter(this.propertyCollectorMor,
                pfs, partialUpdates);
    }

    /**
     * WaitForUpdatesEx to happen. It's synchronous block call. Method will wait
     * until any updates happens on create filter objects.
     *
     * @param version Updates Version. Empty version string will gives current state
     *                of the filter objects. Valid version string will give
     *                incremental updates since last udpates returned.
     * @return UpdateSet object
     * @throws MethodFault, Exception
     */
    public UpdateSet waitForUpdatesEx(String version, WaitOptions options)
            throws Exception {

        UpdateSet updateset = connection.getVimPort()
                .waitForUpdatesEx(this.propertyCollectorMor, version, options);

        return updateset;
    }

    /**
     * Destroy PropertyFilter MOR object It's synchronous operation. Method will
     * print the updates and monitor the task for completion. On task
     * completion, method will return control to client
     *
     * @param filterMor PropertyFilter MOR object
     * @return true, if successfully destroyed MOR object false, if MOR object
     * not destroyed
     * @throws RuntimeFaultFaultMsg
     * @throws MethodFault,         Exception
     */
    public boolean destroyPropertyFilter(ManagedObjectReference filterMor)
            throws RuntimeFaultFaultMsg {
        connection.getVimPort().destroyPropertyFilter(filterMor);
        return true;
    }

    /**
     * Get Recent Task for a managed entity
     *
     * @param managedEntityMor
     * @return List of active tasks, null if no active tasks
     */
    public Vector<ManagedObjectReference> getRecentActiveTask(
            ManagedObjectReference managedEntityMor) {
        Task taskOps = new Task(connection);
        ManagedObjectReference recentTaskMor[] = null;
        Vector<ManagedObjectReference> recentTaskList = null;
        try {
            recentTaskMor = (ManagedObjectReference[]) getDynamicProperty(
                    managedEntityMor, "recentTask");
            if (recentTaskMor != null) {
                recentTaskList = new Vector<>(Arrays.asList(recentTaskMor));
            }
        } catch (Exception ex) {
            logger.error("Could not find the recent task for the given entity.",
                    ex);
        }
        /*
         * We extracted all recent tasks, but only print active tasks'
         * information
         */
        for (int i = 0; i < recentTaskList.size(); i++) {
            ManagedObjectReference task = recentTaskList.elementAt(i);
            TaskInfo taskInfo = taskOps.getTaskInfo(task);
            if (taskInfo != null) {
                String entityName = null;
                try {
                    entityName = getName(managedEntityMor);
                } catch (Exception ex) {
                    logger.error(
                            "Could not retrieve the name for the given entity.",
                            ex);
                }
                if (entityName != null) {
                    if (taskInfo.getState().equals(TaskInfoState.QUEUED)
                            || taskInfo.getState()
                            .equals(TaskInfoState.RUNNING)) {
                        logger.info("Entity = " + entityName
                                + " | Active task name = " + taskInfo.getName()
                                + " | Active task description = "
                                + taskInfo.getDescriptionId());
                    }
                }
            } else {
                // Remove the task from the List if taskInfo is empty
                recentTaskList.remove(task);
            }
        }
        return recentTaskList;
    }

    /**
     * Method to get the Name of the Managed Entity
     *
     * @param mor, Managed MOR Object
     * @return Name of this object, unique relative to its parent folder null if
     * could not get the name
     */
    public String getName(ManagedObjectReference mor) {
        String name = null;
        try {
            name = (String) getDynamicProperty(mor, "name");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Get name property error.", e);
        }
        return name;
    }
}
