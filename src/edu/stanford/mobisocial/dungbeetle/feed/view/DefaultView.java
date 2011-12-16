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

package edu.stanford.mobisocial.dungbeetle.feed.view;

import android.support.v4.app.Fragment;
import edu.stanford.mobisocial.dungbeetle.feed.iface.FeedView;
import edu.stanford.mobisocial.dungbeetle.ui.fragments.FeedViewFragment;

public class DefaultView implements FeedView {
    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public Fragment getFragment() {
        return new FeedViewFragment();
    }
}
