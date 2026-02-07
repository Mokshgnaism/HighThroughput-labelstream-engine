package HmacGenerator;
import LabelClasses.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static HmacGenerator.LabelGenerator.luhnCheckDigit;

public class LabelGenerator2 {
    public static List<Palet>generatePaletIds(Integer noOfPalets,String FactoryId,String employeeId,String companyPrefix,String ProductId){
        List<Palet> palets = new ArrayList<Palet>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//        ssic = companyPrefix+timestamp+productId+seqnCounter+(empId,factId)+luhnCheckDigit
        for(Integer i=0;i<noOfPalets;i++){
            Integer cnt = i+1;
            StringBuilder ssic = new StringBuilder(companyPrefix+timestamp+ProductId+cnt.toString());
            Integer lchd = luhnCheckDigit(ssic);
            ssic.append(lchd);
            String hash = LabelCrypto.toHex(LabelCrypto.hmacSha256(new String(ssic),"abracadabra"));
            String hashPre = hash.substring(0, 8);
            palets.add(new Palet(FactoryId,employeeId,new String(ssic),hash,hashPre ));
        }
        return palets;
    }
    public static List<Carton> generateCartonForPalet(String paletssic,Integer cartonsPerPalet){
        List<Carton> cartons = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for(Integer i = 0; i< cartonsPerPalet; i++){
            Integer cnt = i+1;
            StringBuilder ssic = new StringBuilder(paletssic);
            ssic.append(cnt);
            Integer lchd = luhnCheckDigit(ssic);
            ssic.append(lchd);
            String hash = LabelCrypto.toHex(LabelCrypto.hmacSha256(new String(ssic),"abracadabra"));
            String hashPre = hash.substring(0, 8);
            cartons.add(new Carton(paletssic,ssic.toString(),hash,hashPre));
        }
        return cartons;
    }
    public static List<Unit> generateUnitsForCarton(String cartonssic,Integer UnitsPerCarton){
        List<Unit> units = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for(Integer i = 0; i< UnitsPerCarton; i++){
            Integer cnt = i+1;
            StringBuilder ssic = new StringBuilder(cartonssic);
            ssic.append(cnt);
            Integer lchd = luhnCheckDigit(ssic);
            ssic.append(lchd);
            String hash = LabelCrypto.toHex(LabelCrypto.hmacSha256(new String(ssic),"abracadabra"));
            String hashPre = hash.substring(0, 8);
            units.add(new Unit(hash,hashPre,cartonssic,ssic.toString()));
        }
        return units;
    }
}
