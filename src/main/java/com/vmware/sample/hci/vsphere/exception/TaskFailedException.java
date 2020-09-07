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

package com.vmware.sample.hci.vsphere.exception;

public class TaskFailedException extends HciServerException {

    private int errorCode;
    private String TaskName;
    private String targetName;

    public TaskFailedException(String message, int error, String task, String target) {
        super(message);
        this.errorCode = error;
        this.TaskName = task;
        this.targetName = target;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getTaskName() {
        return TaskName;
    }

    public String getTargetName() {
        return targetName;
    }
}
