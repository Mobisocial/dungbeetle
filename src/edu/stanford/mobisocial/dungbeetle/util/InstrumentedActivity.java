package edu.stanford.mobisocial.dungbeetle.util;

import android.app.Dialog;
import android.content.Intent;

public interface InstrumentedActivity {
    public void doActivityForResult(ActivityCallout callout);
    public void onActivityResult(int requestCode, int resultCode, Intent data);
    public void showDialog(Dialog dialog);
}
