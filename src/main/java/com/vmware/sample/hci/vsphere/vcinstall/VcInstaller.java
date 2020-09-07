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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.sample.hci.vsphere.exception.HciServerException;
import com.vmware.sample.hci.vsphere.utils.ProgressCallback;
import com.vmware.sample.hci.vsphere.utils.VsphereConstants;

public class VcInstaller {
    private static final Logger logger = LoggerFactory.getLogger(VcInstaller.class);

    public static int runCommand(String[] cmds, int start, int end, ProgressCallback pc) {

        StringBuilder sb = new StringBuilder();
        for (String s : cmds) {
            sb.append(s).append(" ");
        }
        logger.debug("Running command :{}", sb.toString());

        int rtnVal = -999;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmds);
            final Process pro = pb.start();

			/* No input for this command
             * Comment this snippet for further use
			 *
			final OutputStreamWriter writer = new OutputStreamWriter(pro.getOutputStream());
			new Thread(new Runnable()
			{
				@Override
				public void run(){
					try{
						writer.write("000000000");
						writer.write(System.getProperty("line.separator"));
						writer.flush();
						writer.close();
					}catch(IOException e){
						logger.error(e.getMessage(), e);
					}
				}
			}).start();
			*/

            Thread inthread = new VcInstallerLog(pro.getInputStream(), VsphereConstants.LOG_INFO);
            inthread.start();

            Thread errthread = new VcInstallerLog(pro.getErrorStream(), VsphereConstants.LOG_ERROR,
                    true, start, end, pc);
            errthread.start();

            rtnVal = pro.waitFor();

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return rtnVal;
    }

    public static int runCommand(String[] cmds) {
        return runCommand(cmds, -1, 0, null);
    }

    /*
     *	vcInstallerInfo: all the information needed to install a VC
     *  mountNeeded:	if mounting iso manually is need, then specify true;
     *					otherwise, false
     */
    public static void installVc(VcInstallerInfo vcInstallerInfo, boolean mountNeeded,
                                 int startProgress, int endProgress, ProgressCallback pc) {
        String errorMsg;
        logger.info("Generating /tmp/vc.json");
        vcInstallerInfo.saveToTemplate();
        logger.info("Generated /tmp/vc.json successfully");

        int returnVal = -999;
        if (mountNeeded) {
            final String[] mountCmds = {
                    "mount", "-t", "auto", "-o", "loop", vcInstallerInfo.getVcIsoPath(),
                    vcInstallerInfo.getVcMntPath()
            };

            returnVal = runCommand(mountCmds);
            if (0 == returnVal) {
                logger.info("Mount iso successfully");
            } else {
                errorMsg = String.format("Failed to mount VC iso manually with error code %d. ", returnVal);
                logger.error(errorMsg);
                throw new HciServerException(errorMsg);
            }
        }

        final String[] installCmds = {
                String.format("%s/vcsa-cli-installer/lin64/vcsa-deploy", vcInstallerInfo.getVcMntPath()),
                "install", "--accept-eula", "--no-esx-ssl-verify", "/tmp/vc.json"
        };

        returnVal = runCommand(installCmds, startProgress, endProgress, pc);
        if (0 == returnVal) {
            logger.info("VC is installed successfully");
        } else {
            errorMsg = String.format("VC failed to install with error code %d. "
                    + "check debug log for detail", returnVal);
            logger.info(errorMsg);
            throw new HciServerException(errorMsg);
        }
    }
}
