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
 */
package org.apache.chemistry;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * A SPI connection to a CMIS Repository.
 * <p>
 * This API contains low-level methods that mirror the CMIS specification.
 */
public interface SPI {

    /**
     * Gets the repository for this SPI connection.
     *
     * @return the repository
     */
    Repository getRepository();

    /**
     * Closes this SPI connection.
     */
    void close();

    /*
     * ----- Factories -----
     */

    /**
     * Creates a new reference to a given object.
     *
     * @param id the object ID
     * @return a reference to that object
     */
    ObjectId newObjectId(String id);

    /**
     * Creates a new empty object entry.
     *
     * @param typeId the object type ID
     * @return a new object entry
     */
    ObjectEntry newObjectEntry(String typeId);

    /*
     * ----- Navigation Services -----
     */

    /**
     * Gets the tree of descendant folders contained in the specified folder.
     *
     * @param folder the folder
     * @param depth the depth, or {@code -1} for all levels
     * @param inclusion the {@link Inclusion#properties properties} and
     *            {@link Inclusion#allowableActions allowable actions} to
     *            include in the result
     * @return a tree of folder entries (a forest)
     *
     * @throws FilterNotValidException if the filter is not valid
     * @throws IllegalArgumentException if the object is not a folder or the
     *             depth is invalid
     */
    Tree<ObjectEntry> getFolderTree(ObjectId folder, int depth,
            Inclusion inclusion);

    /**
     * Gets the descendants of a folder.
     * <p>
     * Returns the descendant objects contained at one or more levels in the
     * tree rooted at the specified folder. The ordering and tree walk algorithm
     * is repository-specific, but should be consistent. A depth of 1 means
     * returning only the direct children (same as {@link #getChildren}).
     * <p>
     * Only the filter-selected properties associated with each object are
     * returned. The content stream is not returned.
     * <p>
     * For a repository that supports version-specific filing, this will return
     * the version of the documents in the folder specified by the user filing
     * the documents. Otherwise, the latest version of the documents will be
     * returned.
     * <p>
     * As relationships are not fileable, type cannot be
     * {@link BaseType#RELATIONSHIP}. However, if
     * {@link Inclusion#relationships} is {@code true} then relationships are
     * also returned for each returned object, according to the value of the
     * parameter.
     *
     * @param folder the folder
     * @param depth the depth, or {@code -1} for all levels
     * @param orderBy an {@code ORDER BY} clause, or {@code null}
     * @param inclusion the {@link Inclusion#properties properties},
     *            {@link Inclusion#allowableActions allowable actions},
     *            {@link Inclusion#relationships relationships} and
     *            {@link Inclusion#renditions renditions} to include in the
     *            result
     * @return a tree of folder entries (a forest)
     *
     * @throws FilterNotValidException if the filter is not valid
     * @throws IllegalArgumentException if the object is not a folder or the
     *             depth is invalid
     */
    Tree<ObjectEntry> getDescendants(ObjectId folder, int depth,
            String orderBy, Inclusion inclusion);

    /**
     * Gets the direct children of a folder.
     * <p>
     * Only the filter-selected properties associated with each object are
     * returned. The content stream is not returned.
     * <p>
     * For a repository that supports version-specific filing, this will return
     * the version of the documents in the folder specified by the user filing
     * the documents. Otherwise, the latest version of the documents will be
     * returned.
     * <p>
     * Ordering is repository-specific, except that if the repository state has
     * not changed then the ordering remains consistent across invocations.
     *
     * @param folder the folder
     * @param inclusion the {@link Inclusion#properties properties},
     *            {@link Inclusion#allowableActions allowable actions},
     *            {@link Inclusion#relationships relationships} and
     *            {@link Inclusion#renditions renditions} to include in the
     *            result
     * @param orderBy an {@code ORDER BY} clause, or {@code null}
     * @param paging paging information, or {@code null} for a
     *            repository-specific default
     * @throws FilterNotValidException if the filter is not valid
     * @throws IllegalArgumentException if the object is not a folder
     */
    ListPage<ObjectEntry> getChildren(ObjectId folder, Inclusion inclusion,
            String orderBy, Paging paging);

