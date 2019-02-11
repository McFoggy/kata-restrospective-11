package fr.umlv.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class PatternLexer<T> implements Lexer<T> {
    private final List<String> regexps;
    private final List<Function<? super String, T>> mappers;
    private final Lazy<Optional<Pattern>> patternBuilder;

    private PatternLexer() {
        this.regexps = new ArrayList<>();
        this.mappers = new ArrayList<>();
        this.patternBuilder = Lazy.of(this::aggregatedPattern);
    }

    PatternLexer(List<String> regexps, List<Function<? super String, T>> mappers) {
        this();
        Objects.requireNonNull(regexps).forEach(this::checkPatternHasExactlyOneCapturingGroup);
        Objects.requireNonNull(mappers);
        if (regexps.size() != mappers.size()) {
            throw new IllegalArgumentException("both lists must have the same size");
        }
        this.regexps.addAll(regexps);
        this.mappers.addAll(mappers);
    }

    PatternLexer(Pattern pattern, Function<? super String, T> mapper) {
        this();
        checkPatternHasExactlyOneCapturingGroup(Objects.requireNonNull(pattern));
        this.regexps.add(pattern.pattern());
        this.mappers.add(mapper);
    }

    private Optional<Pattern> aggregatedPattern() {
        String aggregated = regexps.stream().collect(Collectors.joining("|"));
        if (aggregated.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Pattern.compile(aggregated));
    }

    @Override
    public Optional<T> tryParse(String input) {
        Objects.requireNonNull(input);

        return patternBuilder.get().map(p -> p.matcher(input))
                .filter(Matcher::matches)
                .flatMap(this::transformedMatchingGroup);
    }

    private Optional<T> transformedMatchingGroup(Matcher matcher) {
        int matchedGroupIndex = IntStream.rangeClosed(1, matcher.groupCount())
                .filter(idx -> matcher.group(idx) != null)
                .findFirst()
                .orElseThrow();
        return Optional.ofNullable(mappers.get(matchedGroupIndex - 1).apply(matcher.group(matchedGroupIndex)));
    }

    @Override
    public Lexer<T> with(String regexp, Function<? super String, T> mapper) {
        checkPatternHasExactlyOneCapturingGroup(regexp);
        ArrayList<String> regexps = new ArrayList<>(this.regexps);
        ArrayList<Function<? super String, T>> mappers = new ArrayList<>(this.mappers);

        regexps.add(regexp);
        mappers.add(mapper);
        return new PatternLexer<>(regexps, mappers);
    }

    @Override
    public Lexer<T> or(Lexer<? extends T> other) {
        Objects.requireNonNull(other);
        if (other instanceof PatternLexer) {
            PatternLexer opl = (PatternLexer) other;
            List<String> combinedRegexps = new ArrayList<>(this.regexps);
            combinedRegexps.addAll(opl.regexps);
            List<Function<? super String, T>> combinedMappers = new ArrayList<>(this.mappers);
            combinedMappers.addAll(opl.mappers);
            return Lexer.from(combinedRegexps, combinedMappers);
        }

        return with("(.*)", s -> other.tryParse(s).orElse(null));
    }

    @Override
    public <R> Lexer<R> map(Function<? super T, R> mapper) {
        Objects.requireNonNull(mapper);
        List<? extends Function<? super String, R>> mappedMappers = this.mappers.stream().map(f -> f.andThen(mapper)).collect(Collectors.toList());
        return Lexer.from(this.regexps, mappedMappers);
    }

    private void checkPatternHasExactlyOneCapturingGroup(String regexp) {
        checkPatternHasExactlyOneCapturingGroup(Pattern.compile(Objects.requireNonNull(regexp)));
    }

    private void checkPatternHasExactlyOneCapturingGroup(Pattern pattern) {
        if (Objects.requireNonNull(pattern).matcher("").groupCount() != 1) {
            throw new IllegalArgumentException("pattern should have exactly one capturing group");
        }
    }
}
