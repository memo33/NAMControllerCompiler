package model;

public final class NetworkTile /*implements Comparable<NetworkTile>*/ {
    
//    public static final long PREVENT_ID = -1L;
//    public static final NetworkTile PREVENT_TILE = new NetworkTile(PREVENT_ID, 0, false);
    
    final TileIID id;
    final int rot;
    final boolean flip;
    
    public NetworkTile(TileIID id, int rot, boolean flip) {
        if (rot < 0 || rot >= 4 || 
                (id.isPrevent() && (rot != 0 || flip))) {
            throw new IllegalArgumentException();
        }
        this.id = id;
        this.rot = rot;
        this.flip = flip;
    }
    
    public NetworkTile rotate(int rot, int flip) {
        if (this.id.isPrevent()) {
            return this;
        } else {
            return new NetworkTile(this.id, (this.rot + (this.flip ? -1 : 1) * rot) & 0x3, this.flip ^ (flip % 2 != 0));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (flip ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + rot;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NetworkTile other = (NetworkTile) obj;
        if (flip != other.flip) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (rot != other.rot) {
            return false;
        }
        return true;
    }

//    @Override
//    public int compareTo(NetworkTile nt) {
//        if (this.id != nt.id) {
//            return this.id < nt.id ? -1 : 1;
//        } else if (this.rot != nt.rot) {
//            return this.rot < nt.rot ? -1 : 1;
//        } else if (this.flip != nt.flip) {
//            return !this.flip ? -1 : 1;
//        } else {
//            return 0;
//        }
//    }
    
//    @Override
//    public boolean equals(Object o) {
//        if (o instanceof NetworkTile) {
//            return this.compareTo((NetworkTile) o) == 0;
//        } else {
//            return false;
//        }
//    }
//    
//    @Override
//    public int hashCode() {
//        return (int) this.id + 2 * this.rot + (this.flip ? 1 : 0); 
//    }
    
    public TileIID getID() {
        return this.id;
    }
    
    public int getRotation() {
        return this.rot;
    }
    
    public boolean isFlipped() {
        return this.flip;
    }
    
    @Override
    public String toString() {
//        return String.format("%s,%d,%d", this.id.asString(), this.rot, this.flip ? 1 : 0);
        return this.id.asString() + "," + this.rot + "," + (flip ? 1 : 0);
    }
    
    public static interface TileIID {
        
        public boolean isPrevent();
        
        public String asString();
    }
    
    public static class LongIID implements TileIID {
        
        public static final long PREVENT_IID = -1L;

        private final long value;
        
        public LongIID(long value) {
            if ((value < 0L || value > 0xffffffffL) && value != PREVENT_IID) {
                throw new IllegalArgumentException("IID out of range");
            }
            this.value = value;
        }

        @Override
        public boolean isPrevent() {
            return value == PREVENT_IID;
        }

        @Override
        public String asString() {
            return "0x" + Long.toString(value, 16); // TODO fill with zeroes
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (value ^ (value >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LongIID other = (LongIID) obj;
            if (value != other.value) {
                return false;
            }
            return true;
        }
        
    }
    
    public static class StringIID implements TileIID {
        
        private final String value;
        
        public StringIID(String value) {
            if (value == null) {
                throw new IllegalArgumentException("null is not permitted");
            }
            this.value = value;
        }

        @Override
        public boolean isPrevent() {
            return value.equals("0");
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            StringIID other = (StringIID) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equalsIgnoreCase(other.value)) {
                return false;
            }
            return true;
        }
        
    }
}
