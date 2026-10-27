
package mizzen.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dispatches input lines to callbacks based on regular expressions.
 *
 * Rules are evaluated in registration order. The first matching rule
 * consumes the line. Unmatched lines are forwarded to the text handler.
 *
 * By default, patterns must match the entire line. A null line is ignored.
 */
public class RegexLineParser implements Consumer<String> {

    private static class Rule {
        final Pattern pattern;
        final Consumer<Matcher> action;
        final boolean find;

        Rule(Pattern pattern, Consumer<Matcher> action, boolean find) {
            this.pattern = pattern;
            this.action = action;
            this.find = find;
        }
    }

    private final List<Rule> rules = new ArrayList<>();
    private Consumer<String> textHandler = line -> {};

    public RegexLineParser addRule(
            String regex, Consumer<Matcher> action) {
        return addRule(Pattern.compile(regex), action);
    }

    public RegexLineParser addRule(
            Pattern pattern, Consumer<Matcher> action) {
        rules.add(new Rule(
            Objects.requireNonNull(pattern),
            Objects.requireNonNull(action),
            false
        ));
        return this;
    }

    /**
     * Match anywhere within a line, rather than the entire line.
     */
    public RegexLineParser addFindRule(
            String regex, Consumer<Matcher> action) {
        return addFindRule(Pattern.compile(regex), action);
    }

    public RegexLineParser addFindRule(
            Pattern pattern, Consumer<Matcher> action) {
        rules.add(new Rule(
            Objects.requireNonNull(pattern),
            Objects.requireNonNull(action),
            true
        ));
        return this;
    }

    public RegexLineParser onText(Consumer<String> handler) {
        textHandler = Objects.requireNonNull(handler);
        return this;
    }

    /**
     * Whether a rule would take this line - without running it. For routing: e.g. decide on a
     * reading thread whether a line goes to this parser or somewhere else.
     */
    public boolean matches(String line) {
        if (line == null) {
            return false;
        }
        for (Rule rule : rules) {
            Matcher matcher = rule.pattern.matcher(line);
            if (rule.find ? matcher.find() : matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void accept(String line) {
        if (line == null) {
            return;
        }

        for (Rule rule : rules) {
            Matcher matcher = rule.pattern.matcher(line);

            boolean matched = rule.find
                    ? matcher.find()
                    : matcher.matches();

            if (matched) {
                rule.action.accept(matcher);
                return;
            }
        }

        textHandler.accept(line);
    }
}
