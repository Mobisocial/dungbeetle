package edu.stanford.mobisocial.dungbeetle.util;

import android.content.Context;
import android.content.Intent;

public abstract class ContactPickerCallout implements ActivityCallout {
    private final Context mContext;

    public ContactPickerCallout(Context context) {
        mContext = context;
    }
    @Override
    public Intent getStartIntent() {
        return new Intent(mContext, AddressBookPicker.class);
    }
}
