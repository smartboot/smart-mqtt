// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightImageZoomPlugin from "starlight-image-zoom";
import starlightScrollToTop from 'starlight-scroll-to-top';
import mermaid from 'astro-mermaid';


// https://astro.build/config
export default defineConfig({
    site: 'https://smartboot.tech/',
    base: '/smart-mqtt',
    trailingSlash: "always",
    integrations: [mermaid({
        theme: 'forest',
        autoTheme: true
    }),
        starlight({
            title: 'smart-mqtt',
            logo: {
                src: './src/assets/logo.svg',
            },
            customCss: [
                // 你的自定义 CSS 文件的相对路径
                './src/styles/custom.css',
            ],
            head: [
                {
                    tag: 'meta',
                    attrs: {
                        property: 'keywords',
                        content: 'smart-mqtt,mqtt,broker,mqtt broker',
                    }
                }, {
                    tag: 'meta',
                    attrs: {
                        property: 'description',
                        content: 'smart-mqtt 是 smartboot 开源组织推出的商业化 MQTT Broker 产品，专为拥有上万级设备连接量的企业级物联网场景设计。',
                    }
                },
                {
                    tag: 'script',
                    content: `
                var _hmt = _hmt || [];
                (function() {
                  var hm = document.createElement("script");
                  hm.src = "https://hm.baidu.com/hm.js?ee8630857921d8030d612dbd7d751b55";
                  var s = document.getElementsByTagName("script")[0]; 
                  s.parentNode.insertBefore(hm, s);
                })();
          `
                }
            ],
            social: [
                {icon: 'github', label: 'GitHub', href: 'https://github.com/smartboot/smart-mqtt'},
                {icon: 'seti:git', label: 'Gitee', href: 'https://gitee.com/smartboot/smart-mqtt'}
            ],
            plugins: [starlightImageZoomPlugin(),starlightScrollToTop({
                // Button position
                // Tooltip text
                tooltipText: 'Back to top',
                showTooltip: true,
                // Use smooth scrolling
                // smoothScroll: true,
                // Visibility threshold (show after scrolling 20% down)
                threshold: 20,
                // Customize the SVG icon
                borderRadius: '50',
                // Show scroll progress ring
                showProgressRing: true,
                // Customize progress ring color
                progressRingColor: '#ff6b6b',
            })],
            // 为此网站设置英语为默认语言。
            defaultLocale: 'root',
            locales: {
                root: {
                    label: '简体中文',
                    lang: 'zh-CN',
                },
                // 英文文档在 `src/content/docs/en/` 中。
                'en': {
                    label: 'English',
                    lang: 'en'
                }
            },
            sidebar: [
                {
                    label: '快速开始',
                    link: '/',
                },
                {
                    label: '产品手册',
                    autogenerate: {directory: 'product'}
                },
                {
                    label: '插件',
                    autogenerate: {directory: 'plugins'}
                },
                {
                    label: '开发参考',
                    autogenerate: {directory: 'development'}
                },
            ],
        }),
    ],
});
