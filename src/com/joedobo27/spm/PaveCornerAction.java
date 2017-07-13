package com.joedobo27.spm;


import com.wurmonline.math.TilePos;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Constants;
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
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import static com.joedobo27.spm.PaveAction.activeIsCavePaver;
import static com.joedobo27.spm.PaveAction.activeIsSurfacePaver;
import static com.joedobo27.spm.PaveAction.tileIsPavable;

public class PaveCornerAction implements ModAction, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;

    @Override
    public short getActionId() {
        return this.actionId;
    }

    PaveCornerAction() {
        this.actionId = Actions.ROAD_PAVE_CORNER;
        this.actionEntry = Actions.actionEntrys[Actions.ROAD_PAVE_CORNER];
    }

    @Override
    public boolean action(Action action, Creature performer, Item activeItem, int tileX, int tileY, boolean onSurface,
                          int heightOffset, int encodedTile, short _actionId, float counter) {
        if (_actionId == this.actionId && tileIsPavable(encodedTile) && ((onSurface && activeIsSurfacePaver(activeItem)) ||
                (!onSurface && activeIsCavePaver(activeItem)))) {
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
                    youMessage = "You start to remove the paving.";
                    broadcastMessage = String.format("%s starts to remove the paving.", performer.getName());
                }
                else {
                    youMessage = String.format("You start to make %s pavement.", PaveAction.getPavementName(activeItem));
                    broadcastMessage = String.format("%s starts to make %s pavement.", performer.getName(), PaveAction.getPavementName(activeItem));
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
            TileEvent.log(tileX, tileY, -1, performer.getWurmId(), _actionId);
            byte roadDirection = Tiles.TileRoadDirection.DIR_STRAIGHT.getCode();
            if (_actionId == Actions.ROAD_PAVE_CORNER) {
                roadDirection = TileUtilities.getDiagonalRoadDir(performer, tileX*4, tileY*4);
            }
            byte tileType = PaveAction.getPavementTileType(activeItem);
            if (onSurface) {
                if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                    tileType = Tiles.Tile.TILE_DIRT_PACKED.id;
                    roadDirection = Tiles.TileRoadDirection.DIR_STRAIGHT.getCode();
                }
                Server.surfaceMesh.setTile(tileX, tileY, Tiles.encode(Tiles.decodeHeight(encodedTile), tileType, roadDirection));
                TileUtilities.voidWorldResourceEntry(TilePos.fromXY(tileX, tileY));
                Server.modifyFlagsByTileType(tileX, tileY, tileType);
                Players.getInstance().sendChangedTile(tileX, tileY, onSurface, false);
            }
            else {
                Server.caveMesh.setTile(tileX, tileY, Tiles.encode(Tiles.decodeHeight(encodedTile), PaveAction.getPavementTileType(activeItem), roadDirection));
                TileUtilities.voidWorldResourceEntry(TilePos.fromXY(tileX, tileY));
                Server.modifyFlagsByTileType(tileX, tileY, tileType);
                TileRockBehaviour.sendCaveTile(tileX, tileY, 0, 0);
            }
            try {
                Zone z = Zones.getZone(tileX, tileY, onSurface);
                z.changeTile(tileX, tileY);
            }catch (NoSuchZoneException ignored){}

            if (activeItem.getTemplateId() != ItemList.stoneChisel)
                activeItem.setWeight(activeItem.getWeightGrams() - activeItem.getTemplate().getWeightGrams(), true);

            if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                youMessage = "You finish to removing the paving.";
                broadcastMessage = String.format("%s finishes removing the paving.", performer.getName());
            }
            else {
                youMessage = String.format("You finish making %s pavement.", PaveAction.getPavementName(activeItem));
                broadcastMessage = String.format("%s finishes making %s pavement.", performer.getName(), PaveAction.getPavementName(activeItem));
            }
            performer.getCommunicator().sendNormalServerMessage(youMessage);
            Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
            return true;
        }
        return ActionPerformer.super.action(action, performer, activeItem, tileX, tileY, onSurface, heightOffset, encodedTile, _actionId, counter);
    }

    @Override
    public boolean action( Action action,  Creature performer,  Item activeItem,  boolean onSurface, BridgePart bridgePart,
                           int encodedTile,  short _actionId,  float counter) {
        if (_actionId == this.actionId && activeIsSurfacePaver(activeItem)) {
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
                    youMessage = "You start to remove the paving.";
                    broadcastMessage = String.format("%s starts to remove the paving.", performer.getName());
                }
                else {
                    youMessage = String.format("You start to make %s pavement.", PaveAction.getPavementName(activeItem));
                    broadcastMessage = String.format("%s starts to make %s pavement.", performer.getName(), PaveAction.getPavementName(activeItem));
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
            TileEvent.log(bridgePart.getTileX(), bridgePart.getTileY(), -1, performer.getWurmId(), _actionId);
            if (activeItem.getTemplateId() == ItemList.stoneChisel)
                bridgePart.saveRoadType((byte)0);
            else
                bridgePart.saveRoadType(PaveAction.getPavementTileType(activeItem));
            VolaTile volaTile = Zones.getOrCreateTile(bridgePart.getTileX(), bridgePart.getTileY(), bridgePart.isOnSurface());
            if (volaTile != null) {
                volaTile.updateBridgePart(bridgePart);
            }
            if (activeItem.getTemplateId() != ItemList.stoneChisel)
                activeItem.setWeight(activeItem.getWeightGrams() - activeItem.getTemplate().getWeightGrams(), true);

            if (activeItem.getTemplateId() == ItemList.stoneChisel) {
                youMessage = "You finish removing the paving.";
                broadcastMessage = String.format("%s finishes removing the paving.", performer.getName());
            }
            else {
                youMessage = String.format("You finish making %s pavement.", PaveAction.getPavementName(activeItem));
                broadcastMessage = String.format("%s finishes making %s pavement.", performer.getName(), PaveAction.getPavementName(activeItem));
            }
            performer.getCommunicator().sendNormalServerMessage(youMessage);
            Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
            return true;
        }
        return ActionPerformer.super.action(action, performer, activeItem, onSurface, bridgePart, encodedTile, actionId, counter);
    }

    private boolean hasAFailureCondition(Creature performer, Item activeItem,  int tileX,  int tileY, int encodedTile,
                                         BridgePart bridgePart) {
        boolean serverBoarderTooClose = tileX < 0 || tileX > 1 << Constants.meshSize || tileY < 0 || tileY > 1 << Constants.meshSize;
        if (serverBoarderTooClose) {
            performer.getCommunicator().sendNormalServerMessage("You are too close to the server's boarder to do that.");
            return true;
        }
        if (!Methods.isActionAllowed(performer, Actions.ROAD_PAVE, tileX, tileY)) {
            return true;
        }
        TilePos performerTilePos = TileUtilities.getPerformerNearestTile(performer);
        boolean distanceExcessive = Math.abs(performerTilePos.x - tileX) > 2 || Math.abs(performerTilePos.y - tileY) > 2;
        if (distanceExcessive) {
            performer.getCommunicator().sendNormalServerMessage("You are far away to pave that tile.");
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
}
