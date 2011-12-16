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

package edu.stanford.mobisocial.dungbeetle.obj;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.obj.action.DeleteAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.EditPhotoAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.ExportPhotoAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.ForwardObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.OpenObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.PlayAllAudioAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.RelatedObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.SetProfileObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.ViewFeedObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

public class ObjActions {
    private static final List<ObjAction> sActions = new ArrayList<ObjAction>();
    static {
        sActions.add(new ForwardObjAction());
        sActions.add(new OpenObjAction());
        sActions.add(new EditPhotoAction());
        sActions.add(new ExportPhotoAction());
        sActions.add(new DeleteAction());
        sActions.add(new ViewFeedObjAction());
        sActions.add(new SetProfileObjAction());
        sActions.add(new PlayAllAudioAction());
        sActions.add(new RelatedObjAction());
    }

    public static List<ObjAction> getObjActions() {
        return sActions;
    }
}
