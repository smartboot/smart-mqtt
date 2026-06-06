---
name: "version-upgrader"
description: "升级 smart-mqtt 版本时，自动更新 README.md、README_zh.md 和 Astro 页面中的版本号和下载链接。当用户需要发布新版本或升级版本号时调用。"
---

# 版本升级工具

该 Skill 自动化升级版本号并同步更新所有文档文件中的对应下载链接。

## 目标文件

| 文件路径 | 更新内容 |
|---------|---------|
| `README.md` | 版本徽章、本地安装部分的下载链接 |
| `README_zh.md` | 版本徽章、本地部署部分的下载链接 |
| `pages/src/components/DeploymentSection.astro` | 下载链接、复制按钮的 data-code 属性 |
| `pages/src/components/en/DeploymentSection.astro` | 下载链接、复制按钮的 data-code 属性 |
| `pages/src/pages/index.astro` | 页脚的版本显示 |

## 匹配规则

**版本号格式**: `v\d+\.\d+\.\d+` (例如：`v1.5.5`)

**下载链接格式**:
- GitHub: `https://github.com/smartboot/smart-mqtt/releases/download/v{版本号}/smart-mqtt-full-v{版本号}.zip`
- Gitee: `https://gitee.com/smartboot/smart-mqtt/releases/download/v{版本号}/smart-mqtt-full-v{版本号}.zip`

## 使用说明

1. **调用时**，首先询问用户目标版本号（例如：`v1.5.6`）
2. **验证**版本号符合语义化版本格式：`vX.Y.Z`
3. **执行**以下更新：

### 步骤 1：更新 README.md
- 徽章版本: `https://img.shields.io/badge/version-v{版本号}-green.svg`
- "Local Installation" 部分的下载链接

### 步骤 2：更新 README_zh.md
- 徽章版本: `https://img.shields.io/badge/version-v{版本号}-green.svg`
- "本地部署" 部分的下载链接

### 步骤 3：更新中文 DeploymentSection.astro
- 代码块中的下载链接（Gitee 源）
- 复制按钮的 `data-code` 属性

### 步骤 4：更新英文 DeploymentSection.astro
- 代码块中的下载链接（GitHub 源）
- 复制按钮的 `data-code` 属性

### 步骤 5：更新 Astro 首页页脚版本
- 显示版本: `<span class="stat-item">v{版本号}</span>`

## 校验清单

✅ 所有下载链接都包含新版本号
✅ README 徽章显示正确版本
✅ Astro 组件页脚显示正确版本
✅ 复制按钮的 `data-code` 属性和可见代码内容同步更新
✅ 中文页面使用 Gitee 链接，英文页面使用 GitHub 链接
