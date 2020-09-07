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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.exception.HciServerException;

/**
 * This class is used to get/set/increase progress.
 * <ul>
 * <li>We can either set progress directly or</li>
 * <li>increase the progress by a certain value.</li>
 * <li>We also provide a callback function so that the </li>
 * <li>client can be notified immediately after progress changes</li>
 * </ul>
 */

public class ConfigProgress {
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigProgress.class);

    private static int progress = 0;

    /**
     * Execute SSH command through exec channel on ESXi host.
     *
     * @return The result of execution. if successful, 0 returned.
     */
    public static int getProgress() {
        if (progress < 0) {
            throw new HciServerException("Progress should not less than 0");
        } else if ((progress >= 0) && (progress <= 100)) {
            return progress;
        } else {
            return 100;
        }
    }

    public synchronized static void setProgress(int pro) {
        progress = pro;
    }

    /**
     * Set the progress to pro and callback immediately.
     *
     * @param pro the new progress value.
     * @param pc  the callback function
     */
    public synchronized static void setProgress(int pro, ProgressCallback pc) {
        progress = pro;
        if (null != pc) {
            pc.printProgress(progress);
        }
    }

    public synchronized static void increaseProgressBy(int pro) {
        progress = progress + pro;
    }

    /**
     * Increase the progress by pro and callback immediately.
     *
     * @param pro the new progress value to be added.
     * @param pc  the callback function
     */
    public synchronized static void increaseProgressBy(int pro, ProgressCallback pc) {
        progress = progress + pro;
        if (null != pc) {
            pc.printProgress(progress);
        }
    }
}
