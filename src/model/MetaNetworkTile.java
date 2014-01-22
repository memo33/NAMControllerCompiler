package model;

import java.util.HashSet;
import java.util.Set;

import org.parboiled.errors.ParsingException;

public class MetaNetworkTile {

    private final Set<MetaNetwork> metaNetworks;
    
    public MetaNetworkTile(MetaNetwork... metaNetworks) {
        if (metaNetworks == null || metaNetworks.length == 0) {
            throw new IllegalArgumentException();
        }
        this.metaNetworks = new HashSet<MetaNetwork>(4);
        for (MetaNetwork mn : metaNetworks) {
            this.metaNetworks.add(mn);
        }
    }
    
    public NetworkTile convert(MetaController mapper) throws ParsingException {
        return mapper.convert(this);
    }
    
    public void append(MetaNetwork metaNetwork) {
        this.metaNetworks.add(metaNetwork);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (MetaNetwork mn : metaNetworks) {
            if (!first) {
                sb.append(';');
            } else {
                first = false;
            }
            sb.append(mn);
        }
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((metaNetworks == null) ? 0 : metaNetworks.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetaNetworkTile other = (MetaNetworkTile) obj;
        if (metaNetworks == null) {
            if (other.metaNetworks != null)
                return false;
        } else if (!metaNetworks.equals(other.metaNetworks))
            return false;
        return true;
    }

    public static class MetaNetwork {
        private final String name;
        private final String direction;
        
        public MetaNetwork(String name, String direction) {
            if (name == null || direction == null) {
                throw new IllegalArgumentException();
            }
            this.name = name;
            this.direction = direction;
        }
        
        @Override
        public String toString() {
            return name + ',' + direction;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((direction == null) ? 0 : direction.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MetaNetwork other = (MetaNetwork) obj;
            if (direction == null) {
                if (other.direction != null)
                    return false;
            } else if (!direction.equals(other.direction))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
        
    }
}
