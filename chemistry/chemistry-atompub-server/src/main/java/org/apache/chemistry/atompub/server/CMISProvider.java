/*
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
 *
 * Authors:
 *     Florent Guillaume, Nuxeo
 *     Amelie Avramo, EntropySoft
 */
package org.apache.chemistry.atompub.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.protocol.Resolver;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetBuilder;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.WorkspaceManager;
import org.apache.abdera.protocol.server.impl.AbstractProvider;
import org.apache.abdera.protocol.server.impl.AbstractWorkspaceManager;
import org.apache.abdera.protocol.server.impl.RegexTargetResolver;
import org.apache.abdera.protocol.server.impl.SimpleWorkspaceInfo;
import org.apache.abdera.protocol.server.impl.TemplateTargetBuilder;
import org.apache.abdera.util.Constants;
import org.apache.chemistry.Repository;
import org.apache.chemistry.atompub.AtomPub;
import org.apache.chemistry.atompub.AtomPubCMIS;
import org.apache.chemistry.atompub.URITemplate;

/**
 * Abdera provider for the CMIS bindings used by Chemistry.
 */
public class CMISProvider extends AbstractProvider {

    protected final Repository repository;

    protected final AbstractWorkspaceManager workspaceManager;

    protected final TemplateTargetBuilder targetBuilder;

    protected final RegexTargetResolver targetResolver;

