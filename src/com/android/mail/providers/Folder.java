/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.providers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.view.View;

import com.android.mail.utils.LogUtils;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A folder is a collection of conversations, and perhaps other folders.
 */
public class Folder implements Parcelable {
    /**
     *
     */
    private static final String FOLDER_UNINITIALIZED = "Uninitialized!";

    // Try to match the order of members with the order of constants in UIProvider.

    /**
     * Unique id of this folder.
     */
    public String id;

    /**
     * The content provider URI that returns this folder for this account.
     */
    public Uri uri;

    /**
     * The human visible name for this folder.
     */
    public String name;

    /**
     * The possible capabilities that this folder supports.
     */
    public int capabilities;

    /**
     * Whether or not this folder has children folders.
     */
    public boolean hasChildren;

    /**
     * How large the synchronization window is: how many days worth of data is retained on the
     * device.
     */
    public int syncWindow;

    /**
     * The content provider URI to return the list of conversations in this
     * folder.
     */
    public Uri conversationListUri;

    /**
     * The content provider URI to return the list of child folders of this folder.
     */
    public Uri childFoldersListUri;

    /**
     * The number of messages that are unread in this folder.
     */
    public int unreadCount;

    /**
     * The total number of messages in this folder.
     */
    public int totalCount;

    /**
     * The content provider URI to force a refresh of this folder.
     */
    public Uri refreshUri;

    /**
     * The current sync status of the folder
     */
    public int syncStatus;

    /**
     * The result of the last sync for this folder
     */
    public int lastSyncResult;

    /**
     * Folder type. 0 is default.
     */
    public int type;

    /**
     * Icon for this folder; 0 implies no icon.
     */
    public long iconResId;

    public String bgColor;
    public String fgColor;

    /**
     * The content provider URI to request additional conversations
     */
    public Uri loadMoreUri;

    /**
     * Total number of members that comprise an instance of a folder. This is
     * the number of members that need to be serialized or parceled.
     */
    private static final int NUMBER_MEMBERS = UIProvider.FOLDERS_PROJECTION.length;

    /**
     * Used only for debugging.
     */
    private static final String LOG_TAG = new LogUtils().getLogTag();

    /**
     * Examples of expected format for the joined folder strings
     *
     * Example of a joined folder string:
     *       630107622^*^^i^*^^i^*^0
     *       <id>^*^<canonical name>^*^<name>^*^<color index>
     *
     * The sqlite queries will return a list of folder strings separated with "^**^"
     * Example of a query result:
     *     630107622^*^^i^*^^i^*^0^**^630107626^*^^u^*^^u^*^0^**^630107627^*^^f^*^^f^*^0
     */
    private static final String FOLDER_COMPONENT_SEPARATOR = "^*^";
    private static final Pattern FOLDER_COMPONENT_SEPARATOR_PATTERN =
            Pattern.compile("\\^\\*\\^");

    private static final String FOLDER_SEPARATOR = "^**^";

    public Folder(Parcel in) {
        assert (in.dataSize() == NUMBER_MEMBERS);
        id = in.readString();
        uri = in.readParcelable(null);
        name = in.readString();
        capabilities = in.readInt();
        // 1 for true, 0 for false.
        hasChildren = in.readInt() == 1;
        syncWindow = in.readInt();
        conversationListUri = in.readParcelable(null);
        childFoldersListUri = in.readParcelable(null);
        unreadCount = in.readInt();
        totalCount = in.readInt();
        refreshUri = in.readParcelable(null);
        syncStatus = in.readInt();
        lastSyncResult = in.readInt();
        type = in.readInt();
        iconResId = in.readLong();
        bgColor = in.readString();
        fgColor = in.readString();
        loadMoreUri = in.readParcelable(null);
     }

