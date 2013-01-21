/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openwatch.reporter.feeds;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import net.openwatch.reporter.RecordingViewActivity;
import net.openwatch.reporter.R;
import net.openwatch.reporter.StoryViewActivity;
import net.openwatch.reporter.constants.Constants.OWContentType;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.http.OWServiceRequests;

/**
 * Demonstration of the implementation of a custom Loader.
 */
public class RemoteFeedFragmentActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            RemoteRecordingsListFragment list = new RemoteRecordingsListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
        
    }


    public static class RemoteRecordingsListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {
    	
    	static String TAG = "RemoteFeedFragment";
    	static boolean didRefreshFeed = false;
    	
    	OWFeedType feed;

        // This is the Adapter being used to display the list's data.
        //AppListAdapter mAdapter;
    	OWMediaObjectAdapter mAdapter;

        // If non-null, this is the current filter the user has provided.
        String mCurFilter;

        OnQueryTextListenerCompat mOnQueryTextListenerCompat;

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Give some text to display if there is no data.  In a real
            // application this would come from a resource.
            setEmptyText(getString(R.string.loading_feed));

            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);

            // Initialize adapter without cursor. Let loader provide it when ready
            mAdapter = new OWMediaObjectAdapter(getActivity(), null); 
            setListAdapter(mAdapter);

            // Start out with a progress indicator.
            setListShown(false);
            
            feed = (OWFeedType) this.getArguments().getSerializable(Constants.OW_FEED);
            Log.i(TAG, "got feed name: " +  feed.toString() );
            
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
            
            // Refresh the feed view
            if(!didRefreshFeed){
            	OWServiceRequests.getFeed(this.getActivity().getApplicationContext(), feed, 1);
            	didRefreshFeed = true;
            }

        }

        @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Place an action bar item for searching.
        	/*
            MenuItem item = menu.add("Search");
            item.setIcon(android.R.drawable.ic_menu_search);
            MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                    | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            View searchView = SearchViewCompat.newSearchView(getActivity());
            if (searchView != null) {
                SearchViewCompat.setOnQueryTextListener(searchView,
                        new OnQueryTextListenerCompat() {
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        // Called when the action bar search text has changed.  Since this
                        // is a simple array adapter, we can just have it do the filtering.
                        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
                        mAdapter.getFilter().filter(mCurFilter);
                        return true;
                    }
                });
                MenuItemCompat.setActionView(item, searchView);
            }
            */
        }

        @Override public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("LoaderCustom", "Item clicked: " + id);
        	try{
        		Intent i = null;
        		switch((OWContentType)v.getTag(R.id.list_item_model_type)){
        		case VIDEO:
        			i = new Intent(this.getActivity(), RecordingViewActivity.class);
        			break;
        		case STORY:
        			i = new Intent(this.getActivity(), StoryViewActivity.class);
        			break;
        		}
        		i.putExtra(Constants.INTERNAL_DB_ID, (Integer)v.getTag(R.id.list_item_model));
        		if(i != null)
        			startActivity(i);
        	}catch(Exception e){
        		Log.e(TAG, "failed to load list item model tag");
        		return;
        	}
        	
        }

        
		@Override
		public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
			mAdapter.swapCursor(cursor);
			// The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
            
           if(cursor != null && cursor.getCount() == 0){
        		setEmptyText(getString(R.string.feed_empty));
           }
			
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			// TODO Auto-generated method stub
			mAdapter.swapCursor(null);
		}
		
		static final String[] PROJECTION = new String[] {
			DBConstants.ID,
			DBConstants.RECORDINGS_TABLE_TITLE,
			DBConstants.RECORDINGS_TABLE_VIEWS,
			DBConstants.RECORDINGS_TABLE_ACTIONS,
			DBConstants.RECORDINGS_TABLE_THUMB_URL,
			DBConstants.RECORDINGS_TABLE_USERNAME,
			DBConstants.MEDIA_OBJECT_STORY,
			DBConstants.MEDIA_OBJECT_VIDEO

	    };

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			Uri baseUri = OWContentProvider.getFeedUri(feed);
			String selection = null;
            String[] selectionArgs = null;
            String order = null;
			
			return new CursorLoader(getActivity(), baseUri, PROJECTION, selection, selectionArgs, order);
		}
    }

}
