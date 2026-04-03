export const languages = {
  'zh-CN': '简体中文',
  en: 'English',
};

export const defaultLang = 'zh-CN';

export const ui = {
  'zh-CN': {
    'nav.features': '产品特性',
    'nav.cost': '成本优势',
    'nav.performance': '性能表现',
    'nav.deployment': '部署方式',
    'nav.enterprise': '企业服务',
    'sidebar.product': '产品手册',
    'sidebar.plugins': '插件',
    'sidebar.development': '开发参考',
    'footer.ecosystem': '开源生态',
    'footer.developer': '开发者',
    'footer.resources': '资源',
    'footer.about': '关于',
    'footer.docs': '使用文档',
    'footer.plugin': '关于Plugin',
    'footer.changelog': '更新日志',
    'footer.guide': '选型指南',
  },
  en: {
    'nav.features': 'Features',
    'nav.cost': 'Cost',
    'nav.performance': 'Performance',
    'nav.deployment': 'Deployment',
    'nav.enterprise': 'Enterprise',
    'sidebar.product': 'Product Manual',
    'sidebar.plugins': 'Plugins',
    'sidebar.development': 'Development Reference',
    'footer.ecosystem': 'Open Source Ecosystem',
    'footer.developer': 'Developer',
    'footer.resources': 'Resources',
    'footer.about': 'About',
    'footer.docs': 'Documentation',
    'footer.plugin': 'About Plugin',
    'footer.changelog': 'Changelog',
    'footer.guide': 'Selection Guide',
  },
} as const;

export function getLangFromUrl(url: URL) {
  const [, lang] = url.pathname.split('/');
  if (lang in ui) return lang as keyof typeof ui;
  return defaultLang;
}

export function useTranslations(lang: keyof typeof ui) {
  return function t(key: keyof (typeof ui)[typeof defaultLang]) {
    return ui[lang][key] || ui[defaultLang][key];
  };
}
