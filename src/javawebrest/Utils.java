package javawebrest;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * @author Tibor Péter Szabó
 */
/**
 * Extra utilities.
 */
public class Utils {

    /**
     * Hashes a string
     *
     * @param stringToHash - String to be hashed.
     * @return String - Hashed string.
     */
    public static String toSHA256(String stringToHash) {
        try {
            byte[] hash = MessageDigest.getInstance("sha-256").digest(stringToHash.getBytes());
            BigInteger signum = new BigInteger(1, hash);
            StringBuilder hashBuilder = new StringBuilder(signum.toString(16));
            while (hashBuilder.length() < 64) {
                hashBuilder.insert(0, '0');
            }
            return hashBuilder.toString();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Encodes a string to Base64.
     *
     * @param stringToEncode - String to be encoded.
     * @return String - Encoded string.
     */
    static String toBase64(String stringToEncode) {
        return Base64.getEncoder().encodeToString(stringToEncode.getBytes());
    }

    /**
     * Decodes a Base64 string.
     *
     * @param base64ToDecode - String to be decoded.
     * @return String - Decoded string.
     */
    static String Base64ToText(String base64ToDecode) {
        return new String(Base64.getDecoder().decode(base64ToDecode));
    }
}
