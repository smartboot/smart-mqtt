# 平台配置目录

本目录存储 illustrator skill 的平台配置文件。

## 如何添加新平台

1. 复制 `_template.yml` 并重命名为 `{platform-name}.yml`
2. 根据平台自定义配置
3. Skill 会自动检测并使用配置

## 配置结构

```yaml
platform:
  name: "平台名称"
  description: "简介"

sizes:
  cover-large:
    width: 900
    height: 383
    description: "主封面图"
  # ... 更多尺寸

safe-area:
  top: 60
  bottom: 40
  left: 40
  right: 40

format:
  type: "static-html"
  output: "png"
  max-file-size: "2MB"

mobile:
  base-device: "iPhone 14 Pro Max"
  base-width: 430
  min-readable-font: 14

constraints:
  animation: false
  interaction: false
```

## 可用配置

| 文件 | 平台 | 描述 |
|------|------|------|
| `_template.yml` | 模板 | 配置模板 |
| `wechat.yml` | 微信公众号 | 微信公众号配置 |
