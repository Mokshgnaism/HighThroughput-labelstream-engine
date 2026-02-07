package LabelClasses;

public class Unit{
    public String Hash;
    public String prefix;
    public String parentCartonID;
    public String serialId;
    public Unit(String hash, String prefix, String parentCartonID, String serialId){
        this.Hash = hash;
        this.prefix = prefix;
        this.parentCartonID = parentCartonID;
        this.serialId = serialId;
    }
}