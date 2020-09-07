
# vSphere ICM (Installation/Configuration/Management) Sample Code

## Overview

This project is aiming at VMware partners and customers who want to install / configure / manage vSphere and vSAN with VMware public APIs. It provides sample code
of vSphere and vSAN installation, configuration and some management operations for partners and customers to refer to when they build up their own automatic scripts.

## Try it out
Download the entire code and uncompress on your disk. This project follows mainstream java code structure and you can directly import to any IDE (Eclipse or IDEA) without any modification.

### Prerequisites
The sample code are written in Java, requirs JDK (5, 6, 7, 8).
A Java IDE should be installed first.
The code can run on MacOS and any linux operating systems.

* Download JDK, install it on your local disk, setup enviroment variables (PATH and CLASSPATH) with Java development guide.
* Download Maven, and uncompress to local disk, add the absolute path of maven bin folder into PATH.
* Download sample code, and uncompress to your desired project folder.
* For each test case, the required hardware should be ready before you run it. By reading the input arguments of each case you setup the corresponding environment.

Any problems when installing JDK and maven, please refer to their manuals for help.

### Build & Run

For either Eclipse users or IDEA users,
1. You should have 3 hosts with ESXi 6.7 installed
2. You should have VCSA 6.7 file placed on /root/Downloads folder
3. Open your IDE, import the project. Your IDE should be able to connect to internet and your ESXi hosts.
4. Expand test folder from project view
5. In each test case, replace strings starting with "YOUR_", say "YOUR_HOST_IP_ADDRESS", "YOUR_HOST_HOSTNAME" with corresponding values.
6. Select any test method and click "Run Test" button to run test case.


## Documentation
This sample code demonstrates how to use vSphere WebService SDK 6.7. WebService SDK 7.0 will be supported in future.
To download vSphere WebService SDK 6.7, please go to https://code.vmware.com/web/sdk/6.7/vsphere-automation-java to find the download link.
You can also get more details about the SDK from this link.

If you don't have this SDK installed in your maven repo, after downloading please unzip it and find the path of vim25.jar, and run the command
written in mvn-install-vsphere-sdk67 to install.

## Contributing

The vsphere-icm-sample-code project team welcomes contributions from the community. Before you start working with vsphere-icm-sample-code, please
read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All contributions to this repository must be
signed as described on that page. Your signature certifies that you wrote the patch or have the right to pass it on
as an open-source patch. For more detailed information, refer to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

BSD 2-Clause License

Copyright 2020 VMware Inc.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
