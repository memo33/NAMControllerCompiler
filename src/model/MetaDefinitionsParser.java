package model;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

public class MetaDefinitionsParser extends BaseParser<String> {
    
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

    /**
     * After match, top element of stack is the first network name, followed by
     * the rest of the network names that are synonyms.
     */
    Rule NameSynonymDefinition() {
        return Sequence(
                "DEFINE-SYNONYM-NAME ",
                "= ",
                Name(), push(match().trim()),
                OneOrMore(
                        "; ",
                        Name(), push(match().trim()), swap()
                ),
                EOI
        );
    }
    
    /**
     * After match, top element of stack is Group name, followed by network
     * names that are members of this group.
     */
    Rule GroupDefinition() {
        return Sequence(
                "DEFINE-GROUP ",
                Name(), push(match().trim()),
                "= ",
                Name(), push(match().trim()), swap(),
                ZeroOrMore(
                        "; ",
                        Name(), push(match().trim()), swap()
                ),
                EOI
        );
    }

    /**
     * Afterward match, top element of stack is Group name, second is the first
     * direction name, followed by the rest of the direction names.
     */
    Rule DirectionSynonymDefinition() {
        return Sequence(
                "DEFINE-SYNONYM-DIRECTION ",
                Name(), push(match().trim()),
                "= ",
                Direction(), push(match().trim()), swap(),
                OneOrMore(
                        "; ",
                        Direction(), push(match().trim()), swap3(), swap() 
                ),
                EOI
        );
    }

    @SuppressSubnodes
    Rule Name() {
        return OneOrMore(TestNot(AnyOf("[]=;,")), ANY);
    }
    
    @SuppressSubnodes
    Rule Direction() {
        return OneOrMore(TestNot(AnyOf("[]=;")), ANY);
    }    

}
