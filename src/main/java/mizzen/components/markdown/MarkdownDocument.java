package mizzen.components.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown once it is read: a list of blocks, each with runs of styled text. Nothing here knows about
 * width or the screen - MarkdownParser makes it, MarkdownView lays it out.
 * <pre>
 *     ## Quick **start**       HEADING, level 2: "Quick " PLAIN, "start" BOLD
 *     See [the code](A.java)   PARAGRAPH: "See " PLAIN, "the code" LINK with target A.java
 * </pre>
 */
public final class MarkdownDocument {

    public enum BlockKind { HEADING, PARAGRAPH, LIST_ITEM, QUOTE, CODE, TABLE, RULE }

    public enum RunStyle { PLAIN, BOLD, ITALIC, CODE, LINK, IMAGE }

    /** some text in one style; target: where a LINK or IMAGE points, else null */
    public static final class Run {
        public final String text;
        public final RunStyle style;
        public final String target;

        Run(String text, RunStyle style, String target) {
            this.text = text;
            this.style = style;
            this.target = target;
        }
    }

    /** one block; which fields count depends on its kind */
    public static final class Block {
        public final BlockKind kind;
        public int level = 0;                                                  // HEADING: 1 to 6; LIST_ITEM: how deep, from 0
        public String marker = "";                                             // LIST_ITEM: "• " or "1. "
        public String language = "";                                           // CODE: what follows the opening fence
        public List<Run> runs = new ArrayList<Run>();                          // HEADING, PARAGRAPH, LIST_ITEM, QUOTE
        public final List<String> codeLines = new ArrayList<String>();        // CODE
        public final List<List<List<Run>>> tableRows = new ArrayList<List<List<Run>>>();   // TABLE: rows of cells, the heading row first

        Block(BlockKind kind) {
            this.kind = kind;
        }
    }

    public final List<Block> blocks = new ArrayList<Block>();
}
