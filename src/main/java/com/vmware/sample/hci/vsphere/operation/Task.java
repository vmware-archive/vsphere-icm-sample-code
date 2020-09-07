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

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.VsphereClient;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.WaitOptions;

public class Task {
    public static final int MAX_WAITFORUPDATE_SEC = 3;
    public static final int MAX_WAITFORUPDATES_ATTEMPT = 30;
    private static final Logger logger = LoggerFactory.getLogger(Task.class);
    private VsphereClient mConnection;
    private PropertyCollector mPropCollector;

    public Task(VsphereClient vcConnection) {
        mConnection = vcConnection;
        mPropCollector = new PropertyCollector(mConnection);
    }

    /**
     * Returns TaskInfo for a given Task Mor object
     *
     * @param taskMor Task Mor
     * @return TaskInfo Information about the task null, if not task information
     * is found
     * @throws MethodFault, Exception
     */
    public TaskInfo getTaskInfo(ManagedObjectReference taskMor) {
        TaskInfo taskInfo = null;
        try {
            taskInfo = (TaskInfo) mPropCollector.getDynamicProperty(taskMor,
                    PropertyCollector.TASK_INFO_PROPERTYNAME);
            // print error info
            if (taskInfo != null && taskInfo.getError() != null
                    && taskInfo.getError().getFault() != null) {
                logger.error("----Error details of the task {} ----",
                        taskInfo.getName());
                logger.error(taskInfo.getError().toString());
                Iterator<LocalizableMessage> it = taskInfo.getError().getFault()
                        .getFaultMessage().iterator();
                while (it.hasNext()) {
                    logger.error(it.toString());
                    it.next();
                }
                logger.error("---- End of Error details ------------");
            } else if (taskInfo == null) {
                logger.error("Unable to get any taskInfo for the taskMor.");
            }
        } catch (Exception e) {
            // At least print the exception...
            logger.error("Exception found in getTaskInfo: ", e);
        }

        return taskInfo;
    }

    /**
     * Method to get the Name of the Managed Entity
     *
     * @param mor Managed MOR Object
     * @return Name of this object, unique relative to its parent folder
     * @throws MethodFault, Exception
     */
    public String getName(ManagedObjectReference mor) throws Exception {
        String name = (String) mPropCollector.getDynamicProperty(mor,
                PropertyCollector.MANAGEDENTITY_NAME_PROPERTYNAME);
        return name;
    }

    /**
     * Monitor Task by doing updates on task
     *
     * @param mor Task ManagedObjectReference object
     * @return boolean true, on successful task completion false, if task failed
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     * @throws MethodFault,            Exception
     */
    public boolean monitorTask(ManagedObjectReference mor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        boolean taskSuccess = true;
        String methodInfo = "";
        String entityName = null;
        WaitOptions options = null;

        /*
         * Check task is part of managedentity.recentTask property
         */
        TaskInfo taskInfo = this.getTaskInfo(mor);
        if (taskInfo != null) {
            methodInfo = methodInfo + taskInfo.getName() + " | ";
            /*
             * Print the managed entity to which the operation applies
             */
            ManagedObjectReference entity = taskInfo.getEntity();

            if (entity != null) {
                try {
                    entityName = getName(entity);
                } catch (Exception fExcep) {
                    logger.warn(
                            "Could not get entity name, entity may not exist {}",
                            entity.getValue());
                    entityName = entity.getValue();
                }
                if (entityName != null) {
                    logger.info(
                            "Task Operation: {} | task id: {} | task type: {}",
                            taskInfo.getName(),
                            taskInfo.getKey(),
                            taskInfo.getDescriptionId());
                    logger.info(
                            "{} Task defined on ManagedEntity. type = {}, name ={}",
                            methodInfo, entity.getType(),
                            entityName);
                }
            }

            /*
             * Monitor task progress
             */
            ManagedObjectReference filter = null;
            try {
                filter = mPropCollector.createFilter(mor,
                        PropertyCollector.TASK_INFO_PROPERTYNAME);
                logger.info("Filter for monitor task: " + "Type = "
                        + filter.getType() + "Value = " + filter.getValue());
                String updatesVersion = "";
                boolean updateCompleted = false;

                UpdateSet updateSet = null;
                List<PropertyFilterUpdate> filterUpdate = null;

                int maxWaitForUpdateAttempts = MAX_WAITFORUPDATES_ATTEMPT;

                logger.info("Monitoring task begin");
                options = new WaitOptions();
                options.setMaxWaitSeconds(MAX_WAITFORUPDATE_SEC);

                int noUpdateCounter = 0;
                do {
                    for (int i = 1; i <= maxWaitForUpdateAttempts + 1; i++) {
                        try {
                            updateSet = mPropCollector
                                    .waitForUpdatesEx(updatesVersion, options);
                            break;
                        } catch (Exception exception) {
                            logger.error("WaitForUpdatesEx error: ", exception);
                            logger.warn(
                                    "{} Attempt to waitforudpates again. Current Attempt = {}",
                                    methodInfo, i);
                        }
                    }
                    if (updateSet == null) {
                        noUpdateCounter++;
                        if (noUpdateCounter == MAX_WAITFORUPDATES_ATTEMPT) {
                            throw new RuntimeException(
                                    "Operation Timed Out. No update happened for over: "
                                            + ((options.getMaxWaitSeconds()
                                            / 60000)
                                            * MAX_WAITFORUPDATES_ATTEMPT)
                                            + " Minutes.");
                        } else {
                            logger.info("No update happened");
                        }
                    } else {
                        noUpdateCounter = 0;
                        logger.info("Received the update from VC.");
                        updatesVersion = updateSet.getVersion();
                        filterUpdate = updateSet.getFilterSet();
                        TaskInfoState taskState =
                                handlePropertyUpdateList(filterUpdate, mor);
                        if (taskState != null) {
                            if (TaskInfoState.SUCCESS.equals(taskState)) {
                                taskSuccess = true;
                                updateCompleted = true;
                            } else if (TaskInfoState.ERROR.equals(taskState)) {
                                taskSuccess = false;
                                updateCompleted = true;
                            }
                        }
                    }
                    waitBetweenUpdateCheck();

                } while (!updateCompleted);

            } finally {
                if (filter != null) {
                    mPropCollector.destroyPropertyFilter(filter);
                }
            }
            logger.info("Monitoring task end");
        } else {
            logger.error(
                    "Could not monitor task because taskInfo property was null");
            taskSuccess = false;
        }
        return taskSuccess;
    }

