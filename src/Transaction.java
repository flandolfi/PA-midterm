import java.security.*;
import java.util.*;

public class Transaction {
    private ArrayList<Entry<Hash, Integer>> inputs;
    private ArrayList<Entry<Integer, PublicKey>> outputs;
    private byte[] signature;

    public static Transaction createCoinbase(PrivateKey signer, PublicKey payee,
                                             int amount) throws InvalidKeyException {
        if (amount <= 0)
            throw new IllegalArgumentException();

        Transaction coinbase = new Transaction();
        coinbase.outputs.add(new Entry<>(amount, payee));

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(signer);
            signature.update((coinbase.inputs.toString() +
                    coinbase.outputs.toString()).getBytes());
            coinbase.signature = signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException ignored) {}

        return coinbase;
    }

    private Transaction() {
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
    }

    public Transaction(ArrayList<Entry<Transaction, Integer>> inputs,
                       ArrayList<Entry<Integer, PublicKey>> outputs, KeyPair payer)
            throws AmountExceededException, InvalidKeyException,
            UnauthorizedTransactionException {
        this();
        checkKeyPair(payer);
        Integer total = 0;

        for (Entry<Transaction, Integer> in : inputs) {
            Entry<Integer, PublicKey> out = in.getFst().getOutputs().get(in.getSnd());

            if (!out.getSnd().equals(payer.getPublic()))
                throw new UnauthorizedTransactionException();

            this.inputs.add(new Entry<>(new Hash(in.getFst()), in.getSnd()));
            total += out.getFst();
        }

        for (Entry<Integer, PublicKey> e : outputs) {
            if (e.getFst() < 0)
                throw new UnauthorizedTransactionException();
            
            if((total -= e.getFst()) < 0)
                throw new AmountExceededException();

            this.outputs.add(e);
        }

        if (total > 0)
            this.outputs.add(new Entry<>(total, payer.getPublic()));

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(payer.getPrivate());
            signature.update((this.inputs.toString() +
                    this.outputs.toString()).getBytes());
            this.signature = signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException ignored) {}
    }

    private void checkKeyPair(KeyPair keys) throws InvalidKeyException {
        try {
            SecureRandom random = new SecureRandom();
            byte[] randomBytes = new byte[32];
            random.nextBytes(randomBytes);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keys.getPrivate());
            signature.update(randomBytes);
            byte[] sigBytes = signature.sign();
            signature.initVerify(keys.getPublic());
            signature.update(randomBytes);

            if (!signature.verify(sigBytes))
                throw new InvalidKeyException();
        } catch (NoSuchAlgorithmException | SignatureException ignored) {}
    }

    public ArrayList<Entry<Hash, Integer>> getInputs() { return inputs; }
    public ArrayList<Entry<Integer, PublicKey>> getOutputs() { return outputs; }
}
