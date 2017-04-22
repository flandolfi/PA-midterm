import java.nio.*;
import java.security.*;

public class Hash {
    private static MessageDigest SHA256 = null;
    private final byte[] hash;

    public Hash(Object... objects) {
        if (SHA256 == null)
            try {
                SHA256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ignored) {}

        ByteBuffer buffer = ByteBuffer.allocate(objects.length*4);

        for (Object obj : objects) {
            buffer.putInt(obj.hashCode());
        }

        hash = SHA256.digest(buffer.array());
    }

    public Hash() { hash = new byte[32]; }
    public boolean isValid() { return (hash[0] | (hash[1] & 0xF0)) == 0; }
    public byte[] getHash(){ return hash; }

    public String getShortHash() {
        return String.format("%02x%02x%02x...%02x%02x%02x",
                    hash[0], hash[1], hash[2], hash[29], hash[30], hash[31]);
    }

    @Override
    public int hashCode() {
        int code = 23;

        for (byte b : hash)
            code = 43*code + (int) b;

        return code;
    }

    @Override
    public boolean equals(Object obj) { return obj != null && obj instanceof Hash && equals(((Hash) obj).getHash()); }
    public boolean equals(byte[] hash) { return MessageDigest.isEqual(this.hash, hash); }
}
