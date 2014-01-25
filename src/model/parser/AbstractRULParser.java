package model.parser;

import model.NetworkTile;
import model.NetworkTile.StringIID;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

public abstract class AbstractRULParser extends BaseParser<Object> {

    /**
     * Pushes {@link NetworkTile} IID,rot,flip on stack.  
     */
    @Cached
    Rule IIDTile(boolean preventAllowed) {
        if (preventAllowed) {
            return FirstOf(
                    Prevent(),
                    IIDTile(false)
            );
        } else {
            return Sequence(
                    HexUInt(),
                    ", ",
                    Rot(),
                    ", ",
                    Flip(),
                    swap3() && push(new NetworkTile(new StringIID((String) pop()), (Integer) pop(), (Integer) pop() != 0))
            );
        }
    }
    
    /**
     * Pushes {@link NetworkTile} 0,0,0 (prevent) on stack.
     */
    Rule Prevent() {
        return Sequence(
                "0 ",
                ", ",
                "0 ",
                ", ",
                "0 ",
                push(new NetworkTile(new StringIID("0"), 0, false))
        );
    }

    /**
     * Pushes {@code String} HexUInt on stack.
     */
    @SuppressSubnodes
    Rule HexUInt() {
        return Sequence(
                Sequence("0",
                    AnyOf("xX"),
                    NTimes(8, FirstOf(
                            CharRange('0', '9'),
                            CharRange('a', 'f'),
                            CharRange('A', 'F')
                    ))
                ), push(match()),
                WhiteSpace()
        );
    }

    /**
     * Pushes {@code Integer} rot on stack.
     */
    Rule Rot() {
        return Sequence(
                CharRange('0', '3'), push((int) (matchedChar() - '0')), WhiteSpace()
        );
    }

    /**
     * Pushes {@code Integer} flip on stack.
     */
    Rule Flip() {
        return Sequence(
                AnyOf("01"), push((int) (matchedChar() - '0')), WhiteSpace()
        );
    }

    @SuppressNode
    Rule WhiteSpace() {
        return ZeroOrMore(AnyOf(" \t\f"));
    }

    /*
     * we redefine the rule creation for string literals to automatically match
     * trailing whitespace if the string literal ends with a space character,
     * this way we don't have to insert extra whitespace() rules after each
     * character or string literal
     */
    @Override
    @SuppressNode
    @Cached
    protected Rule fromStringLiteral(String string) {
        return string.endsWith(" ") ?
                Sequence(String(string.substring(0, string.length() - 1)), WhiteSpace()) :
                String(string);
    }
}
