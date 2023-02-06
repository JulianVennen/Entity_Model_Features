package traben.entity_model_features.models.anim;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.dimension.DimensionTypes;

import java.util.Iterator;
import java.util.Optional;

public class AnimationGetters {




        public boolean riding = false;

        public boolean child = false;

        public Entity entity = null;
        public float limbAngle = 0;
        public float limbDistance = 0;
        public float animationProgress = 0;
        public float headYaw = 0;
        public float headPitch = 0;
        public float tickDelta = 0;

        public Entity getEntity () {
            //System.out.println("ran");
            return entity;
        }

        public float getDimension () {
            if (entity == null || entity.getWorld() == null) return 0;
            Identifier id = entity.getWorld().getDimensionKey().getValue();
            if (id.equals(DimensionTypes.THE_NETHER_ID)) return -1;
            if (id.equals(DimensionTypes.THE_END_ID)) return 1;
            return 0;
        }

        public float getPlayerX () {
            return MinecraftClient.getInstance().player == null ? 0 : (float) MathHelper.lerp(tickDelta, MinecraftClient.getInstance().player.prevX, MinecraftClient.getInstance().player.getX());
        }

        public float getPlayerY () {
            return MinecraftClient.getInstance().player == null ? 0 : (float) MathHelper.lerp(tickDelta, MinecraftClient.getInstance().player.prevY, MinecraftClient.getInstance().player.getY());
        }

        public float getPlayerZ () {
            return MinecraftClient.getInstance().player == null ? 0 : (float) MathHelper.lerp(tickDelta, MinecraftClient.getInstance().player.prevZ, MinecraftClient.getInstance().player.getZ());
        }