    public Folder(Cursor cursor) {
        assert (cursor.getColumnCount() == NUMBER_MEMBERS);
        id = cursor.getString(UIProvider.FOLDER_ID_COLUMN);
        uri = Uri.parse(cursor.getString(UIProvider.FOLDER_URI_COLUMN));
        name = cursor.getString(UIProvider.FOLDER_NAME_COLUMN);
        capabilities = cursor.getInt(UIProvider.FOLDER_CAPABILITIES_COLUMN);
        // 1 for true, 0 for false.
        hasChildren = cursor.getInt(UIProvider.FOLDER_HAS_CHILDREN_COLUMN) == 1;
        syncWindow = cursor.getInt(UIProvider.FOLDER_SYNC_WINDOW_COLUMN);
        String convList = cursor.getString(UIProvider.FOLDER_CONVERSATION_LIST_URI_COLUMN);
        conversationListUri = !TextUtils.isEmpty(convList) ? Uri.parse(convList) : null;
        String childList = cursor.getString(UIProvider.FOLDER_CHILD_FOLDERS_LIST_COLUMN);
        childFoldersListUri = (hasChildren && !TextUtils.isEmpty(childList)) ? Uri.parse(childList)
                : null;
        unreadCount = cursor.getInt(UIProvider.FOLDER_UNREAD_COUNT_COLUMN);
        totalCount = cursor.getInt(UIProvider.FOLDER_TOTAL_COUNT_COLUMN);
        String refresh = cursor.getString(UIProvider.FOLDER_REFRESH_URI_COLUMN);
        refreshUri = !TextUtils.isEmpty(refresh) ? Uri.parse(refresh) : null;
        syncStatus = cursor.getInt(UIProvider.FOLDER_SYNC_STATUS_COLUMN);
        lastSyncResult = cursor.getInt(UIProvider.FOLDER_LAST_SYNC_RESULT_COLUMN);
        type = cursor.getInt(UIProvider.FOLDER_TYPE_COLUMN);
        iconResId = cursor.getLong(UIProvider.FOLDER_ICON_RES_ID_COLUMN);
        bgColor = cursor.getString(UIProvider.FOLDER_BG_COLOR_COLUMN);
        fgColor = cursor.getString(UIProvider.FOLDER_FG_COLOR_COLUMN);
        String loadMore = cursor.getString(UIProvider.FOLDER_LOAD_MORE_URI_COLUMN);
        loadMoreUri = !TextUtils.isEmpty(loadMore) ? Uri.parse(loadMore) : null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeParcelable(uri, 0);
        dest.writeString(name);
        dest.writeInt(capabilities);
        // 1 for true, 0 for false.
        dest.writeInt(hasChildren ? 1 : 0);
        dest.writeInt(syncWindow);
        dest.writeParcelable(conversationListUri, 0);
        dest.writeParcelable(childFoldersListUri, 0);
        dest.writeInt(unreadCount);
        dest.writeInt(totalCount);
        dest.writeParcelable(refreshUri, 0);
        dest.writeInt(syncStatus);
        dest.writeInt(lastSyncResult);
        dest.writeInt(type);
        dest.writeLong(iconResId);
        dest.writeString(bgColor);
        dest.writeString(fgColor);
        dest.writeParcelable(loadMoreUri, 0);
    }

    /**
     * Return a serialized String for this folder.
     */
    public synchronized String serialize(){
        StringBuilder out = new StringBuilder();
        out.append(id).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(uri).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(name).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(capabilities).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(hasChildren ? "1": "0").append(FOLDER_COMPONENT_SEPARATOR);
        out.append(syncWindow).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(conversationListUri).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(childFoldersListUri).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(unreadCount).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(totalCount).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(refreshUri).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(syncStatus).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(lastSyncResult).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(type).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(iconResId).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(bgColor).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(fgColor).append(FOLDER_COMPONENT_SEPARATOR);
        out.append(loadMoreUri);
        return out.toString();
    }

    /**
     * Construct a folder that queries for search results. Do not call on the UI
     * thread.
     */
    public static Folder forSearchResults(Account account, String query, Context context) {
        Folder searchFolder = null;
        if (account.searchUri != null) {
            Builder searchBuilder = account.searchUri.buildUpon();
            searchBuilder.appendQueryParameter(UIProvider.SearchQueryParameters.QUERY, query);
            Uri searchUri = searchBuilder.build();
            Cursor folderCursor = context.getContentResolver().query(searchUri,
                    UIProvider.FOLDERS_PROJECTION, null, null, null);
            if (folderCursor != null) {
                folderCursor.moveToFirst();
                searchFolder = new Folder(folderCursor);
            }
        }
        return searchFolder;
    }

