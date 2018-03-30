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
import com.wurmonline.server.zones.Zone;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.function.Function;

import static com.joedobo27.libs.CardinalDirection.*;

public class CornerPaveAction extends ActionMaster {

    private final TilePos targetTile;
    private final ArrayList<Function<ActionMaster, Boolean>> failureTestFunctions;

    private static WeakHashMap<Action, CornerPaveAction> performers = new WeakHashMap<>();

    CornerPaveAction(Action action, Creature performer, @Nullable Item activeTool, @Nullable Integer usedSkill, int minSkill, int maxSkill, int longestTime, int shortestTime, int minimumStamina, TilePos targetTile, ArrayList<Function<ActionMaster, Boolean>> failureTestFunctions) {
        super(action, performer, activeTool, usedSkill, minSkill, maxSkill, longestTime, shortestTime, minimumStamina);
        this.targetTile = targetTile;
        this.failureTestFunctions = failureTestFunctions;
        performers.put(action, this);
    }

    @Nullable static CornerPaveAction getCornerPaveAction(Action action) {
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
            byte roadDirection = getDiagonalDir();
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

    private byte getDiagonalDir() {
        TilePos tilePosPerformerNearest = TileUtilities.getPerformerNearestTile(this.performer);

        if (this.targetTile.equals(tilePosPerformerNearest)) {
            return Tiles.TileRoadDirection.DIR_NW.getCode();
        }
        if (offsetByOneCardinal(CARDINALS_E.getId(), this.targetTile).equals(tilePosPerformerNearest)) {
            return Tiles.TileRoadDirection.DIR_NE.getCode();
        }
        if (offsetByOneCardinal(CARDINALS_SE.getId(), this.targetTile).equals(tilePosPerformerNearest)) {
            return Tiles.TileRoadDirection.DIR_SE.getCode();
        }
        if ( offsetByOneCardinal(CARDINALS_S.getId(), this.targetTile).equals(tilePosPerformerNearest)) {
            return Tiles.TileRoadDirection.DIR_SW.getCode();
        }
        return 0;
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

    void consumePaver() {
        if (this.activeTool == null || this.activeTool.getTemplateId() == ItemList.stoneChisel)
            return;
        this.activeTool.setWeight(this.activeTool.getWeightGrams() - this.activeTool.getTemplate().getWeightGrams(),true);
    }

    @Override
    public Item getActiveTool() {
        return activeTool;
    }

    @Override
    public Item getTargetItem() {
        return null;
    }

    @Override
    public TilePos getTargetTile() {
        return targetTile;
    }
}
