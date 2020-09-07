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

package com.vmware.sample.hci.vsphere.vcinstall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.utils.ConfigProgress;
import com.vmware.sample.hci.vsphere.utils.ProgressCallback;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;

public class VcInstallerLog extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(VcInstallerLog.class);

    private InputStream input;
    private int iType;
    private boolean toProgress;
    private int startProgress;
    private int endProgress;
    private ProgressCallback pcFun;

    public VcInstallerLog(InputStream input, int iType) {
        this(input, iType, false, 0, 0, null);
    }

    public VcInstallerLog(InputStream input, int iType, boolean toProg,
                          int startProgress, int endProgress, ProgressCallback pc) {
        this.input = input;
        this.iType = iType;
        this.toProgress = toProg;
        this.startProgress = startProgress;
        this.endProgress = endProgress;
        this.pcFun = pc;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(this.input));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (this.toProgress) {
                    this.vcInstallProgress(line);
                }
                switch (this.iType) {
                    case VsphereConstants.LOG_INFO:
                        logger.info("Stdin: {}", line);
                        break;
                    case VsphereConstants.LOG_ERROR:
                        logger.error("Stderr: {}", line);
                        break;
                    default:
                        logger.debug(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                //if Exception in close(), then try to run close() again.
                reader.close();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public void vcInstallProgress(String line) {
        int all = this.endProgress - this.startProgress;
        int pro = this.startProgress;
        if (StringUtils.contains(line, "Performing basic template verification")) {
            //start + 1
            pro = pro + 1;
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "Transfer Completed")) {
            //20%
            pro = pro + (int) (0.2 * all);
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "Setting up storage")) {
            //28%
            pro = pro + (int) (0.28 * all);
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "VMware-Postgres-plpython")) {
            //38%
            pro = pro + (int) (0.38 * all);
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "Services installations succeeded")) {
            //48%
            pro = pro + (int) (0.48 * all);
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "Starting VMware Appliance Management Service")) {
            //65%
            pro = pro + (int) (0.65 * all);
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "Starting VMware vSphere Profile-Driven Storage")) {
            //82%
            pro = pro + (int) (0.82 * all);
            ConfigProgress.setProgress(pro, this.pcFun);
        } else if (StringUtils.contains(line, "Finished successfully")) {
            //end - 1
            pro = this.endProgress - 1;
            ConfigProgress.setProgress(pro, this.pcFun);
        }
    }
}