    private void waitBetweenUpdateCheck() {
        int taskUpdateInSec = 1;
        try {
            if (taskUpdateInSec > 0) {
                Thread.sleep(taskUpdateInSec * 1000);
            }
        } catch (Exception e) {
            logger.error("Exception when waiting for task: ", e);
        }
    }

    private TaskInfoState handlePropertyUpdateList(
            List<PropertyFilterUpdate> updateList, ManagedObjectReference mor) {

        TaskInfoState taskstate = null;
        for (PropertyFilterUpdate update : updateList) {

            List<ObjectUpdate> objUpdateList = update.getObjectSet();

            if (objUpdateList == null) {
                logger.warn("Return value of getObjectSet() is null");
            }
            if (objUpdateList != null
                    && objUpdateList.get(0).getObj() == null) {
                logger.warn("FilterUpdate objectset.object is null");
            }
            if (objUpdateList != null && objUpdateList.get(0).getObj() != null
                    && objUpdateList.get(0).getObj().getValue()
                    .equals(mor.getValue())) {
                taskstate = handleObjUpdateList(objUpdateList, mor);
            }

            if (taskstate != null)
                return taskstate;
        }

        return null;
    }

    private TaskInfoState handleObjUpdateList(List<ObjectUpdate> objUpdateList,
                                              ManagedObjectReference mor) {
        List<PropertyChange> propertyChange =
                objUpdateList.get(0).getChangeSet();
        if (propertyChange != null) {
            return handlePropertyChangeSet(propertyChange, mor);
        }

        logger.warn("filterUpdate[i].getObjectSet()[0].getChangeSet() is null");
        return null;
    }

    private TaskInfoState handlePropertyChangeSet(
            List<PropertyChange> propertyChangeList,
            ManagedObjectReference mor) {
        TaskInfoState prevState = null;
        TaskInfoState currState = null;
        int taskPerc = 0;
        int prevTaskPerc = 0;
        boolean printedPercDesc = false;
        TaskInfo taskInfo = getTaskInfo(mor);

        String methodInfo = "";
        methodInfo = methodInfo + taskInfo.getName() + " | ";

        for (PropertyChange change : propertyChangeList) {
            if (!change.getName()
                    .equals(PropertyCollector.TASK_INFO_PROPERTYNAME)) {
                logger.warn("propertyChange.getName() == {}, it should be {}",
                        change.getName(),
                        PropertyCollector.TASK_INFO_PROPERTYNAME);
            } else {
                taskInfo = (TaskInfo) change.getVal();
                currState = taskInfo.getState();
                /*
                 * Print Task Progress
                 */
                if (!TaskInfoState.QUEUED.equals(currState)
                        && taskInfo.getProgress() != null) {
                    taskPerc = taskInfo.getProgress();
                    if (!printedPercDesc) {
                        printedPercDesc = true;
                        logger.info("{} INFO : Task Progress {}", methodInfo,
                                taskPerc);
                    }
                }
                /*
                 * Print Task State
                 */
                if (currState != prevState) {
                    prevState = currState;
                    LocalizedMethodFault fault = taskInfo.getError();
                    String errorMessage = "null";
                    if (fault != null) {
                        errorMessage = fault.getLocalizedMessage();
                    }
                    logger.info("{} State = {} | Error = {} | Result = {}",
                            methodInfo, taskInfo.getState(),
                            errorMessage, taskInfo.getResult());
                }

                if (TaskInfoState.SUCCESS.equals(taskInfo.getState())
                        || (TaskInfoState.ERROR.equals(taskInfo.getState()))) {
                    // task completed, return from here.
                    return taskInfo.getState();
                }
            }
        }
        return null;
    }
}