    /**
     * Gets the parent of a folder.
     * <p>
     * Returns {@code null} if the specified folder is the root folder.
     * <p>
     * To find the parent of a non-folder, use {@link #getObjectParents}.
     *
     * @param folder the folder
     * @param filter the properties filter, or {@code null} for all properties
     * @return the parents and optionally relationships
     *
     * @throws FilterNotValidException if the filter is not valid
     * @throws IllegalArgumentException if the folder is not a folder or is the
     *             root
     */
    ObjectEntry getFolderParent(ObjectId folder, String filter);

    /**
     * Gets the direct parents of an object.
     * <p>
     * The object must be a non-folder, fileable object.
     * <p>
     * To find the parent of a folder, use {@link #getFolderParent}.
     *
     * @param object the object
     * @param filter the properties filter, or {@code null} for all properties
     * @return the collection of parent folders
     *
     * @throws ConstraintViolationException if the object is not fileable
     * @throws FilterNotValidException if the filter is not valid
     * @throws IllegalArgumentException if the object is a folder
     */
    Collection<ObjectEntry> getObjectParents(ObjectId object, String filter);

    /**
     * Gets the list of documents that are checked out that the user has access
     * to.
     * <p>
     * Most likely this will be the set of documents checked out by the user,
     * but the repository may also include checked-out objects that the calling
     * user has access to, but did not check out.
     * <p>
     * If folder is not {@code null}, then the result includes only direct
     * children of that folder.
     *
     * @param folder the folder, or {@code null}
     * @param inclusion the {@link Inclusion#properties properties},
     *            {@link Inclusion#allowableActions allowable actions} and
     *            {@link Inclusion#relationships relationships} to include in
     *            the result
     * @param paging paging information, or {@code null} for a
     *            repository-specific default
     * @throws FilterNotValidException if the filter is not valid
     * @throws IllegalArgumentException if the object is not a folder
     */
    ListPage<ObjectEntry> getCheckedOutDocuments(ObjectId folder,
            Inclusion inclusion, Paging paging);

    /*
     * ----- Object Services -----
     */

    /**
     * Creates a document.
     * <p>
     * Creates a document, and optionally adds it to a folder.
     * <p>
     * The versioningState input is used to create a document in a
     * {@link VersioningState#CHECKED_OUT CHECKED_OUT} state, or as a checked-in
     * {@link VersioningState#MINOR MINOR} version, or as a checked-in
     * {@link VersioningState#MAJOR MAJOR} version. If created in a
     * {@link VersioningState#CHECKED_OUT CHECKED_OUT} state, the object is a
     * private working copy and there is no corresponding checked out document.
     *
     * @param properties the properties
     * @param folder the containing folder, or {@code null}
     * @param contentStream the content stream, or {@code null}
     * @param versioningState the versioning state
     * @return the ID of the created document
     *
     * @throws ConstraintViolationException if the properties are not legal
     * @throws StreamNotSupportedException if <em>contentStream</em> is not
     *             {@code null} and a content stream is not supported on the
     *             document
     * @throws NameConstraintViolationException if the name is not legal
     */
    // TODO policies, addACEs, removeACEs
    ObjectId createDocument(Map<String, Serializable> properties,
            ObjectId folder, ContentStream contentStream,
            VersioningState versioningState)
            throws NameConstraintViolationException;

