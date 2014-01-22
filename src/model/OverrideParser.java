package model;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;


// TODO comment/uncomment
//@BuildParseTree
public class OverrideParser extends BaseParser<Object> {
    
    @SuppressNode
    Rule Override() {
        return Sequence(
                Tile(), ",", Tile(), "=", Tile(), ",", Tile()
                );
    }
    
    Rule Tile() {
        return Sequence(
                IID(), push(match()), ",", Rot(), ",", Flip()
                );
    }
    
    @SuppressSubnodes
    Rule IID() {
        return FirstOf(
                Sequence(
                        "0", AnyOf("xX"), NTimes(8, AnyOf("0123456789abcdefABCDEF"))
                ),
                "0");
    }
    
    Rule Rot() {
        return CharRange('0', '3');
    }
    
    Rule Flip() {
        return AnyOf("01");
    }
}