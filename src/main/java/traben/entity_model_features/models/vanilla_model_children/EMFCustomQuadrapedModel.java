package traben.entity_model_features.models.vanilla_model_children;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import traben.entity_model_features.mixin.accessor.BipedEntityModelAccessor;
import traben.entity_model_features.mixin.accessor.QuadrupedEntityModelAccessor;
import traben.entity_model_features.models.EMFArmorableModel;
import traben.entity_model_features.models.EMFCustomModel;
import traben.entity_model_features.models.EMF_EntityModel;
import traben.entity_model_features.models.EMF_ModelPart;

import java.util.ArrayList;
import java.util.List;

public class EMFCustomQuadrapedModel<T extends LivingEntity> extends QuadrupedEntityModel<T> implements EMFCustomModel<T> {

    public EMF_EntityModel<T> getThisEMFModel() {
        return thisEMFModel;
    }

    public boolean doesThisModelNeedToBeReset() {
        return false;
    }

    private final  EMF_EntityModel<T> thisEMFModel;


    public EMFCustomQuadrapedModel(EMF_EntityModel<T> model) {
        super(QuadrupedEntityModel.getModelData(1,Dilation.NONE).getRoot().createPart(0,0),
                false,0,0,0,0,0);
        thisEMFModel=model;

        List<EMF_ModelPart> headCandidates = new ArrayList<>();
        List<EMF_ModelPart> bodyCandidates = new ArrayList<>();
        List<EMF_ModelPart> rFLegCandidates = new ArrayList<>();
        List<EMF_ModelPart> lFLegCandidates = new ArrayList<>();
        List<EMF_ModelPart> lBLegCandidates = new ArrayList<>();
        List<EMF_ModelPart> rBLegCandidates = new ArrayList<>();

        for (EMF_ModelPart part:
                thisEMFModel.childrenMap.values()) {
            switch (part.selfModelData.part){
                case "head"->{
                    headCandidates.add(part);
                }
                case "body"->{
                    bodyCandidates.add(part);
                }
                case "leg1"->{
                    rBLegCandidates.add(part);
                }
                case "leg2"->{
                    lBLegCandidates.add(part);
                }
                case "leg3"->{
                    rFLegCandidates.add(part);
                }
                case "leg4"->{
                    lFLegCandidates.add(part);
                }
                default->{

                }
            }
        }

        setPart(headCandidates,((QuadrupedEntityModelAccessor)this)::setHead);
        setPart(bodyCandidates,((QuadrupedEntityModelAccessor)this)::setBody);
        setPart(lFLegCandidates,((QuadrupedEntityModelAccessor)this)::setLeftFrontLeg);
        setPart(lBLegCandidates,((QuadrupedEntityModelAccessor)this)::setLeftHindLeg);
        setPart(rFLegCandidates,((QuadrupedEntityModelAccessor)this)::setRightFrontLeg);
        setPart(rBLegCandidates,((QuadrupedEntityModelAccessor)this)::setRightHindLeg);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {

            thisEMFModel.render(matrices, vertices, light, overlay, red, green, blue, alpha);

    }

    @Override
    public void setAngles(T livingEntity, float f, float g, float h, float i, float j) {

            thisEMFModel.child = child;
            //thisEMFModel.sneaking = sneaking;
            thisEMFModel.riding = riding;
            thisEMFModel.handSwingProgress = handSwingProgress;
            thisEMFModel.setAngles(livingEntity, f, g, h, i, j);

    }

    @Override
    public void animateModel(T livingEntity, float f, float g, float h) {
        //super.animateModel(livingEntity, f, g, h);

            thisEMFModel.animateModel(livingEntity, f, g, h);

    }


}
