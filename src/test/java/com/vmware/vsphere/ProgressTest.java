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

package com.vmware.vsphere;

import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.utils.ConfigProgress;
import com.vmware.sample.hci.vsphere.utils.ProgressCallback;

/**
 * This class is used to test Progress.java
 */
public class ProgressTest {
    private static final Logger logger = LoggerFactory
            .getLogger(ProgressTest.class);

    public static void main(String[] args) {
        ProgressTest pt = new ProgressTest();
        pt.integrestionTest();
    }

    @Test
    public void integrestionTest() {
        System.out.println("test starts");
        try {
            final int threadnum = 3;
            ClientLogger cl = new ClientLogger();
            ConfigProgress.setProgress(20, cl);
            logger.info("__{}", ConfigProgress.getProgress());
            List<Thread> lstThread = new ArrayList<>();
            for (int i = 1; i <= threadnum; i++) {
                ProgressThread pt = new ProgressThread("pt" + i, 10, 2 * i);
                lstThread.add(pt);
                pt.start();
                logger.info("__{}:{}", i, ConfigProgress.getProgress());
            }
            for (Thread t : lstThread) {
                t.join();
            }
            ConfigProgress.setProgress(100, cl);
            logger.info("__{}", ConfigProgress.getProgress());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    class ProgressThread extends Thread {

        final private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private int start;
        private int delta;

        public ProgressThread(String name, int strt, int del) {
            super(name);
            this.start = strt;
            this.delta = del;
        }

        @Override
        public void run() {
            logger.info("{} starts at {}", this.getName(), this.df.format(new Date()));
            ConfigProgress.increaseProgressBy(this.delta);
            logger.info("{} : {}", this.getName(), ConfigProgress.getProgress());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                logger.error(ie.getMessage(), ie);
            }
            ConfigProgress.increaseProgressBy(this.delta);
            logger.info("{} : {}", this.getName(), ConfigProgress.getProgress());
            logger.info("{} ends at {}", this.getName(), this.df.format(new Date()));
        }
    }

    class ClientLogger implements ProgressCallback {

        @Override
        public void printProgress(int progress) {
            logger.info("Print from callback: {}", progress);
        }
    }
}
