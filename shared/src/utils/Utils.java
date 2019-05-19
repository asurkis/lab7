package utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static String md2(String value) {
        try {
            MessageDigest md2 = MessageDigest.getInstance("MD2");
            byte[] messageDigest = md2.digest(value.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder passwordHash = new StringBuilder(no.toString());
            while (passwordHash.length() < 32) {
                passwordHash.insert(0, "0");
            }
            return passwordHash.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