        public float getPlayerRX () {
            return (MinecraftClient.getInstance().player == null) ? 0 :
                    (float) Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, MinecraftClient.getInstance().player.prevPitch, MinecraftClient.getInstance().player.getPitch()));
        }

        public float getPlayerRY () {
            return (MinecraftClient.getInstance().player == null) ? 0 :
                    (float) Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, MinecraftClient.getInstance().player.prevYaw, MinecraftClient.getInstance().player.getYaw()));
        }

        public float getEntityX () {
            return getEntity() == null ? 0 : (float) MathHelper.lerp(getTickDelta(), getEntity().prevX, getEntity().getX());
        }

        public float getEntityY () {
            return getEntity() == null ? 0 :
                    //(float) getEntity().getY();
                    (float) MathHelper.lerp(getTickDelta(), getEntity().prevY, getEntity().getY());
        }

        public float getEntityZ () {
            return getEntity() == null ? 0 : (float) MathHelper.lerp(getTickDelta(), getEntity().prevZ, getEntity().getZ());
        }

        public float getEntityRX () {
            return (getEntity() == null) ? 0 :
                    //(float) Math.toRadians(getEntity().getPitch(tickDelta));
                    (float) Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, getEntity().prevPitch, getEntity().getPitch()));
        }

        public float getEntityRY () {
            return (getEntity() instanceof LivingEntity alive) ?
                    (float) Math.toRadians(MathHelper.lerpAngleDegrees(tickDelta, alive.prevBodyYaw, alive.getBodyYaw())) : 0;
        }

        //long changed to float... should be fine tbh
        public float getTime () {
            return entity == null || entity.getWorld() == null ? 0 : entity.getWorld().getTime() + tickDelta;
        }

        public float getHealth () {
            return entity instanceof LivingEntity alive ? alive.getHealth() : 1;
        }

        public float getDeathTime () {
            return entity instanceof LivingEntity alive ? alive.deathTime : 0;
        }

        public float getAngerTime () {
            return !(entity instanceof Angerable) ? 0 : ((Angerable) entity).getAngerTime();
        }

        public float getMaxHealth () {
            return entity instanceof LivingEntity alive ? alive.getMaxHealth() : 1;
        }

        public float getId () {
            return entity == null ? 0 : entity.getUuid().hashCode();
        }

        public float getHurtTime () {
            return entity instanceof LivingEntity alive ? alive.hurtTime : 0;
        }

        public boolean isInWater () {
            return entity != null && entity.isTouchingWater();
        }

        public boolean isBurning () {
            return entity != null && entity.isOnFire();
        }

        public boolean isRiding () {
            return riding;
            // return entity != null && entity.hasVehicle();
        }

        public boolean isChild () {
            return child;
            //return entity instanceof LivingEntity alive && alive.isBaby();
        }

        public boolean isOnGround () {
            return entity != null && entity.isOnGround();
        }

        public float getClosestCollisionX () {
            if (entity != null && entity.world != null) {
                Iterator<VoxelShape> bob = entity.world.getEntityCollisions(entity, entity.getBoundingBox()).iterator();
                Vec3d entitypos = entity.getPos();
                float closest = Float.MAX_VALUE;
                while (bob.hasNext()) {
                    Optional<Vec3d> current = bob.next().getClosestPointTo(entitypos);
                    if (current.isPresent()) {
                        float newVec = (float) current.get().x;
                        closest = (float) Math.min(closest, Math.max(entitypos.x, newVec) - Math.min(entitypos.x, newVec));
                    }
                }
                if (closest != Float.MAX_VALUE) return closest;
            }
            return 0;
        }
        public float getClosestCollisionY () {
            if (entity != null && entity.world != null) {
                Iterator<VoxelShape> bob = entity.world.getEntityCollisions(entity, entity.getBoundingBox()).iterator();
                Vec3d entitypos = entity.getPos();
                float closest = Float.MAX_VALUE;
                while (bob.hasNext()) {
                    Optional<Vec3d> current = bob.next().getClosestPointTo(entitypos);
                    if (current.isPresent()) {
                        float newVec = (float) current.get().y;
                        closest = (float) Math.min(closest, Math.max(entitypos.y, newVec) - Math.min(entitypos.y, newVec));
                    }
                }
                if (closest != Float.MAX_VALUE) return closest;
            }
            return 0;
        }
        public float getClosestCollisionZ () {
            if (entity != null && entity.world != null) {
                Iterator<VoxelShape> bob = entity.world.getEntityCollisions(entity, entity.getBoundingBox()).iterator();
                Vec3d entitypos = entity.getPos();
                float closest = Float.MAX_VALUE;
                while (bob.hasNext()) {
                    Optional<Vec3d> current = bob.next().getClosestPointTo(entitypos);
                    if (current.isPresent()) {
                        float newVec = (float) current.get().z;
                        closest = (float) Math.min(closest, Math.max(entitypos.z, newVec) - Math.min(entitypos.z, newVec));
                    }
                }
                if (closest != Float.MAX_VALUE) return closest;
            }
            return 0;
        }

        public boolean isClimbing () {

            return entity instanceof LivingEntity alive && alive.isClimbing();
        }

        public boolean isAlive () {
            return entity != null && entity.isAlive();
        }

        public boolean isAggressive () {
            return entity instanceof MobEntity && ((MobEntity) entity).isAttacking();
        }

        public boolean isGlowing () {
            return entity != null && entity.isGlowing();
        }

        public boolean isHurt () {
            return entity instanceof LivingEntity alive && alive.hurtTime > 0;
        }

        public boolean isInHand () {
            return false;
        }

        public boolean isInItemFrame () {
            return false;
        }

        public boolean isInGround () {
            return entity instanceof ArrowEntity arrow && arrow.isOnGround();
        }

        public boolean isInGui () {
            return false;
        }

        public boolean isInLava () {
            return entity != null && entity.isInLava();
        }

        public boolean isInvisible () {
            return entity != null && entity.isInvisible();
        }

        public boolean isOnHead () {
            return false;
        }

        public boolean isOnShoulder () {
            return false;
        }

        public boolean isRidden () {
            return entity != null && entity.hasPassengers();
        }

        public boolean isSitting () {
            return entity != null && (
                    entity instanceof TameableEntity tame && tame.isSitting() ||
                            entity instanceof FoxEntity fox && fox.isSitting()

            );
        }

        public boolean isSneaking () {
            return entity != null && entity.isSneaking();
        }

        public boolean isSprinting () {
            return entity != null && entity.isSprinting();
        }

        public boolean isTamed () {
            return entity instanceof TameableEntity tame && tame.isTamed();
        }

        public boolean isWet () {
            return entity != null && entity.isWet();
        }

        public float getSwingProgress () {
            return entity instanceof LivingEntity alive ? alive.getHandSwingProgress(tickDelta) : 0;
        }

        public float getAge () {
            //return entity == null ? 0 : entity.age + tickDelta;
            return animationProgress;
        }

        public float getLimbAngle () {
            return limbAngle;
        }

        public float getLimbDistance () {
            return limbDistance;
        }

        public float getHeadYaw () {
            return headYaw;
        }

        public float getHeadPitch () {
            return headPitch;

        }

        public float getTickDelta () {
            return tickDelta;
        }
    
}