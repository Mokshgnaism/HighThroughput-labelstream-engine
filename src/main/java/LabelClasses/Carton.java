package LabelClasses;

public class Carton {
    public String Hash;
    public String prefix;
    public String parentPaletID;
    public String serialId;
    public Carton(String parentPaletID,String serialId,String Hash,String prefix){
        this.parentPaletID = parentPaletID;
        this.serialId = serialId;
        this.Hash = Hash;
        this.prefix = prefix;
    }
}
