package model;

public class OverrideRule {
    
    private final OverrideTuple in, out;

    public OverrideRule(NetworkTile tileA, NetworkTile tileB, NetworkTile tileC, NetworkTile tileD) {
        this.in = new OverrideTuple(tileA, tileB);
        this.out = new OverrideTuple(tileC, tileD);
    }
    
//    /**
//     * @param line line of override without comments ';'
//     */
//    public OverrideRule(String line) {
//        String[] splits = line.split(",|=");
//        if (splits.length != 12) {
//            throw new IllegalArgumentException("Override format mismatch: " + line);
//        }
//        for (int i = 0; i < splits.length; i++) {
//            splits[i] = splits[i].trim();
//        }
//        
//        NetworkTile[] tiles = new NetworkTile[4];
//        for (int i = 0; i < tiles.length; i++) {
//            long id = -2L;
//            if (splits[3 * i].toLowerCase().startsWith("0x")) {
//                id = Long.parseLong(splits[3 * i].substring(2), 16);
//            } else if (splits[3 * i].equals("0")) {
//                id = -1L;
//            } else {
//                throw new IllegalArgumentException("Override format mismatch");
//            }
//            int rot = Integer.parseInt(splits[3 * i + 1]);
//            boolean flip = Integer.parseInt(splits[3 * i + 2]) == 1;
//            tiles[i] = new NetworkTile(id, rot, flip);
//        }
//        this.in = new OverrideTuple(tiles[0], tiles[1]);
//        this.out = new OverrideTuple(tiles[2], tiles[3]);
//    }
    
    public OverrideTuple getInputTuple() {
        return this.in;
    }
    
    public OverrideTuple getOutputTuple() {
        return this.out;
    }
    
    @Override
    public String toString() {
        return this.in.toString() + "=" + this.out.toString();
    }
    
    public static class OverrideTuple /*implements Comparable<OverrideTuple>*/ {
        
        private NetworkTile left, right;
        
        private OverrideTuple(NetworkTile left, NetworkTile right) {
            this.left = left;
            this.right = right;
        }
        
//        public OverrideTuple getStandardRepresentation() {
//            OverrideTuple result1 = null, result2 = null;
//            if (left.id <= right.id) {
//                if (!left.flip) {
//                    result1 = this;
//                } else {
//                    result1 = new OverrideTuple(left.rotate(2,1), right.rotate(2,1));
//                }
//                if (left.id != right.id) {
//                    return result1;
//                }
//            }
//            if (left.id >= right.id) {
//                if (!right.flip) {
//                    result2 = new OverrideTuple(right.rotate(2,0), left.rotate(2,0));
//                } else {
//                    result2 = new OverrideTuple(right.rotate(0,1), left.rotate(0,1));
//                }
//                if (left.id != right.id) {
//                    return result2;
//                }
//            }
//            // left.id == right.id
//            return result1.left.compareTo(result2.left) <= 0 ? result1 : result2;
//        }
//        
//        @Override
//        public int compareTo(OverrideTuple oh) {
//            OverrideTuple std1 = this.getStandardRepresentation();
//            OverrideTuple std2 = oh.getStandardRepresentation();
//            int result;
//            result = std1.left.compareTo(std2.left);
//            if (result == 0) {
//                result = std1.right.compareTo(std2.right);
//            }
//            return result;
//        }
//        
//        @Override
//        public boolean equals(Object o) {
//            if (o instanceof OverrideTuple) {
//                return this.compareTo((OverrideTuple) o) == 0;
//            }
//            return false;
//        }
//        
//        @Override
//        public int hashCode() {
//            OverrideTuple std = this.getStandardRepresentation();
//            return 2 * std.left.hashCode() + std.right.hashCode();
//        }
        
        public NetworkTile getLeftTile() {
            return this.left;
        }
        
        public NetworkTile getRightTile() {
            return this.right;
        }
        
        @Override
        public String toString() {
            return this.left.toString() + "," + this.right.toString();
        }
    }
}