    public CMISProvider(Repository repository) {
        this.repository = repository;

        targetBuilder = new TemplateTargetBuilder();
        targetResolver = new ServletRegexTargetResolver();

        // service
        targetBuilder.setTemplate(TargetType.TYPE_SERVICE,
                "{target_base}/repository");
        targetResolver.setPattern("/repository(\\?.*)?",
                TargetType.TYPE_SERVICE);

        // entry
        targetBuilder.setTemplate(TargetType.TYPE_ENTRY,
                "{target_base}/{entrytype}/{id}");
        targetResolver.setPattern("/object(?:/?)([^/?]*)(\\?.*)?",
                TargetType.TYPE_ENTRY, "objectid");
        targetResolver.setPattern("/allowableactions/([^/?]+)",
                TargetType.TYPE_ENTRY, "objectid");
        targetResolver.setPattern("/type/([^/?]+)(\\?.*)?",
                TargetType.TYPE_ENTRY, "typeid");
        targetResolver.setPattern("/path/([^?]*)(\\?.*)?",
                TargetType.TYPE_ENTRY, "path");

        // media
        targetBuilder.setTemplate(TargetType.TYPE_MEDIA,
                "{target_base}/file/{objectid}");
        targetResolver.setPattern("/file/([^/?]+)", TargetType.TYPE_MEDIA,
                "objectid");

        // collection
        // global workpace collections
        targetBuilder.setTemplate(TargetType.TYPE_COLLECTION,
                "{target_base}/{collection}{-prefix|/|id}");
        targetResolver.setPattern("/checkedout(\\?.*)?",
                TargetType.TYPE_COLLECTION);
        targetResolver.setPattern("/unfiled", TargetType.TYPE_COLLECTION);
        targetResolver.setPattern("/query(\\?.*)?",
                CMISQueryFeed.TARGET_TYPE_CMIS_QUERY);
        targetResolver.setPattern("/typechildren(\\?.*)?", //
                TargetType.TYPE_COLLECTION);
        // per-object collections
        targetResolver.setPattern("/parents/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "objectid");
        targetResolver.setPattern("/children/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "objectid");
        targetResolver.setPattern("/descendants/([^/?]+)(\\?.*)?",
                CMISObjectsCollection.TARGET_TYPE_CMIS_DESCENDANTS, "objectid");
        targetResolver.setPattern("/foldertree/([^/?]+)(\\?.*)?",
                CMISObjectsCollection.TARGET_TYPE_CMIS_FOLDER_TREE, "objectid");
        targetResolver.setPattern("/allversions/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "objectid");
        targetResolver.setPattern("/relationships/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "objectid");
        targetResolver.setPattern("/policies/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "objectid");
        targetResolver.setPattern("/typechildren/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "typeid");
        targetResolver.setPattern("/typedescendants(\\?.*)?",
                TargetType.TYPE_COLLECTION);
        targetResolver.setPattern("/typedescendants/([^/?]+)(\\?.*)?",
                TargetType.TYPE_COLLECTION, "typeid");

        // CMIS workspaces available

        SimpleWorkspaceInfo workspaceInfo = new SimpleWorkspaceInfo();
        workspaceInfo.setTitle(repository.getInfo().getName());

        workspaceInfo.addCollection(new CMISChildrenCollection(
                AtomPubCMIS.COL_ROOT,
                repository.getInfo().getRootFolderId().getId(), repository));

        workspaceInfo.addCollection(new CMISCollectionForOther(
                AtomPubCMIS.COL_UNFILED, "unfiled", null, repository));

        workspaceInfo.addCollection(new CMISCollectionForOther(
                AtomPubCMIS.COL_CHECKED_OUT, "checkedout", null, repository));

        workspaceInfo.addCollection(new CMISTypesCollection(
                AtomPubCMIS.COL_TYPES, null, repository));

        workspaceInfo.addCollection(new CMISQueryFeed(repository));

        workspaceManager = new CMISWorkspaceManager(this);
        workspaceManager.addWorkspace(workspaceInfo);
    }

    public Repository getRepository() {
        return repository;
    }

    public List<URITemplate> getURITemplates(RequestContext request) {
        String base = request.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // add servlet path
        base += request.getTargetBasePath().substring(
                request.getContextPath().length());
        List<URITemplate> list = new ArrayList<URITemplate>(3);
        list.add(new URITemplate(AtomPubCMIS.URITMPL_OBJECT_BY_ID,
                AtomPub.MEDIA_TYPE_ATOM_ENTRY, addURITemplateParameters(
                        base + "/object/{id}", //
                        AtomPubCMIS.PARAM_FILTER,
                        AtomPubCMIS.PARAM_RENDITION_FILTER,
                        AtomPubCMIS.PARAM_INCLUDE_RELATIONSHIPS,
                        AtomPubCMIS.PARAM_INCLUDE_ALLOWABLE_ACTIONS,
                        AtomPubCMIS.PARAM_INCLUDE_POLICY_IDS,
                        AtomPubCMIS.PARAM_INCLUDE_ACL)));
        list.add(new URITemplate(AtomPubCMIS.URITMPL_OBJECT_BY_PATH,
                AtomPub.MEDIA_TYPE_ATOM_ENTRY, addURITemplateParameters(
                        base + "/object", //
                        AtomPubCMIS.PARAM_PATH, //
                        AtomPubCMIS.PARAM_FILTER,
                        AtomPubCMIS.PARAM_RENDITION_FILTER,
                        AtomPubCMIS.PARAM_INCLUDE_RELATIONSHIPS,
                        AtomPubCMIS.PARAM_INCLUDE_ALLOWABLE_ACTIONS,
                        AtomPubCMIS.PARAM_INCLUDE_POLICY_IDS,
                        AtomPubCMIS.PARAM_INCLUDE_ACL)));
        list.add(new URITemplate(AtomPubCMIS.URITMPL_QUERY, //
                AtomPub.MEDIA_TYPE_ATOM_FEED, addURITemplateParameters(
                        base + "/query",
                        AtomPubCMIS.PARAM_QUERY, //
                        AtomPubCMIS.PARAM_SEARCH_ALL_VERSIONS,
                        AtomPubCMIS.PARAM_MAX_ITEMS,
                        AtomPubCMIS.PARAM_SKIP_COUNT,
                        AtomPubCMIS.PARAM_INCLUDE_RELATIONSHIPS,
                        AtomPubCMIS.PARAM_INCLUDE_ALLOWABLE_ACTIONS)));
        list.add(new URITemplate(AtomPubCMIS.URITMPL_TYPE_BY_ID, //
                AtomPub.MEDIA_TYPE_ATOM_FEED, //
                base + "/type/{id}"));
        return list;
    }

    protected static String addURITemplateParameters(String s, String... params) {
        StringBuilder buf = new StringBuilder(s);
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            buf.append(i == 0 ? '?' : '&');
            buf.append(param);
            buf.append("={");
            buf.append(param);
            buf.append('}');
        }
        return buf.toString();
    }

    @Override
    protected TargetBuilder getTargetBuilder(RequestContext request) {
        return targetBuilder;
    }

    @Override
    protected Resolver<Target> getTargetResolver(RequestContext request) {
        return targetResolver;
    }

    @Override
    public WorkspaceManager getWorkspaceManager(RequestContext request) {
        return workspaceManager;
    }

    @Override
    public ResponseContext getServiceDocument(final RequestContext request) {
        CMISServiceResponse response = new CMISServiceResponse(this, request);
        response.setStatus(200);
        response.setContentType(Constants.APP_MEDIA_TYPE);
        return response;
    }

}