    /**
     * Copies a document.
     * <p>
     * Creates a document as a copy of the given source document.
     * <p>
     * A set of properties that have to be changed in the copy can be specified.
     * This allows a "copy-and-update" operation.
     * <p>
     * The versioningState input is used to create a document in a
     * {@link VersioningState#CHECKED_OUT CHECKED_OUT} state, or as a checked-in
     * {@link VersioningState#MINOR MINOR} version, or as a checked-in
     * {@link VersioningState#MAJOR MAJOR} version. If created in a
     * {@link VersioningState#CHECKED_OUT CHECKED_OUT} state, the object is a
     * private working copy and there is no corresponding checked out document.
     *
     * @param source the source document
     * @param folder the parent folder for the newly created document, or
     *            {@code null} for to create an unfiled document
     * @param properties the properties to change
     * @param versioningState the versioning state
     * @return the ID of the created document
     *
     * @throws ConstraintViolationException if the properties are not legal
     * @throws NameConstraintViolationException if the name is not legal
     */
    // TODO policies, addACEs, removeACEs
    ObjectId createDocumentFromSource(ObjectId source, ObjectId folder,
            Map<String, Serializable> properties,
            VersioningState versioningState)
            throws NameConstraintViolationException;

    /**
     * Creates a folder.
     *
     * @param properties the properties
     * @param folder the containing folder
     * @return the ID of the created folder
     *
     * @throws ConstraintViolationException if the properties are not legal
     * @throws NameConstraintViolationException if the name is not legal
     */
    // TODO policies, addACEs, removeACEs
    ObjectId createFolder(Map<String, Serializable> properties, ObjectId folder)
            throws NameConstraintViolationException;

    /**
     * Creates a relationship.
     *
     * @param properties the properties
     * @return the ID of the created relationship
     *
     * @throws ConstraintViolationException if the properties are not legal
     */
    // TODO policies, addACEs, removeACEs
    ObjectId createRelationship(Map<String, Serializable> properties);

    /**
     * Creates a policy.
     * <p>
     * Creates a policy of the specified type, and optionally adds the policy to
     * a folder.
     *
     * @param properties the properties
     * @param folder the containing folder, or {@code null}
     * @return the ID of the created policy
     *
     * @throws ConstraintViolationException if the properties are not legal
     */
    // TODO policies, addACEs, removeACEs
    ObjectId createPolicy(Map<String, Serializable> properties, ObjectId folder);

    /**
     * Gets the allowable actions.
     * <p>
     * Returns the set of allowable actions on an object based on the current
     * user's context, subject to any access constraints that are currently
     * imposed by the repository.
     *
     * @param object the object
     * @return the set of allowable actions
     * @see AllowableAction
     * @see AllowableAction#DEFAULT
     */
    Set<QName> getAllowableActions(ObjectId object);

    /**
     * Gets an object.
     * <p>
     * Returns the properties of an object, and optionally associated
     * information.
     * <p>
     * The content stream of the object is not returned, use
     * {@link #getContentStream} for that.
     * <p>
     * If the passed object is an {@link ObjectEntry}, the repository
     * implementation may fill it with additional data and return the same
     * object.
     *
     * @param object the object
     * @param inclusion the {@link Inclusion}s to have in the result
     * @return the object, or {@code null} if the object is not found
     *
     * @throws FilterNotValidException if the filter is not valid
     */
    ObjectEntry getObject(ObjectId object, Inclusion inclusion);

    /**
     * Gets the properties of an object.
     * <p>
     * Returns the properties of an object, and optionally the operations that
     * the user is allowed to perform on the object.
     * <p>
     * The content stream of the object is not returned, use
     * {@link #getContentStream} for that.
     * <p>
     * If the passed object is an {@link ObjectEntry}, the repository
     * implementation may fill it with additional data and return the same
     * object.
     *
     * @param object the object
     * @param inclusion the {@link Inclusion#properties properties} to include
     *            in the result
     * @return the properties of the object, or {@code null} if the object is
     *         not found
     *
     * @throws FilterNotValidException if the filter is not valid
     */
    ObjectEntry getProperties(ObjectId object, Inclusion inclusion);

    /**
     * Gets the properties of an object, given its path.
     *
     * @param path the object path
     * @param inclusion the {@link Inclusion#properties properties},
     *            {@link Inclusion#allowableActions allowable actions} and
     *            {@link Inclusion#relationships relationships} to include in
     *            the result
     * @return the properties of the object, or {@code null} if the object is
     *         not found
     *
     * @throws FilterNotValidException if the filter is not valid
     */
    ObjectEntry getObjectByPath(String path, Inclusion inclusion);

