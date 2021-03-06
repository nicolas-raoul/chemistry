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

import org.apache.abdera.i18n.text.UrlEncoding;
import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.impl.AbstractWorkspaceManager;
import org.apache.chemistry.Repository;

/**
 * Workspace manager that correctly finds the appropriate collection adapter by
 * comparing paths, given that ci.getHref returns an absolute URI.
 */
public class CMISWorkspaceManager extends AbstractWorkspaceManager {

    private final CMISProvider provider;

    public CMISWorkspaceManager(CMISProvider provider) {
        this.provider = provider;
    }

    public CollectionAdapter getCollectionAdapter(RequestContext request) {
        Repository repository = provider.getRepository();
        String uri = request.getUri().toString();
        if (uri.indexOf('?') > 0) {
            uri = uri.substring(0, uri.lastIndexOf('?'));
        }
        String spath = request.getTargetBasePath();
        String path = spath == null ? uri : uri.substring(spath.length());
        String paths = path + '/';

        if (paths.startsWith("/typechildren/")) {
            String param = request.getTarget().getParameter("typeid");
            String tid = UrlEncoding.decode(param);
            return new CMISTypesCollection(
                    CMISObjectsCollection.COLTYPE_CHILDREN, tid, repository);
        }
        if (paths.startsWith("/typedescendants/")) {
            String param = request.getTarget().getParameter("typeid");
            String tid = UrlEncoding.decode(param);
            return new CMISTypesCollection(
                    CMISObjectsCollection.COLTYPE_DESCENDANTS, tid, repository);
        }
        if (paths.startsWith("/type/")) {
            return new CMISTypesCollection(null, null, repository);
        }
        if (paths.startsWith("/children/")) {
            String param = request.getTarget().getParameter("objectid");
            String id = UrlEncoding.decode(param);
            return new CMISChildrenCollection(null, id, repository);
        }
        if (paths.startsWith("/descendants/")) {
            String param = request.getTarget().getParameter("objectid");
            String id = UrlEncoding.decode(param);
            return new CMISChildrenCollection(
                    CMISObjectsCollection.COLTYPE_DESCENDANTS, id, repository);
        }
        if (paths.startsWith("/foldertree/")) {
            String param = request.getTarget().getParameter("objectid");
            String id = UrlEncoding.decode(param);
            return new CMISChildrenCollection(
                    CMISObjectsCollection.COLTYPE_FOLDER_TREE, id, repository);
        }
        if (paths.startsWith("/parents/")) {
            String param = request.getTarget().getParameter("objectid");
            String id = UrlEncoding.decode(param);
            return new CMISParentsCollection(null, id, repository);
        }
        if (paths.startsWith("/object/")) {
            // TODO has a different feed type than children
            return new CMISChildrenCollection(null, null, repository);
        }
        if (paths.startsWith("/path/")) {
            return new CMISChildrenCollection(
                    CMISObjectsCollection.COLTYPE_PATH, null, repository);
        }
        if (paths.startsWith("/file/")) {
            return new CMISChildrenCollection(null, null, repository);
        }
        if (paths.startsWith("/unfiled/")) {
            return new CMISCollectionForOther(null, "unfiled", null, repository);
        }
        if (paths.startsWith("/checkedout/")) {
            return new CMISCheckedOutCollection(repository);
        }
        if (paths.startsWith("/query/") || paths.startsWith("/query?")) {
            return new CMISQueryFeed(repository);
        }
        if (paths.startsWith("/allowableactions/")) {
            return new CMISAllowableActionsEntry(repository);
        }
        return null;
    }

}
