package edu.stanford.mobisocial.dungbeetle.ui.fragments;
import java.util.Collection;
import java.util.Collections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.ActionItem;
import edu.stanford.mobisocial.dungbeetle.App;
import edu.stanford.mobisocial.dungbeetle.DBHelper;
import edu.stanford.mobisocial.dungbeetle.DBIdentityProvider;
import edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider;
import edu.stanford.mobisocial.dungbeetle.Helpers;
import edu.stanford.mobisocial.dungbeetle.IdentityProvider;
import edu.stanford.mobisocial.dungbeetle.QuickAction;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.UIHelpers;
import edu.stanford.mobisocial.dungbeetle.model.Contact;
import edu.stanford.mobisocial.dungbeetle.model.Group;
import edu.stanford.mobisocial.dungbeetle.ui.MusubiBaseActivity;
import edu.stanford.mobisocial.dungbeetle.util.BitmapManager;
import edu.stanford.mobisocial.dungbeetle.util.Maybe;
import edu.stanford.mobisocial.dungbeetle.util.Maybe.NoValError;

/**
 * Displays a list of contacts. If the intent used to create
 * this activity as Long extra "group_id", contacts are chosen
 * from this group. Otherwise, lists all known contacts.
 *
 */
public class FeedMembersFragment extends ListFragment implements OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
	private ContactListCursorAdapter mContacts;
	protected final BitmapManager mBitmaps = new BitmapManager(20);
	private static final int REQUEST_INVITE_TO_GROUP = 471;
	public static final String TAG = "ContactsActivity";

	private DBHelper mHelper;
    private Maybe<Group> mGroup = Maybe.unknown();
    private Uri mFeedUri;
    private String mFeedName;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHelper = DBHelper.getGlobal(getActivity());
		getLoaderManager().initLoader(0, null, this);

		groupUpdateHack();
	}

    private void groupUpdateHack() {
        final Context context = getActivity();
        if (MusubiBaseActivity.isDeveloperModeEnabled(context)) {
            for (int i = 0; i < 29; i++) {
                // If you are reading me, consider fixing
                // the real problem rather than removing this spew.
                Log.d("MUSUBI", "THIS IS A HORRIBLE HACK.");
            }
        }

        new Thread() {
            public void run() {
                final IdentityProvider ident = new DBIdentityProvider(mHelper);
                Maybe<Group> mg = mHelper.groupByFeedName(mFeedName);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                try {
                    // group exists already, load view
                    final Group g = mg.get();
                    Collection<Contact> existingContacts = g.contactCollection(mHelper);
                    g.forceUpdate(context);
                    Collection<Contact> newContacts = g.contactCollection(mHelper);
                    newContacts.removeAll(existingContacts);

                    Helpers.resendProfile(context, newContacts, true);
                }
                catch(Maybe.NoValError e) { }
                ident.close();
            };
        }.start();
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor)mContacts.getItem(info.position);
        final Contact c = new Contact(cursor);
        menu.setHeaderTitle(c.name);
        String[] menuItems = new String[]{ "Delete" };
        for (int i = 0; i<menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();

        Cursor cursor = (Cursor)mContacts.getItem(info.position);
        final Contact c = new Contact(cursor);

 
        switch(menuItemIndex) {
        case 0:
            Helpers.deleteContact(getActivity(), c.id);
            break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Cursor cursor = (Cursor)mContacts.getItem(position);
        new Contact(cursor).view(getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setFastScrollEnabled(true);
        //registerForContextMenu(lv);
        lv.setOnItemClickListener(this);
        //lv.setCacheColorHint(Feed.colorFor(groupName, Feed.BACKGROUND_ALPHA));
    }

    private class ContactListCursorAdapter extends CursorAdapter {
        public ContactListCursorAdapter (Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.contacts_item, parent, false);
            bindView(v, context, c);
            return v;
        }

        @Override
        public void bindView(View v, Context context, Cursor cursor) {
            final Contact c = new Contact(cursor);

            TextView unreadCount = (TextView)v.findViewById(R.id.unread_count);
            unreadCount.setTextColor(Color.RED);
            unreadCount.setText(c.numUnread + " unread");
            unreadCount.setVisibility(c.numUnread == 0 ? View.INVISIBLE : View.VISIBLE);

            TextView nameText = (TextView) v.findViewById(R.id.name_text);
            nameText.setText(c.name);

            TextView statusText = (TextView) v.findViewById(R.id.status_text);
            statusText.setText(c.status);
            
            final ImageView icon = (ImageView)v.findViewById(R.id.icon);
            ((App)getActivity().getApplication()).contactImages.lazyLoadContactPortrait(c, icon);

            final ImageView presenceIcon = (ImageView)v.findViewById(R.id.presence_icon);
            presenceIcon.setImageResource(c.currentPresenceResource());

            final ImageView nearbyIcon = (ImageView)v.findViewById(R.id.nearby_icon);
        	nearbyIcon.setVisibility(c.nearby ? View.VISIBLE : View.GONE);

            final ImageView more = (ImageView)v.findViewById(R.id.more);

            more.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ActionItem send_im = new ActionItem();
                        send_im.setTitle("Send IM");
                        send_im.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.sendIM(getActivity(), Collections.singletonList(c));
                                }
                            });
                        /*
                        final ActionItem start_app = new ActionItem();
                        start_app.setTitle("Start App");
                        start_app.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AppReferenceObj.promptForApplication(ContactsActivity.this, new AppReferenceObj.Callback() {
                                        @Override
                                        public void onAppSelected(String packageName, String arg, Intent localLaunch) {
                                            DbObject obj = new AppReference(packageName, arg);
                                            Helpers.sendMessage(ContactsActivity.this, Collections.singletonList(c), obj);
                                            startActivity(localLaunch);
                                        }
                                    });
                                }
                            });
                    */
                        final ActionItem manage_groups = new ActionItem();
                        manage_groups.setTitle("Show Groups");
                        manage_groups.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    UIHelpers.showGroupPicker(
                                        getActivity(), 
                                        c,
                                        null);
                                }
                            });

                        QuickAction qa = new QuickAction(v);
                        //qa.addActionItem(send_im);
                        //qa.addActionItem(start_app);
                        qa.addActionItem(manage_groups);
                        qa.setAnimStyle(QuickAction.ANIM_GROW_FROM_RIGHT);

                        qa.show();
                    }
                });
        }
    }

    public boolean onCreateOptionsMenu(Menu menu){
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHelper.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_INVITE_TO_GROUP) {
            if (resultCode == Activity.RESULT_OK) {
                long[] contactIds = data.getLongArrayExtra("contacts");
                try {
                    Helpers.sendGroupInvite(getActivity(), contactIds, mGroup.get());
                } catch(Maybe.NoValError e) {}
            }
        }
    }

    @SuppressWarnings("unused")
    private final void toast(final String text) {
    	getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View findViewById(int id) {
        return getActivity().findViewById(id);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        mFeedUri = getArguments().getParcelable("feed_uri");
        mFeedName = mFeedUri.getLastPathSegment();
        mGroup = mHelper.groupForFeedName(mFeedName);
        
        long gid;
        try {
            gid = mGroup.get().id;
        } catch (NoValError e) {
            Log.e(TAG, "No group for feed.");
            return null;
        }
        Uri memberlist = Uri.parse(DungBeetleContentProvider.CONTENT_URI +
                "/group_contacts/" + gid);
        return new CursorLoader(getActivity(), memberlist, null, null, null, Contact.NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mContacts = new ContactListCursorAdapter(getActivity(), cursor);
        setListAdapter(mContacts);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }
}
