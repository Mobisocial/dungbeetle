/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * This file is part of Musubi, a mobile social network.
 *
 *  This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package edu.stanford.mobisocial.dungbeetle.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.Obj;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.DbObjects;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.feed.iface.Filterable;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfileObj;
import edu.stanford.mobisocial.dungbeetle.feed.view.PresenceView;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.AppsViewFragment;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

/**
 * TODO: This should be split into two classes: ViewProfileActivity and ViewContactActivity.
 */
public class ChangePictureActivity extends MusubiBaseActivity implements ViewPager.OnPageChangeListener, Filterable {
    @SuppressWarnings("unused")
    private static final String TAG = "ProfileActivity";
    private long mContactId;
    private Handler mHandler = new Handler();

    private ViewPager mViewPager;
    private final List<Button> mButtons = new ArrayList<Button>();
    private final List<Fragment> mFragments = new ArrayList<Fragment>();
    private final List<String> mLabels = new ArrayList<String>();
    ProfileContentObserver mProfileContentObserver;
    

    private final String[] filterTypes = DbObjects.getRenderableTypes();
    private boolean[] checked;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_home);

        
        checked = new boolean[filterTypes.length];
    	
        for(int x = 0; x < filterTypes.length; x++) {
        	checked[x] = true;
        }
        
        findViewById(R.id.btn_broadcast).setVisibility(View.GONE);
        mContactId = getIntent().getLongExtra("contact_id", -1);
        if (mContactId == -1) {
            Uri data = getIntent().getData();
            if (data != null) {
                try {
                    mContactId = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {}
            }
        }
        
        Bundle args = new Bundle();
        args.putLong("contact_id", mContactId);
        Fragment profileFragment = new ViewProfileFragment();
        profileFragment.setArguments(args);
        
        doTitleBar(this, "My Profile");
        mLabels.add("View");
        mLabels.add("Edit");
        mFragments.add(profileFragment);
        mFragments.add(new EditProfileFragment());

        Uri privateUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/private");
        mLabels.add("Notes");
        Fragment feedView = new FeedViewFragment();
        args = new Bundle(args);
        args.putParcelable(FeedViewFragment.ARG_FEED_URI, privateUri);
        feedView.setArguments(args);
        //mFragments.add(feedView);
        

        PagerAdapter adapter = new ViewFragmentAdapter(getSupportFragmentManager(), mFragments);
        mViewPager = (ViewPager)findViewById(R.id.feed_pager);
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(this);

        ViewGroup group = (ViewGroup)findViewById(R.id.tab_frame);
        int i = 0;
        for (String s : mLabels) {
            Button button = new Button(this);
            button.setText(s);
            button.setTextSize(18f);
            
            button.setLayoutParams(CommonLayouts.FULL_HEIGHT);
            button.setTag(i++);
            button.setOnClickListener(mViewSelected);

            group.addView(button);
            mButtons.add(button);
        }

        // Listen for future changes
        Uri feedUri;
        if (mContactId == Contact.MY_ID) {
            feedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        } else {
            feedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        }
        mProfileContentObserver = new ProfileContentObserver(mHandler);
        getContentResolver().registerContentObserver(feedUri, true, mProfileContentObserver);

        onPageSelected(0);
        
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Click on the avatar to take a profile picture of yourself.")
               .setCancelable(false)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mProfileContentObserver);
    }

    private class ProfileContentObserver extends ContentObserver {
        public ProfileContentObserver(Handler h) {
            super(h);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            for (Fragment f : mFragments) {
                if (f instanceof ViewProfileFragment) {
                    ((ViewProfileFragment)f).refresh();
                }
            }
        }
    }

    public class ViewFragmentAdapter extends FragmentPagerAdapter {
        final int NUM_ITEMS;
        final List<Fragment> mFragments;

        public ViewFragmentAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
            NUM_ITEMS = mFragments.size();
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    @Override
    public void onPageSelected(int position) {
        int c = mButtons.size();
        for (int i = 0; i < c; i++) {
            mButtons.get(i).setBackgroundColor(Color.TRANSPARENT);
        }
        mButtons.get(position).setBackgroundColor(R.color.default_tab_selected);
    }

    private View.OnClickListener mViewSelected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Integer i = (Integer)v.getTag();
            mViewPager.setCurrentItem(i);
        }
    };

    /**
     * Fragments for various Profile UIs
     */

    public static class EditProfileFragment extends Fragment {
        private ImageView mIcon;
        private EditText mProfileName;
        private EditText mProfileAbout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.edit_profile, container, false);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            final DBHelper mHelper = DBHelper.getGlobal(getActivity());
            final DBIdentityProvider mIdent = new DBIdentityProvider(mHelper);
            try {
	            mProfileName = (EditText) getView().findViewById(R.id.edit_profile_name);
	            mProfileAbout = (EditText) getView().findViewById(R.id.edit_profile_about);
	
	            mProfileName.setText(mIdent.userName());
	            Contact c = mIdent.contactForUser();
                mProfileName.setText(c.name);
                mProfileAbout.setText(c.status);
                Button saveButton = (Button) getView().findViewById(R.id.save_profile_button);
	            saveButton.setOnClickListener(new OnClickListener() {
	                public void onClick(View v) {
	                    String name = mProfileName.getText().toString();
	                    String about = mProfileAbout.getText().toString();
	                    MyInfo.setMyName(mHelper, name);
	                    MyInfo.setMyAbout(mHelper, about);
	                    Obj obj = ProfileObj.forLocalUser(getActivity(), name, about);
	                    Helpers.sendToEveryone(getActivity(), obj);
	                    Toast.makeText(getActivity(), "Profile updated.", Toast.LENGTH_SHORT).show();
	                    

			            Intent linkAccounts = new Intent(getActivity(), AccountLinkerActivity.class);
			            getActivity().startActivity(linkAccounts);
	                }
	            });
            } finally {
            	mIdent.close();
            	mHelper.close();
            }
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
        }
    }

    public static class ViewProfileFragment extends Fragment {
        private ImageView mIcon;
        private long mContactId;

        private TextView mProfileName;
        private TextView mProfileEmail;
        private TextView mProfileAbout;
		private Activity mActivity;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
        @Override
        public void onAttach(Activity activity) {
        	//we have to save this because we get a callback
        	mActivity = activity;
        	super.onDetach();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mContactId = getArguments().getLong("contact_id");
            View v = inflater.inflate(R.layout.view_self_profile, container, false);
            mIcon = (ImageView) v.findViewById(R.id.icon);
            
            final DBHelper mHelper = DBHelper.getGlobal(mActivity);
            final DBIdentityProvider mIdent = new DBIdentityProvider(mHelper);
            Contact c = null;
            try {
            	c = mIdent.contactForUser();
            } finally {
            	mHelper.close();
            	mIdent.close();
            }
            if (mContactId == Contact.MY_ID) {
                if (c.picture == null) {
                    mIcon.setImageResource(R.drawable.anonymous);
                } else {
                    mIcon.setImageBitmap(c.picture);
                }
                mIcon.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Toast.makeText(mActivity, "Loading camera...", Toast.LENGTH_SHORT)
                                .show();
                        ((InstrumentedActivity) mActivity).doActivityForResult(new PhotoTaker(
                        		mActivity, new PhotoTaker.ResultHandler() {
                                    @Override
                                    public void onResult(Uri imageUri, byte[] data) {
                                        Helpers.updatePicture(mActivity, data);
                                        // updateProfileToGroups();
                                        
                                        
                                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                                        builder.setMessage("Great! now you have a profile picture. Next, click on the Edit tab at the top so we can set your name.")
                                               .setCancelable(false)
                                               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                   public void onClick(DialogInterface dialog, int id) {
                                                        
                                                   }
                                               });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                    }
                                }, 200, false));
                    }
                });
            } else {
            }
            mProfileName = (TextView) v.findViewById(R.id.view_profile_name);
            mProfileEmail = (TextView) v.findViewById(R.id.view_profile_email);
            mProfileAbout = (TextView) v.findViewById(R.id.view_profile_about);
            return v;
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        public void refresh() {
        	View view = getView(); 
        	if(view == null)
        		return;
            Spinner presence = (Spinner) view.findViewById(R.id.presence);
            if (mContactId == Contact.MY_ID) {
                final DBHelper mHelper = DBHelper.getGlobal(mActivity);
                final DBIdentityProvider mIdent = new DBIdentityProvider(mHelper);
                Contact c = null;
                try {
                    c = mIdent.contactForUser();
                } finally {
                    mHelper.close();
                    mIdent.close();
                }

                try {
                    mProfileName.setText(c.name);
                    mProfileEmail.setText(c.email);
                    mProfileAbout.setText(c.status);
                    mIcon.setImageBitmap(c.picture);
                } finally {
                    mIdent.close();
                    mHelper.close();
                }

                presence.setOnItemSelectedListener(new PresenceOnItemSelectedListener(getActivity()));
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, Presence.presences);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                presence.setAdapter(adapter);

                Cursor cursor = getActivity().getContentResolver().query(
                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"), null,
                        DbObject.TYPE + "=?", new String[] {
                            PresenceObj.TYPE
                        }, DbObject.TIMESTAMP + " DESC");
                try {
	                if (cursor.moveToFirst()) {
	                    String jsonSrc = cursor.getString(cursor.getColumnIndexOrThrow(DbObject.JSON));
	                    try {
	                        JSONObject obj = new JSONObject(jsonSrc);
	                        int myPresence = Integer.parseInt(obj.optString("presence"));
	                        presence.setSelection(myPresence);
	                    } catch (JSONException e) {
	                    }
	                }
                } finally {
                    cursor.close();
                }
            } else {
                presence.setVisibility(View.GONE);
                try {
                    Contact contact = Contact.forId(getActivity(), mContactId).get();
                    mProfileName.setText(contact.name);
                    mProfileEmail.setText(contact.email);
                    mProfileAbout.setText(contact.status);
                    mIcon.setImageBitmap(contact.picture);
                } catch (NoValError e) {}
            }
        }

        private class PresenceOnItemSelectedListener implements OnItemSelectedListener {
            private boolean mEnablePresenceUpdates = false;

            private Activity mActivity;

            public PresenceOnItemSelectedListener(Activity context) {
                mActivity = context;
            }

            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // fix bug where initial selection firing event
                if (mEnablePresenceUpdates) {
                    Helpers.updatePresence(mActivity, pos);
                } else {
                    mEnablePresenceUpdates = true;
                }
            }

            public void onNothingSelected(AdapterView parent) {
                // Do nothing.
            }
        }
    }

    @Override
	public String[] getFilterTypes() {
		return filterTypes;
	}

	@Override
	public boolean[] getFilterCheckboxes() {
		return checked;
	}

	@Override
	public void setFilterCheckbox(int position, boolean check) {
		checked[position] = check;
	}
}
