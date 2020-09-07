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

package com.vmware.sample.hci.connection.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.sample.hci.connection.helpers.builders.ObjectSpecBuilder;
import com.vmware.sample.hci.connection.helpers.builders.PropertyFilterSpecBuilder;
import com.vmware.sample.hci.connection.helpers.builders.PropertySpecBuilder;
import com.vmware.sample.hci.connection.helpers.builders.SelectionSpecBuilder;
import com.vmware.sample.hci.connection.helpers.builders.TraversalSpecBuilder;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

public class GetMOREF {

    VimPortType vimPort;
    ServiceContent serviceContent;

    public GetMOREF(VimPortType vimPort, ServiceContent serviceContent) {
        this.vimPort = vimPort;
        this.serviceContent = serviceContent;
    }

    public static String populate(final RetrieveResult result, final Map<String, ManagedObjectReference> targetMor) {
        String token = null;
        if (result != null) {
            token = result.getToken();
            for (ObjectContent oc : result.getObjects()) {
                ManagedObjectReference mr = oc.getObj();
                String entityNm = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        entityNm = (String) dp.getVal();
                    }
                }
                targetMor.put(entityNm, mr);
            }
        }
        return token;
    }

    public static String populate(final RetrieveResult result, final List<ObjectContent> listobjcontent) {
        String token = null;
        if (result != null) {
            token = result.getToken();
            listobjcontent.addAll(result.getObjects());
        }
        return token;
    }

    public RetrieveResult containerViewByType(
            final ManagedObjectReference container,
            final String moRefType,
            final RetrieveOptions retrieveOptions
    ) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        return this.containerViewByType(container, moRefType, retrieveOptions, "name");
    }

    /**
     * Returns the raw RetrieveResult object for the provided container filtered on properties list
     *
     * @param container       - container to look in
     * @param moRefType       - type to filter for
     * @param moRefProperties - properties to include
     * @return com.vmware.vim25.RetrieveResult for this query
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     */
    public RetrieveResult containerViewByType(
            final ManagedObjectReference container,
            final String moRefType,
            final RetrieveOptions retrieveOptions,
            final String... moRefProperties
    ) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        PropertyFilterSpec[] propertyFilterSpecs = propertyFilterSpecs(container, moRefType, moRefProperties);
        return containerViewByType(container, moRefType, moRefProperties, retrieveOptions, propertyFilterSpecs);
    }

    public PropertyFilterSpec[] propertyFilterSpecs(
            ManagedObjectReference container,
            String moRefType,
            String... moRefProperties
    ) throws RuntimeFaultFaultMsg {
        ManagedObjectReference viewManager = serviceContent.getViewManager();
        ManagedObjectReference containerView =
                vimPort.createContainerView(viewManager, container,
                        Arrays.asList(moRefType), true);

        return new PropertyFilterSpec[]{
                new PropertyFilterSpecBuilder()
                        .propSet(
                                new PropertySpecBuilder()
                                        .all(Boolean.FALSE)
                                        .type(moRefType)
                                        .pathSet(moRefProperties)
                        )
                        .objectSet(
                        new ObjectSpecBuilder()
                                .obj(containerView)
                                .skip(Boolean.TRUE)
                                .selectSet(
                                        new TraversalSpecBuilder()
                                                .name("view")
                                                .path("view")
                                                .skip(false)
                                                .type("ContainerView")
                                )
                )
        };
    }

    public RetrieveResult containerViewByType(
            final ManagedObjectReference container,
            final String moRefType,
            final String[] moRefProperties,
            final RetrieveOptions retrieveOptions,
            final PropertyFilterSpec... propertyFilterSpecs
    ) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        return vimPort.retrievePropertiesEx(
                serviceContent.getPropertyCollector(),
                Arrays.asList(propertyFilterSpecs),
                retrieveOptions
        );
    }

    /**
     * Returns all the MOREFs of the specified type that are present under the
     * folder
     *
     * @param folder    {@link com.vmware.vim25.ManagedObjectReference} of the folder to begin the search
     *                  from
     * @param moRefType Type of the managed entity that needs to be searched
     * @return Map of name and MOREF of the managed objects present. If none
     * exist then empty Map is returned
     * @throws com.vmware.vim25.InvalidPropertyFaultMsg
     * @throws com.vmware.vim25.RuntimeFaultFaultMsg
     */
    public Map<String, ManagedObjectReference> inFolderByType(
            final ManagedObjectReference folder, final String moRefType, final RetrieveOptions retrieveOptions
    ) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        final PropertyFilterSpec[] propertyFilterSpecs = propertyFilterSpecs(folder, moRefType, "name");

        // reuse this property collector again later to scroll through results
        final ManagedObjectReference propertyCollector = serviceContent.getPropertyCollector();

        RetrieveResult results = vimPort.retrievePropertiesEx(
                propertyCollector,
                Arrays.asList(propertyFilterSpecs),
                retrieveOptions);

        final Map<String, ManagedObjectReference> targetMor =
                new HashMap<>();
        while (results != null && !results.getObjects().isEmpty()) {
            resultsTotargetMorMap(results, targetMor);
            final String token = results.getToken();
            // if we have a token, we can scroll through additional results, else there's nothing to do.
            results =
                    (token != null) ?
                            vimPort.continueRetrievePropertiesEx(propertyCollector, token) : null;
        }

        return targetMor;
    }

    private void resultsTotargetMorMap(RetrieveResult results, Map<String, ManagedObjectReference> targetMor) {
        List<ObjectContent> oCont = (results != null) ? results.getObjects() : null;

        if (oCont != null) {
            for (ObjectContent oc : oCont) {
                ManagedObjectReference mr = oc.getObj();
                String entityNm = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        entityNm = (String) dp.getVal();
                    }
                }
                targetMor.put(entityNm, mr);
            }
        }
    }

    /**
     * Returns all the MOREFs of the specified type that are present under the
     * container
     *
     * @param container       {@link ManagedObjectReference} of the container to begin the
     *                        search from
     * @param moRefType       Type of the managed entity that needs to be searched
     * @param moRefProperties Array of properties to be fetched for the moref
     * @return Map of MOREF and Map of name value pair of properties requested of
     * the managed objects present. If none exist then empty Map is
     * returned
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     */
    public Map<ManagedObjectReference, Map<String, Object>> inContainerByType(
            ManagedObjectReference container, String moRefType,
            String[] moRefProperties, RetrieveOptions retrieveOptions) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {
        List<ObjectContent> oCont = containerViewByType(container, moRefType, retrieveOptions, moRefProperties).getObjects();

        Map<ManagedObjectReference, Map<String, Object>> targetMor = new HashMap();

        if (oCont != null) {
            for (ObjectContent oc : oCont) {
                Map<String, Object> propMap = new HashMap<>();
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        propMap.put(dp.getName(), dp.getVal());
                    }
                }
                targetMor.put(oc.getObj(), propMap);
            }
        }
        return targetMor;
    }

    /**
     * Returns all the MOREFs of the specified type that are present under the
     * container
     *
     * @param folder    {@link ManagedObjectReference} of the container to begin the
     *                  search from
     * @param moRefType Type of the managed entity that needs to be searched
     * @return Map of name and MOREF of the managed objects present. If none
     * exist then empty Map is returned
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     */
    public Map<String, ManagedObjectReference> inContainerByType(
            ManagedObjectReference folder, String moRefType, RetrieveOptions retrieveOptions)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        RetrieveResult result = containerViewByType(folder, moRefType, retrieveOptions);
        return toMap(result);
    }

    public Map<String, ManagedObjectReference> toMap(RetrieveResult result) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        final Map<String, ManagedObjectReference> targetMor = new HashMap<>();
        String token = populate(result, targetMor);

        while (token != null && !token.isEmpty()) {
            // fetch results based on new token
            result = vimPort.continueRetrievePropertiesEx(
                    serviceContent.getPropertyCollector(), token);

            token = populate(result, targetMor);
        }

        return targetMor;
    }

    /**
     * Get the MOR of the Virtual Machine by its name.
     *
     * @param vmName           The name of the Virtual Machine
     * @param propCollectorRef Reference of PropertyCollector
     * @return The Managed Object reference for this VM
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     */
    public ManagedObjectReference vmByVMname(
            final String vmName, final ManagedObjectReference propCollectorRef
    ) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {


        ManagedObjectReference retVal = null;
        ManagedObjectReference rootFolder = serviceContent.getRootFolder();
        TraversalSpec tSpec = getVMTraversalSpec();
        // Create Property Spec
        PropertySpec propertySpec = new PropertySpecBuilder()
                .all(Boolean.FALSE)
                .pathSet("name")
                .type("VirtualMachine");

        // Now create Object Spec
        ObjectSpec objectSpec = new ObjectSpecBuilder()
                .obj(rootFolder)
                .skip(Boolean.TRUE)
                .selectSet(tSpec);

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        // created above.
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpecBuilder()
                .propSet(propertySpec)
                .objectSet(objectSpec);

        List<PropertyFilterSpec> listpfs =
                new ArrayList<>(1);
        listpfs.add(propertyFilterSpec);

        RetrieveOptions options = new RetrieveOptions();
        List<ObjectContent> listobcont =
                vimPort.retrievePropertiesEx(propCollectorRef, listpfs, options).getObjects();

        if (listobcont != null) {
            for (ObjectContent oc : listobcont) {
                ManagedObjectReference mr = oc.getObj();
                String vmnm = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        vmnm = (String) dp.getVal();
                    }
                }
                if (vmnm != null && vmnm.equals(vmName)) {
                    retVal = mr;
                    break;
                }
            }
        }
        return retVal;
    }

    /**
     * @return TraversalSpec specification to get to the VirtualMachine managed
     * object.
     */
    public TraversalSpec getVMTraversalSpec() {
        // Create a traversal spec that starts from the 'root' objects
        // and traverses the inventory tree to get to the VirtualMachines.
        // Build the traversal specs bottoms up

        //Traversal to get to the VM in a VApp
        TraversalSpec vAppToVM = new TraversalSpecBuilder()
                .name("vAppToVM")
                .type("VirtualApp")
                .path("vm");

        //Traversal spec for VApp to VApp
        TraversalSpec vAppToVApp = new TraversalSpecBuilder()
                .name("vAppToVApp")
                .type("VirtualApp")
                .path("resourcePool")
                .selectSet(
                        //SelectionSpec for both VApp to VApp and VApp to VM
                        new SelectionSpecBuilder().name("vAppToVApp"),
                        new SelectionSpecBuilder().name("vAppToVM")
                );


        //This SelectionSpec is used for recursion for Folder recursion
        SelectionSpec visitFolders = new SelectionSpecBuilder().name("VisitFolders");

        // Traversal to get to the vmFolder from DataCenter
        TraversalSpec dataCenterToVMFolder = new TraversalSpecBuilder()
                .name("DataCenterToVMFolder")
                .type("Datacenter")
                .path("vmFolder")
                .skip(false)
                .selectSet(visitFolders);

        // TraversalSpec to get to the DataCenter from rootFolder
        return new TraversalSpecBuilder()
                .name("VisitFolders")
                .type("Folder")
                .path("childEntity")
                .skip(false)
                .selectSet(
                        visitFolders,
                        dataCenterToVMFolder,
                        vAppToVM,
                        vAppToVApp
                );
    }

    /**
     * Method to retrieve properties of a {@link ManagedObjectReference}
     *
     * @param entityMor {@link ManagedObjectReference} of the entity
     * @param props     Array of properties to be looked up
     * @return Map of the property name and its corresponding value
     * @throws InvalidPropertyFaultMsg If a property does not exist
     * @throws RuntimeFaultFaultMsg
     */
    public Map<String, Object> entityProps(
            ManagedObjectReference entityMor, String[] props)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        final HashMap<String, Object> retVal = new HashMap<>();

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        PropertyFilterSpec[] propertyFilterSpecs = {
                new PropertyFilterSpecBuilder()
                        .propSet(
                                // Create Property Spec
                                new PropertySpecBuilder()
                                        .all(Boolean.FALSE)
                                        .type(entityMor.getType())
                                        .pathSet(props)
                        )
                        .objectSet(
                        // Now create Object Spec
                        new ObjectSpecBuilder().obj(entityMor)
                )
        };

        List<ObjectContent> oCont =
                vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(),
                        Arrays.asList(propertyFilterSpecs), new RetrieveOptions()).getObjects();

        if (oCont != null) {
            for (ObjectContent oc : oCont) {
                List<DynamicProperty> dps = oc.getPropSet();
                for (DynamicProperty dp : dps) {
                    retVal.put(dp.getName(), dp.getVal());
                }
            }
        }
        return retVal;
    }

    /**
     * Method to retrieve properties of list of {@link ManagedObjectReference}
     *
     * @param entityMors List of {@link ManagedObjectReference} for which the properties
     *                   needs to be retrieved
     * @param props      Common properties that need to be retrieved for all the
     *                   {@link ManagedObjectReference} passed
     * @return Map of {@link ManagedObjectReference} and their corresponding name
     * value pair of properties
     * @throws InvalidPropertyFaultMsg
     * @throws RuntimeFaultFaultMsg
     */
    public Map<ManagedObjectReference, Map<String, Object>> entityProps(
            List<ManagedObjectReference> entityMors, String[] props)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {


        Map<ManagedObjectReference, Map<String, Object>> retVal =
                new HashMap<>();
        // Create PropertyFilterSpec
        PropertyFilterSpecBuilder propertyFilterSpec = new PropertyFilterSpecBuilder();
        Map<String, String> typesCovered = new HashMap<>();

        for (ManagedObjectReference mor : entityMors) {
            if (!typesCovered.containsKey(mor.getType())) {
                // Create & add new property Spec
                propertyFilterSpec.propSet(
                        new PropertySpecBuilder()
                                .all(Boolean.FALSE)
                                .type(mor.getType())
                                .pathSet(props)
                );
                typesCovered.put(mor.getType(), "");
            }
            // Now create & add Object Spec
            propertyFilterSpec.objectSet(
                    new ObjectSpecBuilder().obj(mor)
            );
        }
        List<PropertyFilterSpec> propertyFilterSpecs =
                new ArrayList<>();
        propertyFilterSpecs.add(propertyFilterSpec);

        RetrieveResult result =
                vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(),
                        propertyFilterSpecs, new RetrieveOptions());

        List<ObjectContent> listobjcontent = new ArrayList<>();
        String token = populate(result, listobjcontent);
        while (token != null && !token.isEmpty()) {
            result =
                    vimPort.continueRetrievePropertiesEx(
                            serviceContent.getPropertyCollector(), token);

            token = populate(result, listobjcontent);
        }

        for (ObjectContent oc : listobjcontent) {
            List<DynamicProperty> dps = oc.getPropSet();
            Map<String, Object> propMap = new HashMap<>();
            if (dps != null) {
                for (DynamicProperty dp : dps) {
                    propMap.put(dp.getName(), dp.getVal());
                }
            }
            retVal.put(oc.getObj(), propMap);
        }
        return retVal;
    }


    public Map<String, ManagedObjectReference> inContainerByType(ManagedObjectReference container, String moRefType) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return inContainerByType(container, moRefType, new RetrieveOptions());
    }

    public Map<String, ManagedObjectReference> inFolderByType(ManagedObjectReference folder, String moRefType) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        return inFolderByType(folder, moRefType, new RetrieveOptions());
    }

    public Map<ManagedObjectReference, Map<String, Object>> inContainerByType(ManagedObjectReference container, String moRefType, String[] strings) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return inContainerByType(container, moRefType, strings, new RetrieveOptions());
    }
}
