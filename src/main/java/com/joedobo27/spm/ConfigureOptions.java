package com.joedobo27.spm;


import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.util.Properties;

class ConfigureOptions {

    private ActionOptions surfaceWholeTilePave;
    private ActionOptions caveWholeTilePave;
    private ActionOptions bridgeWholeTilePave;
    private ActionOptions surfaceCornerPave;

    private static final ConfigureOptions instance;
    private static final String DEFAULT_ACTION_OPTION = "" +
            "{\"minSkill\":10 ,\"maxSkill\":95 , \"longestTime\":100 , \"shortestTime\":10 , \"minimumStamina\":6000}";

    static {
        instance = new ConfigureOptions();
    }

    class ActionOptions {
        private final int minSkill;
        private final int maxSkill;
        private final int longestTime;
        private final int shortestTime;
        private final int minimumStamina;

        ActionOptions(int minSkill, int maxSkill, int longestTime, int shortestTime, int minimumStamina) {
            this.minSkill = minSkill;
            this.maxSkill = maxSkill;
            this.longestTime = longestTime;
            this.shortestTime = shortestTime;
            this.minimumStamina = minimumStamina;
        }

        int getMinSkill() {
            return minSkill;
        }

        int getMaxSkill() {
            return maxSkill;
        }

        int getLongestTime() {
            return longestTime;
        }

        int getShortestTime() {
            return shortestTime;
        }

        int getMinimumStamina() {
            return minimumStamina;
        }
    }

    synchronized static void setOptions(Properties properties) {
        instance.surfaceWholeTilePave = doPropertiesToActionOptions(properties.getProperty("surfaceWholeTilePave",
                DEFAULT_ACTION_OPTION));
        instance.caveWholeTilePave = doPropertiesToActionOptions(properties.getProperty("caveWholeTilePave",
                DEFAULT_ACTION_OPTION));
        instance.bridgeWholeTilePave = doPropertiesToActionOptions(properties.getProperty("bridgeWholeTilePave",
                DEFAULT_ACTION_OPTION));
        instance.surfaceCornerPave = doPropertiesToActionOptions(properties.getProperty("surfaceCornerPave",
                DEFAULT_ACTION_OPTION));
    }

    synchronized static void resetOptions() {
        Properties properties = getProperties();
        if (properties == null)
            throw new RuntimeException("properties can't be null here.");
        instance.surfaceWholeTilePave = doPropertiesToActionOptions(properties.getProperty("surfaceWholeTilePave",
                DEFAULT_ACTION_OPTION));
        instance.caveWholeTilePave = doPropertiesToActionOptions(properties.getProperty("caveWholeTilePave",
                DEFAULT_ACTION_OPTION));
        instance.bridgeWholeTilePave = doPropertiesToActionOptions(properties.getProperty("bridgeWholeTilePave",
                DEFAULT_ACTION_OPTION));
        instance.surfaceCornerPave = doPropertiesToActionOptions(properties.getProperty("surfaceCornerPave",
                DEFAULT_ACTION_OPTION));
    }

    private static ActionOptions doPropertiesToActionOptions(String values) {
        Reader reader = new StringReader(values);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonObject = jsonReader.readObject();
        int minSkill = jsonObject.getInt("minSkill", 10);
        int maxSkill = jsonObject.getInt("maxSkill", 95);
        int longestTime = jsonObject.getInt("longestTime", 100);
        int shortestTime = jsonObject.getInt("shortestTime", 10);
        int minimumStamina = jsonObject.getInt("minimumStamina", 6000);
        return instance.new ActionOptions(minSkill, maxSkill, longestTime, shortestTime, minimumStamina);
    }

    private static Properties getProperties() {
        try {
            File configureFile = new File("mods/SimplePavingMod.properties");
            FileInputStream configureStream = new FileInputStream(configureFile);
            Properties configureProperties = new Properties();
            configureProperties.load(configureStream);
            return configureProperties;
        }catch (IOException e) {
            SimplePavingMod.logger.warning(e.getMessage());
            return null;
        }
    }

    static ConfigureOptions getInstance() {
        return instance;
    }

    ActionOptions getSurfaceWholeTilePave() {
        return surfaceWholeTilePave;
    }

    ActionOptions getCaveWholeTilePave() {
        return caveWholeTilePave;
    }

    ActionOptions getBridgeWholeTilePave() {
        return bridgeWholeTilePave;
    }

    ActionOptions getSurfaceCornerPave() {
        return surfaceCornerPave;
    }
}
