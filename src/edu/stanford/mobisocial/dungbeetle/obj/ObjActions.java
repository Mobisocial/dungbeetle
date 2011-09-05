package edu.stanford.mobisocial.dungbeetle.obj;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.mobisocial.dungbeetle.obj.action.ClipboardObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.OpenObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.SetProfileObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.action.ViewFeedObjAction;
import edu.stanford.mobisocial.dungbeetle.obj.iface.ObjAction;

public class ObjActions {
    private static final List<ObjAction> sActions = new ArrayList<ObjAction>();
    static {
        sActions.add(new ClipboardObjAction());
        sActions.add(new SetProfileObjAction());
        sActions.add(new OpenObjAction());
        sActions.add(new ViewFeedObjAction());
    }

    public static List<ObjAction> getObjActions() {
        return sActions;
    }
}
