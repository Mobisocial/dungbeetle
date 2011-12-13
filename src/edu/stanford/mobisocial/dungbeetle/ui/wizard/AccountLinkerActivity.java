package edu.stanford.mobisocial.dungbeetle.ui.wizard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.stanford.mobisocial.dungbeetle.R;

public class AccountLinkerActivity extends Activity {

	int step;
	TextView step1, step2, step3;
	final CharSequence[] accounts = {"Phone Number", "Google", "Facebook"};
	final boolean[] tempSelected = new boolean[3];
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_base);
        
        step = 0;

    	
        tempSelected[0] = true;
        tempSelected[1] = true;
        
        RelativeLayout layout = (RelativeLayout)findViewById(R.id.content);
        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
        View view = layoutInflater.inflate(R.layout.wizard_account_linker, null, false);
        layout.addView(view, 0);

        step1 = (TextView) view.findViewById(R.id.step1);
        step2 = (TextView) view.findViewById(R.id.step2);
        step3 = (TextView) view.findViewById(R.id.step3);
        
        step1.setTypeface(null, Typeface.BOLD);
        
        ((Button)findViewById(R.id.back_button)).setOnClickListener(
    			new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						AccountLinkerActivity.this.finish();
					}
    				
    			});
        
        ((Button)findViewById(R.id.next_button)).setOnClickListener(
    			new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						step++;
						switch(step) {
							case 1:
								pickAccounts();
								break;
							case 2:
								linkAccounts();
								break;
							case 3:
								findFriends();
								break;
							case 4:
								AccountLinkerActivity.this.finish();
								break;
								
						}
					}
    				
    			});
        

    }
    
    public void findFriends() {
    	ProgressDialog dialog = ProgressDialog.show(AccountLinkerActivity.this, "", 
                "Finding friends...", true);
    	dialog.setCancelable(true);
    	dialog.show();
    }
    
    public void pickAccounts() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Accounts");
        
        builder.setMultiChoiceItems(
            accounts, tempSelected, 
            new DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                }
            });

        builder.setPositiveButton("Next",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	step1.setTypeface(null, Typeface.NORMAL);
            	step2.setTypeface(null, Typeface.BOLD);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    public void linkAccounts() {
    	/*for (int i = 0; i < tempSelected.length; i++) {
	    	if (tempSelected[i]) {
	    		linkAccount(i);
	    	}
    	}

    	step2.setTypeface(null, Typeface.NORMAL);
    	step3.setTypeface(null, Typeface.BOLD);*/
    	linkAccount(0);
    }
    
    public void linkAccount(int acc) {

		final LayoutInflater factory = LayoutInflater.from(this);
		AlertDialog dialog;
		
    	switch(acc) {
    		case 0:
    			if (tempSelected[0]) {
	    			TelephonyManager tMgr =(TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
					String mPhoneNumber = tMgr.getLine1Number();
	
		            final View phoneView = factory.inflate(R.layout.wizard_phone, null);
	
		            EditText number1 = (EditText) phoneView.findViewById(R.id.number1);
		            EditText number2 = (EditText) phoneView.findViewById(R.id.number2);
		            
		            if(number1 == null) {
		            	Log.w("acc", "number1 is null");
		            	
		            }
		            if(mPhoneNumber == null) {
		            	Log.w("acc", "phone number is null");
		            	mPhoneNumber = "";
		            }
		            Log.w("acc", mPhoneNumber);
		            number1.setText(mPhoneNumber);
		            
		            
		            dialog = new AlertDialog.Builder(AccountLinkerActivity.this)
		                .setTitle("Phone Number")
		                .setView(phoneView)
		                .setPositiveButton("Next", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
	
		    		            String number = ((EditText) phoneView.findViewById(R.id.number1)).getText().toString();
		    		            String confirmNumber = ((EditText) phoneView.findViewById(R.id.number2)).getText().toString();
		    		            Log.w("Acc", number + "=" + confirmNumber);
		    		            if(number.equalsIgnoreCase(confirmNumber)) {
			                    	linkAccount(1);
		    		            }
		    		            else {
		    		            	Toast.makeText(AccountLinkerActivity.this, "Numbers don't match", Toast.LENGTH_SHORT).show();
			                    	linkAccount(0);
		    		            }
		                    }
		                })
		                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
	
		                        /* User clicked cancel so do some stuff */
		                    	linkAccount(1);
		                    }
		                })
		                .create();
		            dialog.show();
    			}
    			break;
	    	case 1:
	    		if (tempSelected[1]) {
		            final View googleView = factory.inflate(R.layout.wizard_user_pass, null);
		            
		            Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
		            

		            EditText userName = (EditText) googleView.findViewById(R.id.username);
		            //Account account = new Account(name, "com.google");
		            Account account = accounts[0];
		            userName.setText(account.name);
		            Log.w("acc", account.toString());
		            //AccountManagerFuture<Bundle> accFut = AccountManager.get(this).getAuthToken(account, "ah", null, AccountLinkerActivity, null, null);
		            //Bundle authTokenBundle = accFut.getResult();
		            //String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
		            
		            dialog = new AlertDialog.Builder(AccountLinkerActivity.this)
		                .setTitle("Google")
		                .setView(googleView)
		                .setPositiveButton("Next", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {

		                    	linkAccount(2);
		                        /* User clicked OK so do some stuff */
		                    }
		                })
		                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {

		                    	linkAccount(2);
		                        /* User clicked cancel so do some stuff */
		                    }
		                })
		                .create();
		            dialog.show();
	    		}
	    		
	    		break;
	    	case 2:
	        	step2.setTypeface(null, Typeface.NORMAL);
	        	step3.setTypeface(null, Typeface.BOLD);
	        	break;
    	}
    }
}