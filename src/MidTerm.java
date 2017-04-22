import java.security.*;
import java.util.*;

public class MidTerm {
    private static KeyPairGenerator kpg = null;
    private static KeyPair a, b, c;
    private static BlockChain chain;
    private static HashMap<Key, String> aliases = new HashMap<>();
    private static HashMap<String, Key> keyring = new HashMap<>();

    private static void init() {
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            a = kpg.generateKeyPair();
            b = kpg.generateKeyPair();
            c = kpg.generateKeyPair();
            aliases.put(a.getPrivate(), "SkA");
            aliases.put(a.getPublic(), "PkA");
            aliases.put(b.getPrivate(), "SkB");
            aliases.put(b.getPublic(), "PkB");
            aliases.put(c.getPrivate(), "SkC");
            aliases.put(c.getPublic(), "PkC");
            aliases.forEach((k, v) -> keyring.put(v, k));
            chain = new BlockChain(b.getPrivate(), a.getPublic(), 10000);
        } catch (NoSuchAlgorithmException | InvalidKeyException ignored) {}
    }

    private static void printUsers() {
        System.out.println("\nUsers:\n======\n" +
                "  Alice:\tPrivate Key: SkA\n        \tPublic Key: PkA\n\n" +
                "  Bob:  \tPrivate Key: SkB\n        \tPublic Key: PkB\n\n" +
                "  Carol:\tPrivate Key: SkC\n        \tPublic Key: PkC\n");
    }

    private static void printBlockChain() {
        System.out.println("\nCurrent BlockChain:\n" +
                "===================\n");

        for (Block b : chain.getBlockChain()) {
            System.out.print("  Block " + b.getNumber() + ":\tInputs:\n");

            if (b.getData().getInputs().size() == 0)
                System.out.println("          \t   None.");
            else {
                ArrayList<Entry<Hash, Integer>> inputs = b.getData().getInputs();

                for (int i = 0; i < inputs.size(); i++) {
                    Entry<Hash, Integer> in = inputs.get(i);
                    System.out.println("          \t   " + i + ". Hash: " +
                            in.getFst().getShortHash() + ", Index: " + in.getSnd());
                }
            }

            ArrayList<Entry<Integer, PublicKey>> outputs = b.getData().getOutputs();
            System.out.println("          \tOutputs:");

            for (int i = 0; i < outputs.size(); i++) {
                Entry<Integer, PublicKey> out = outputs.get(i);
                System.out.println("          \t   " + i + ". Amount: " + out.getFst() +
                        ", Payee: " + aliases.get(out.getSnd()));
            }

            System.out.println("          \tNonce value:   " + b.getNonce() +
                    "\n          \tCurrent Hash:  " + b.getCurrentHash().getShortHash() +
                    "\n          \tPrevious Hash: " + b.getPreviousHash().getShortHash() +
                    "\n");
        }
    }

    private static void printHelp() {
        System.out.println("\nCOMMANDS\n========\n\n" +
                "  help\n\t\tShow the list of possible commands.\n\n" +
                "  users\n\t\tShow the list of users with their RSA key aliases.\n\n" +
                "  status\n\t\tShow the current block chain.\n\n" +
                "  mine INDEX\n\t\tDiscovers the nonce number of the block of index " +
                "INDEX.\n\n" +
                "  transfer --in=BLOCK,OUTPUT --out=AMOUNT,PAYEE --sign=PRIVATE," +
                "PUBLIC\n\t\tPerforms a transaction from output number OUTPUT of " +
                "block number\n\t\tBLOCK to the beneficiary with public key PAYEE " +
                "with the amount\n\t\tof AMOUNT, then signs the transaction with " +
                "the key pair\n\t\tPRIVATE,PUBLIC. All switches are mandatory and " +
                "'--in' and '--out'\n\t\tmay be reused.\n\n" +
                "  remove\n\t\tRemoves the last block of the chain.\n\n" +
                "  check\n\t\tChecks that the chain is valid.\n\n" +
                "  report PUBLIC_KEY\n\t\tReports the balance of the user having " +
                "the public key PUBLIC_KEY.\n\n" +
                "  quit\n\t\tQuits the program.\n\n");
    }

    public static void main(String args[]) {
        String input;
        String[] tokens;
        Scanner reader = new Scanner(System.in);
        init();
        printUsers();
        printBlockChain();
        System.out.println("Type 'help' for a list of commands.\n");

        do {
            do {
                System.out.print("$> ");
                input = reader.nextLine().trim();
            } while (input.equals(""));

            tokens = input.split("\\s+");

            switch (tokens[0]) {
                case "quit":
                    break;

                case "help":
                    printHelp();
                    break;

                case "users":
                    printUsers();
                    break;

                case "status":
                    printBlockChain();
                    break;

                case "check":
                    if (chain.isValidBlockChain())
                        System.out.println("The block chain is valid!");
                    else
                        System.out.println("The block chain is not valid.");

                    break;

                case "remove":
                    if (!chain.removeLast())
                        System.err.println("Error: you can not remove the first block.");
                    else {
                        System.out.println("Last block removed.");
                        printBlockChain();
                    }

                    break;

                case "report":
                    if (tokens.length < 2)
                        System.err.println("Error: missing argument.");
                    else {
                        Key key = keyring.get(tokens[1]);

                        if (key == null)
                            System.out.println("User '" + tokens[1] + "' not found.");
                        else
                            System.out.println("Balance: " +
                                    chain.getBalance((PublicKey) key));
                    }

                    break;

                case "mine":
                    if (tokens.length < 2)
                        System.err.println("Error: missing argument");
                    else
                        try {
                            int nonce, index = Integer.parseInt(tokens[1]);

                            nonce = chain.mine(index);
                            System.out.println("Found nonce value " + nonce + ".");
                            printBlockChain();
                        } catch (NumberFormatException e) {
                            System.err.println("Error: invalid block index.");
                        } catch (IndexOutOfBoundsException e) {
                            System.err.println("Error: index out of bounds.");
                        }

                    break;

                case "transfer":
                    ArrayList<Entry<Transaction, Integer>> inputs = new ArrayList<>();
                    ArrayList<Entry<Integer, PublicKey>> outputs = new ArrayList<>();
                    KeyPair signer = null;
                    boolean error = false;

                    loop: for (int i = 1; i < tokens.length; i++) {
                        String[] entry = tokens[i].split("[=,]");

                        if (entry.length != 3) {
                            System.err.println("Error: invalid argument.");
                            error = true;
                            break;
                        }

                        switch (entry[0]) {
                            case "--in":
                                try {
                                    inputs.add(new Entry<>(chain.getBlockChain()
                                            .get(Integer.parseInt(entry[1]))
                                            .getData(), Integer.parseInt(entry[2])));
                                } catch (NumberFormatException e) {
                                    System.err.println("Error: invalid index.");
                                    error = true;
                                    break loop;
                                } catch (IndexOutOfBoundsException e) {
                                    System.err.println("Error: index out of bounds.");
                                    error = true;
                                    break loop;
                                }

                                break;

                            case "--out":
                                try {
                                    Key key = keyring.get(entry[2]);

                                    if (key == null) {
                                        System.err.println("Error: key '" + entry[2] +
                                                "' not found.");
                                        error = true;
                                        break loop;
                                    }

                                    outputs.add(new Entry<>(Integer.parseInt(entry[1]),
                                            (PublicKey) key));
                                } catch (NumberFormatException e) {
                                    System.err.println("Error: invalid amount.");
                                    error = true;
                                    break loop;
                                }

                                break;

                            case "--sign":
                                if (signer != null) {
                                    System.err.println("Error: a transaction must have " +
                                            "only one signer.");
                                    error = true;
                                    break loop;
                                }

                                PrivateKey sk = (PrivateKey) keyring.get(entry[1]);
                                PublicKey pk = (PublicKey) keyring.get(entry[2]);

                                if (sk == null || pk == null) {
                                    System.err.println("Error: invalid key pair.");
                                    error = true;
                                    break loop;
                                }

                                signer = new KeyPair(pk, sk);
                                break;

                            default:
                                System.err.println("Error: invalid argument.");
                                error = true;
                                break loop;
                        }
                    }

                    if (!error && signer != null && outputs.size() > 0
                            && inputs.size() > 0) {
                        try {
                            chain.addBlock(new Transaction(inputs, outputs, signer));
                        } catch (AmountExceededException e) {
                            System.err.println("Error: amount exceeded.");
                        } catch (InvalidKeyException e) {
                            System.err.println("Error: Invalid key.");
                        } catch (UnauthorizedTransactionException e) {
                            System.err.println("Error: unauthorized transaction.");
                        }

                        printBlockChain();
                    }

                    break;

                default:
                    System.err.println("Command '" + tokens[0] + "' not found.");
                    break;
            }
        } while(!tokens[0].equals("quit"));
    }
}
