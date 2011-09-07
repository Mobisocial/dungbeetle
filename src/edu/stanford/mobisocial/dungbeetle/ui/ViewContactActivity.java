package edu.stanford.mobisocial.dungbeetle.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
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
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.feed.objects.PresenceObj;
import edu.stanford.mobisocial.dungbeetle.feed.objects.ProfilePictureObj;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.DbObject;
import edu.stanford.mobisocial.dungbeetle.model.MyInfo;
import edu.stanford.mobisocial.dungbeetle.model.Presence;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;
import edu.stanford.mobisocial.dungbeetle.util.CommonLayouts;
import edu.stanford.mobisocial.dungbeetle.util.InstrumentedActivity;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;
import edu.stanford.mobisocial.dungbeetle.util.PhotoTaker;

public class ViewContactActivity extends MusubiBaseActivity implements ViewPager.OnPageChangeListener {
    private Handler handler = new Handler();
    @SuppressWarnings("unused")
    private static final String TAG = "ProfileActivity";
    private long mContactId;

    private ViewPager mViewPager;
    private final List<Button> mButtons = new ArrayList<Button>();
    private final List<Fragment> mFragments = new ArrayList<Fragment>();
    private final List<String> mLabels = new ArrayList<String>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_home);
        mContactId = getIntent().getLongExtra("contact_id", -1);
        
        Bundle args = new Bundle();
        args.putLong("contact_id", mContactId);
        Fragment profileFragment = new ViewProfileFragment();
        profileFragment.setArguments(args);
        if (mContactId == Contact.MY_ID) {
            doTitleBar(this, "My Profile");
            mLabels.add("View");
            mLabels.add("Edit");
            mFragments.add(profileFragment);
            mFragments.add(new EditProfileFragment());
        } else {
            String title = "Profile";
            Uri feedUri = null;
            try {
                Contact contact = Contact.forId(this, mContactId).get();
                title = contact.name;
                feedUri = null; // TODO: contact.getFeedUri();
            } catch (NoValError e) {}
            doTitleBar(this, title);
            //mLabels.add("Feed");
            mLabels.add("Profile");
            args.putParcelable(FeedViewFragment.ARG_FEED_URI, feedUri);
            //mFragments.add(new FeedViewFragment());
            mFragments.add(profileFragment);
        }

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
        ProfileContentObserver profileContentObserver = new ProfileContentObserver(handler);
        getContentResolver().registerContentObserver(
            Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend"), 
            true, profileContentObserver);
        onPageSelected(0);
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
            // TODO, this hack relies on the fragments having all
            // ui-populating code in onActivityCreated().
            for (Fragment f : mFragments) {
                f.onActivityCreated(null);
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
        private DBHelper mHelper;
        private IdentityProvider mIdent;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.edit_profile, container, false);
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mHelper = new DBHelper(getActivity());
            mIdent = new DBIdentityProvider(mHelper);

            final EditText profileName = (EditText) getView().findViewById(R.id.edit_profile_name);
            final EditText profileAbout = (EditText) getView()
                    .findViewById(R.id.edit_profile_about);

            profileName.setText(mIdent.userName());
            Cursor c = getActivity().getContentResolver().query(
                    Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"), null,
                    DbObject.TYPE + "=? AND " + DbObject.CONTACT_ID + "=?", new String[] {
                            "profile", Long.toString(Contact.MY_ID)
                    }, DbObject.TIMESTAMP + " DESC");

            if (c.moveToFirst()) {
                String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
                try {
                    JSONObject obj = new JSONObject(jsonSrc);
                    String name = obj.optString("name");
                    String about = obj.optString("about");
                    profileName.setText(name);
                    profileAbout.setText(about);
                } catch (JSONException e) {
                }
            }

            Button saveButton = (Button) getView().findViewById(R.id.save_profile_button);
            saveButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String name = profileName.getText().toString();
                    String about = profileAbout.getText().toString();
                    MyInfo.setMyName(mHelper, profileName.getText().toString());
                    Helpers.updateProfile(getActivity(), name, about);
                    Toast.makeText(getActivity(), "Profile updated.", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    public static class ViewProfileFragment extends Fragment {
        private IdentityProvider mIdent;
        private DBHelper mHelper;
        private ImageView mIcon;
        private long mContactId;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mHelper = new DBHelper(getActivity());
            mIdent = new DBIdentityProvider(mHelper);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mContactId = getArguments().getLong("contact_id");
            return getProfileView(inflater, container);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            TextView profileName = (TextView) getView().findViewById(R.id.view_profile_name);
            TextView profileEmail = (TextView) getView().findViewById(R.id.view_profile_email);
            TextView profileAbout = (TextView) getView().findViewById(R.id.view_profile_about);

            Spinner presence = (Spinner) getView().findViewById(R.id.presence);
            if (mContactId == Contact.MY_ID) {
                Cursor c = getActivity().getContentResolver().query(
                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head"), null,
                        DbObject.TYPE + "= ? AND " + DbObject.CONTACT_ID + "= ?", new String[] {
                                "profile", Long.toString(mContactId)
                        }, DbObject.TIMESTAMP + " DESC");

                if (c.moveToFirst()) {
                    String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
                    try {
                        JSONObject obj = new JSONObject(jsonSrc);
                        String name = obj.optString("name");
                        String about = obj.optString("about");
                        profileName.setText(name);
                        profileEmail.setText(mIdent.userEmail());
                        profileAbout.setText(about);
                    } catch (JSONException e) {
                    }
                }

                presence.setOnItemSelectedListener(new PresenceOnItemSelectedListener(getActivity()));
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_spinner_item, Presence.presences);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                presence.setAdapter(adapter);

                c = getActivity().getContentResolver().query(
                        Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head"), null,
                        DbObject.TYPE + "=?", new String[] {
                            PresenceObj.TYPE
                        }, DbObject.TIMESTAMP + " DESC");

                if (c.moveToFirst()) {
                    String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));
                    try {
                        JSONObject obj = new JSONObject(jsonSrc);
                        int myPresence = Integer.parseInt(obj.optString("presence"));
                        presence.setSelection(myPresence);
                    } catch (JSONException e) {
                    }
                }

                Uri profileUri = Uri
                        .parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/friend/head");
                c = getActivity().getContentResolver().query(profileUri, null, DbObject.TYPE + "=?",
                        new String[] {
                            ProfilePictureObj.TYPE
                        }, DbObject.TIMESTAMP + " DESC");

                if (c.moveToFirst()) {
                    String jsonSrc = c.getString(c.getColumnIndexOrThrow(DbObject.JSON));

                    try {
                        JSONObject obj = new JSONObject(jsonSrc);
                        String bytes = obj.optString(ProfilePictureObj.DATA);
                        ((App) getActivity().getApplication()).objectImages.lazyLoadImage(
                                bytes.hashCode(), bytes, mIcon);
                    } catch (JSONException e) {
                    }
                }
            } else {
                presence.setVisibility(View.GONE);
                try {
                    Contact contact = Contact.forId(getActivity(), mContactId).get();
                    profileName.setText(contact.name);
                    profileEmail.setText(contact.email);
                    profileAbout.setText(contact.status);
                    ((App)getActivity().getApplication()).contactImages.lazyLoadContactPortrait(
                            contact, mIcon, 200);
                } catch (NoValError e) {}
            }
        }

        private View getProfileView(LayoutInflater inflater, ViewGroup container) {
            View v = inflater.inflate(R.layout.view_self_profile, container, false);
            mIcon = (ImageView) v.findViewById(R.id.icon);
            if (mContactId == Contact.MY_ID) {
                mIcon.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        Toast.makeText(getActivity(), "Loading camera...", Toast.LENGTH_SHORT)
                                .show();
                        ((InstrumentedActivity) getActivity()).doActivityForResult(new PhotoTaker(
                                getActivity(), new PhotoTaker.ResultHandler() {
                                    @Override
                                    public void onResult(byte[] data) {
                                        Helpers.updatePicture(getActivity(), data);
                                        // updateProfileToGroups();
                                    }
                                }, 200, false));
                    }
                });
            }
            return v;
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
}
