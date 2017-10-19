package com.joedobo27.spm;


import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Properties;
import java.util.logging.Logger;

public class SimplePavingMod implements WurmServerMod, Initable, Configurable, ServerStartedListener, PlayerMessageListener {

    static final Logger logger = Logger.getLogger(SimplePavingMod.class.getName());

    @Override public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        if (communicator.getPlayer().getPower() == 5 && message.startsWith("/SimplePavingMod properties")) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Reloading properties for SimplePavingMod."
            );
            ConfigureOptions.resetOptions();
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(PaveActionPerformer.getPaveActionPerformer());
        ModActions.registerAction(CornerPaveActionPerformer.getCornerPaveActionPerformer());
    }

    @Override
    public void configure(Properties properties) {
        ConfigureOptions.setOptions(properties);
    }

    @Override
    public void init() {
        ModActions.init();
    }

    static boolean activeIsSurfacePaver(Item activeItem) {
        if (activeItem == null)
            return false;
        switch (activeItem.getTemplateId()){
            case ItemList.stoneChisel:
            case ItemList.stoneBrick:
            case ItemList.roundedBrick:
            case ItemList.colossusPart:
            case ItemList.stoneSlab:
            case ItemList.slateBrick:
            case ItemList.slateSlab:
            case ItemList.sandstoneBrick:
            case ItemList.sandstoneSlab:
            case ItemList.marbleBrick:
            case ItemList.marbleSlab:
            case ItemList.brickPottery:
            case ItemList.floorBoards:
            case ItemList.rock:
                return true;
            default:
                return false;
        }
    }

    static boolean activeIsCavePaver(Item activeItem) {
        if (activeItem == null)
            return false;
        switch (activeItem.getTemplateId()){
            case ItemList.stoneChisel:
            case ItemList.joists:
            case ItemList.mortar:
            case ItemList.stoneBrick:
            case ItemList.roundedBrick:
            case ItemList.colossusPart:
            case ItemList.stoneSlab:
            case ItemList.slateBrick:
            case ItemList.slateSlab:
            case ItemList.sandstoneBrick:
            case ItemList.sandstoneSlab:
            case ItemList.marbleBrick:
            case ItemList.marbleSlab:
            case ItemList.brickPottery:
            case ItemList.floorBoards:
            case ItemList.rock:
                return true;
            default:
                return false;
        }
    }

    static boolean tileCanBePaved(int encodedTile){
        int tileType = Byte.toUnsignedInt(Tiles.decodeType(encodedTile));
        switch (tileType){
            case Tiles.TILE_TYPE_CAVE_EXIT:
            case Tiles.TILE_TYPE_CAVE:
            case Tiles.TILE_TYPE_CAVE_PREPARED_FLOOR_REINFORCED:
            case Tiles.TILE_TYPE_CAVE_FLOOR_REINFORCED:
            case Tiles.TILE_TYPE_COBBLESTONE:
            case Tiles.TILE_TYPE_COBBLESTONE_ROUND:
            case Tiles.TILE_TYPE_COBBLESTONE_ROUGH:
            case Tiles.TILE_TYPE_STONE_SLABS:
            case Tiles.TILE_TYPE_SLATE_BRICKS:
            case Tiles.TILE_TYPE_SLATE_SLABS:
            case Tiles.TILE_TYPE_SANDSTONE_BRICKS:
            case Tiles.TILE_TYPE_SANDSTONE_SLABS:
            case Tiles.TILE_TYPE_MARBLE_BRICKS:
            case Tiles.TILE_TYPE_MARBLE_SLABS:
            case Tiles.TILE_TYPE_PLANKS:
            case Tiles.TILE_TYPE_PLANKS_TARRED:
            case Tiles.TILE_TYPE_GRAVEL:
            case Tiles.TILE_TYPE_DIRT_PACKED:
            case Tiles.TILE_TYPE_POTTERY_BRICKS:
                return true;
            default:
                return false;
        }
    }
}
