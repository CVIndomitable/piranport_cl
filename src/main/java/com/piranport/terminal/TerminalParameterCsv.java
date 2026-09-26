package com.piranport.terminal;

import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/** 服务器固定目录内的参数表，严格解析并整批提交。 */
public final class TerminalParameterCsv {
    private static final int MAX_BYTES = 1_048_576;
    private static final int MAX_ROWS = 4096;
    private static final String HEADER = "key,group,target,property,type,base,value,override";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private TerminalParameterCsv() {}

    private static Path directory(ServerLevel level) {
        return level.getServer().getServerDirectory().resolve("config/piranport/exports");
    }

    public static String export(ServerLevel level, TerminalParametersSavedData data) throws IOException {
        Path dir = directory(level);
        checkDirectory(dir);
        Files.createDirectories(dir);
        checkDirectory(dir);
        String filename = "terminal_parameters_" + LocalDateTime.now().format(STAMP) + ".csv";
        Path output = dir.resolve(filename);
        Path temporary = Files.createTempFile(dir, "terminal_parameters_", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            Map<String, String> overrides = data.overrides();
            TerminalParameterCatalog.all().stream().sorted((a, b) -> a.key().compareTo(b.key()))
                    .forEach(spec -> lines.add(String.join(",", quote(spec.key()), quote(spec.group()), quote(spec.target()),
                            quote(spec.property()), quote(spec.type().name()), quote(spec.baseValue()),
                            quote(overrides.getOrDefault(spec.key(), spec.baseValue())),
                            Boolean.toString(overrides.containsKey(spec.key())))));
            if (lines.size() > MAX_ROWS + 1) throw new IOException("参数数量超出 CSV 上限");
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            if (Files.size(temporary) > MAX_BYTES) throw new IOException("导出 CSV 超过 1 MiB");
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static int importFile(ServerLevel level, TerminalParametersSavedData data, String filename)
            throws IOException {
        if (filename == null || !filename.matches("[A-Za-z0-9_-]{1,96}\\.csv")) {
            throw new IllegalArgumentException("文件名只能使用字母、数字、下划线和连字符，且必须为 csv");
        }
        Path dir = directory(level);
        checkDirectory(dir);
        Path file = dir.resolve(filename);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > MAX_BYTES) {
            throw new IOException("CSV 不存在、不是普通文件或超过 1 MiB");
        }

        Map<String, String> proposed = parse(Files.readString(file, StandardCharsets.UTF_8),
                data.overrides(), TerminalParameterCatalog.all());
        data.replace(proposed);
        return proposed.size();
    }

    private static void checkDirectory(Path dir) throws IOException {
        for (Path path = dir.toAbsolutePath(); path != null; path = path.getParent()) {
            if (Files.isSymbolicLink(path)) throw new IOException("CSV 目录不可使用符号链接");
        }
    }

    public static Map<String, String> parse(String content, Map<String, String> existing,
                                             Collection<TerminalParameterSpec> metadata) throws IOException {
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IOException("CSV 超过 1 MiB");
        Map<String, TerminalParameterSpec> specs = new HashMap<>();
        for (TerminalParameterSpec spec : metadata) specs.put(spec.key(), spec);
        Map<String, String> proposed = new HashMap<>(existing);
        Set<String> seen = new HashSet<>();
        int count = 0;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.StringReader(content))) {
            String line = reader.readLine();
            if (line == null) throw new IOException("CSV 为空");
            List<String> headers = parseRow(line.replaceFirst("^\\uFEFF", ""));
            int keyColumn = headers.indexOf("key");
            int valueColumn = headers.indexOf("value");
            if (keyColumn < 0 || valueColumn < 0 || keyColumn == valueColumn) {
                throw new IOException("CSV 缺少 key/value 列");
            }
            while ((line = reader.readLine()) != null) {
                if (++count > MAX_ROWS) throw new IOException("CSV 超过 4096 行");
                if (line.isBlank()) continue;
                List<String> cells = parseRow(line);
                if (cells.size() != headers.size()) throw new IOException("第 " + (count + 1) + " 行列数错误");
                String key = cells.get(keyColumn).trim();
                if (key.length() > 128) throw new IOException("第 " + (count + 1) + " 行参数名过长");
                if (!seen.add(key)) throw new IOException("重复参数: " + key);
                TerminalParameterSpec spec = specs.get(key);
                if (spec == null) throw new IOException("未知参数: " + key);
                String value;
                try {
                    value = spec.canonical(cells.get(valueColumn));
                } catch (RuntimeException e) {
                    throw new IOException("第 " + (count + 1) + " 行参数非法: " + key, e);
                }
                if (value.equals(spec.canonical(spec.baseValue()))) proposed.remove(key);
                else proposed.put(key, value);
            }
        }
        TerminalParameterValidation.checked(proposed, metadata);
        return proposed;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseRow(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else if (ch == '"') {
                    quoted = false; afterQuote = true;
                } else {
                    field.append(ch);
                }
            } else if (ch == ',' ) {
                fields.add(field.toString()); field.setLength(0); afterQuote = false;
            } else if (ch == '"' && field.isEmpty() && !afterQuote) {
                quoted = true;
            } else if (afterQuote || ch == '"') {
                throw new IOException("CSV 引号格式错误");
            } else {
                field.append(ch);
            }
        }
        if (quoted) throw new IOException("CSV 引号未闭合");
        fields.add(field.toString());
        return fields;
    }
}
