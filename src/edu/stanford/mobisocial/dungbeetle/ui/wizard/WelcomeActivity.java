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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import edu.stanford.mobisocial.dungbeetle.R;
import edu.stanford.mobisocial.dungbeetle.ui.HomeActivity;

public class WelcomeActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_base);
        
        
        RelativeLayout layout = (RelativeLayout)findViewById(R.id.content);
        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
        layout.addView(layoutInflater.inflate(R.layout.wizard_introduction, null, false), 0 );

        ((Button)findViewById(R.id.back_button)).setOnClickListener(
    			new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						WelcomeActivity.this.finish();
					}
    				
    			});
        
        ((Button)findViewById(R.id.next_button)).setOnClickListener(
    			new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub

			            Intent setProfile = new Intent(WelcomeActivity.this, SetProfileInstructionsActivity.class);
			            WelcomeActivity.this.startActivity(setProfile);
						//WelcomeActivity.this.finish();
					}
    				
    			});
        

    }
}