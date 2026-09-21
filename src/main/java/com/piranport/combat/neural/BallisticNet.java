package com.piranport.combat.neural;

/**
 * 神经网络弹道解算的实验实现。
 *
 * <p>对应文档：{@code docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md}
 *
 * <p><b>本类只服务于实验炮 {@code neural_ballistic_test_gun}，不参与任何既有火炮。</b>
 * {@link com.piranport.combat.BallisticSolver} 仍是权威解算器。
 *
 * <h2>为什么是「飞行时间 + 闭式反解」而不是直接输出角度</h2>
 * 同一水平距离存在高低两根（实测 13.4° / 64.9°），且近射程上限处两根收敛。
 * 网络回归角度只能得到条件均值——两根中间那个值必然打不中。飞行时间 t 是单值
 * 函数，误差线性可传播，再由 t 闭式反解角度即可。
 *
 * <h2>验收标准是落点高度误差，不是角度误差</h2>
 * 反解可以合法地选中另一支，此时角度误差很大但落点完全正确。
 * 见文档 4.5 关键发现 4。
 *
 * @see BallisticNetMath 闭式反解与物理复刻
 */
public final class BallisticNet {

    /**
     * 网络结构：每层一个 {@link Layer}，最后一层无激活函数。
     *
     * <p>权重全部平铺在 {@code double[]} 里。**禁止用 {@code double[][]}**——
     * 后者会让每次前向传播退化成数十次指针跳转，缓存局部性崩溃（文档 2.3 节）。
     */
    private final Layer[] layers;

    /** 输入标准化参数：{@code (x - xMean) / xStd} */
    private final double[] xMean;
    private final double[] xStd;

    /** 输出反标准化：{@code pred * yStd + yMean} */
    private final double yMean;
    private final double yStd;

    /** 推理过程中的重算次数，仅用于自检。 */
    private long inferenceCount;

    private BallisticNet(Layer[] layers, double[] xMean, double[] xStd, double yMean, double yStd) {
        this.layers = layers;
        this.xMean = xMean;
        this.xStd = xStd;
        this.yMean = yMean;
        this.yStd = yStd;
    }

    /**
     * 单层全连接。权重按行主序平铺：{@code weights[i * inDim + j]} 是
     * 第 i 个输出对第 j 个输入的权重。
     */
    private static final class Layer {
        final double[] weights;
        final double[] biases;
        final int inDim;
        final int outDim;
        final boolean relu;

        Layer(double[] weights, double[] biases, int inDim, int outDim, boolean relu) {
            this.weights = weights;
            this.biases = biases;
            this.inDim = inDim;
            this.outDim = outDim;
            this.relu = relu;
        }
    }

    /**
     * 输入标准化 + 逐层前向 + 输出反标准化。
     *
     * <p><b>注意：网络学的是残差，不是飞行时间本身。</b>训练脚本
     * （{@code tools/neural_ballistic/train.py}）把目标定义为
     * {@code t − hDist / initialSpeed}——因为残差幅值只有 t 本身的约 10%，
     * 让 MLP 逼近它才可能达到精度要求（train.py:157 的 {@code residual_baseline}）。
     * 因此反标准化后必须把基线加回来，否则得到的是一段没有物理意义的数。
     *
     * <p>踩坑：曾漏掉这一步，症状是解算结果整体偏小、落点误差最大 47 格
     * （54 格距离处预测 4.87 tick，真值 22.87 tick）。这个 bug 不会报错、
     * 不会抛异常，只会让精度全线崩掉，靠单元测试才发现。
     *
     * @param input 5 维输入，长度必须为 5
     * @return 飞行时间（tick），未做闭式反解
     */
    public double forwardTicks(double[] input) {
        double residual = forwardResidual(input);
        // 加回残差基线 h/v0。v0 非法时训练侧取 0（见 residual_baseline 的 np.where），此处对齐。
        double v0 = input[2];
        double baseline = v0 > 1e-9 ? input[0] / v0 : 0.0;
        return residual + baseline;
    }