    /**
     * Gets the list of associated renditions for an object.
     * <p>
     * A rendition filter can be included. It is either {@code "*"} (
     * {@link Rendition#FILTER_ALL}), {@code "cmis:none"} (
     * {@link Rendition#FILTER_NONE}), or a comma-separated list of either kinds
     * of MIME types (which may have a subtype of {@code "*"}). The {@code null}
     * value is equivalent to {@code "cmis:none"}.
     *
     * @param object the object
     * @param inclusion the {@link Inclusion#renditions renditions} to include
     *            in the result
     * @param paging paging information, or {@code null} for a
     *            repository-specific default
     * @return the list of renditions
     *
     * @throws UnsupportedOperationException if renditions are not supported
     *
     * @see Rendition#FILTER_ALL
     * @see Rendition#FILTER_NONE
     */
    List<Rendition> getRenditions(ObjectId object, Inclusion inclusion,
            Paging paging);

    /**
     * Checks if the document has an associated content stream.
     * <p>
     * Note that the content stream may be present but still have length 0.
     *
     * @return {@code true} if the document has an associated content stream
     */
    boolean hasContentStream(ObjectId document);

    /**
     * Gets a content stream for a document or folder.
     * <p>
     * A content stream ID can be provided, to access alternate content streams
     * (renditions for instance).
     * <p>
     * If no content stream is available, {@code null} may be returned or
     * {@link ConstraintViolationException} may be thrown.
     *
     * @param object the document or folder
     * @param contentStreamId the content stream ID, or {@code null}
     * @return the content stream
     *
     * @throws ConstraintViolationException if the object does not have a
     *             content stream or rendition stream
     * @throws IOException
     */
    ContentStream getContentStream(ObjectId object, String contentStreamId)
            throws IOException;

    // TODO 1.0 getRenditions

    /**
     * Sets the content stream for a document.
     * <p>
     * Sets (creates or replaces) the content stream for the specified document.
     * This is considered an update of the document object.
     * <p>
     * If the document is a private working copy, some repositories may not
     * support updates.
     * <p>
     * Because repositories may automatically create new document versions on a
     * user's behalf, the document returned may have a different ID from the one
     * provided as a parameter to this method.
     *
     * @param document the document
     * @param contentStream the content stream to set
     * @param overwrite {@code true} if an already-existing content stream must
     *            be overwritten
     * @return the resulting document, which may differ from the one passed as
     *         input
     *
     * @throws StreamNotSupportedException if a content stream is not allowed on
     *             the document
     * @throws ContentAlreadyExistsException if the document already has a
     *             content stream and {@code overwrite} is {@code false}
     * @throws UpdateConflictException if the document is no longer current
     * @throws VersioningException if the update cannot be applied to a
     *             non-latest version
     * @throws IOException
     */
    // TODO return ObjectId or ObjectEntry?
    ObjectId setContentStream(ObjectId document, ContentStream contentStream,
            boolean overwrite) throws IOException,
            ContentAlreadyExistsException, UpdateConflictException;

    /**
     * Deletes the content stream for a document.
     * <p>
     * This does not delete properties. If there are other versions this does
     * not affect them, their properties or their content streams. This does not
     * change the ID of the document.
     * <p>
     * This is considered an update of the document object.
     * <p>
     * If the document is a private working copy, some repositories may not
     * support updates.
     *
     * @param document the document
     * @return the resulting document, which may differ from the one passed as
     *         input
     *
     * @throws ConstraintViolationException if a content stream is required on
     *             the document
     * @throws UpdateConflictException if the document is no longer current
     * @throws VersioningException if the update cannot be applied to a
     *             non-latest version
     */
    ObjectId deleteContentStream(ObjectId document)
            throws UpdateConflictException;

