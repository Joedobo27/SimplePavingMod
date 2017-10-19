package com.joedobo27.spm;

import com.joedobo27.libs.TileUtilities;
import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.math.TilePos;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.function.Function;

public class PaveAction extends ActionMaster {

    private final TilePos targetTile;
    private final ArrayList<Function<ActionMaster, Boolean>> failureTestFunctions;

    private static WeakHashMap<Action, PaveAction> performers = new WeakHashMap<>();

    PaveAction(Action action, Creature performer, @Nullable Item activeTool, @Nullable Integer usedSkill,
                         int minSkill, int maxSkill, int longestTime, int shortestTime, int minimumStamina, TilePos targetTile,
                         ArrayList<Function<ActionMaster, Boolean>> failureTestFunctions) {
        super(action, performer, activeTool, usedSkill, minSkill, maxSkill, longestTime, shortestTime, minimumStamina);
        this.targetTile = targetTile;
        this.failureTestFunctions = failureTestFunctions;
        performers.put(action, this);
    }

    @Nullable static PaveAction getPaveAction(Action action) {
        if (!performers.containsKey(action))
            return null;
        return performers.get(action);
    }

    boolean hasAFailureCondition() {
        return failureTestFunctions.stream()
                .anyMatch(function -> function.apply(this));
    }

    @SuppressWarnings("UnusedReturnValue")
    double doSkillCheckAndGetPower(float counter) {
        if (this.usedSkill == null)
            return -1.0d;
        double difficulty;
        if (this.activeTool != null && this.activeTool.getTemplateId() == ItemList.rock)
            difficulty = 5.0d;
        else
            difficulty = 30.0d;
        return Math.max(1.0d,
                this.performer.getSkills().getSkillOrLearn(this.usedSkill).skillCheck(difficulty, this.activeTool,
                        0, false, counter));
    }

    void changeTile(int encodedTile) {
        try {
            byte roadDirection = Tiles.TileRoadDirection.DIR_STRAIGHT.getCode();
            byte newTileType = getPavementTileType();
            if (this.performer.isOnSurface()) {
                Server.setSurfaceTile(this.targetTile.x, this.targetTile.y, TileUtilities.getSurfaceHeight(this.targetTile),
                        newTileType, roadDirection);
                Server.modifyFlagsByTileType(this.targetTile.x, this.targetTile.y, newTileType);
                Players.getInstance().sendChangedTile(this.targetTile.x, this.targetTile.y, this.performer.isOnSurface(), true);
                Zone zone = TileUtilities.getZoneSafe(this.targetTile, this.performer.isOnSurface());
                if (zone != null)
                    zone.changeTile(this.targetTile.x, this.targetTile.y);
            }
            else {
                if (Tiles.decodeType(encodedTile) == Tiles.Tile.TILE_CAVE_EXIT.id) {
                    Server.setClientCaveFlags(this.targetTile.x, this.targetTile.y, newTileType);
                }
                else {
                    int encodedTileNew = Tiles.encode(Tiles.decodeHeight(encodedTile), newTileType, Tiles.decodeData(encodedTile));
                    Server.caveMesh.setTile(this.targetTile.x, this.targetTile.y, encodedTileNew);
                }
                Players.getInstance().sendChangedTile(this.targetTile.x, this.targetTile.y, false, true);
            }
        } catch (RuntimeException e) {
            SimplePavingMod.logger.warning(e.getMessage());
        }
    }

    void consumePaver() {
        if (this.activeTool == null || this.activeTool.getTemplateId() == ItemList.stoneChisel)
            return;
        this.activeTool.setWeight(this.activeTool.getWeightGrams() - this.activeTool.getTemplate().getWeightGrams(),true);
    }

    void updateBridgePaving(BridgePart bridgePart) {
        try {
            byte tileType = getPavementTileType();
            bridgePart.saveRoadType(tileType);

            VolaTile vt = Zones.getOrCreateTile(bridgePart.getTileX(), bridgePart.getTileY(), bridgePart.isOnSurface());
            if (vt != null) {
                vt.updateBridgePart(bridgePart);
            }
        }catch (RuntimeException e) {
            SimplePavingMod.logger.warning(e.getMessage());
        }
    }

    private byte getPavementTileType() throws RuntimeException{
        byte toReturn = 0;
        if (this.activeTool == null){
            throw new RuntimeException("activeTool shouldn't be null here");
        }
        switch (this.activeTool.getTemplateId()) {
            case ItemList.stoneChisel:
                if (this.performer.isOnSurface())
                    toReturn = Tiles.Tile.TILE_DIRT_PACKED.getId();
                if (!this.performer.isOnSurface())
                    toReturn = Tiles.Tile.TILE_CAVE.id;
                break;
            case ItemList.joists:
                toReturn = Tiles.Tile.TILE_CAVE_FLOOR_REINFORCED.id;
                break;
            case ItemList.mortar:
                toReturn = Tiles.Tile.TILE_CAVE_PREPATED_FLOOR_REINFORCED.id;
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

    private String getPavementName() throws RuntimeException{
        byte tileTypeId = 0;
        if (this.activeTool == null){
            throw new RuntimeException("activeTool shouldn't be null here");
        }
        switch (this.activeTool.getTemplateId()){
            case ItemList.stoneChisel:
                if (this.performer.isOnSurface())
                    tileTypeId = Tiles.Tile.TILE_DIRT_PACKED.getId();
                if (!this.performer.isOnSurface())
                    tileTypeId = Tiles.Tile.TILE_CAVE.id;
                break;
            case ItemList.joists:
                tileTypeId = Tiles.Tile.TILE_CAVE_FLOOR_REINFORCED.id;
                break;
            case ItemList.mortar:
                tileTypeId = Tiles.Tile.TILE_CAVE_PREPATED_FLOOR_REINFORCED.id;
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
                .filter(tile2 -> tile2 != null && tile2.getId() == tileTypeId1)
                .findFirst()
                .orElse(null);
        return  tile1 == null ? "none" : tile1.getName();
    }

    @Override
    public Item getActiveTool() {
        return activeTool;
    }

    @Override
    public TilePos getTargetTile() {
        return targetTile;
    }

    @Override
    public void doActionStartMessages() {
        try {
            String youMessage = String.format("You start %s a %s.", this.action.getActionEntry().getVerbString(), getPavementName());
            this.performer.getCommunicator().sendNormalServerMessage(youMessage);
            String broadcastMessage = String.format("%s starts to %s a %s.", this.performer.getName(),
                    this.action.getActionEntry().getActionString().toLowerCase(), getPavementName());
            Server.getInstance().broadCastAction(broadcastMessage, this.performer, 5);
        } catch (RuntimeException e) {
            SimplePavingMod.logger.warning(e.getMessage());
        }
    }

    @Override
    public void doActionEndMessages() {
        try {
            String youMessage = String.format("You finish %s a %s.", this.action.getActionEntry().getVerbString(), getPavementName());
            this.performer.getCommunicator().sendNormalServerMessage(youMessage);
            String broadcastMessage = String.format("%s finishes %s a %s.", this.performer.getName(),
                    this.action.getActionEntry().getVerbString(), getPavementName());
            Server.getInstance().broadCastAction(broadcastMessage, this.performer, 5);
        } catch (RuntimeException e) {
            SimplePavingMod.logger.warning(e.getMessage());
        }
    }
}
