package com.joedobo27.spm;


import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class SimplePavingMod implements WurmServerMod, ServerStartedListener, Initable {

    @Override
    public void init() {
        ModActions.init();
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new PaveAction());
        ModActions.registerAction(new PaveCornerAction());
    }
}