    /**
     * Updates the properties of an object.
     * <p>
     * To remove a property, specify property with a {@code null} value. For
     * multi-value properties, the whole list of values must be provided on
     * every update. Properties not specified are not changed.
     * <p>
     * If the object is a private working copy, some repositories may not
     * support updates.
     * <p>
     * If a changeToken was provided when the object was retrieved, the change
     * token must be included as-is when calling this method.
     * <p>
     * Because repositories may automatically create new document versions on a
     * user's behalf, the object returned may have a different ID from the one
     * provided as a parameter to this method.
     *
     * @param object the object
     * @param changeToken the change token, or {@code null}
     * @param properties the properties to change
     * @return the resulting object, which may differ from the one passed as
     *         input
     *
     * @throws ConstraintViolationException if the properties are not legal, for
     *             instance if any of the property violates the
     *             min/max/required/length constraints specified in the relevant
     *             property definition
     * @throws NameConstraintViolationException if the name is not legal
     * @throws UpdateConflictException if the object is no longer current
     * @throws VersioningException if the update cannot be applied to a
     *             non-latest version
     */
    // TODO return ObjectId or ObjectEntry?
    ObjectId updateProperties(ObjectId object, String changeToken,
            Map<String, Serializable> properties)
            throws NameConstraintViolationException, UpdateConflictException;

    /**
     * Moves the specified filed object from one folder to another.
     * <p>
     * The targetFolder is the target folder into which the object has to be
     * moved. When the object is multi-filed, a source folder to be moved out of
     * must be specified.
     *
     * @param object the object to move
     * @param targetFolder the target folder
     * @param sourceFolder the source folder, or {@code null}
     * @return the resulting object, which may differ from the one passed as
     *         input
     *
     * @throws IllegalArgumentException if sourceFolder is not a parent of the
     *             object
     * @throws ConstraintViolationException if the object's type isn't allowed
     *             as a child object type in the targetFolder
     * @throws NameConstraintViolationException if the object name is not legal
     *             in its new parent folder
     * @throws UpdateConflictException if the object is no longer current
     * @throws VersioningException if the move cannot be done to a non-latest
     *             version
     */
    ObjectId moveObject(ObjectId object, ObjectId targetFolder,
            ObjectId sourceFolder) throws NameConstraintViolationException,
            UpdateConflictException;

    /**
     * Deletes the specified object.
     * <p>
     * When a filed object is deleted, it is removed from all folders it is
     * filed in.
     * <p>
     * If allVersions is {@code false}, this deletes a specific version of a
     * document object. If allVersions is {@code true}, then the whole version
     * series is deleted.
     * <p>
     * Deletion of a private working copy (checked out version) is the same as
     * the canceling of a checkout.
     *
     * @param object the object to delete
     * @param allVersions if {@code true}, then delete all versions as well
     *
     * @throws ConstraintViolationException if the object is a non-empty folder
     * @throws UpdateConflictException if the object is no longer current
     */
    void deleteObject(ObjectId object, boolean allVersions)
            throws UpdateConflictException;

    /**
     * Deletes a tree of objects.
     * <p>
     * Deletes the tree rooted at the specified folder (including that folder).
     * <p>
     * If a non-folder object is removed from the last folder it is filed in, it
     * can continue to survive outside of the folder structure if the repository
     * supports unfiling; this is controlled based on the unfiling parameter.
     * <p>
     * For repositories that support version-specific filing, this may delete
     * some versions of a document but not necessarily all versions. For
     * repositories that do not support version-specific filing, if a document
     * is to be deleted, all versions are deleted.
     * <p>
     * This method is not atomic. However, if
     * {@link Unfiling#DELETE_SINGLE_FILED DELETE_SINGLE_FILED} and some objects
     * fail to delete, then single-filed objects are either deleted or kept,
     * never just unfiled. This is so that user can call this method again to
     * recover from the error by using the same tree.
     * <p>
     * The order in which deletion will happen is unspecified; however, any
     * objects that are not deleted (e.g., because a previous object failed to
     * delete) remain valid objects (including any applicable filing
     * constraint).
     * <p>
     * Returns the collection of IDs of objects that could not be deleted. If
     * all objects could be deleted, an empty collection is returned. If at
     * least one object could not be deleted, then if continueOnFailure is
     * {@code false} that single object ID is returned, otherwise all IDs of
     * objects that could not be deleted are returned.
     *
     * @param folder the folder to delete
     * @param unfiling how to unfile non-folder objects, if {@code null} then
     *            same as {@link Unfiling#DELETE}
     * @param continueOnFailure {@code true} if failure to delete one object
     *            should not stop deletion of other objects
     * @return the collection of IDs of objects that could not be deleted
     *
     * @throws UpdateConflictException if the object is no longer current
     */
    Collection<ObjectId> deleteTree(ObjectId folder, Unfiling unfiling,
            boolean continueOnFailure) throws UpdateConflictException;

