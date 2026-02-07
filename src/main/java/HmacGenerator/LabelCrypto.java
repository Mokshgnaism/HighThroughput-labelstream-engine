package HmacGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LabelCrypto {

    private static final ThreadLocal<Mac> TL_MAC =
            ThreadLocal.withInitial(() -> {
                try {
                    Mac mac = Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec(
                            "SECRET_KEY_BYTES".getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"
                    ));
                    return mac;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    public static byte[] hmacSha256(String payload,String secretKey){
        try{
            Mac mac = TL_MAC.get();
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        }catch (Exception e){
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
    public static String toHex(byte[]bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes){
            sb.append(String.format("%02x",b));
        }
        return String.valueOf(sb);
    }
}
//ourr requirements . create a batch -> 1st one . then
