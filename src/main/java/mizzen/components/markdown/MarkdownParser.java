package mizzen.components.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mizzen.components.markdown.MarkdownDocument.Block;
import mizzen.components.markdown.MarkdownDocument.BlockKind;
import mizzen.components.markdown.MarkdownDocument.Run;
import mizzen.components.markdown.MarkdownDocument.RunStyle;

/**
 * Reads the markdown people write in a README: headings, paragraphs, lists, quotes, fenced code, pipe
 * tables, rules; bold, italic, code, links, images, a link around an image. Not all of CommonMark: what
 * it doesn't know shows as text. HTML tags are dropped, their text kept (the README's sub tags).
 * <pre>
 *     MarkdownDocument document = MarkdownParser.parse("# Title\n\nSome **bold** text");
 * </pre>
 */
public final class MarkdownParser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final Pattern RULE = Pattern.compile("^\\s*([-*_])(\\s*\\1){2,}\\s*$");
    private static final Pattern FENCE = Pattern.compile("^\\s*```\\s*(\\S*).*$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*+]\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^(\\s*)(\\d+)[.)]\\s+(.*)$");
    private static final Pattern QUOTE = Pattern.compile("^\\s*>\\s?(.*)$");
    private static final Pattern TABLE_LINE = Pattern.compile("^\\s*\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$");
    private static final Pattern HTML_TAG = Pattern.compile("^</?[a-zA-Z][^>]*>");

    private MarkdownParser() {
    }

    public static MarkdownDocument parse(String markdown) {
        MarkdownDocument document = new MarkdownDocument();
        String[] lines = markdown.split("\r?\n", -1);
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            Matcher m;
            if (line.trim().isEmpty()) {
                i++;
            } else if ((m = FENCE.matcher(line)).matches()) {
                Block code = new Block(BlockKind.CODE);
                code.language = m.group(1);
                for (i++; i < lines.length && !FENCE.matcher(lines[i]).matches(); i++) {
                    code.codeLines.add(lines[i].replace("\t", "    "));
                }
                i++;                                                           // past the closing fence
                document.blocks.add(code);
            } else if ((m = HEADING.matcher(line)).matches()) {
                Block heading = new Block(BlockKind.HEADING);
                heading.level = m.group(1).length();
                heading.runs = runs(m.group(2));
                document.blocks.add(heading);
                i++;
            } else if (RULE.matcher(line).matches()) {
                document.blocks.add(new Block(BlockKind.RULE));
                i++;
            } else if (isTableStart(lines, i)) {
                Block table = new Block(BlockKind.TABLE);
                table.tableRows.add(cells(lines[i]));
                for (i += 2; i < lines.length && lines[i].contains("|"); i++) {
                    table.tableRows.add(cells(lines[i]));
                }
                document.blocks.add(table);
            } else if (QUOTE.matcher(line).matches()) {
                StringBuilder text = new StringBuilder();
                for (; i < lines.length && (m = QUOTE.matcher(lines[i])).matches(); i++) {
                    text.append(m.group(1)).append(' ');
                }
                Block quote = new Block(BlockKind.QUOTE);
                quote.runs = runs(text.toString().trim());
                document.blocks.add(quote);
            } else if (BULLET.matcher(line).matches() || NUMBERED.matcher(line).matches()) {
                i = listItem(document, lines, i);
            } else {
                StringBuilder text = new StringBuilder();
                for (; i < lines.length && !startsABlock(lines, i); i++) {
                    text.append(lines[i].trim()).append(' ');
                }
                Block paragraph = new Block(BlockKind.PARAGRAPH);
                paragraph.runs = runs(text.toString().trim());
                if (!paragraph.runs.isEmpty()) {                               // only tags, e.g. <p align="center">: nothing to show
                    document.blocks.add(paragraph);
                }
            }
        }
        return document;
    }

    private static int listItem(MarkdownDocument document, String[] lines, int start) {
        Matcher bullet = BULLET.matcher(lines[start]);
        Block item = new Block(BlockKind.LIST_ITEM);
        String text;
        if (bullet.matches()) {
            item.level = bullet.group(1).length() / 2;
            item.marker = "• ";
            text = bullet.group(2);
        } else {
            Matcher numbered = NUMBERED.matcher(lines[start]);
            numbered.matches();
            item.level = numbered.group(1).length() / 2;
            item.marker = numbered.group(2) + ". ";
            text = numbered.group(3);
        }
        StringBuilder all = new StringBuilder(text);
        int i = start + 1;
        for (; i < lines.length && !startsABlock(lines, i); i++) {             // its text goes on until the next block
            all.append(' ').append(lines[i].trim());
        }
        item.runs = runs(all.toString());
        document.blocks.add(item);
        return i;
    }

    private static boolean startsABlock(String[] lines, int i) {
        String line = lines[i];
        return line.trim().isEmpty() || FENCE.matcher(line).matches() || HEADING.matcher(line).matches()
                || RULE.matcher(line).matches() || QUOTE.matcher(line).matches() || BULLET.matcher(line).matches()
                || NUMBERED.matcher(line).matches() || isTableStart(lines, i);
    }

    private static boolean isTableStart(String[] lines, int i) {
        return lines[i].contains("|") && i + 1 < lines.length && lines[i + 1].contains("-") && lines[i + 1].contains("|")
                && TABLE_LINE.matcher(lines[i + 1]).matches();
    }

    private static List<List<Run>> cells(String line) {
        String row = line.trim();
        row = row.startsWith("|") ? row.substring(1) : row;
        row = row.endsWith("|") ? row.substring(0, row.length() - 1) : row;
        List<List<Run>> cells = new ArrayList<List<Run>>();
        for (String cell : row.split("(?<!\\\\)\\|", -1)) {
            cells.add(runs(cell.trim().replace("\\|", "|")));
        }
        return cells;
    }

    // ---- inside a block

    /** text as runs of one style each: "a **b** c" is "a " PLAIN, "b" BOLD, " c" PLAIN */
    static List<Run> runs(String text) {
        List<Run> runs = new ArrayList<Run>();
        inline(text, RunStyle.PLAIN, runs);
        return runs;
    }

    private static void inline(String text, RunStyle style, List<Run> runs) {
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int end;
            if (c == '\\' && i + 1 < text.length() && !Character.isLetterOrDigit(text.charAt(i + 1))) {
                plain.append(text.charAt(i + 1));
                i += 2;
            } else if (c == '`' && (end = text.indexOf('`', i + 1)) > 0) {
                flush(plain, style, runs);
                runs.add(new Run(text.substring(i + 1, end), RunStyle.CODE, null));
                i = end + 1;
            } else if (c == '!' && i + 1 < text.length() && text.charAt(i + 1) == '[' && (end = linkEnd(text, i + 1)) > 0) {
                flush(plain, style, runs);
                runs.add(new Run("[" + linkText(text, i + 1) + "]", RunStyle.IMAGE, linkTarget(text, i + 1)));
                i = end;
            } else if (c == '[' && (end = linkEnd(text, i)) > 0) {
                flush(plain, style, runs);
                runs.add(new Run(plainText(linkText(text, i)), RunStyle.LINK, linkTarget(text, i)));
                i = end;
            } else if ((c == '*' || c == '_') && (end = emphasisEnd(text, i)) > 0) {
                flush(plain, style, runs);
                int marks = text.startsWith("" + c + c, i) ? 2 : 1;
                inline(text.substring(i + marks, end), marks == 2 ? RunStyle.BOLD : RunStyle.ITALIC, runs);
                i = end + marks;
            } else if (c == '<' && HTML_TAG.matcher(text.substring(i)).find()) {
                Matcher tag = HTML_TAG.matcher(text.substring(i));
                tag.find();
                i += tag.end();
            } else {
                plain.append(c);
                i++;
            }
        }
        flush(plain, style, runs);
    }

    private static void flush(StringBuilder plain, RunStyle style, List<Run> runs) {
        if (plain.length() > 0) {
            runs.add(new Run(plain.toString(), style, null));
            plain.setLength(0);
        }
    }

    // where the closing marks of the emphasis opening at start are; -1: none. A _ inside a word (snake_case) opens nothing.
    private static int emphasisEnd(String text, int start) {
        char c = text.charAt(start);
        if (c == '_' && start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
            return -1;
        }
        String marks = text.startsWith("" + c + c, start) ? "" + c + c : "" + c;
        int from = start + marks.length();
        if (from >= text.length() || Character.isWhitespace(text.charAt(from))) {
            return -1;                                                         // "2 * 3": not emphasis
        }
        int end = text.indexOf(marks, from);
        return end > from && !Character.isWhitespace(text.charAt(end - 1)) ? end : -1;
    }

    // [text](target) from open: the ] that closes the [, counting nested ones - a link around an image has them
    private static int closingBracket(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            if (text.charAt(i) == '[') {
                depth++;
            } else if (text.charAt(i) == ']' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    // just past the ) of the link opening at open; -1: not a link
    private static int linkEnd(String text, int open) {
        int close = closingBracket(text, open);
        if (close < 0 || close + 1 >= text.length() || text.charAt(close + 1) != '(') {
            return -1;
        }
        int end = text.indexOf(')', close + 2);
        return end < 0 ? -1 : end + 1;
    }

    private static String linkText(String text, int open) {
        return text.substring(open + 1, closingBracket(text, open));
    }

    private static String linkTarget(String text, int open) {
        int close = closingBracket(text, open);
        String target = text.substring(close + 2, text.indexOf(')', close + 2)).trim();
        int title = target.indexOf(' ');                                       // [a](b "a title")
        return title < 0 ? target : target.substring(0, title);
    }

    // a link's text without its marks: a link around an image reads as the image's words
    private static String plainText(String text) {
        StringBuilder plain = new StringBuilder();
        for (Run run : runs(text)) {
            plain.append(run.style == RunStyle.IMAGE ? run.text.substring(1, run.text.length() - 1) : run.text);
        }
        return plain.toString();
    }
}
