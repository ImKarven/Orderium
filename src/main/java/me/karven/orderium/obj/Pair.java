package me.karven.orderium.obj;

public class Pair<F, S> {
    public F first;
    public S second;
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F first() { return this.first; }
    public S second() { return this.second; }
}