    /**
     * Construct a new Folder instance from a previously serialized string.
     * @param serializedFolder string obtained from {@link #serialize()} on a valid folder.
     */
    public Folder(String serializedFolder) {
        String[] folderMembers = TextUtils.split(serializedFolder,
                FOLDER_COMPONENT_SEPARATOR_PATTERN);
        if (folderMembers.length != NUMBER_MEMBERS) {
            throw new IllegalArgumentException(
                    "Folder de-serializing failed. Wrong number of members detected.");
        }
        id = folderMembers[0];
        uri = Uri.parse(folderMembers[1]);
        name = folderMembers[2];
        capabilities = Integer.valueOf(folderMembers[3]);
        // 1 for true, 0 for false
        hasChildren = folderMembers[4] == "1";
        syncWindow = Integer.valueOf(folderMembers[5]);
        String convList = folderMembers[6];
        conversationListUri = !TextUtils.isEmpty(convList) ? Uri.parse(convList) : null;
        String childList = folderMembers[7];
        childFoldersListUri = (hasChildren && !TextUtils.isEmpty(childList)) ? Uri.parse(childList)
                : null;
        unreadCount = Integer.valueOf(folderMembers[8]);
        totalCount = Integer.valueOf(folderMembers[9]);
        String refresh = folderMembers[10];
        refreshUri = !TextUtils.isEmpty(refresh) ? Uri.parse(refresh) : null;
        syncStatus = Integer.valueOf(folderMembers[11]);
        lastSyncResult = Integer.valueOf(folderMembers[12]);
        type = Integer.valueOf(folderMembers[13]);
        iconResId = Long.valueOf(folderMembers[14]);
        bgColor = folderMembers[15];
        fgColor = folderMembers[16];
        String loadMore = folderMembers[17];
        loadMoreUri = !TextUtils.isEmpty(loadMore) ? Uri.parse(loadMore) : null;
    }

    /**
     * Constructor that leaves everything uninitialized. For use only by {@link #serialize()}
     * which is responsible for filling in all the fields
     */
    public Folder() {
        name = FOLDER_UNINITIALIZED;
    }

    @SuppressWarnings("hiding")
    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel source) {
            return new Folder(source);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };

    @Override
    public int describeContents() {
        // Return a sort of version number for this parcelable folder. Starting with zero.
        return 0;
    }

    /**
     * Create a Folder map from a string of serialized folders. This can only be done on the output
     * of {@link #serialize(Map)}.
     * @param serializedFolder A string obtained from {@link #serialize(Map)}
     * @return a Map of folder name to folder.
     */
    public static Map<String, Folder> parseFoldersFromString(String serializedFolder) {
        LogUtils.d(LOG_TAG, "folder query result: %s", serializedFolder);

        Map<String, Folder> folderMap = Maps.newHashMap();
        if (serializedFolder == null || serializedFolder == "") {
            return folderMap;
        }
        String[] folderPieces = TextUtils.split(
                serializedFolder, FOLDER_COMPONENT_SEPARATOR_PATTERN);
        for (int i = 0, n = folderPieces.length; i < n; i++) {
            Folder folder = new Folder(folderPieces[i]);
            if (folder.name != FOLDER_UNINITIALIZED) {
                folderMap.put(folder.name, folder);
            }
        }
        return folderMap;
    }

    /**
     * Returns a boolean indicating whether network activity (sync) is occuring for this folder.
     */
    public boolean isSyncInProgress() {
        return 0 != (syncStatus & (UIProvider.SyncStatus.BACKGROUND_SYNC |
                UIProvider.SyncStatus.USER_REFRESH |
                UIProvider.SyncStatus.USER_QUERY |
                UIProvider.SyncStatus.USER_MORE_RESULTS));
    }

    /**
     * Serialize the given list of folders
     * @param folderMap A valid map of folder names to Folders
     * @return a string containing a serialized output of folder maps.
     */
    public static String serialize(Map<String, Folder> folderMap) {
        Collection<Folder> folderCollection = folderMap.values();
        Folder[] folderList = folderCollection.toArray(new Folder[]{} );
        int numFolders = folderList.length;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < numFolders; i++) {
          if (i > 0) {
              result.append(FOLDER_SEPARATOR);
          }
          Folder folder = folderList[i];
          result.append(folder.serialize());
        }
        return result.toString();
    }

    public boolean supportsCapability(int capability) {
        return (capabilities & capability) != 0;
    }

    // Show black text on a transparent swatch for system folders, effectively hiding the
    // swatch (see bug 2431925).
    public static void setFolderBlockColor(Folder folder, View colorBlock) {
        final boolean showBg = !TextUtils.isEmpty(folder.bgColor);
        final int backgroundColor = showBg ? Color.parseColor(folder.bgColor) : 0;

        if (!showBg) {
            colorBlock.setBackgroundDrawable(null);
        } else {
            PaintDrawable paintDrawable = new PaintDrawable();
            paintDrawable.getPaint().setColor(backgroundColor);
            colorBlock.setBackgroundDrawable(paintDrawable);
        }
    }
}
