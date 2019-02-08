package fr.umlv.lexer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FunctionalInterface
public interface Lexer<T> {
    /**
     * Extract data from the given input
     * @param input a non null input from which the Lexer should infer data
     * @return the recognized data
     */
    Optional<T> tryParse(String input);

    /**
     * Returns a Lexer able to transform recognized data from the current lexer into another datatype using the
     * given mapper.
     * @param mapper a non null function able to transform data of the current Lexer into another type
     * @param <R> the type of the transformed data
     * @return a Lexer recognizing the same kind of input than the current one but exposing the result in another datatype
     */
    default <R> Lexer<R> map(Function<? super T, R> mapper) {
        Objects.requireNonNull(mapper);
        return s -> tryParse(s).map(mapper);
    };

    /**
     * Chain with another lexer of a compatible type. If the current lexer does not retrieve data during parsing then
     * it will delegate to the provided other Lexer to try to parse the data.
     * @param other another non null Lexer able to recognized data of compatible datatype
     * @return a chained Lexer of the same datatype than the current one
     */
    default Lexer<T> or(Lexer<? extends T> other) {
        Objects.requireNonNull(other);
        return s -> tryParse(s).or(() -> other.tryParse(s));
    }

    /**
     * Provide a Lexer of the same datatype chained with another lexer recognising the given non null regular expression
     * and transforming recognized string data into another type using the given non null mapper.
     * @param regexp a non null regexp with a mandatory single capture group
     * @param mapper the data transformer
     * @return a chained Lexer of the same datatype than the current one
     */
    default Lexer<T> with(String regexp, Function<? super String, T> mapper) {
        Objects.requireNonNull(regexp);
        Objects.requireNonNull(mapper);
        return this.or(Lexer.from(regexp).map(mapper));
    }

    /**
     * Creates a typed Lexer that never recognizes anything.
     * @param <T> the datatype of the parsed data of the produced Lexer
     * @return a non null Lexer
     */
    static <T> Lexer<T> create() {
        return ALWAYS_EMPTY_LEXER;
    }

    /**
     * Creates a Lexer able to extract String data using a given regular expression.
     * @param regexp a non null regular expression containing exactly one capturing group
     * @return a Lexer able to extract matched string from an input
     */
    static Lexer<String> from(String regexp) {
        return from(Pattern.compile(Objects.requireNonNull(regexp)));
    }

    /**
     * Creates a Lexer able to extract String data using the given Pattern object.
     * @param pattern a non null Pattern which regular expression contains exactly one capturing group
     * @return a Lexer able to extract matched string from an input
     */
    static Lexer<String> from(Pattern pattern) {
        if (Objects.requireNonNull(pattern).matcher("").groupCount() != 1) {
            throw new IllegalArgumentException("pattern should have exactly one capturing group");
        }

        return s -> {
            Matcher matcher = pattern.matcher(Objects.requireNonNull(s));
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
            return Optional.empty();
        };
    }

    /**
     * A singleton instance of a Lexer that never recognizes anything.
     */
    Lexer ALWAYS_EMPTY_LEXER = s -> {
        Objects.requireNonNull(s);
        return Optional.empty();
    };
}
