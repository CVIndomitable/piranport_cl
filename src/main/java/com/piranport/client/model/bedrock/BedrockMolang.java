package com.piranport.client.model.bedrock;

import java.util.HashMap;
import java.util.Map;

/**
 * 极简 Molang 求值器，只覆盖 YSM 模型动画里实际出现的语法子集。
 *
 * <p>WHY：项目没有 GeckoLib 依赖，而 Bedrock 动画的少量通道把数值写成了 Molang 表达式
 * （例如 {@code 0.4*ysm.head_yaw}）。完整实现 Molang 代价过大，这里只做
 * 算术 / 比较 / 三元 / 常用 {@code math.*} 函数，未知函数统一返回 0，
 * 保证模型不会因为表达式而渲染失败。</p>
 */
public final class BedrockMolang {
    /** 一次采样的变量上下文。 */
    public static final class Context {
        public float animTime;
        public float headYaw;
        public float headPitch;
        public final Map<String, Float> variables = new HashMap<>();
    }

    public interface Node {
        float eval(Context ctx);
    }

    private static final Map<String, Node> CACHE = new HashMap<>();

    private BedrockMolang() {}

    public static Node constant(float value) {
        return ctx -> value;
    }

    /** 编译表达式（带字符串缓存）。 */
    public static Node compile(String expression) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(expression, BedrockMolang::parse);
        }
    }

    /** 直接求值；空串或解析失败返回 0。 */
    public static float eval(String expression, Context ctx) {
        if (expression == null || expression.isBlank()) return 0.0F;
        return compile(expression).eval(ctx);
    }

    // ===== 解析 =====

    private static Node parse(String source) {
        try {
            Parser parser = new Parser(source);
            Node node = parser.parseStatement();
            return node;
        } catch (RuntimeException ex) {
            return constant(0.0F);
        }
    }

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
        }

        Node parseStatement() {
            skip();
            int save = pos;
            // v.xxx = <expr>：赋值语句，求值 RHS 并把结果写回变量表
            if (peekIdentifier()) {
                String name = readIdentifier();
                skip();
                if (pos < src.length() && src.charAt(pos) == '=' && (pos + 1 >= src.length() || src.charAt(pos + 1) != '=')) {
                    pos++;
                    Node value = parseExpression();
                    return ctx -> {
                        float v = value.eval(ctx);
                        ctx.variables.put(name, v);
                        return v;
                    };
                }
            }
            pos = save;
            return parseExpression();
        }

        Node parseExpression() {
            return parseTernary();
        }

        private Node parseTernary() {
            Node condition = parseOr();
            skip();
            if (pos < src.length() && src.charAt(pos) == '?') {
                pos++;
                Node whenTrue = parseExpression();
                skip();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                Node whenFalse = parseExpression();
                return ctx -> condition.eval(ctx) != 0.0F ? whenTrue.eval(ctx) : whenFalse.eval(ctx);
            }
            return condition;
        }

        private Node parseOr() {
            Node left = parseAnd();
            while (true) {
                skip();
                if (startsWith("||")) {
                    pos += 2;
                    Node right = parseAnd();
                    Node l = left;
                    left = ctx -> (l.eval(ctx) != 0.0F || right.eval(ctx) != 0.0F) ? 1.0F : 0.0F;
                } else {
                    return left;
                }
            }
        }

        private Node parseAnd() {
            Node left = parseEquality();
            while (true) {
                skip();
                if (startsWith("&&")) {
                    pos += 2;
                    Node right = parseEquality();
                    Node l = left;
                    left = ctx -> (l.eval(ctx) != 0.0F && right.eval(ctx) != 0.0F) ? 1.0F : 0.0F;
                } else {
                    return left;
                }
            }
        }

        private Node parseEquality() {
            Node left = parseComparison();
            while (true) {
                skip();
                if (startsWith("==")) {
                    pos += 2;
                    Node right = parseComparison();
                    Node l = left;
                    left = ctx -> l.eval(ctx) == right.eval(ctx) ? 1.0F : 0.0F;
                } else if (startsWith("!=")) {
                    pos += 2;
                    Node right = parseComparison();
                    Node l = left;
                    left = ctx -> l.eval(ctx) != right.eval(ctx) ? 1.0F : 0.0F;
                } else {
                    return left;
                }
            }
        }

        private Node parseComparison() {
            Node left = parseAdd();
            while (true) {
                skip();
                if (startsWith("<=")) {
                    pos += 2;
                    Node right = parseAdd();
                    Node l = left;
                    left = ctx -> l.eval(ctx) <= right.eval(ctx) ? 1.0F : 0.0F;
                } else if (startsWith(">=")) {
                    pos += 2;
                    Node right = parseAdd();
                    Node l = left;
                    left = ctx -> l.eval(ctx) >= right.eval(ctx) ? 1.0F : 0.0F;
                } else if (pos < src.length() && src.charAt(pos) == '<') {
                    pos++;
                    Node right = parseAdd();
                    Node l = left;
                    left = ctx -> l.eval(ctx) < right.eval(ctx) ? 1.0F : 0.0F;
                } else if (pos < src.length() && src.charAt(pos) == '>') {
                    pos++;
                    Node right = parseAdd();
                    Node l = left;
                    left = ctx -> l.eval(ctx) > right.eval(ctx) ? 1.0F : 0.0F;
                } else {
                    return left;
                }
            }
        }

        private Node parseAdd() {
            Node left = parseMul();
            while (true) {
                skip();
                if (pos < src.length() && src.charAt(pos) == '+') {
                    pos++;
                    Node right = parseMul();
                    Node l = left;
                    left = ctx -> l.eval(ctx) + right.eval(ctx);
                } else if (pos < src.length() && src.charAt(pos) == '-') {
                    pos++;
                    Node right = parseMul();
                    Node l = left;
                    left = ctx -> l.eval(ctx) - right.eval(ctx);
                } else {
                    return left;
                }
            }
        }

        private Node parseMul() {
            Node left = parseUnary();
            while (true) {
                skip();
                if (pos < src.length() && src.charAt(pos) == '*') {
                    pos++;
                    Node right = parseUnary();
                    Node l = left;
                    left = ctx -> l.eval(ctx) * right.eval(ctx);
                } else if (pos < src.length() && src.charAt(pos) == '/') {
                    pos++;
                    Node right = parseUnary();
                    Node l = left;
                    left = ctx -> {
                        float d = right.eval(ctx);
                        return Math.abs(d) < 1.0E-8F ? 0.0F : l.eval(ctx) / d;
                    };
                } else {
                    return left;
                }
            }
        }

        private Node parseUnary() {
            skip();
            if (pos < src.length() && src.charAt(pos) == '-') {
                pos++;
                Node value = parseUnary();
                return ctx -> -value.eval(ctx);
            }
            if (pos < src.length() && src.charAt(pos) == '+') {
                pos++;
                return parseUnary();
            }
            if (pos < src.length() && src.charAt(pos) == '!') {
                pos++;
                Node value = parseUnary();
                return ctx -> value.eval(ctx) == 0.0F ? 1.0F : 0.0F;
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            skip();
            if (pos >= src.length()) return constant(0.0F);
            char c = src.charAt(pos);
            if (c == '(') {
                pos++;
                Node inner = parseExpression();
                skip();
                if (pos < src.length() && src.charAt(pos) == ')') pos++;
                return inner;
            }
            if (c == '\'' || c == '"') {
                // 字符串参数（例如 ysm.second_order('鞘翅yaw', ...)），不参与数值计算
                char quote = c;
                pos++;
                while (pos < src.length() && src.charAt(pos) != quote) pos++;
                if (pos < src.length()) pos++;
                return constant(0.0F);
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
                try {
                    return constant(Float.parseFloat(src.substring(start, pos)));
                } catch (NumberFormatException ex) {
                    return constant(0.0F);
                }
            }
            if (peekIdentifier()) {
                String name = readIdentifier();
                skip();
                if (pos < src.length() && src.charAt(pos) == '(') {
                    pos++;
                    java.util.List<Node> args = new java.util.ArrayList<>();
                    skip();
                    if (pos < src.length() && src.charAt(pos) != ')') {
                        while (true) {
                            args.add(parseExpression());
                            skip();
                            if (pos < src.length() && src.charAt(pos) == ',') {
                                pos++;
                            } else {
                                break;
                            }
                        }
                    }
                    skip();
                    if (pos < src.length() && src.charAt(pos) == ')') pos++;
                    return function(name, args);
                }
                return variable(name);
            }
            // 未知字符：跳过，避免死循环
            pos++;
            return constant(0.0F);
        }

        private boolean peekIdentifier() {
            if (pos >= src.length()) return false;
            char c = src.charAt(pos);
            return Character.isLetter(c) || c == '_';
        }

        private String readIdentifier() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            return src.substring(start, pos);
        }

        private boolean startsWith(String token) {
            return src.startsWith(token, pos);
        }

        private void skip() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }

    private static Node variable(String name) {
        return switch (name) {
            case "query.anim_time", "q.anim_time", "query.life_time", "q.life_time" -> ctx -> ctx.animTime;
            case "ysm.head_yaw" -> ctx -> ctx.headYaw;
            case "ysm.head_pitch" -> ctx -> ctx.headPitch;
            case "math.pi" -> constant((float) Math.PI);
            default -> {
                if (name.startsWith("v.") || name.startsWith("variable.")) {
                    yield ctx -> ctx.variables.getOrDefault(name, 0.0F);
                }
                yield constant(0.0F);
            }
        };
    }

    private static Node function(String name, java.util.List<Node> args) {
        return switch (name) {
            case "math.sin" -> unaryDeg(args, v -> (float) Math.sin(v));
            case "math.cos" -> unaryDeg(args, v -> (float) Math.cos(v));
            case "math.tan" -> unaryDeg(args, v -> (float) Math.tan(v));
            case "math.asin" -> unaryDeg(args, v -> (float) Math.asin(Math.max(-1.0F, Math.min(1.0F, v))));
            case "math.acos" -> unaryDeg(args, v -> (float) Math.acos(Math.max(-1.0F, Math.min(1.0F, v))));
            case "math.atan" -> unaryDeg(args, v -> (float) Math.atan(v));
            case "math.abs" -> unary(args, v -> Math.abs(v));
            case "math.exp" -> unary(args, (float v) -> (float) Math.exp(v));
            case "math.sqrt" -> unary(args, (float v) -> (float) Math.sqrt(Math.max(0.0F, v)));
            case "math.floor" -> unary(args, (float v) -> (float) Math.floor(v));
            case "math.round" -> unary(args, (float v) -> (float) Math.round(v));
            case "math.ceil" -> unary(args, (float v) -> (float) Math.ceil(v));
            case "math.clamp" -> (ctx) -> {
                if (args.size() < 3) return 0.0F;
                float v = args.get(0).eval(ctx);
                float min = args.get(1).eval(ctx);
                float max = args.get(2).eval(ctx);
                return Math.max(min, Math.min(max, v));
            };
            case "math.lerp" -> (ctx) -> {
                if (args.size() < 3) return 0.0F;
                float a = args.get(0).eval(ctx);
                float b = args.get(1).eval(ctx);
                float t = args.get(2).eval(ctx);
                return a + (b - a) * t;
            };
            case "math.min" -> (ctx) -> {
                if (args.isEmpty()) return 0.0F;
                float v = args.get(0).eval(ctx);
                for (int i = 1; i < args.size(); i++) v = Math.min(v, args.get(i).eval(ctx));
                return v;
            };
            case "math.max" -> (ctx) -> {
                if (args.isEmpty()) return 0.0F;
                float v = args.get(0).eval(ctx);
                for (int i = 1; i < args.size(); i++) v = Math.max(v, args.get(i).eval(ctx));
                return v;
            };
            case "math.pow" -> (ctx) -> {
                if (args.size() < 2) return 0.0F;
                return (float) Math.pow(args.get(0).eval(ctx), args.get(1).eval(ctx));
            };
            case "math.mod" -> (ctx) -> {
                if (args.size() < 2) return 0.0F;
                float d = args.get(1).eval(ctx);
                return Math.abs(d) < 1.0E-8F ? 0.0F : args.get(0).eval(ctx) % d;
            };
            // ysm.second_order 等 YSM 专有函数：只影响细节姿态，直接归零
            default -> constant(0.0F);
        };
    }

    private interface FloatUnary {
        float apply(float v);
    }

    private static Node unary(java.util.List<Node> args, FloatUnary fn) {
        if (args.isEmpty()) return constant(0.0F);
        Node arg = args.get(0);
        return ctx -> fn.apply(arg.eval(ctx));
    }

    private static Node unaryDeg(java.util.List<Node> args, FloatUnary fn) {
        if (args.isEmpty()) return constant(0.0F);
        Node arg = args.get(0);
        return ctx -> fn.apply((float) Math.toRadians(arg.eval(ctx)));
    }
}
