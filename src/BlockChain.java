import java.security.*;
import java.util.*;

public class BlockChain {
    private LinkedList<Block> chain = new LinkedList<>();

    public BlockChain(PrivateKey signer, PublicKey payee, int amount)
            throws InvalidKeyException { chain.add(new Block(0, new Hash(),
                Transaction.createCoinbase(signer, payee, amount))); }

    public boolean isValidBlockChain() {
        HashMap<Hash, BitSet> spentTransactions = new HashMap<>();
        Iterator<Block> it = chain.iterator();
        Block b = it.next();
        Transaction tx = b.getData();

        if (!b.getCurrentHash().isValid())
            return false;

        spentTransactions.put(new Hash(tx), new BitSet(tx.getOutputs().size()));

        while (it.hasNext()) {
            Hash previous = b.getCurrentHash();
            b = it.next();
            tx = b.getData();

            if (!b.getCurrentHash().isValid() ||
                    !previous.equals(b.getPreviousHash()))
                return false;

            for (Entry<Hash, Integer> input : tx.getInputs()) {
                if (spentTransactions.computeIfPresent(input.getFst(), (k, v) -> {
                    if (v.get(input.getSnd()))
                        return null;

                    v.set(input.getSnd());

                    return v;
                }) == null)
                    return false;
            }

            spentTransactions.put(new Hash(tx), new BitSet(tx.getOutputs().size()));
        }

        return true;
    }

    public int getBalance(PublicKey user) {
        HashMap<Hash, BitSet> spentTransactions = new HashMap<>();
        Iterator<Block> it = chain.descendingIterator();
        int balance = 0;

        while (it.hasNext()) {
            Block b = it.next();
            Transaction tx = b.getData();
            Hash hash = new Hash(tx);
            ArrayList<Entry<Integer, PublicKey>> outputs = tx.getOutputs();

            for (int i = 0; i < outputs.size(); i++)
                if (user.equals(outputs.get(i).getSnd()) &&
                        (spentTransactions.get(hash) == null ||
                        !spentTransactions.get(hash).get(i)))
                    balance += outputs.get(i).getFst();

            tx.getInputs().forEach(in -> spentTransactions
                    .computeIfAbsent(in.getFst(), v -> new BitSet()).set(in.getSnd()));
        }

        return balance;
    }

    public int mine(int index) {
        int nonce = chain.get(index).mine();

        for (int i = index + 1; i < chain.size(); i++)
            chain.get(i).setPreviousHash(chain.get(i - 1).getCurrentHash());

        return nonce;
    }

    public void addBlock(Transaction transaction) { chain.add(new Block(chain.size(), chain.getLast().getCurrentHash(), transaction)); }
    public boolean removeLast() { return chain.size() > 1 && chain.removeLast() != null; }
    public LinkedList<Block> getBlockChain() { return chain; }
}
