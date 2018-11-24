package com.joedobo27.spm;


import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.math.TilePos;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.structures.BridgePart;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import java.util.*;
import java.util.function.Function;

import static com.joedobo27.libs.action.ActionFailureFunction.*;
import static com.joedobo27.spm.SimplePavingMod.activeIsCavePaver;
import static com.joedobo27.spm.SimplePavingMod.activeIsSurfacePaver;
import static com.joedobo27.spm.SimplePavingMod.tileCanBePaved;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.*;

public class PaveActionPerformer implements ModAction, BehaviourProvider, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;

    private PaveActionPerformer(short actionId, ActionEntry actionEntry){
        this.actionId = actionId;
        this.actionEntry = actionEntry;
    }

    private static class SingletonHelper {
        private static final PaveActionPerformer _performer;
        static {
            _performer = new PaveActionPerformer( Actions.ROAD_PAVE, Actions.actionEntrys[Actions.ROAD_PAVE]);
        }
    }

    @Override
    public short getActionId() {
        return actionId;
    }

    // surface tile
    //////////////////
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activeItem ,int tileX, int tileY, boolean onSurface,
                                              int encodedTile) {
        if (!onSurface || Tiles.decodeType(encodedTile) == Tiles.Tile.TILE_DIRT_PACKED.getId() || !tileCanBePaved(encodedTile) ||
                !activeIsSurfacePaver(activeItem))
            return BehaviourProvider.super.getBehavioursFor(performer, activeItem, tileX, tileY, onSurface, encodedTile);

        List<ActionEntry> toReturn = Arrays.asList(
                new ActionEntry((short)(-2), "Pave", "paving"),
                Actions.actionEntrys[Actions.ROAD_PAVE],
                Actions.actionEntrys[Actions.ROAD_PAVE_CORNER]);
        if (activeItem.getTemplateId() == ItemList.stoneChisel)
            toReturn = Collections.singletonList(Actions.actionEntrys[this.getActionId()]);
        return toReturn;
    }

    @Override
    public boolean action(Action action, Creature performer, Item activeItem, int tileX, int tileY, boolean onSurface,
                          int heightOffset, int encodedTile, short actionId, float counter) {
        if (!tileCanBePaved(encodedTile) || (onSurface && !activeIsSurfacePaver(activeItem)) ||
                (!onSurface && !activeIsCavePaver(activeItem)))
            return propagate(action, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        PaveAction paveAction = PaveAction.getPaveAction(action);
        if (paveAction == null){
            ArrayList<Function<ActionMaster, Boolean>> functions = new ArrayList<>();
            functions.add(getFunction(FAILURE_FUNCTION_NULL_TARGET_TILE));
            functions.add(getFunction(FAILURE_FUNCTION_NULL_ACTIVE_ITEM));
            functions.add(getFunction(FAILURE_FUNCTION_INSUFFICIENT_STAMINA));
            functions.add(getFunction(FAILURE_FUNCTION_SERVER_BOARDER_TOO_CLOSE));
            functions.add(getFunction(FAILURE_FUNCTION_TILE_GOD_PROTECTED));
            functions.add(getFunction(FAILURE_FUNCTION_TILE_OCCUPIED_BY_BRIDGE_SUPPORT));
            functions.add(getFunction(FAILURE_FUNCTION_PAVING_DEPTH));
            functions.add(getFunction(FAILURE_FUNCTION_PARTIAL_PAVER));

            ConfigureOptions.ActionOptions options = ConfigureOptions.getInstance().getSurfaceWholeTilePave();
            paveAction = new PaveAction(action, performer, activeItem, SkillList.PAVING, options.getMinSkill(),
                    options.getMaxSkill(),options.getLongestTime(), options.getShortestTime(), options.getMinimumStamina(),
                    TilePos.fromXY(tileX, tileY), functions);
        }

        if(paveAction.isActionStartTime(counter) && paveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        if (paveAction.isActionStartTime(counter)) {
            paveAction.doActionStartMessages();
            paveAction.setInitialTime(actionEntry);
            activeItem.setDamage(activeItem.getDamage() + 0.0015f * activeItem.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        if (!paveAction.isActionTimedOut(action, counter))
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (paveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        paveAction.doSkillCheckAndGetPower(counter);
        paveAction.changeTile(encodedTile);
        paveAction.consumePaver();

        performer.getStatus().modifyStamina(-5000.0f);
        paveAction.doActionEndMessages();
        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }

    // cave floor tile
    //////////////////
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activeItem, int tileX, int tileY, boolean onSurface,
                                              int encodedTile, int dir) {
        if (onSurface || Tiles.decodeType(encodedTile) == Tiles.Tile.TILE_CAVE_PREPATED_FLOOR_REINFORCED.getId() ||
                !tileCanBePaved(encodedTile) || !activeIsCavePaver(activeItem))
            return BehaviourProvider.super.getBehavioursFor(performer, activeItem, tileX, tileY, onSurface, encodedTile, dir);
        return Collections.singletonList(Actions.actionEntrys[this.getActionId()]);
    }

    @Override
    public boolean action(Action action, Creature performer,  Item activeItem, int tileX, int tileY, boolean onSurface,
                          int heightOffset, int encodedTile, int dir, short actionId, float counter) {
        if (!tileCanBePaved(encodedTile) || !activeIsCavePaver(activeItem))
            return propagate(action, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        PaveAction paveAction = PaveAction.getPaveAction(action);
        if (paveAction == null){
            ArrayList<Function<ActionMaster, Boolean>> functions = new ArrayList<>();
            functions.add(getFunction(FAILURE_FUNCTION_NULL_TARGET_TILE));
            functions.add(getFunction(FAILURE_FUNCTION_NULL_ACTIVE_ITEM));
            functions.add(getFunction(FAILURE_FUNCTION_INSUFFICIENT_STAMINA));
            functions.add(getFunction(FAILURE_FUNCTION_SERVER_BOARDER_TOO_CLOSE));
            functions.add(getFunction(FAILURE_FUNCTION_TILE_GOD_PROTECTED));
            functions.add(getFunction(FAILURE_FUNCTION_TILE_OCCUPIED_BY_BRIDGE_SUPPORT));
            functions.add(getFunction(FAILURE_FUNCTION_PAVING_DEPTH));
            functions.add(getFunction(FAILURE_FUNCTION_PARTIAL_PAVER));

            ConfigureOptions.ActionOptions options = ConfigureOptions.getInstance().getCaveWholeTilePave();
            paveAction = new PaveAction(action, performer, activeItem, SkillList.PAVING, options.getMinSkill(),
                    options.getMaxSkill(),options.getLongestTime(), options.getShortestTime(), options.getMinimumStamina(),
                    TilePos.fromXY(tileX, tileY), functions);
        }

        if(paveAction.isActionStartTime(counter) && paveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        if (paveAction.isActionStartTime(counter)) {
            paveAction.doActionStartMessages();
            paveAction.setInitialTime(actionEntry);
            activeItem.setDamage(activeItem.getDamage() + 0.0015f * activeItem.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        if (!paveAction.isActionTimedOut(action, counter))
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (paveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        paveAction.doSkillCheckAndGetPower(counter);
        paveAction.changeTile(encodedTile);
        paveAction.consumePaver();
        performer.getStatus().modifyStamina(-5000.0f);
        paveAction.doActionEndMessages();
        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }

    // bridge tile
    //////////////////
    @Override
    public List<ActionEntry> getBehavioursFor( Creature performer,  Item activeItem,  boolean onSurface,  BridgePart bridgePart) {
        if (!activeIsSurfacePaver(activeItem))
            return BehaviourProvider.super.getBehavioursFor(performer, activeItem, onSurface, bridgePart);

        List<ActionEntry> toReturn = Arrays.asList(
                new ActionEntry((short)(-2), "Pave", "paving"),
                Actions.actionEntrys[Actions.ROAD_PAVE],
                Actions.actionEntrys[Actions.ROAD_PAVE_CORNER]);
        if (activeItem.getTemplateId() == ItemList.stoneChisel)
            toReturn = Collections.singletonList(Actions.actionEntrys[this.getActionId()]);
        return toReturn;
    }

    @Override
    public boolean action(Action action, Creature performer, Item activeItem, boolean onSurface, BridgePart bridgePart, int encodedTile,
                          short actionId, float counter) {
        if (!activeIsSurfacePaver(activeItem))
            return propagate(action, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        PaveAction paveAction = PaveAction.getPaveAction(action);
        if (paveAction == null){
            ArrayList<Function<ActionMaster, Boolean>> functions = new ArrayList<>();
            functions.add(getFunction(FAILURE_FUNCTION_NULL_ACTIVE_ITEM));
            functions.add(getFunction(FAILURE_FUNCTION_INSUFFICIENT_STAMINA));
            functions.add(getFunction(FAILURE_FUNCTION_PARTIAL_PAVER));

            ConfigureOptions.ActionOptions options = ConfigureOptions.getInstance().getBridgeWholeTilePave();
            paveAction = new PaveAction(action, performer, activeItem, SkillList.PAVING, options.getMinSkill(),
                    options.getMaxSkill(),options.getLongestTime(), options.getShortestTime(), options.getMinimumStamina(),
                    null, functions);
        }
        if(paveAction.isActionStartTime(counter) && paveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        if (paveAction.isActionStartTime(counter)) {
            paveAction.doActionStartMessages();
            paveAction.setInitialTime(actionEntry);
            activeItem.setDamage(activeItem.getDamage() + 0.0015f * activeItem.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        if (!paveAction.isActionTimedOut(action, counter))
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (paveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        paveAction.updateBridgePaving(bridgePart);
        paveAction.consumePaver();
        performer.getStatus().modifyStamina(-5000.0f);
        paveAction.doActionEndMessages();
        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

    }

    static PaveActionPerformer getPaveActionPerformer() {
        return SingletonHelper._performer;
    }
}
