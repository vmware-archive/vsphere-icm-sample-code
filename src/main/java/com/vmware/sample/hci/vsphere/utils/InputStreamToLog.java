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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to create a thread to write content from Inputstream to Logger.
 * <ul>
 * <li>input is for inputsteam</li>
 * <li>iType is to specify the logger type. 1 for info, 2 for error, default is debug</li>
 * </ul>
 */
public class InputStreamToLog extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(InputStreamToLog.class);
    private InputStream input;
    private int iType;

    public InputStreamToLog(InputStream input, int iType) {
        this.input = input;
        this.iType = iType;
    }

    @Override
    public void run() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(this.input));
            String line = null;
            while ((line = reader.readLine()) != null) {
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
}
