package com.piranport.combat.neural;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 实验网络权重的二进制加载器。
 *
 * <p><b>为什么不做成 Java 源码常量</b>：42753 个 double 字面量会撑爆 JVM 的
 * 「常量过多 / 代码过长」限制（单个静态初始化方法上限 64 KB 字节码），
 * 编译直接失败。必须走资源文件。
 *
 * <p><b>为什么是 float32 而非 double</b>：权重体积减半（342 KB → 167 KB），
 * 而网络本身是用 float32 训练的（PyTorch 默认），转 double 并不增加信息量。
 *
 * <h2>文件格式（小端）</h2>
 * <pre>
 * magic   "PPNW"        4 字节
 * version uint32
 * inDim   uint32
 * yMean   float32
 * yStd    float32
 * xMean   float32 × inDim
 * xStd    float32 × inDim
 * nLayers uint32
 * 每层：
 *   in, out  uint32 × 2
 *   relu     uint8
 *   weights  float32 × (in × out)，行主序
 *   biases   float32 × out
 * </pre>
 */
final class BallisticNetWeights {

    private BallisticNetWeights() {}

    private static final int MAGIC = 0x50504E57;  // "PPNW"
    private static final int VERSION = 1;

    /** 单个权重的字节数（float32）。 */
    private static final int WEIGHT_BYTES = 4;

    /**
     * 从 classpath 读取并构造网络。
     *
     * @param resourcePath 资源路径，如 {@code /data/piranport/neural/ballistic_net_256.bin}
     */
    static BallisticNet load(String resourcePath) {
        java.net.URL where = BallisticNetWeights.class.getResource(resourcePath);
        try (InputStream raw = BallisticNetWeights.class.getResourceAsStream(resourcePath)) {
            if (raw == null) {
                throw new IOException("找不到网络权重资源：" + resourcePath
                        + "（classloader=" + BallisticNetWeights.class.getClassLoader() + "）");
            }
            return parse(new DataInputStream(raw), resourcePath + " @ " + where);
        } catch (IOException e) {
            throw new IllegalStateException("载入网络权重失败：" + resourcePath
                    + "（解析自 " + where + "）", e);
        }
    }

    private static BallisticNet parse(DataInputStream in, String path) throws IOException {
        // 一次性读入内存再解析，避免逐字段 readFloat 的调用开销
        byte[] bytes = in.readAllBytes();
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IOException("权重文件魔数不匹配：" + Integer.toHexString(magic));
        }
        int version = buf.getInt();
        if (version != VERSION) {
            throw new IOException("权重文件版本不支持：" + version + "（期望 " + VERSION + "）");
        }

        int inDim = buf.getInt();
        double yMean = buf.getFloat();
        double yStd = buf.getFloat();

        double[] xMean = new double[inDim];
        for (int i = 0; i < inDim; i++) xMean[i] = buf.getFloat();
        double[] xStd = new double[inDim];
        for (int i = 0; i < inDim; i++) xStd[i] = buf.getFloat();

        int nLayers = buf.getInt();
        if (nLayers <= 0 || nLayers > 64) {
            throw new IOException("层数异常：" + nLayers);
        }

        double[][] weights = new double[nLayers][];
        double[][] biases = new double[nLayers][];
        int[] ins = new int[nLayers];
        int[] outs = new int[nLayers];
        boolean[] relus = new boolean[nLayers];

        for (int li = 0; li < nLayers; li++) {
            int lin = buf.getInt();
            int lout = buf.getInt();
            boolean relu = buf.get() != 0;
            int wCount = lin * lout;
            double[] w = new double[wCount];
            for (int i = 0; i < wCount; i++) w[i] = buf.getFloat();
            double[] b = new double[lout];
            for (int i = 0; i < lout; i++) b[i] = buf.getFloat();

            weights[li] = w;
            biases[li] = b;
            ins[li] = lin;
            outs[li] = lout;
            relus[li] = relu;
        }

        return BallisticNet.of(weights, biases, ins, outs, relus, xMean, xStd, yMean, yStd);
    }

    /**
     * 估算给定参数量的权重文件大小（字节），用于体积预算核对。
     */
    static long estimatedFileBytes(int parameterCount) {
        return (long) parameterCount * WEIGHT_BYTES;
    }
}
