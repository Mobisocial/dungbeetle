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

package edu.stanford.mobisocial.dungbeetle;

import android.content.Context;

import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import android.widget.PopupWindow;

/**
 * This class does most of the work of wrapping the {@link PopupWindow} so it's simpler to use. 
 * Edited by Lorensius. W. L. T
 * 
 * @author qberticus
 * 
 */
public class CustomPopupWindow {
	protected final View anchor;
	protected final PopupWindow window;
	private View root;
	private Drawable background = null;
	protected final WindowManager windowManager;
	
	/**
	 * Create a QuickAction
	 * 
	 * @param anchor
	 *            the view that the QuickAction will be displaying 'from'
	 */
	public CustomPopupWindow(View anchor) {
		this.anchor = anchor;
		this.window = new PopupWindow(anchor.getContext());

		// when a touch even happens outside of the window
		// make the window go away
		window.setTouchInterceptor(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					CustomPopupWindow.this.window.dismiss();
					
					return true;
				}
				
				return false;
			}
		});

		windowManager = (WindowManager) anchor.getContext().getSystemService(Context.WINDOW_SERVICE);
		
		onCreate();
	}

	/**
	 * Anything you want to have happen when created. Probably should create a view and setup the event listeners on
	 * child views.
	 */
	protected void onCreate() {}

	/**
	 * In case there is stuff to do right before displaying.
	 */
	protected void onShow() {}

	protected void preShow() {
		if (root == null) {
			throw new IllegalStateException("setContentView was not called with a view to display.");
		}
		
		onShow();

		if (background == null) {
			window.setBackgroundDrawable(new BitmapDrawable());
		} else {
			window.setBackgroundDrawable(background);
		}

		// if using PopupWindow#setBackgroundDrawable this is the only values of the width and hight that make it work
		// otherwise you need to set the background of the root viewgroup
		// and set the popupwindow background to an empty BitmapDrawable
		
		window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setTouchable(true);
		window.setFocusable(true);
		window.setOutsideTouchable(true);

		window.setContentView(root);
	}

	public void setBackgroundDrawable(Drawable background) {
		this.background = background;
	}

	/**
	 * Sets the content view. Probably should be called from {@link onCreate}
	 * 
	 * @param root
	 *            the view the popup will display
	 */
	public void setContentView(View root) {
		this.root = root;
		
		window.setContentView(root);
	}

	/**
	 * Will inflate and set the view from a resource id
	 * 
	 * @param layoutResID
	 */
	public void setContentView(int layoutResID) {
		LayoutInflater inflator =
				(LayoutInflater) anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setContentView(inflator.inflate(layoutResID, null));
	}

	/**
	 * If you want to do anything when {@link dismiss} is called
	 * 
	 * @param listener
	 */
	public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
		window.setOnDismissListener(listener);
	}

	/**
	 * Displays like a popdown menu from the anchor view
	 */
	public void showDropDown() {
		showDropDown(0, 0);
	}

	/**
	 * Displays like a popdown menu from the anchor view.
	 * 
	 * @param xOffset
	 *            offset in X direction
	 * @param yOffset
	 *            offset in Y direction
	 */
	public void showDropDown(int xOffset, int yOffset) {
		preShow();

		window.setAnimationStyle(R.style.Animations_PopDownMenu_Left);

		window.showAsDropDown(anchor, xOffset, yOffset);
	}

	/**
	 * Displays like a QuickAction from the anchor view.
	 */
	public void showLikeQuickAction() {
		showLikeQuickAction(0, 0);
	}

	/**
	 * Displays like a QuickAction from the anchor view.
	 * 
	 * @param xOffset
	 *            offset in the X direction
	 * @param yOffset
	 *            offset in the Y direction
	 */
	public void showLikeQuickAction(int xOffset, int yOffset) {
		preShow();

		window.setAnimationStyle(R.style.Animations_PopUpMenu_Center);

		int[] location = new int[2];
		anchor.getLocationOnScreen(location);

		Rect anchorRect =
				new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1]
					+ anchor.getHeight());

		root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		int rootWidth 		= root.getMeasuredWidth();
		int rootHeight 		= root.getMeasuredHeight();

		int screenWidth 	= windowManager.getDefaultDisplay().getWidth();
		//int screenHeight 	= windowManager.getDefaultDisplay().getHeight();

		int xPos 			= ((screenWidth - rootWidth) / 2) + xOffset;
		int yPos	 		= anchorRect.top - rootHeight + yOffset;

		// display on bottom
		if (rootHeight > anchorRect.top) {
			yPos = anchorRect.bottom + yOffset;
			
			window.setAnimationStyle(R.style.Animations_PopDownMenu_Center);
		}

		window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}
	
	public void dismiss() {
		window.dismiss();
	}
}
