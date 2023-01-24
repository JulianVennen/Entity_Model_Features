package traben.entity_model_features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.Identifier;
import traben.entity_model_features.config.EMFConfig;
import traben.entity_model_features.models.vanilla_model_children.*;
import traben.entity_model_features.models.EMF_EntityModel;
import traben.entity_model_features.models.jemJsonObjects.EMF_JemData;
import traben.entity_model_features.utils.EMFUtils;
import traben.entity_model_features.utils.etfPropertyReader;
import traben.entity_model_features.vanilla_part_mapping.VanillaMappings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class EMFData {

    private EMFConfig EMFConfigData;
    private static EMFData self = null;

    public EMFConfig getConfig(){
        if(EMFConfigData == null){
            loadConfig();
        }
        return EMFConfigData;
    }
    public  Int2BooleanOpenHashMap alreadyCalculatedForRenderer = new Int2BooleanOpenHashMap();
    public  Int2ObjectOpenHashMap<EMF_EntityModel<?>> JEMPATH_CustomModel = new Int2ObjectOpenHashMap<>();


    private EMFData(){
        alreadyCalculatedForRenderer.defaultReturnValue(false);
        getConfig();
    }

    public static EMFData getInstance(){
        if(self == null){
           self = new EMFData();
        }
        return self;
    }

    public static void reset(){
        self = new EMFData();
    }

    public void EMF_saveConfig() {
        File config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "entity_model_features.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!config.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            config.getParentFile().mkdir();
        }
        try {
            FileWriter fileWriter = new FileWriter(config);
            fileWriter.write(gson.toJson(EMFConfigData));
            fileWriter.close();
        } catch (IOException e) {
            EMFUtils.EMF_modMessage("Config could not be saved", false);
        }
    }

    // config code based on bedrockify & actually unbreaking fabric config code
    // https://github.com/juancarloscp52/BedrockIfy/blob/1.17.x/src/main/java/me/juancarloscp52/bedrockify/Bedrockify.java
    // https://github.com/wutdahack/ActuallyUnbreakingFabric/blob/1.18.1/src/main/java/wutdahack/actuallyunbreaking/ActuallyUnbreaking.java
    public void loadConfig() {
        try {
            File config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "entity_model_features.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (config.exists()) {
                try {
                    FileReader fileReader = new FileReader(config);
                    EMFConfigData = gson.fromJson(fileReader, EMFConfig.class);
                    fileReader.close();
                    EMF_saveConfig();
                } catch (IOException e) {
                    EMFUtils.EMF_modMessage("Config could not be loaded, using defaults", false);
                }
            } else {
                EMFConfigData = new EMFConfig();
                EMF_saveConfig();
            }
            if (EMFConfigData == null) {
                EMFConfigData = new EMFConfig();
                EMF_saveConfig();
            }
        }catch (Exception e){
            EMFConfigData = new EMFConfig();
        }
    }

    public  <T extends LivingEntity> EMF_EntityModel<T> createEMFModelOnly(String modelJemName, EntityModel<T> vanillaModel){
        int hashKeyTypicallyEntityType = modelJemName.hashCode();
        if(!JEMPATH_CustomModel.containsKey(hashKeyTypicallyEntityType)) {
            String modelID = "optifine/cem/" + modelJemName + ".jem";
            System.out.println("checking " + modelID);
            try {
                EMF_JemData jem = EMFUtils.EMF_readJemData(modelID);
                VanillaMappings.VanillaMapper vanillaPartSupplier = VanillaMappings.getVanillaModelPartsMapSupplier(hashKeyTypicallyEntityType, vanillaModel);
                //vanillaPartsByType.put(typeHash,vanillaPartList);
                EMF_EntityModel<T> model = new EMF_EntityModel<>(jem, modelID, vanillaPartSupplier, vanillaModel);
                JEMPATH_CustomModel.put(hashKeyTypicallyEntityType, (EMF_EntityModel<LivingEntity>) model);
                System.out.println("put emfpart in data =" + model.toString());

            } catch (Exception e) {
                EMFUtils.EMF_modMessage("failed for " + modelID + e, false);
                e.printStackTrace();
                JEMPATH_CustomModel.put(hashKeyTypicallyEntityType, null);
            }
        }
        return (EMF_EntityModel<T>) JEMPATH_CustomModel.get(hashKeyTypicallyEntityType);
    }

    private <T extends LivingEntity, M extends EntityModel<T>> EntityModel<?> getCustomModelForRendererGeneric(EMF_EntityModel<?> alreadyBuiltSubmodel, EntityModel<?> vanillaModelForInstanceCheck){
        return getCustomModelForRenderer((EMF_EntityModel<T>)alreadyBuiltSubmodel,(M)vanillaModelForInstanceCheck);
    }

    private <T extends LivingEntity, M extends EntityModel<T>> M getCustomModelForRenderer(EMF_EntityModel<T> alreadyBuiltSubmodel,M vanillaModelForInstanceCheck){
        //figure out whether to send a vanilla child model or a direct EMF custom model
        if(vanillaModelForInstanceCheck instanceof CowEntityModel<?>){
            return (M) new EMFCustomCowModel<T>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof LlamaEntityModel){
            return (M) new EMFCustomLlamaModel<T, LlamaEntity>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof FoxEntityModel){
            return (M) new EMFCustomFoxModel<T, FoxEntity>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof EndermanEntityModel){
            return (M) new EMFCustomEndermanModel<T>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof HorseEntityModel){
            return (M) new EMFCustomHorseModel<T, AbstractHorseEntity>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof PlayerEntityModel){
            return (M) new EMFCustomPlayerModel<T>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof WitchEntityModel){
            return (M) new EMFCustomWitchModel<T>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof IllagerEntityModel){
            return (M) new EMFCustomIllagerModel<T, IllagerEntity>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof VillagerResemblingModel){
            return (M) new EMFCustomVillagerModel<T>(alreadyBuiltSubmodel);
        }
        //this for instance allows vanilla features like non custom armour and hand held items to work for bipeds
        if(vanillaModelForInstanceCheck instanceof BipedEntityModel){
            return (M) new EMFCustomBipedModel<T>(alreadyBuiltSubmodel);
        }
        if(vanillaModelForInstanceCheck instanceof QuadrupedEntityModel){
            return (M) new EMFCustomQuadrapedModel<T>(alreadyBuiltSubmodel);
        }
        //this for instance allows vanilla features like flower holding to work
        if(vanillaModelForInstanceCheck instanceof IronGolemEntityModel){
            return (M) new EMFCustomIronGolemModel<T,IronGolemEntity>(alreadyBuiltSubmodel);
        }

        return (M) alreadyBuiltSubmodel;

    }
    public<T extends LivingEntity, M extends EntityModel<T>> M getModelVariantGeneric(Entity entity, String entityTypeName, EntityModel<?> vanillaModel){
        return getModelVariant(entity,entityTypeName,(EntityModel<T>)vanillaModel);
    }
    public<T extends LivingEntity, M extends EntityModel<T>> M getModelVariant(Entity entity, String entityTypeName, EntityModel<T> vanillaModel){
        EMF_EntityModel<T> emfModel = createEMFModelOnly(entityTypeName,vanillaModel);
        if(emfModel != null) {
            // jem exists so decide if variation occurs
            if (FabricLoader.getInstance().isModLoaded("entity_texture_features")) {
                Identifier propertyID = new Identifier("optifine/cem/" + entityTypeName + ".properties");
                if (MinecraftClient.getInstance().getResourceManager().getResource(propertyID).isPresent()) {
                    List<etfPropertyReader.EMFPropertyCase> emfCases = etfPropertyReader.getAllValidPropertyObjects(propertyID, "models", entityTypeName);
                    if (emfCases != null){
                        for (etfPropertyReader.EMFPropertyCase emfCase:
                             emfCases) {
                            if (emfCase.testCase(entity,false,hmm))
                        }
                    }
                }
            }
            return (M) getCustomModelForRenderer(emfModel, vanillaModel);
        }
        return null;
    }


    public EMFCustomPlayerModel<?> clientPlayerModel = null;



}
