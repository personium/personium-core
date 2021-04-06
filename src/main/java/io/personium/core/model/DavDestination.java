/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.model;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.PersoniumUrl;
import io.personium.core.PersoniumCoreException;

/**
 * Class managing Dav's destination information.
 */
public class DavDestination {

    private DavRsCmp destinationRsCmp = null;
    private int destinationHierarchyNumber = 0; //Number of hierarchies to move to
    private DavRsCmp boxRsCmp = null; //box information
    PersoniumUrl destUrl;

    /**
     * constructor.
     * @param destinationUriString String indicating the destination path
     * @param box Box information on the destination
     * @throws URISyntaxException URI parse error
     */
    public DavDestination(String destinationUriString, DavRsCmp box) throws URISyntaxException {
        this.destUrl = PersoniumUrl.create(destinationUriString);
        boxRsCmp = box;
    }

    /**
     * Get the Uri string of the destination.
     * @return Uri string to move to
     */
    public String getDestinationUri() {
        return destUrl.toHttp();
    }

    /**
     * Get the destination DavRsCmp.
     * @return DavRsCmp to move to
     */
    public DavRsCmp getDestinationRsCmp() {
        return destinationRsCmp;
    }

    /**
     * Get the destination DavCmp.
     * @return DavCmp to move to
     */
    public DavCmp getDestinationCmp() {
        return destinationRsCmp.getDavCmp();
    }

    /**
     * Validate the resource to be moved using the MOVE method.
     * @param overwrite Whether to overwrite if the destination resource already exists
     * @param davCmp DavCmp of source resource
     */
    public void validateDestinationResource(String overwrite, DavCmp davCmp) {
        List<String> destinationPaths = Arrays.asList(this.destUrl.pathUnderBox.split("/"));
        DavCmp currentCmp =  this.destinationRsCmp.getDavCmp();
        DavCmp parentCmp = this.destinationRsCmp.getParent().getDavCmp();

        //Check if there is a parent resource in the middle of the move destination
        checkHasParent(destinationPaths, this.destinationHierarchyNumber);

        //Check if you are about to overwrite the resource of the move destination
        checkIsProhibitedResource(overwrite, currentCmp, parentCmp, davCmp.getType());

        //Check if the number of child elements of the parent resource has reached the maximum value
        checkParentChildCount(currentCmp, parentCmp);

        //Check that the hierarchy of the resource after the TODO move does not exceed the maximum depth of the hierarchy of the collection
        checkDepthLimit();

        //If uuid is the same even if the source / destination name is different, it is a 404 error.
        //Considering the case where the same request has already been executed, when the uuid of the reloaded DavNode is the same, it is regarded as the state that the source does not exist.
        if (equalsDestinationNodeId(davCmp)) {
            throw davCmp.getNotFoundException().params(davCmp.getUrl());
        }
}

    /**
     * Check that the hierarchy of the resource after the movement does not exceed the maximum depth of the hierarchy of the collection.
     */
    private void checkDepthLimit() {
        //Check that the hierarchy of the resource after the TODO move does not exceed the maximum depth of the hierarchy of the collection
    }

    /**
     * It checks whether the number of child elements of the parent resource has reached the maximum value.
     * @param currentCmp DavCmp of the resource to be moved
     * @param parentCmp DavCmp of the parent resource of the destination
     */
    private void checkParentChildCount(DavCmp currentCmp, DavCmp parentCmp) {
        if (!currentCmp.exists()
                && PersoniumUnitConfig.getMaxChildResourceCount() <= parentCmp.getChildrenCount()) {
            //If there is no resource in the destination and the number of child elements of the destination parent has already reached the maximum value
            //* If the resource already exists in the migration destination (overwriting), check on the maximum value is done at the time of creation of the resource of the migration destination, so it will not be implemented here
            throw PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
        }
    }

