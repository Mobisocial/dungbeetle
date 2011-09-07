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
import android.util.Log;
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
    @SuppressWarnings("unused")
    private static final String TAG = "ProfileActivity";
    private long mContactId;
    private Handler mHandler = new Handler();

    private ViewPager mViewPager;
    private final List<Button> mButtons = new ArrayList<Button>();
    private final List<Fragment> mFragments = new ArrayList<Fragment>();
    private final List<String> mLabels = new ArrayList<String>();
    ProfileContentObserver mProfileContentObserver;

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
                feedUri = contact.getFeedUri();
            } catch (NoValError e) {}
            doTitleBar(this, title);
            mLabels.add("Feed");
            mLabels.add("Profile");
            args.putParcelable(FeedViewFragment.ARG_FEED_URI, feedUri);
            Fragment feedView = new FeedViewFragment();
            feedView.setArguments(args);
            mFragments.add(feedView);
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
        Uri feedUri;
        if (mContactId == Contact.MY_ID) {
            feedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me");
        } else {
            feedUri = Uri.parse(DungBeetleContentProvider.CONTENT_URI + "/contacts");
        }
        mProfileContentObserver = new ProfileContentObserver(mHandler);
        getContentResolver().registerContentObserver(feedUri, true, mProfileContentObserver);

        onPageSelected(0);
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
        private DBHelper mHelper;
        private IdentityProvider mIdent;
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
            mHelper = new DBHelper(getActivity());
            mIdent = new DBIdentityProvider(mHelper);

            mProfileName = (EditText) getView().findViewById(R.id.edit_profile_name);
            mProfileAbout = (EditText) getView().findViewById(R.id.edit_profile_about);

            mProfileName.setText(mIdent.userName());
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
                    mProfileName.setText(name);
                    mProfileAbout.setText(about);
                } catch (JSONException e) {
                }
            }

            Button saveButton = (Button) getView().findViewById(R.id.save_profile_button);
            saveButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String name = mProfileName.getText().toString();
                    String about = mProfileAbout.getText().toString();
                    MyInfo.setMyName(mHelper, mProfileName.getText().toString());
                    Helpers.updateProfile(getActivity(), name, about);
                    Toast.makeText(getActivity(), "Profile updated.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
        }
    }

    public static class ViewProfileFragment extends Fragment {
        private IdentityProvider mIdent;
        private DBHelper mHelper;
        private ImageView mIcon;
        private long mContactId;

        private TextView mProfileName;
        private TextView mProfileEmail;
        private TextView mProfileAbout;

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
                        mProfileName.setText(name);
                        mProfileEmail.setText(mIdent.userEmail());
                        mProfileAbout.setText(about);
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
                        .parse(DungBeetleContentProvider.CONTENT_URI + "/feeds/me/head");
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
                    mProfileName.setText(contact.name);
                    mProfileEmail.setText(contact.email);
                    mProfileAbout.setText(contact.status);
                    ((App)getActivity().getApplication()).contactImages.lazyLoadContactPortrait(
                            contact, mIcon, 200);
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
}
