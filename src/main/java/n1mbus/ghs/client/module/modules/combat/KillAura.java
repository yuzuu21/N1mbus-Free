package n1mbus.ghs.client.module.modules.combat;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.setting.BooleanSetting;
import n1mbus.ghs.client.module.setting.ModeSetting;
import n1mbus.ghs.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class KillAura extends Module {
    // --- Targeting ---
    private final NumberSetting range       = new NumberSetting("Range", 4.0, 1.0, 6.0);
    private final NumberSetting fov         = new NumberSetting("FOV", 180.0, 30.0, 360.0);
    private final ModeSetting   priority    = new ModeSetting("Priority", "Distance", Arrays.asList("Distance", "Health", "Angle"));
    private final BooleanSetting players    = new BooleanSetting("Players", true);
    private final BooleanSetting monsters   = new BooleanSetting("Monsters", true);
    private final BooleanSetting animals    = new BooleanSetting("Animals", true);
    private final BooleanSetting antiBot    = new BooleanSetting("AntiBot", false);
    private final BooleanSetting walls      = new BooleanSetting("ThroughWalls", true);

    // --- Timing ---
    private final ModeSetting   timing      = new ModeSetting("Timing", "1.8", Arrays.asList("Vanilla", "1.8"));
    private final NumberSetting cps         = new NumberSetting("CPS", 12.0, 1.0, 20.0);
    private final BooleanSetting randomize  = new BooleanSetting("RandomizeCPS", true);

    // --- Rotation ---
    private final ModeSetting   rotation    = new ModeSetting("Rotation", "Silent", Arrays.asList("None", "Silent", "Lock"));
    private final NumberSetting turnSpeed   = new NumberSetting("TurnSpeed", 180.0, 30.0, 360.0);
    private final NumberSetting rotPps      = new NumberSetting("SilentRotPPS", 8.0, 2.0, 20.0);

    // --- Combat Enhancements ---
    private final BooleanSetting keepSprint = new BooleanSetting("KeepSprint", true);
    private final BooleanSetting criticals  = new BooleanSetting("Criticals", false); // Default false, packet crits flag easily

    // --- State ---
    private long   lastAttackMs  = 0;
    private long   nextAttackNs  = 0L;
    private long   lastForcedRotPacketNs = 0L;
    private long   pendingAttackSinceNs = 0L;
    private long   pendingSilentWaitNs = 0L;
    private long   targetLockSinceNs = 0L;
    private long   kbPauseUntilNs = 0L;
    private long   forcedRotWindowStartNs = 0L;
    private int    forcedRotPacketsInWindow = 0;
    private Entity currentTarget = null;
    
    // For Rotation spoofing (used by LocalPlayerMixin)
    private boolean needsAttack = false;
    private float[] spoofedRots = new float[2];
    private boolean isSpoofing = false;
    
    private float lastYaw, lastPitch;

    public KillAura() {
        super("KillAura", "Hypixel-grade combat. Real silent rotations.", Category.COMBAT);
        addSetting(range);
        addSetting(fov);
        addSetting(priority);
        addSetting(players);
        addSetting(monsters);
        addSetting(animals);
        addSetting(antiBot);
        addSetting(walls);
        addSetting(timing);
        addSetting(cps);
        addSetting(randomize);
        addSetting(rotation);
        addSetting(turnSpeed);
        addSetting(rotPps);
        addSetting(keepSprint);
        addSetting(criticals);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isDeadOrDying()) {
            currentTarget = null;
            isSpoofing = false;
            return;
        }
        long nowNs = System.nanoTime();
        if (!isSpoofing) {
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        }
        if (nowNs < kbPauseUntilNs) {
            needsAttack = false;
            isSpoofing = false;
            pendingAttackSinceNs = 0L;
            pendingSilentWaitNs = 0L;
            return;
        }

        // Target selection
        if (currentTarget == null || !isValidTarget(mc, currentTarget) || (mc.player.tickCount % 5 == 0)) {
            Entity prev = currentTarget;
            List<Entity> targets = java.util.stream.StreamSupport
                .stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof LivingEntity le && le.isAlive() && e != mc.player)
                .filter(e -> mc.player.distanceToSqr(e) <= range.getValue() * range.getValue())
                .filter(this::entityFilter)
                .filter(e -> passesFov(mc, e))
                .filter(e -> walls.getValue() || mc.player.hasLineOfSight(e))
                .sorted(targetComparator(mc))
                .collect(Collectors.toList());
            currentTarget = targets.isEmpty() ? null : targets.get(0);
            if (currentTarget != prev) {
                targetLockSinceNs = System.nanoTime();
            }
        }

        if (currentTarget == null) {
            isSpoofing = false;
            return;
        }

        // Timing check
        if (!canAttack(mc)) {
            // Keep looking at target if we want to constantly track them silently
            if (!rotation.getValue().equals("None")) {
                updateRotations(mc, currentTarget, false);
                if (rotation.getValue().equals("Lock")) {
                    mc.player.setYRot(spoofedRots[0]);
                    mc.player.setXRot(spoofedRots[1]);
                    isSpoofing = false;
                } else {
                    // Silent now sends explicit rotation packets only on attack tick.
                    isSpoofing = false;
                }
            }
            return;
        }

        // Preparation and attack execution
        updateRotations(mc, currentTarget, true);
        // Delay to prevent instant snap hits
        if (nowNs - targetLockSinceNs < 35_000_000L) return;
        
        if (rotation.getValue().equals("None")) {
            doAttack(mc);
        } else if (rotation.getValue().equals("Lock")) {
            mc.player.setYRot(spoofedRots[0]);
            mc.player.setXRot(spoofedRots[1]);
            doAttack(mc);
        } else if (rotation.getValue().equals("Silent")) {
            isSpoofing = false;
            queueSilentAttack(mc);
        }
    }

    private void updateRotations(Minecraft mc, Entity target, boolean forAttack) {
        Vec3 eyePos    = mc.player.getEyePosition();
        Vec3 targetPos = aimPoint(eyePos, target);
        float[] idealRots = calcRotations(eyePos, targetPos);
        
        // GCD Smoothing (simulates mouse sensitivity for strict AC)
        float f = (float) (mc.options.sensitivity().get() * 0.6F + 0.2F);
        float gcd = f * f * f * 8.0F * 0.15F;
        
        float deltaYaw = idealRots[0] - lastYaw;
        float deltaPitch = idealRots[1] - lastPitch;
        
        // Normalize
        while (deltaYaw < -180.0f) deltaYaw += 360.0f;
        while (deltaYaw >= 180.0f) deltaYaw -= 360.0f;

        // Clamp turn speed and apply smoothing
        float maxTurn = (float) turnSpeed.getValue().doubleValue();
        deltaYaw = clamp(deltaYaw, -maxTurn, maxTurn);
        deltaPitch = clamp(deltaPitch, -maxTurn, maxTurn);

        float smoothing = forAttack ? 0.08f : 0.30f;
        deltaYaw *= (1.0f - smoothing);
        deltaPitch *= (1.0f - smoothing);

        deltaYaw -= deltaYaw % gcd;
        deltaPitch -= deltaPitch % gcd;
        
        float yaw = lastYaw + deltaYaw;
        float pitch = Math.max(-90, Math.min(90, lastPitch + deltaPitch));

        // Humanization noise: stronger while tracking, lighter while attacking.
        float jitter = forAttack ? 0.10f : 0.24f;
        yaw += ThreadLocalRandom.current().nextFloat(-jitter, jitter);
        pitch += ThreadLocalRandom.current().nextFloat(-jitter, jitter);
        if (!forAttack) {
            yaw += (float) (Math.sin(System.nanoTime() * 0.000000003) * 0.18);
        }

        this.spoofedRots[0] = yaw;
        this.spoofedRots[1] = pitch;
        
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    private float[] calcRotations(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{ yaw, pitch };
    }

    // Called by LocalPlayerMixin after sendPosition builds the packet
    public void onRotationSent() {
        if (System.nanoTime() < kbPauseUntilNs) {
            needsAttack = false;
            pendingAttackSinceNs = 0L;
            return;
        }
        if (needsAttack && currentTarget != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                doAttack(mc);
            }
            needsAttack = false;
            pendingAttackSinceNs = 0L;
        }
    }

    private void queueSilentAttack(Minecraft mc) {
        if (currentTarget == null || mc.player == null) return;
        long nowNs = System.nanoTime();
        long minIntervalNs = (long) (1_000_000_000.0 / Math.max(2.0, rotPps.getValue()));
        if (nowNs - lastForcedRotPacketNs < minIntervalNs) return;
        if (!allowForcedRotPacket(nowNs)) return;

        // Send one precise look packet right before attack so server-side yaw/pitch is fresh.
        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
            spoofedRots[0],
            spoofedRots[1],
            mc.player.onGround(),
            mc.player.horizontalCollision
        ));
        lastForcedRotPacketNs = nowNs;

        doAttack(mc);
        needsAttack = false;
        isSpoofing = false;
        pendingAttackSinceNs = 0L;
        pendingSilentWaitNs = 0L;
    }
    
    private void doAttack(Minecraft mc) {
        if (System.nanoTime() < kbPauseUntilNs) return;
        if (mc.gameMode == null) return;
        if (currentTarget == null || !currentTarget.isAlive()) return;
        double reach = range.getValue() + 0.35;
        if (mc.player.distanceToSqr(currentTarget) > reach * reach) return;

        if (criticals.getValue() && !"Silent".equals(rotation.getValue()) && mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava()) {
            double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, mc.player.horizontalCollision));
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, mc.player.horizontalCollision));
        }

        mc.gameMode.attack(mc.player, currentTarget);
        mc.player.swing(InteractionHand.MAIN_HAND);

        if (keepSprint.getValue() && mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }

        lastAttackMs = System.currentTimeMillis();
    }

    // APIs for Mixin
    public boolean isSpoofingRotation() { return isSpoofing; }
    public float[] getSpoofedRotations() { return spoofedRots; }

    private boolean canAttack(Minecraft mc) {
        if (timing.getValue().equals("Vanilla")) {
            return mc.player.getAttackStrengthScale(0.5f) >= 0.9f;
        } else {
            long now = System.nanoTime();
            if (nextAttackNs == 0L) {
                scheduleNextAttack(now);
            }
            if (now >= nextAttackNs) {
                scheduleNextAttack(now);
                return true;
            }
            return false;
        }
    }

    private boolean entityFilter(Entity e) {
        if (e instanceof Player p) {
            if (!players.getValue()) return false;
            if (antiBot.getValue()) {
                if (p.getGameProfile().getName().length() > 16) return false;
                if (p.isInvisible() && p.getArmorValue() == 0) return false;
                if (p.getUUID().version() != 4 && p.getUUID().version() != 3) return false; // Basic NPC UUID check
            }
            return true;
        }
        if (e instanceof net.minecraft.world.entity.monster.Monster) return monsters.getValue();
        if (e instanceof net.minecraft.world.entity.animal.Animal) return animals.getValue();
        return false;
    }

    private Comparator<Entity> targetComparator(Minecraft mc) {
        return switch (priority.getValue()) {
            case "Health"   -> Comparator.comparingDouble(e -> (e instanceof LivingEntity le) ? le.getHealth() : Float.MAX_VALUE);
            case "Angle"    -> Comparator.comparingDouble(e -> {
                Vec3 look = mc.player.getLookAngle();
                Vec3 diff = e.position().subtract(mc.player.position()).normalize();
                return -look.dot(diff);
            });
            default         -> Comparator.comparingDouble(e -> mc.player.distanceTo(e));
        };
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        }
        nextAttackNs = 0L;
        lastForcedRotPacketNs = 0L;
        pendingAttackSinceNs = 0L;
        pendingSilentWaitNs = 0L;
        kbPauseUntilNs = 0L;
        forcedRotWindowStartNs = 0L;
        forcedRotPacketsInWindow = 0;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        isSpoofing = false;
        needsAttack = false;
        pendingAttackSinceNs = 0L;
        pendingSilentWaitNs = 0L;
        forcedRotWindowStartNs = 0L;
        forcedRotPacketsInWindow = 0;
    }

    public Entity getTarget() { return currentTarget; }

    public void onKnockbackReceived() {
        kbPauseUntilNs = System.nanoTime() + 70_000_000L;
        needsAttack = false;
        pendingAttackSinceNs = 0L;
        pendingSilentWaitNs = 0L;
    }

    public boolean isSilentMode() {
        return "Silent".equals(rotation.getValue());
    }

    private boolean isWithinFov(Minecraft mc, Entity e) {
        double maxFov = fov.getValue();
        maxFov += Math.sin(System.nanoTime() * 0.0000000025) * 6.0;
        if (maxFov >= 359.0) return true;

        Vec3 look = mc.player.getLookAngle().normalize();
        Vec3 toTarget = aimPoint(mc.player.getEyePosition(), e).subtract(mc.player.getEyePosition()).normalize();
        double dot = clamp(look.dot(toTarget), -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle <= (maxFov * 0.5);
    }

    private Vec3 aimPoint(Vec3 eye, Entity target) {
        var bb = target.getBoundingBox();
        double x = clamp(eye.x, bb.minX, bb.maxX);
        double z = clamp(eye.z, bb.minZ, bb.maxZ);
        double span = Math.max(0.05, bb.maxY - bb.minY);
        long bucket = System.nanoTime() / 250_000_000L;
        long seed = (target.getId() * 1103515245L) ^ bucket;
        double n01 = ((seed & 0x7fffffffL) / (double) Integer.MAX_VALUE);
        double randomY = 0.24 + (0.58 * n01);
        double y = bb.minY + span * randomY;
        y = clamp(y, bb.minY + 0.04, bb.maxY - 0.04);
        return new Vec3(x, y, z);
    }

    private void scheduleNextAttack(long nowNs) {
        double targetCps = cps.getValue();
        if (randomize.getValue()) {
            targetCps += ThreadLocalRandom.current().nextDouble(-targetCps * 0.25, targetCps * 0.25);
        }
        // Rate limiting for silent mode
        if ("Silent".equals(rotation.getValue())) {
            targetCps = Math.min(targetCps, 9.5);
        }
        targetCps = Math.max(1.0, targetCps);
        long delayNs = (long) (1_000_000_000.0 / targetCps);
        nextAttackNs = nowNs + delayNs;
    }

    private boolean allowForcedRotPacket(long nowNs) {
        // Hard cap: at most 10 forced look packets/sec from aura.
        if (forcedRotWindowStartNs == 0L || nowNs - forcedRotWindowStartNs >= 1_000_000_000L) {
            forcedRotWindowStartNs = nowNs;
            forcedRotPacketsInWindow = 0;
        }
        if (forcedRotPacketsInWindow >= 10) return false;
        forcedRotPacketsInWindow++;
        return true;
    }

    private boolean isValidTarget(Minecraft mc, Entity e) {
        if (e == null || !e.isAlive() || e == mc.player) return false;
        if (!(e instanceof LivingEntity)) return false;
        if (mc.player.distanceToSqr(e) > range.getValue() * range.getValue()) return false;
        if (!entityFilter(e)) return false;
        if (!passesFov(mc, e)) return false;
        return walls.getValue() || mc.player.hasLineOfSight(e);
    }

    private boolean passesFov(Minecraft mc, Entity e) {
        // Silent aura rotates server-side; hard local FOV checks make targets disappear and attacks fail.
        if ("Silent".equals(rotation.getValue())) return true;
        return isWithinFov(mc, e);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
