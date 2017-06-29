package com.joedobo27.spm;


import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.utils.logging.TileEvent;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PaveAction implements ModAction, BehaviourProvider, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;

    @Override
    public short getActionId() {
        return this.actionId;
    }

    PaveAction() {
        this.actionId = Actions.ROAD_PAVE;
        this.actionEntry = Actions.actionEntrys[Actions.ROAD_PAVE];
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activeItem ,int tileX, int tileY, boolean onSurface,
                                              int encodedTile) {
        if (tileIsPavable(encodedTile) && activeIsPaver(activeItem)) {
            return Collections.singletonList(actionEntry);
        }
        return BehaviourProvider.super.getBehavioursFor(performer, activeItem, tileX, tileY, onSurface, encodedTile);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activeItem, int tileX, int tileY, boolean onSurface,
                                              int encodedTile, int dir) {
        if (tileIsPavable(encodedTile) && activeIsPaver(activeItem)) {
            return Collections.singletonList(actionEntry);
        }
        return BehaviourProvider.super.getBehavioursFor(performer, activeItem, tileX, tileY, onSurface, encodedTile, dir);
    }

    @Override
    public List<ActionEntry> getBehavioursFor( Creature performer,  Item activeItem,  boolean onSurface,  BridgePart bridgePart) {
        if (activeIsPaver(activeItem)) {
            return Collections.singletonList(actionEntry);
        }
        return BehaviourProvider.super.getBehavioursFor(performer, activeItem, onSurface, bridgePart);
    }

    @Override
    public boolean action(Action action, Creature performer, Item activeItem,  int tileX,  int tileY,  boolean onSurface,
                          int heightOffset,  int encodedTile,  short actionId,  float counter) {
        if (tileIsPavable(encodedTile) && activeIsPaver(activeItem)) {
            int time;
             float TIME_TO_COUNTER_DIVISOR = 10.0f;
             float ACTION_START_TIME = 1.0f;
            String youMessage;
            String broadcastMessage;

            if (counter == ACTION_START_TIME) {
                if (hasAFailureCondition(performer, activeItem, tileX, tileY, encodedTile, null)) {
                    return true;
                }
                Skill pavingSkill = performer.getSkills().getSkillOrLearn(SkillList.PAVING);
                time = Actions.getStandardActionTime(performer, pavingSkill, activeItem, 0.0);
                action.setTimeLeft(time);
                if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                    youMessage = String.format("You start to remove the paving.");
                    broadcastMessage = String.format("%s starts to remove the paving.", performer.getName());
                }
                else {
                    youMessage = String.format("You start to make %s pavement.", getPavementName(activeItem));
                    broadcastMessage = String.format("%s starts to make %s pavement.", performer.getName(), getPavementName(activeItem));
                }
                performer.getCommunicator().sendNormalServerMessage(youMessage);
                Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
                performer.sendActionControl(action.getActionEntry().getVerbString(), true, time);
                performer.getStatus().modifyStamina(-1000.0f);
                return false;
            }
            if (action.currentSecond() % 5 == 0) {
                performer.getStatus().modifyStamina(-10000.0f);
            }
            if (action.mayPlaySound()) {
                Methods.sendSound(performer, "sound.work.paving");
            }
            boolean actionInProcess = counter < action.getTimeLeft() / TIME_TO_COUNTER_DIVISOR;
            if (actionInProcess)
                return false;
            if (hasAFailureCondition(performer, activeItem, tileX, tileY, encodedTile, null)) {
                return true;
            }
            Skill pavingSkill = performer.getSkills().getSkillOrLearn(SkillList.PAVING);
            pavingSkill.skillCheck((activeItem.getTemplateId() == ItemList.rock) ? 5.0 : 30.0, activeItem, 0.0, false, counter);
            TileEvent.log(tileX, tileY, -1, performer.getWurmId(), actionId);
            if (onSurface) {
                byte tileType;
                if (activeItem.getTemplateId() == ItemList.stoneChisel)
                    tileType = Tiles.Tile.TILE_DIRT_PACKED.id;
                else
                    tileType= getPavementTileType(activeItem);
                Server.surfaceMesh.setTile(tileX, tileY, Tiles.encode(Tiles.decodeHeight(encodedTile), tileType, Tiles.decodeData(encodedTile)));
                Players.getInstance().sendChangedTile(tileX, tileY, onSurface, false);
            }
            else {
                Server.caveMesh.setTile(tileX, tileY, Tiles.encode(Tiles.decodeHeight(encodedTile), getPavementTileType(activeItem), Tiles.decodeData(encodedTile)));
                TileRockBehaviour.sendCaveTile(tileX, tileY, 0, 0);
            }
            if (activeItem.getTemplateId() != ItemList.stoneChisel)
                activeItem.setWeight(activeItem.getWeightGrams() - activeItem.getTemplate().getWeightGrams(), true);

            if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                youMessage = String.format("You finish to removing the paving.");
                broadcastMessage = String.format("%s finishes removing the paving.", performer.getName());
            }
            else {
                youMessage = String.format("You finish making %s pavement.", getPavementName(activeItem));
                broadcastMessage = String.format("%s finishes making %s pavement.", performer.getName(), getPavementName(activeItem));
            }
            performer.getCommunicator().sendNormalServerMessage(youMessage);
            Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
            return true;
        }
        return ActionPerformer.super.action(action, performer, activeItem, tileX, tileY, onSurface, heightOffset, encodedTile, actionId, counter);
    }

    @Override
    public boolean action( Action action,  Creature performer,  Item activeItem,  boolean onSurface, BridgePart bridgePart,
                           int encodedTile,  short actionId,  float counter) {
        if (activeIsPaver(activeItem)) {
            int time;
            final float TIME_TO_COUNTER_DIVISOR = 10.0f;
            final float ACTION_START_TIME = 1.0f;
            String youMessage;
            String broadcastMessage;

            if (counter == ACTION_START_TIME) {
                if (hasAFailureCondition(performer, activeItem, bridgePart.getTileX(), bridgePart.getTileY(), encodedTile, bridgePart)) {
                    return true;
                }
                Skill pavingSkill = performer.getSkills().getSkillOrLearn(SkillList.PAVING);
                time = Actions.getStandardActionTime(performer, pavingSkill, activeItem, 0.0);
                action.setTimeLeft(time);
                if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                    youMessage = String.format("You start to remove the paving.");
                    broadcastMessage = String.format("%s starts to remove the paving.", performer.getName());
                }
                else {
                    youMessage = String.format("You start to make %s pavement.", getPavementName(activeItem));
                    broadcastMessage = String.format("%s starts to make %s pavement.", performer.getName(), getPavementName(activeItem));
                }
                performer.getCommunicator().sendNormalServerMessage(youMessage);
                Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
                performer.sendActionControl(action.getActionEntry().getVerbString(), true, time);
                performer.getStatus().modifyStamina(-1000.0f);
                return false;
            }
            if (action.currentSecond() % 5 == 0) {
                performer.getStatus().modifyStamina(-10000.0f);
            }
            if (action.mayPlaySound()) {
                Methods.sendSound(performer, "sound.work.paving");
            }
            boolean actionInProcess = counter < action.getTimeLeft() / TIME_TO_COUNTER_DIVISOR;
            if (actionInProcess)
                return false;
            if (hasAFailureCondition(performer, activeItem, bridgePart.getTileX(), bridgePart.getTileY(), encodedTile, bridgePart))
                return true;
            Skill pavingSkill = performer.getSkills().getSkillOrLearn(SkillList.PAVING);
            pavingSkill.skillCheck((activeItem.getTemplateId() == ItemList.rock) ? 5.0 : 30.0, activeItem, 0.0, false, counter);
            TileEvent.log(bridgePart.getTileX(), bridgePart.getTileY(), -1, performer.getWurmId(), actionId);
            if (activeItem.getTemplateId() == ItemList.stoneChisel)
                bridgePart.saveRoadType((byte)0);
            else
                bridgePart.saveRoadType(getPavementTileType(activeItem));
            VolaTile volaTile = Zones.getOrCreateTile(bridgePart.getTileX(), bridgePart.getTileY(), bridgePart.isOnSurface());
            if (volaTile != null) {
                volaTile.updateBridgePart(bridgePart);
            }
            if (activeItem.getTemplateId() != ItemList.stoneChisel)
                activeItem.setWeight(activeItem.getWeightGrams() - activeItem.getTemplate().getWeightGrams(), true);

            if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                youMessage = String.format("You finish to removing the paving.");
                broadcastMessage = String.format("%s finishes removing the paving.", performer.getName());
            }
            else {
                youMessage = String.format("You finish making %s pavement.", getPavementName(activeItem));
                broadcastMessage = String.format("%s finishes making %s pavement.", performer.getName(), getPavementName(activeItem));
            }
            performer.getCommunicator().sendNormalServerMessage(youMessage);
            Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
            return true;
        }
        return ActionPerformer.super.action(action, performer, activeItem, onSurface, bridgePart, encodedTile, actionId, counter);
    }

    private boolean hasAFailureCondition(Creature performer, Item activeItem,  int tileX,  int tileY, int encodedTile,
                                         BridgePart bridgePart) {
        if (!Methods.isActionAllowed(performer, Actions.ROAD_PAVE, tileX, tileY)) {
            return true;
        }
        if (Tiles.decodeHeight(encodedTile) < -100 && bridgePart != null) {
            performer.getCommunicator().sendNormalServerMessage("The water is too deep to pave here.");
            return true;
        }
        if (activeItem.getTemplateId() != ItemList.stoneChisel && activeItem.getWeightGrams() < activeItem.getTemplate().getWeightGrams()) {
                performer.getCommunicator().sendNormalServerMessage("The amount of " + activeItem.getName() + " is too little to pave. You may need to combine it with other " + activeItem.getTemplate().getPlural() + ".");
                return true;
        }

        return false;
    }

    private static byte getPavementTileType(Item activeItem ) {
        byte toReturn = 0;
        switch (activeItem.getTemplateId()) {
            case ItemList.stoneChisel:
                toReturn = Tiles.Tile.TILE_CAVE.id;
                break;
            case ItemList.joists:
                toReturn = Tiles.Tile.TILE_CAVE_FLOOR_REINFORCED.id;
                break;
            case ItemList.stoneBrick:
                toReturn = Tiles.Tile.TILE_COBBLESTONE.id;
                break;
            case ItemList.roundedBrick:
                toReturn = Tiles.Tile.TILE_COBBLESTONE_ROUND.id;
                break;
            case ItemList.colossusPart:
                toReturn = Tiles.Tile.TILE_COBBLESTONE_ROUGH.id;
                break;
            case ItemList.stoneSlab:
                toReturn = Tiles.Tile.TILE_STONE_SLABS.id;
                break;
            case ItemList.slateBrick:
                toReturn = Tiles.Tile.TILE_SLATE_BRICKS.id;
                break;
            case ItemList.slateSlab:
                toReturn = Tiles.Tile.TILE_SLATE_SLABS.id;
                break;
            case ItemList.sandstoneBrick:
                toReturn = Tiles.Tile.TILE_SANDSTONE_BRICKS.id;
                break;
            case ItemList.sandstoneSlab:
                toReturn = Tiles.Tile.TILE_SANDSTONE_SLABS.id;
                break;
            case ItemList.marbleBrick:
                toReturn = Tiles.Tile.TILE_MARBLE_BRICKS.id;
                break;
            case ItemList.marbleSlab:
                toReturn = Tiles.Tile.TILE_MARBLE_SLABS.id;
                break;
            case ItemList.brickPottery:
                toReturn = Tiles.Tile.TILE_POTTERY_BRICKS.id;
                break;
            case ItemList.floorBoards:
                toReturn = Tiles.Tile.TILE_PLANKS.id;
                break;
            case ItemList.rock:
                toReturn = Tiles.Tile.TILE_GRAVEL.id;
                break;
        }
        return toReturn;
    }

    private static String getPavementName(Item activeItem) {
        byte tileTypeId = 0;
        switch (activeItem.getTemplateId()){
            case ItemList.stoneChisel:
                tileTypeId = Tiles.Tile.TILE_CAVE.id;
                break;
            case ItemList.joists:
                tileTypeId = Tiles.Tile.TILE_CAVE_FLOOR_REINFORCED.id;
                break;
            case ItemList.stoneBrick:
                tileTypeId = Tiles.Tile.TILE_COBBLESTONE.id;
                break;
            case ItemList.roundedBrick:
                tileTypeId = Tiles.Tile.TILE_COBBLESTONE_ROUND.id;
                break;
            case ItemList.colossusPart:
                tileTypeId = Tiles.Tile.TILE_COBBLESTONE_ROUGH.id;
                break;
            case ItemList.stoneSlab:
                tileTypeId = Tiles.Tile.TILE_STONE_SLABS.id;
                break;
            case ItemList.slateBrick:
                tileTypeId = Tiles.Tile.TILE_SLATE_BRICKS.id;
                break;
            case ItemList.slateSlab:
                tileTypeId = Tiles.Tile.TILE_SLATE_SLABS.id;
                break;
            case ItemList.sandstoneBrick:
                tileTypeId = Tiles.Tile.TILE_SANDSTONE_BRICKS.id;
                break;
            case ItemList.sandstoneSlab:
                tileTypeId = Tiles.Tile.TILE_SANDSTONE_SLABS.id;
                break;
            case ItemList.marbleBrick:
                tileTypeId = Tiles.Tile.TILE_MARBLE_BRICKS.id;
                break;
            case ItemList.marbleSlab:
                tileTypeId = Tiles.Tile.TILE_MARBLE_SLABS.id;
                break;
            case ItemList.brickPottery:
                tileTypeId = Tiles.Tile.TILE_POTTERY_BRICKS.id;
                break;
            case ItemList.floorBoards:
                tileTypeId = Tiles.Tile.TILE_PLANKS.id;
                break;
            case ItemList.rock:
                tileTypeId = Tiles.Tile.TILE_GRAVEL.id;
                break;
        }
         byte tileTypeId1 = tileTypeId;
        Tiles.Tile tile1 = Arrays.stream(Tiles.Tile.getTiles())
                .filter(tile2 -> tile2.getId() == tileTypeId1)
                .findFirst()
                .orElse(null);
        return  tile1.getName();
    }

    private static boolean activeIsPaver(Item activeItem) {
        switch (activeItem.getTemplateId()){
            case ItemList.stoneChisel:
            case ItemList.joists:
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

    private static boolean tileIsPavable(int encodedTile){
        int tileType = Byte.toUnsignedInt(Tiles.decodeType(encodedTile));
        switch (tileType){
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
                return true;
            default:
                return false;
        }
    }
}