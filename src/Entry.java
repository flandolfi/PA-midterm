public class Entry<F, S> {
    final private F first;
    final private S second;

    public Entry(F first, S second) { this.first = first; this.second = second; }
    public F getFst() { return first; }
    public S getSnd() { return second; }
}
