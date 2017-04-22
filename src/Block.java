import java.security.*;

public class Block {
    private int number;
    private int nonce = 0;
    private Hash previous, current;
    private Transaction data;

    public Block(int number, Hash previous, Transaction data) {
        this.number = number;
        this.data = data;
        this.previous = previous;
        rehash();
    }

    private void rehash() { current = new Hash(number, nonce, previous, data); }
    public int getNumber() { return number; }
    public int getNonce() { return nonce; }
    public void setPreviousHash(Hash hash) { previous = hash; rehash(); }
    public Hash getPreviousHash() { return previous; }
    public Hash getCurrentHash() { return current; }
    public Transaction getData() { return data; }

    public int mine() {
        while (!current.isValid()) {
            nonce++;
            rehash();
        }

        return nonce;
    }
}
