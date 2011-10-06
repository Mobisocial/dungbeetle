package edu.stanford.mobisocial.dungbeetle.model;

public class DbRelation {
    public static final String TABLE = "relations";
    public static final String _ID = "_id";
    public static final String OBJECT_ID_A = "object_id_A";
    public static final String OBJECT_ID_B = "object_id_B";
    public static final String RELATION_TYPE = "relation";

    /**
     * A is the parent of B
     */
    public static final String RELATION_PARENT = "parent";

    /**
     * B updates and replaces A.
     */
    public static final String RELATION_UPDATE = "update";

    /**
     * B is an edit of the data in A.
     */
    public static final String RELATION_EDIT = "edit";
}