    /**
     * 只跑网络本体，返回反标准化后的**残差**（未加基线）。
     *
     * <p>拆出来是为了让 {@code forwardTicks} 的基线步骤显式可见，
     * 避免再次被当成「多余的一行」删掉。
     */
    private double forwardResidual(double[] input) {
        if (input.length != xMean.length) {
            throw new IllegalArgumentException("输入维度应为 " + xMean.length + "，实际 " + input.length);
        }

        double[] buf = scratchA;
        double[] next = scratchB;

        // 输入标准化
        for (int j = 0; j < input.length; j++) {
            buf[j] = (input[j] - xMean[j]) / xStd[j];
        }

        for (int li = 0; li < layers.length; li++) {
            Layer layer = layers[li];
            if (next.length < layer.outDim) {
                next = new double[layer.outDim];
            }
            for (int i = 0; i < layer.outDim; i++) {
                double sum = layer.biases[i];
                int base = i * layer.inDim;
                double[] w = layer.weights;
                // 内层循环按 j 递增遍历连续内存，这是保持缓存友好的关键
                for (int j = 0; j < layer.inDim; j++) {
                    sum += w[base + j] * buf[j];
                }
                next[i] = (layer.relu && sum < 0.0) ? 0.0 : sum;
            }
            double[] swap = buf;
            buf = next;
            next = swap;
        }

        inferenceCount++;
        return buf[0] * yStd + yMean;
    }

    // 复用的前向传播缓冲区。网络是单例、只在服务端/客户端主线程调用，
    // 不存在并发，因此可以用实例字段避免每 tick 分配数组。
    private double[] scratchA = new double[MAX_WIDTH];
    private double[] scratchB = new double[MAX_WIDTH];

    private static final int MAX_WIDTH = 512;

    /**
     * 完整解算：网络推理 t，再闭式反解仰角。
     *
     * @return 仰角（弧度）；不可达时返回 {@code null}
     */
    public double solveAngle(double[] input, double hDist, double vDist,
                             double initialSpeed, double dragCoeff, double gravity) {
        double ticks = forwardTicks(input);
        return BallisticNetMath.angleFromTicks(initialSpeed, dragCoeff, gravity, hDist, vDist, ticks);
    }

    public long getInferenceCount() {
        return inferenceCount;
    }

    public int parameterCount() {
        int n = 0;
        for (Layer l : layers) {
            n += l.weights.length + l.biases.length;
        }
        return n;
    }

    /**
     * 从一组平铺的层数组中构造网络。
     *
     * @param weights 各层权重，行主序平铺
     * @param biases  各层偏置
     * @param inDims  各层输入维度
     * @param outDims 各层输出维度
     * @param relu    各层是否带 ReLU
     */
    public static BallisticNet of(double[][] weights, double[][] biases,
                                  int[] inDims, int[] outDims, boolean[] relu,
                                  double[] xMean, double[] xStd, double yMean, double yStd) {
        int n = weights.length;
        if (biases.length != n || inDims.length != n || outDims.length != n || relu.length != n) {
            throw new IllegalArgumentException("层参数长度不一致");
        }
        Layer[] layers = new Layer[n];
        for (int i = 0; i < n; i++) {
            int expected = inDims[i] * outDims[i];
            if (weights[i].length != expected) {
                throw new IllegalArgumentException("第 " + i + " 层权重长度应为 " + expected
                        + "，实际 " + weights[i].length);
            }
            if (biases[i].length != outDims[i]) {
                throw new IllegalArgumentException("第 " + i + " 层偏置长度应为 " + outDims[i]
                        + "，实际 " + biases[i].length);
            }
            layers[i] = new Layer(weights[i], biases[i], inDims[i], outDims[i], relu[i]);
        }
        int maxWidth = 0;
        for (int d : inDims) maxWidth = Math.max(maxWidth, d);
        for (int d : outDims) maxWidth = Math.max(maxWidth, d);
        BallisticNet net = new BallisticNet(layers, xMean, xStd, yMean, yStd);
        net.scratchA = new double[maxWidth];
        net.scratchB = new double[maxWidth];
        return net;
    }

    /**
     * 载入实验权重（256-128-64，42753 参数，float32 存 167 KB）。
     *
     * <p>训练结果：t MAE 0.025 tick，落点高度误差中位 0.0045 格，
     * 达 0.02 格测试门槛 83.3%。见文档 4.5 关键发现 5。
     *
     * <p>权重来自 classpath 资源而非源码常量——42753 个字面量会超出 JVM
     * 「常量过多」限制。见 {@link BallisticNetWeights}。
     */
    public static BallisticNet loadExperimental() {
        return BallisticNetWeights.load(EXPERIMENTAL_WEIGHTS_PATH);
    }

    /** 实验网络权重资源路径。 */
    public static final String EXPERIMENTAL_WEIGHTS_PATH = "/data/piranport/neural/ballistic_net_256.bin";
}
