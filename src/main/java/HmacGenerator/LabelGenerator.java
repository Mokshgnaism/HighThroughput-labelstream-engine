package HmacGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// this was the first try to generate without prod - cons and it took around 1.45 seconds the bottle neck was hashing
//the storing of 4d arrays might look unconvincing but it scaled well upto nearly 500k labels easily under 20 seconds without prod-cons
//=================================LEGACY ============================================
public class LabelGenerator {
    public static String[] generateLabel(String companyPrefix,Integer noOfpalets,String operatorId){
        // first generate the ssic labels for the companyPrefix and store them in an array
        Integer count = 0;
        String [] arr = new String[noOfpalets];
        for(int i=0;i<noOfpalets;i++){
            StringBuilder sb = new StringBuilder(companyPrefix+operatorId+count.toString()+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            Integer lchd = luhnCheckDigit(sb);
            sb.append(lchd);
            count++;
            arr[i] = sb.toString();
        }
        return  arr;
    }
    public static String[][][] generateSerialNumberForCartons(Integer cartonsPerPalet,Integer noOfPalets,String [] sscs){
        String [][][] arr = new String[noOfPalets][cartonsPerPalet][3];
        Integer count =0;
        for(int i=0;i<noOfPalets;i++){
            for (int j=0;j<cartonsPerPalet;j++){
                String Payload = "serial ->"+sscs[i]+count.toString()+"|"+"date -> "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String hash = LabelCrypto.toHex(LabelCrypto.hmacSha256(Payload,"abracadabra"));
                arr[i][j][0] = Payload;
                arr[i][j][1] = hash;
                arr[i][j][2] = hash.substring(0,8);
//                assuming anyone would use the hash only .. and if required the payload can be encoded as whatever they want . if they really want to use it then we can do the trimming when they get it from db calling from -> to '|';
                count += 1;
            }
        }
        return arr;
    }
    public static String [][][][] generateSerialNumberForUnits(Integer UnitsPerCarton,Integer cartonsPerPalet,String [][][] cartonIds,Integer noOfPalets){
        String [][][][] arr = new String[noOfPalets][cartonsPerPalet][UnitsPerCarton][3];
        Integer count =0;
        for(int i=0;i<noOfPalets;i+=1){
            for(int j=0;j<cartonsPerPalet;j++){
                for (int k=0;k<UnitsPerCarton;k++){
                    String Payload = "serial ->"+cartonIds[i][j][0]+count.toString();
                    String hash = LabelCrypto.toHex(LabelCrypto.hmacSha256(Payload,"abracadabra"));
                    arr[i][j][k][0] = Payload;
                    arr[i][j][k][1] = hash;
                    arr[i][j][k][2] = hash.substring(0,8);
                    count += 1;
                }
            }
        }

        return arr;
    }

    public static Integer luhnCheckDigit(StringBuilder label){
        Integer Total = 0;
        Integer n = label.length();
        for (int i = 0; i < n; i++) {
            Integer digit = Integer.parseInt(String.valueOf(label.charAt(i)));
            if (i%2==n%2){
                digit*=2;
            }
            if(digit > 9){
                digit -= 9;
            }
            Total+=digit;
        }
        return (Total-(Total%10))%10;
    }
}
