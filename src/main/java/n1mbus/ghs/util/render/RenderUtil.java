package n1mbus.ghs.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class RenderUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    // 互換性のためpublicを維持
    public static final Matrix4f lastProjectionMatrix = new Matrix4f();
    public static final Matrix4f lastModelViewMatrix = new Matrix4f();
    private static boolean capturingWorldMatrices = false;

    public static void beginWorldRender() {
        capturingWorldMatrices = true;
    }

    public static void endWorldRender() {
        capturingWorldMatrices = false;
    }

    public static void setWorldMatrices(Matrix4f modelView, Matrix4f projection) {
        lastModelViewMatrix.set(modelView);
        lastProjectionMatrix.set(projection);
    }

    public static Matrix4f getProjectionMatrix() {
        return lastProjectionMatrix;
    }

    public static void setProjection(Matrix4f mat) {
        if (capturingWorldMatrices) {
            lastProjectionMatrix.set(mat);
        }
    }

    public static void setModelView(Matrix4f mat) {
        if (capturingWorldMatrices) {
            lastModelViewMatrix.set(mat);
        }
    }

    public static Vec3 project(Vec3 pos) {
        Vec3 projectedByCaptured = projectWithCapturedMatrices(pos);
        if (projectedByCaptured != null) {
            return projectedByCaptured;
        }

        // Fallback path when captured matrices are unavailable.
        var camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return null;
        Vec3 camPos = camera.getPosition();

        float dx = (float) (pos.x - camPos.x);
        float dy = (float) (pos.y - camPos.y);
        float dz = (float) (pos.z - camPos.z);

        // カメラの逆回転でビュー空間に変換
        Quaternionf rot = camera.rotation();
        Quaternionf invRot = new Quaternionf(rot).conjugate();
        Matrix4f modelView = new Matrix4f().rotate(invRot);

        Vector4f viewPos = new Vector4f(dx, dy, dz, 1.0f);
        modelView.transform(viewPos);

        // FOV から投影行列を計算
        Matrix4f proj = computeProjection();
        proj.transform(viewPos);

        if (viewPos.w() <= 0.0f) return null;

        float invW = 1.0f / viewPos.w();
        float ndcX = viewPos.x * invW;
        float ndcY = viewPos.y * invW;

        float x = (ndcX + 1.0f) * 0.5f * (float) mc.getWindow().getGuiScaledWidth();
        float y = (1.0f - ndcY) * 0.5f * (float) mc.getWindow().getGuiScaledHeight();

        return new Vec3(x, y, viewPos.z * invW);
    }

    private static Vec3 projectWithCapturedMatrices(Vec3 pos) {
        if (!isUsable(lastProjectionMatrix) || !isUsable(lastModelViewMatrix)) {
            return null;
        }

        Vector4f clip = new Vector4f((float) pos.x, (float) pos.y, (float) pos.z, 1.0f);
        new Matrix4f(lastModelViewMatrix).transform(clip);
        new Matrix4f(lastProjectionMatrix).transform(clip);

        if (clip.w() <= 0.0001f) return null;

        float invW = 1.0f / clip.w();
        float ndcX = clip.x() * invW;
        float ndcY = clip.y() * invW;
        float ndcZ = clip.z() * invW;

        // Far off-screen points are not useful for 2D boxes.
        if (ndcX < -1.8f || ndcX > 1.8f || ndcY < -1.8f || ndcY > 1.8f) return null;

        float x = (ndcX + 1.0f) * 0.5f * (float) mc.getWindow().getGuiScaledWidth();
        float y = (1.0f - ndcY) * 0.5f * (float) mc.getWindow().getGuiScaledHeight();
        return new Vec3(x, y, ndcZ);
    }

    private static boolean isUsable(Matrix4f m) {
        // Identity means "not captured yet" in our flow.
        return !(Math.abs(m.m00() - 1.0f) < 1.0e-5f
            && Math.abs(m.m11() - 1.0f) < 1.0e-5f
            && Math.abs(m.m22() - 1.0f) < 1.0e-5f
            && Math.abs(m.m33() - 1.0f) < 1.0e-5f
            && Math.abs(m.m01()) < 1.0e-5f
            && Math.abs(m.m02()) < 1.0e-5f
            && Math.abs(m.m03()) < 1.0e-5f
            && Math.abs(m.m10()) < 1.0e-5f
            && Math.abs(m.m12()) < 1.0e-5f
            && Math.abs(m.m13()) < 1.0e-5f
            && Math.abs(m.m20()) < 1.0e-5f
            && Math.abs(m.m21()) < 1.0e-5f
            && Math.abs(m.m23()) < 1.0e-5f
            && Math.abs(m.m30()) < 1.0e-5f
            && Math.abs(m.m31()) < 1.0e-5f
            && Math.abs(m.m32()) < 1.0e-5f);
    }

    private static Matrix4f computeProjection() {
        float fovDegrees = mc.options.fov().get().floatValue();
        float fov = (float) Math.toRadians(fovDegrees);
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (h == 0) h = 1;
        float aspect = (float) w / (float) h;
        return new Matrix4f().perspective(fov, aspect, 0.05f, 1024.0f);
    }
}
