package fr.umlv.lexer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 * This class has been published by Grzegorz Piwowarek in the below article:
 *    https://4comprehension.com/leveraging-lambda-expressions-for-lazy-evaluation-in-java/
 * All the credits go to him.
 */
public final class Lazy<V> {
    private transient Supplier<V> supplier;
    private volatile V value;
    public Lazy(Supplier<V> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
    }
    public V get() {
        if (value == null) {
            synchronized (this) {
                if (value == null) {
                    value = Objects.requireNonNull(supplier.get());
                    supplier = null;
                }
            }
        }
        return value;
    }
    public <R> Lazy<R> map(Function<V, R> mapper) {
        return new Lazy<>(() -> mapper.apply(this.get()));
    }
    public <R> Lazy<R> flatMap(Function<V, Lazy<R>> mapper) {
        return new Lazy<>(() -> mapper.apply(this.get()).get());
    }
    public Lazy<Optional<V>> filter(Predicate<V> predicate) {
        return new Lazy<>(() -> Optional.of(get()).filter(predicate));
    }
    public static <V> Lazy<V> of(Supplier<V> supplier) {
        return new Lazy<>(supplier);
    }
}