    /*
     * ----- Multi-Filing Services -----
     */

    /**
     * Adds an existing non-folder, fileable object to a folder.
     *
     * @param object the object
     * @param folder the folder
     * @param allVersions TODO
     *
     * @throws ConstraintViolationException if the object's type isn't allowed
     *             as a child object type in the folder
     */
    void addObjectToFolder(ObjectId object, ObjectId folder);

    /**
     * Removes a non-folder object from a folder or from all folders.
     * <p>
     * If folder is {@code null}, then the the object is removed from all
     * folders.
     * <p>
     * This never deletes the object, which means that if unfiling is not
     * supported, and an object is to be removed from the last folder it exists
     * in, or is to be removed from all folders, an exception will be thrown.
     *
     * @param object the object
     * @param folder the folder, or {@code null} for all folders
     *
     * @throws ConstraintViolationException if removing the object would delete
     *             it
     */
    void removeObjectFromFolder(ObjectId object, ObjectId folder);

    /*
     * ----- Discovery Services -----
     */

    /**
     * Queries the repository for queryable objects.
     * <p>
     * The query is based on properties or an optional full-text string.
     * <p>
     * Content-streams are not returned as part of the query.
     * <p>
     * If searchAllVersions is {@code true}, and {@code CONTAINS()} is used in
     * the query, an exception will be thrown if full-text search is not
     * supported or if the repository does not have previous versions in the
     * full-text index.
     * <p>
     * Returns a result table produced by the query statement. Typically each
     * row of this table corresponds to an object, and each column corresponds
     * to a property or a computed value as specified by the {@code SELECT}
     * clause of the query statement.
     * <p>
     * If {@link Inclusion#allowableActions} is {@code true}, the repository
     * will return the allowable actions for the current user for each result
     * object in the output table as an additional multi-valued column
     * containing computed values of type String, provided that each row in the
     * output table indeed corresponds to one object (which is true for a query
     * without {@code JOIN} ). If each row in the output table does not
     * correspond to a specific object and {@link Inclusion#allowableActions} is
     * {@code true}, then an exception will be thrown.
     *
     * @param statement the SQL statement
     * @param searchAllVersions {@code true} if all versions (not only that
     *            latest) must be searched
     * @param inclusion the {@link Inclusion#allowableActions allowable actions}
     *            , {@link Inclusion#relationships relationships} and
     *            {@link Inclusion#renditions renditions} to include in the
     *            result
     * @param paging paging information, or {@code null} for a
     *            repository-specific default
     * @return the matching objects
     *
     * @throws IllegalArgumentException if the statement is invalid
     */
    // TODO returns a result set actually, there may be computed values
    ListPage<ObjectEntry> query(String statement, boolean searchAllVersions,
            Inclusion inclusion, Paging paging);

    /**
     * Gets a list of content changes.
     * <p>
     * Returns all the change events, or only those starting with the provided
     * changeLogToken.
     * <p>
     * This is intended to be used by search crawlers or other applications that
     * need to efficiently understand what has changed in the repository.
     * <p>
     * The content stream is <em>not</em> returned for any change event.
     * <p>
     * The latest change log token for a repository can be acquired via
     * {@link RepositoryInfo#getLatestChangeToken}.
     * <p>
     * The return value latestChangeLogToken contains the change token of the
     * last change event returned by the iterator.
     *
     * @param changeLogToken the change log token, or {@code null}
     * @param includeProperties {@code true} if values are returned in the
     *            change events for updated objects
     * @param paging paging information, or {@code null} for a
     *            repository-specific default
     * @return a paged list of change events
     *
     * @throws ConstraintViolationException if the event corresponding to
     *             {@code changeLogToken} is no longer available in the change
     *             log
     *
     * @see Repository#getInfo
     * @see RepositoryInfo#getLatestChangeLogToken
     */
    // TODO 1.0 rename to getContentChanges
    ListPage<ObjectEntry> getChangeLog(String changeLogToken,
            boolean includeProperties, Paging paging,
            String[] latestChangeLogToken);

