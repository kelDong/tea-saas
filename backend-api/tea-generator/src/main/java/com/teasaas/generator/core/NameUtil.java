package com.teasaas.generator.core;

/** 命名转换工具（snake_case &lt;-&gt; camelCase / PascalCase） */
public final class NameUtil {

    private NameUtil() {
    }

    /** snake_case -&gt; PascalCase：bom_material -&gt; BomMaterial */
    public static String toPascal(String snake) {
        StringBuilder sb = new StringBuilder(snake.length());
        for (String part : snake.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** snake_case -&gt; camelCase：material_code -&gt; materialCode */
    public static String toCamel(String snake) {
        String pascal = toPascal(snake);
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /** 首字母小写：BomMaterialMapper -&gt; bomMaterialMapper */
    public static String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
