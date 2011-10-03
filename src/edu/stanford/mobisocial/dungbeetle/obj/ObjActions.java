package edu.stanford.mobisocial.dungbeetle.obj;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.obj.action.PassItOnAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.EditPhotoAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.OpenObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.RelatedObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.SetProfileObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.ViewFeedObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.PlayAllAudioAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.ExportPhotoAction;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

public class ObjActions {
    private static final List<ObjAction> sActions = new ArrayList<ObjAction>();
    static {
        sActions.add(new PassItOnAction());
        sActions.add(new OpenObjAction());
        sActions.add(new EditPhotoAction());
        sActions.add(new ExportPhotoAction());
        sActions.add(new ViewFeedObjAction());
        sActions.add(new SetProfileObjAction());
        sActions.add(new PlayAllAudioAction());
        sActions.add(new RelatedObjAction());
    }

    public static List<ObjAction> getObjActions() {
        return sActions;
    }
}