    /*
     * ----- Versioning Services -----
     */

    /**
     * Checks out a document.
     * <p>
     * Creates a private working copy of the document, copies the metadata and
     * optionally content.
     * <p>
     * It is up to the repository to determine if updates to the current version
     * (not private working copy) and prior versions are allowed if checked out.
     * <p>
     * Some repositories may not support updating of private working copies and
     * the updates must then be supplied via {@link #checkIn}.
     * <p>
     * This method may remove update permission on prior versions.
     * <p>
     * The ID of the private working copy is returned.
     * <p>
     * The return value contentCopied[0] is set to {@code true} if the content
     * is copied, {@code false} if not. Whether the content is copied on
     * check-out or not is repository-specific.
     *
     * @param document the document
     * @param contentCopied a return array of size 1
     * @return a reference to the private working copy
     *
     * @throws ConstraintViolationException if the document is not versionable
     * @throws UpdateConflictException if the object is no longer current
     * @throws VersioningException if the checkout cannot be applied to a
     *             non-latest version
     */
    ObjectId checkOut(ObjectId document, boolean[] contentCopied)
            throws UpdateConflictException;

    /**
     * Cancels a check-out.
     * <p>
     * Reverses the effect of a check-out. Removes the private working copy of
     * the checked-out document, allowing other documents in the version series
     * to be checked out again.
     *
     * @param document the private working copy
     *
     * @throws ConstraintViolationException if the document is not versionable
     * @throws UpdateConflictException if the object is no longer current
     * @throws VersioningException if the checkout cannot be applied to a
     *             non-latest version
     */
    void cancelCheckOut(ObjectId document) throws UpdateConflictException;

    /**
     * Checks in a private working copy.
     * <p>
     * Makes the private working copy the current version of the document.
     *
     * @param document the private working copy
     * @param properties the properties to set on the document, or {@code null}
     * @param contentStream the content stream to set on the document, or
     *            {@code null}
     * @param major {@code true} if the version is a major version
     * @param comment a check-in comment, or {@code null}
     * @return a reference to the new version of the document
     *
     * @throws ConstraintViolationException if the document is not versionable
     * @throws StreamNotSupportedException if <em>contentStream</em> is not
     *             {@code null} and a content stream is not supported on the
     *             document
     * @throws UpdateConflictException if the object is no longer current
     */
    ObjectId checkIn(ObjectId document, Map<String, Serializable> properties,
            ContentStream contentStream, boolean major, String comment)
            throws UpdateConflictException;

    // TODO 1.0 getObjectOfLatestVersion

    /**
     * Gets the properties of the latest version.
     * <p>
     * Returns the properties of the latest version, or the latest major
     * version, of the specified version series.
     * <p>
     * If the latest major version is requested and the series has no major
     * version, an exception is thrown.
     *
     * @param versionSeriesId the version series ID
     * @param major {@code true} if the last major version is requested
     * @param filter the properties filter, or {@code null} for all properties
     * @return a collection of properties
     *
     * @throws FilterNotValidException if the filter is not valid
     * @throws ObjectNotFoundException if <em>major</em> is {@code true} and the
     *             version series contains no major version
     */
    Map<String, Serializable> getPropertiesOfLatestVersion(
            String versionSeriesId, boolean major, String filter);

