package model;

import org.parboiled.Rule;

public class RUL1Parser extends AbstractRULParser {
    
    /**
     * Pushes String flags (bottom), String flags, {@link NetworkTile}
     * IID,rot,flip (top) on stack.
     */
    Rule RUL1IntersectionDefinition() {
        return Sequence(
                ZeroOrMore(TestNot(AnyOf("=;")), ANY),
                "= ",
                HexFlags(),
                ", ",
                HexFlags(),
                ", ",
                IIDTile(false)
        );
    }
    
    /**
     * Pushes hex flags as comma separated string on stack.
     */
    Rule HexFlags() {
        return Sequence(
                HexUInt(),
                push(convertHexFlags(Long.parseLong(((String) pop()).substring(2), 16)))
        );
    }
    
    static String convertHexFlags(long uint) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(uint & 0xff);
//        sb.append(',');
//        sb.append((uint >>> 8) & 0xff);
//        sb.append(',');
//        sb.append((uint >>> 16) & 0xff);
//        sb.append(',');
//        sb.append((uint >>> 24) & 0xff);
//        return sb.toString();
        return String.format("%X,%X,%X,%X",
                uint & 0xff,
                (uint >>> 8) & 0xff,
                (uint >>> 16) & 0xff,
                (uint >>> 24) & 0xff);
    }
}