    /**
     * It is checked whether or not the parent resource of the movement destination path exists.
     * @param destinationPaths Path information of the destination
     */
    private void checkHasParent(List<String> destinationPaths, int hierarchyNumber) {
        if (hierarchyNumber < destinationPaths.size() - 1) {
            //If there is no resource in the path of the movement destination, 409 error is set
            throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(destinationPaths.get(hierarchyNumber));
        }
    }

    private void checkIsProhibitedResource(String overwrite,
            DavCmp currentCmp,
            DavCmp parentCmp,
            String sourceResourceType) {
        if (currentCmp.exists()) {
            //Check required if you are about to overwrite the destination resource
            if (DavCommon.OVERWRITE_FALSE.equalsIgnoreCase(overwrite)) {
                //When F is specified in the Overwrite header, it is an error because it can not be overwritten
                throw PersoniumCoreException.Dav.DESTINATION_ALREADY_EXISTS;

            } else if (DavCmp.TYPE_COL_SVC.equals(parentCmp.getType())
                    && DavCmp.SERVICE_SRC_COLLECTION.equals(currentCmp.getName())) {
                //Since it can not overwrite the Service source collection, it is regarded as an error
                throw PersoniumCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE;

            } else if (!DavCmp.TYPE_DAV_FILE.equals(currentCmp.getType())) {
                //In the current specification, overwriting the collection can not be performed, so it is regarded as an error
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            }
        } else {
            //Check required when resources after migration are newly created

            if (DavCmp.TYPE_COL_ODATA.equals(parentCmp.getType())) {
                //Can not move because parent resource is OData collection
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_ODATA_COLLECTION;

            } else if (DavCmp.TYPE_DAV_FILE.equals(parentCmp.getType())) {
                //Can not move because parent resource is WebDav file
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_FILE;

            } else if (DavCmp.TYPE_COL_SVC.equals(parentCmp.getType())) {
                //Can not move because parent resource is Service collection
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_SERVICE_COLLECTION;
            }
        }

        if (parentCmp.getParent() != null && DavCmp.TYPE_COL_SVC.equals(parentCmp.getParent().getType())
                && !DavCmp.TYPE_DAV_FILE.equals(sourceResourceType)) {
            //Collection can not be moved under ServiceSource collection
            //Example) ServiceSource collection / __ src / collection
            throw PersoniumCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_CONTAIN_COLLECTION;
        }
    }

    /**
     * Load hierarchy information of destination.
     */
    public void loadDestinationHierarchy() {
        //Check whether the destination path exists from the highest level to the lowest level
        List<String> destinationPaths = Arrays.asList(this.destUrl.pathUnderBox.split("/"));
        DavRsCmp parentRsCmp = boxRsCmp;
        DavRsCmp currentRsCmp = boxRsCmp;
        int pathIndex;
        for (pathIndex = 0; pathIndex < destinationPaths.size(); pathIndex++) {
            DavCmp parentCmp = parentRsCmp.getDavCmp();
            String resourceName = destinationPaths.get(pathIndex);
            DavCmp currentCmp = parentCmp.getChild(resourceName);
            currentRsCmp = new DavRsCmp(parentRsCmp, currentCmp);
            if (!currentCmp.exists()) {
                //Resource of hierarchy being processed does not exist
                break;
            }
            //Refer to the next hierarchy
            parentRsCmp = currentRsCmp;
        }

        //Hierarchical information is held as a class variable
        this.destinationRsCmp = currentRsCmp;
        this.destinationHierarchyNumber = pathIndex;
    }

    /**
     * Whether the uuid of the DavNode passed in the argument is the same as the uuid of the destination DavNode is judged.
     * @param davCmp DavNode to be compared
     * @return Returns true for the same uuid, false otherwise. <br />
     * If the entity of the destination DavNode does not exist, false is also returned.
     */
    public boolean equalsDestinationNodeId(DavCmp davCmp) {
        if (null != this.getDestinationCmp() && this.getDestinationCmp().exists()
                && this.getDestinationCmp().getId().equals(davCmp.getId())) {
            return true;
        }
        return false;
    }
}
