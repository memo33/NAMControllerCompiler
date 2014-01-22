package model;

import model.MetaNetworkTile.MetaNetwork;

import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;

//@BuildParseTree
public class MetaOverrideParser extends AbstractRULParser {

//    @SuppressNode
    Rule Override() {
        return Sequence(
                Tile(false),
                OptionalComma(),
                Tile(false),
                "= ",
                Tile(true),
                OptionalComma(),
                Tile(true),
                swap4() && push(new OverrideRule((NetworkTile) pop(), (NetworkTile) pop(), (NetworkTile) pop(), (NetworkTile) pop()))
        );
    }
    
    @SuppressNode
    Rule OptionalComma() {
        return FirstOf(
                ", ",
                Sequence(WhiteSpace(), Test(AnyOf("[%0"))) // note: currently also allows "rot,flip0x12345678" without space or comma 
        );
    }
    
    @Cached
    Rule Tile(boolean preventAllowed) {
        return FirstOf(
                IIDTile(preventAllowed),
                MetaTile(),
                Sequence(Test(preventAllowed), "% ", push(peek(1)))
        );
    }

    Rule MetaTile() {
        return Sequence(
                "[ ",
                Intersection(), push(((MetaNetworkTile) pop()).convert(metaController)),
                "] ",
                Optional(
                        ", ",
                        Rot(),
                        ", ",
                        Flip(),
                        swap3() && push(((NetworkTile) pop()).rotate((Integer) pop(), (Integer) pop()))
                )
        );
    }
    
    /**
     * Pushes {@link MetaNetworkTile} on stack.
     */
    Rule Intersection() {
        return Sequence(
                Network(), push(new MetaNetworkTile((MetaNetwork) pop())),
                ZeroOrMore(
                        "; ", Network(), metaController.appendMetaNetwork((MetaNetworkTile) peek(1), (MetaNetwork) pop())
                )
        );
    }
    
    Rule Network() {
        return Sequence(
                Name(), push(match().trim()),
                ", ",
                Direction(), push(metaController.createMetaNetwork((String) pop(), match().trim()))
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
    
    /**
     * Pushes {@link MetaNetworkTile} (bottom) and {@link NetworkTile} (top) on stack.
     */
    Rule MetaIntersectionDefinition() {
        return Sequence(
                Intersection(),
                "= ",
                IIDTile(false)
        );
    }
    
    final MetaController metaController;
    
    MetaOverrideParser(MetaController metaController) {
        this.metaController = metaController;
    }
        
//    public static void main(String[] args) {
//        
//        String[] meta1 = {
//                // TLA3 x Street
//                "tla3,0,2,0,2; street,0,0,1,3 = 0x51005000,0,0",
//                "tla3,2,0,2,0; street,3,0,0,1 = 0x51005000,1,0",
//                "tla3,0,2,0,2; street,1,3,0,0 = 0x51005000,2,0",
//                "tla3,2,0,2,0; street,0,1,3,0 = 0x51005000,3,0",
//                "tla3,0,2,0,2; street,0,1,3,0 = 0x51005080,0,0",
//                "tla3,2,0,2,0; street,0,0,1,3 = 0x51005080,1,0",
//                "tla3,0,2,0,2; street,3,0,0,1 = 0x51005080,2,0",
//                "tla3,2,0,2,0; street,1,3,0,0 = 0x51005080,3,0",
//                // Road x Street
//                "road,0,2,0,2; street,0,0,1,3 = 0x5F502200,0,0",
//                "road,2,0,2,0; street,3,0,0,1 = 0x5F502200,1,0",
//                "road,0,2,0,2; street,1,3,0,0 = 0x5F502200,2,0",
//                "road,2,0,2,0; street,0,1,3,0 = 0x5F502200,3,0",
//                "road,0,2,0,2; street,0,1,3,0 = 0x5F502200,2,1",
//                "road,2,0,2,0; street,0,0,1,3 = 0x5F502200,1,1",
//                "road,0,2,0,2; street,3,0,0,1 = 0x5F502200,0,1",
//                "road,2,0,2,0; street,1,3,0,0 = 0x5F502200,3,1",
//                // TLA3 orthogonal
//                "tla3,0,2,0,2 = 0x51000000,0,0",
//                "tla3,2,0,2,0 = 0x51000000,1,0",
//        };
//        
//        String[] meta2 = {
//                "[tla3, we]     [rd,   we;      street, 0,0,1,3]     = % [tla3, 2,0,2,0; street, 0,0,1,3]",
//                "[tla3, we]     [road, 2,0,2,0; street, 0,1,3,0]     = % [tla3, 2,0,2,0; street, 0,1,3,0]",
//                "[tla3, we],2,0 [road, 2,0,2,0; street, 0,0,1,3]     = % [tla3, 2,0,2,0; street, 0,0,1,3]",
//                "[tla3, we],2,0 [road, 2,0,2,0; street, 0,1,3,0]     = % [tla3, 2,0,2,0; street, 0,1,3,0]"
//        };
//        
//        MetaOverrideParser parser = Parboiled.createParser(MetaOverrideParser.class);
//        ReportingParseRunner<?> meta1Runner= new ReportingParseRunner<Object>(parser.MetaIntersectionDefinition());
//        ReportingParseRunner<OverrideRule> meta2Runner = new ReportingParseRunner<OverrideRule>(parser.Override());
//        
//        for (String line : meta1) {
//            ParsingResult<?> result = meta1Runner.run(line);
//            if (!result.parseErrors.isEmpty()) {
//                System.out.println(ErrorUtils.printParseError(result.parseErrors.get(0)));
//            } else {
//                result.valueStack.swap();
//                metaController.putMetaNetworkDefinition((MetaNetworkTile) result.valueStack.pop(), (NetworkTile) result.valueStack.pop());
//            }
//        }
//        
////        for (Entry<MetaNetworkTile, NetworkTile> e : mapper.map.entrySet()) {
////            System.out.println(e.getKey() + "  =  " + e.getValue());
////        }
//        
//        System.out.println();
//        
////        List<MetaNetworkTile> metaTiles = new ArrayList<MetaNetworkTile>(mapper.map.keySet());
////        int size = metaTiles.size();
//
//        long time = System.currentTimeMillis();
//        /*for (int i = 0; i < 1000000/4; i++)*/ {
////            String line = "[" + metaTiles.get((int) (Math.random() * size)) + "] [" + metaTiles.get((int) (Math.random() * size)) + "] = [" + metaTiles.get((int) (Math.random() * size)) + "] [" + metaTiles.get((int) (Math.random() * size)) + "]";
//            for (String line : meta2) {
//                ParsingResult<?> result = meta2Runner.run(line);
//                if (!result.parseErrors.isEmpty()) {
//                    System.out.println(ErrorUtils.printParseError(result.parseErrors.get(0)));
//                } else {
//                    System.out.println(line + "\n" + result.resultValue);
//                }
//            }
////            if (i % 40000 == 0) {
////                System.out.println(i);
////            }
//        }
//        System.out.println("Time: " + (System.currentTimeMillis() - time));
//    }
}
