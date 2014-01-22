package model;

import java.util.HashMap;
import java.util.Map;

import model.MetaNetworkTile.MetaNetwork;

import org.parboiled.errors.ParsingException;

public class MetaController {
    
    // FIXME run all RUL entries on the same thread to avoid synchronization
    
    private final Map<MetaNetworkTile, NetworkTile> metaTileMap = new HashMap<MetaNetworkTile, NetworkTile>();
    private final Map<String, String> nameSynonyms = new HashMap<String, String>();
    // entries of type <network name, group name>
    private final Map<String, String> groupDefinitions = new HashMap<String, String>();
    // entries of type <<group name, phony direction>, direction>
    private final Map<MetaNetwork, String> directionSynonyms = new HashMap<MetaNetwork, String>();

    public void putNameSynonym(String keyName, String valueName) throws DuplicateDefinitionException {
        if (nameSynonyms.containsKey(keyName)) {
            throw new DuplicateDefinitionException("Is already defined: " + keyName);
        }
        nameSynonyms.put(keyName, valueName);
    }
    
    public void putGroupDefinition(String networkName, String groupName) throws DuplicateDefinitionException {
        if (groupDefinitions.containsKey(networkName)) {
            throw new DuplicateDefinitionException("Is already defined: " + networkName);
        }
        groupDefinitions.put(networkName, groupName);
    }
    
    public void putGroupDirectionSynonym(String groupName, String altDirLabel, String defaultDirLabel) throws DuplicateDefinitionException {
        MetaNetwork metaNetwork = new MetaNetwork(groupName, altDirLabel);
        if (directionSynonyms.containsKey(metaNetwork)) {
            throw new DuplicateDefinitionException("Is already defined: " + metaNetwork);
        }
        directionSynonyms.put(metaNetwork, defaultDirLabel);
    }
    
    public NetworkTile convert(MetaNetworkTile metaTile) throws ParsingException {
        if (!metaTileMap.containsKey(metaTile)) {
            throw new ParsingException("Is not defined: " + metaTile);
        }
        return metaTileMap.get(metaTile);
    }
    
    public void putMetaNetworkDefinition(MetaNetworkTile metaTile, NetworkTile tile) throws DuplicateDefinitionException {
        if (metaTileMap.containsKey(metaTile)) {
            throw new DuplicateDefinitionException("Is already defined: " + metaTile);
        } else {
            metaTileMap.put(metaTile, tile);
        }
    }
    
    public MetaNetwork createMetaNetwork(String name, String direction) {
        if (nameSynonyms.containsKey(name)) {
            name = nameSynonyms.get(name);
        }
        if (groupDefinitions.containsKey(name)) {
            String group = groupDefinitions.get(name);
            MetaNetwork groupTile = new MetaNetwork(group, direction);
            if (directionSynonyms.containsKey(groupTile)) {
                direction = directionSynonyms.get(groupTile);
            }
        }
        return new MetaNetwork(name, direction);
    }
    
    public boolean appendMetaNetwork(MetaNetworkTile tile, MetaNetwork network) {
        tile.append(network);
        return true;
    }
}
