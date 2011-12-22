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

package edu.stanford.mobisocial.dungbeetle.util;

import android.view.ViewGroup.LayoutParams;

/**
 * Constants for commonly-used Android layouts.
 *
 */
public class CommonLayouts {
    public static LayoutParams FULL_WIDTH = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

    public static LayoutParams FULL_HEIGHT = new LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);

    public static LayoutParams FULL_SCREEN = new LayoutParams(
            LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

    public static LayoutParams WRAPPED = new LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
}