    /**
     * Gets all the versions of a document.
     * <p>
     * Returns the list of all document versions for the specified version
     * series, sorted by CREATION_DATE descending. All versions that the user
     * can access, including checked-out version and private working copy, are
     * returned.
     *
     * @param versionSeriesId the version series ID
     * @param filter the properties filter, or {@code null} for all properties
     *
     * @throws FilterNotValidException if the filter is not valid
     */
    Collection<ObjectEntry> getAllVersions(String versionSeriesId, String filter);

    /*
     * ----- Relationship Services -----
     */

    /**
     * Gets the relationships having as source or target a given object.
     * <p>
     * Returns a list of relationships associated with the given object,
     * optionally of a specified relationship type, and optionally in a
     * specified direction.
     * <p>
     * If typeId is {@code null}, returns relationships of any type.
     * <p>
     * Ordering is repository specific but consistent across requests.
     *
     * @param object the object
     * @param typeId the type ID, or {@code null}
     * @param includeSubRelationshipTypes {@code true} if relationships of any
     *            sub-type of typeId are to be returned as well
     * @param inclusion the {@link Inclusion#properties properties},
     *            {@link Inclusion#relationships relationships direction} and
     *            {@link Inclusion#allowableActions allowable actions} to
     *            include in the result
     * @param paging paging information, or {@code null} for a
     *            repository-specific default
     * @return the list of relationships
     *
     * @throws FilterNotValidException if the filter is not valid
     */
    // TODO rename to getObjectRelationships
    ListPage<ObjectEntry> getRelationships(ObjectId object, String typeId,
            boolean includeSubRelationshipTypes, Inclusion inclusion,
            Paging paging);

    /*
     * ----- Policy Services -----
     */

    /**
     * Applies a policy to an object.
     * <p>
     * The target object must be controllable.
     *
     * @param object the target object
     * @param policy the policy
     *
     * @throws ConstraintViolationException if the object is not controllable
     */
    void applyPolicy(ObjectId object, ObjectId policy);

    /**
     * Removes a policy from an object.
     * <p>
     * Removes a previously applied policy from a target object. The policy is
     * not deleted, and may still be applied to other objects.
     * <p>
     * The target object must be controllable.
     *
     * @param object the target object
     * @param policy the policy
     *
     * @throws ConstraintViolationException if the object is not controllable
     */
    void removePolicy(ObjectId object, ObjectId policy);

    /**
     * Gets the policies applied to an object.
     * <p>
     * Returns the list of policy objects currently applied to a target object.
     * Only policies that are directly (explicitly) applied to the target object
     * are returned.
     * <p>
     * The target object must be controllable.
     *
     * @param object the target object
     * @param filter the properties filter, or {@code null} for all properties
     *
     * @throws FilterNotValidException if the filter is not valid
     */
    Collection<ObjectEntry> getAppliedPolicies(ObjectId object, String filter);

    /*
     * ----- ACL services -----
     */

    /**
     * Gets the ACL currently applied to the specified document or folder
     * object.
     * <p>
     * The return value exact is {@code true} if the returned ACL fully
     * describes the permissions for this object.
     *
     * @param object the object
     * @param onlyBasicPermissions {@code true} if the result should be
     *            expressed using only basic permissions
     * @return the ACL
     */
    List<ACE> getACL(ObjectId object, boolean onlyBasicPermissions,
            boolean[] exact);

    /**
     * Adds or removes the given ACEs to or from the ACL of the specified
     * document or folder object.
     * <p>
     * The return value exact is {@code true} if the returned ACL fully
     * describes the permissions for this object.
     * <p>
     * The return value changeToken, if not {@code null}, is a change token for
     * the object.
     *
     * @param object the object
     * @param addACEs the ACEs to add
     * @param removeACEs the ACEs to remove
     * @param propagation the ACL propagation to use
     * @return the new ACL
     *
     * @throws ConstraintViolationException if the object is not controllable,
     *             the propagation value is not legal, or an ACE's permission is
     *             not legal
     */
    List<ACE> applyACL(ObjectId object, List<ACE> addACEs,
            List<ACE> removeACEs, ACLPropagation propagation, boolean[] exact,
            String[] changeToken);

}
