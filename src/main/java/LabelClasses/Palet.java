package LabelClasses;

public class Palet {
    public String FactoryId;
    public String employeeId;
    public String PaletSSIC;
    public String hash;
    public String hashPrefix;
    public Palet(String FactoryId, String employeeId, String PaletSSIC, String hash, String hashPrefix) {
        this.FactoryId = FactoryId;
        this.hash = hash;
        this.hashPrefix = hashPrefix;
        this.employeeId = employeeId;
        this.PaletSSIC = PaletSSIC;
    }
}
