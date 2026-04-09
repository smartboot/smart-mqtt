# 品牌配置目录

本目录存储 illustrator skill 的品牌配置文件。

## 如何添加新品牌

1. 复制 `_template.yml` 并重命名为 `{brand-name}.yml`
2. 根据品牌自定义配置
3. Skill 会自动检测并使用配置

## 配置结构

```yaml
brand:
  name: "品牌名称"
  tagline: "品牌标语"
  description: "简介"

colors:
  primary: "#2563eb"
  primary-light: "#3b82f6"
  primary-dark: "#1d4ed8"
  secondary: "#64748b"
  accent: "#f59e0b"

neutral:
  50: "#fafafa"
  # ... 更多中性色

art-style:
  cream: "#fef9f3"
  # ... 手绘风格色

typography:
  font-stack: "..."
  art-font: "..."

logo:
  text: "LOGO"
  position: "bottom-right"

article-types:
  technical-tutorial:
    primary: "#2563eb"
    # ...
```

## 可用配置

| 文件 | 品牌 | 描述 |
|------|------|------|
| `_template.yml` | 模板 | 配置模板 |
