package com.joedobo27.spm;

import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.math.TilePos;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import java.util.ArrayList;
import java.util.function.Function;

import static com.joedobo27.libs.action.ActionFailureFunction.*;
import static com.joedobo27.spm.SimplePavingMod.activeIsSurfacePaver;
import static com.joedobo27.spm.SimplePavingMod.tileCanBePaved;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.*;

public class CornerPaveActionPerformer implements ModAction, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;

    private CornerPaveActionPerformer(short actionId, ActionEntry actionEntry){
        this.actionId = actionId;
        this.actionEntry = actionEntry;
    }

    private static class SingletonHelper {
        private static final CornerPaveActionPerformer _performer;
        static {
            _performer = new CornerPaveActionPerformer( Actions.ROAD_PAVE_CORNER, Actions.actionEntrys[Actions.ROAD_PAVE_CORNER]);
        }
    }

    @Override
    public short getActionId() {
        return actionId;
    }

    // surface tile
    //////////////////

    @Override
    public boolean action(Action action, Creature performer, Item activeItem, int tileX, int tileY, boolean onSurface,
                          int heightOffset, int encodedTile, short actionId, float counter) {
        if (!tileCanBePaved(encodedTile) || (onSurface && !activeIsSurfacePaver(activeItem)))
            return propagate(action, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        CornerPaveAction cornerPaveAction = CornerPaveAction.getCornerPaveAction(action);
        if (cornerPaveAction == null){
            ArrayList<Function<ActionMaster, Boolean>> functions = new ArrayList<>();
            functions.add(getFunction(FAILURE_FUNCTION_NULL_TARGET_TILE));
            functions.add(getFunction(FAILURE_FUNCTION_NULL_ACTIVE_ITEM));
            functions.add(getFunction(FAILURE_FUNCTION_INSUFFICIENT_STAMINA));
            functions.add(getFunction(FAILURE_FUNCTION_SERVER_BOARDER_TOO_CLOSE));
            functions.add(getFunction(FAILURE_FUNCTION_TILE_GOD_PROTECTED));
            functions.add(getFunction(FAILURE_FUNCTION_TILE_OCCUPIED_BY_BRIDGE_SUPPORT));
            functions.add(getFunction(FAILURE_FUNCTION_PAVING_DEPTH));
            functions.add(getFunction(FAILURE_FUNCTION_PARTIAL_PAVER));

            ConfigureOptions.ActionOptions options = ConfigureOptions.getInstance().getSurfaceCornerPave();
            cornerPaveAction = new CornerPaveAction(action, performer, activeItem, SkillList.PAVING, options.getMinSkill(),
                    options.getMaxSkill(),options.getLongestTime(), options.getShortestTime(), options.getMinimumStamina(),
                    TilePos.fromXY(tileX, tileY), functions);
        }

        if(cornerPaveAction.isActionStartTime(counter) && cornerPaveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);

        if (cornerPaveAction.isActionStartTime(counter)) {
            cornerPaveAction.doActionStartMessages();
            cornerPaveAction.setInitialTime(actionEntry);
            activeItem.setDamage(activeItem.getDamage() + 0.0015f * activeItem.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        if (!cornerPaveAction.isActionTimedOut(action, counter))
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (cornerPaveAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        cornerPaveAction.doSkillCheckAndGetPower(counter);
        cornerPaveAction.changeTile(encodedTile);
        cornerPaveAction.consumePaver();

        performer.getStatus().modifyStamina(-5000.0f);
        cornerPaveAction.doActionEndMessages();
        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }

    // Can't pave corners for caves as the caveMesh data field is used for ceiling offset.

    // bridge tile don't have corner paving.

    static CornerPaveActionPerformer getCornerPaveActionPerformer()
    {
        return CornerPaveActionPerformer.SingletonHelper._performer;
    }
}
