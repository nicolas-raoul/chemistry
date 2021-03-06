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
import java.util.Collection;

/**
 * A CMIS Document.
 */
public interface Document extends CMISObject {

    /*
     * ----- Object Services -----
     */

    /**
     * Copies this document to another folder.
     * <p>
     * The target folder may be {@code null} to create an unfiled copy.
     *
     * @param folder the target folder, or {@code null}
     * @return the document copy
     *
     * @throws ConstraintViolationException if the document's type isn't allowed
     *             as a child object type in the folder
     * @throws NameConstraintViolationException if the object name is not legal
     *             in its new folder
     * @throws VersioningException if the move cannot be done to a non-latest
     *             version
     */
    Document copy(Folder folder) throws NameConstraintViolationException;

    /*
     * ----- Versioning Services -----
     */

    /**
     * Checks out this document.
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
     * @return the private working copy
     *
     * @throws ConstraintViolationException if the document is not versionable
     * @throws UpdateConflictException if the document is no longer current
     * @throws VersioningException if the checkout cannot be applied to a
     *             non-latest version
     */
    Document checkOut() throws UpdateConflictException;

    /**
     * Cancels a check-out of a private working copy.
     * <p>
     * Reverses the effect of a check-out. Removes the private working copy of
     * the checked-out document, allowing other documents in the version series
     * of this document to be checked out again.
     *
     * @throws ConstraintViolationException if the document is not versionable
     * @throws UpdateConflictException if the document is no longer current
     * @throws VersioningException if the checkout cannot be applied to a
     *             non-latest version
     */
    void cancelCheckOut() throws UpdateConflictException;

    /**
     * Checks in a private working copy.
     * <p>
     * Makes the private working copy the current version of the document.
     *
     * @param major {@code true} if the version is a major version
     * @param comment a check-in comment, or {@code null}
     * @return the the new version of the document
     *
     * @throws ConstraintViolationException if the document is not versionable
     * @throws StreamNotSupportedException if a content stream is being set but
     *             is not supported on the document
     * @throws UpdateConflictException if the document is no longer current
     */
    Document checkIn(boolean major, String comment)
            throws UpdateConflictException;

    /**
     * Gets the latest version of this document.
     * <p>
     * Returns the latest version, or the latest major version, of this
     * document.
     *
     * @param major {@code true} if the latest major version is requested
     * @return the latest version or latest major version, or {@code null} if
     *         not found
     *
     * @throws ObjectNotFoundException if <em>major</em> is {@code true} and the
     *             version series contains no major version
     */
    Document getLatestVersion(boolean major);

    /**
     * Gets all the versions of this document.
     * <p>
     * Returns the list of all document versions for the specified version
     * series of this document, sorted by CREATION_DATE descending. All versions
     * that the user can access, including checked-out version and private
     * working copy, are returned.
     *
     * @return the collection of all versions
     */
    Collection<Document> getAllVersions();

    /**
     * Deletes all the versions of this document.
     * <p>
     * Deletes all document versions in the version series of this document,
     * including the private working copy if it exists.
     *
     * @throws UpdateConflictException if the object is no longer current
     */
    void deleteAllVersions() throws UpdateConflictException;

    /*
     * ----- data access -----
     */

    /**
     * Gets the primary content stream for this document.
     *
     * @return the content stream, or {@code null} if the document does not have
     *         a content stream
     *
     * @throws IOException if the stream could not be read
     */
    ContentStream getContentStream() throws IOException;

    /**
     * Sets the primary content stream for this document.
     *
     * @param contentStream
     *
     * @throws IOException if the stream could not be written
     * @throws StreamNotSupportedException if a content stream is not allowed on
     *             the document
     * @throws UpdateConflictException if the document is no longer current
     * @throws VersioningException if the update cannot be applied to a
     *             non-latest version
     */
    void setContentStream(ContentStream contentStream) throws IOException,
            UpdateConflictException;

}
