export type Language = 'zh' | 'en';

export interface Translation {
  nav: {
    features: string;
    cost: string;
    performance: string;
    deployment: string;
    enterprise: string;
  };
  hero: {
    badge: string;
    title1: string;
    title2: string;
    desc: string;
    download: string;
    getStarted: string;
    requirements: string;
  };
  features: {
    tag: string;
    title: string;
    description: string;
    cards: {
      performance: {
        metric: string;
        name: string;
        text: string;
        tags: string[];
      };
      security: {
        metric: string;
        name: string;
        text: string;
        tags: string[];
      };
      management: {
        metric: string;
        name: string;
        text: string;
        tags: string[];
      };
      cluster: {
        metric: string;
        name: string;
        text: string;
        tags: string[];
      };
      plugin: {
        metric: string;
        name: string;
        text: string;
        tags: string[];
      };
      developer: {
        metric: string;
        name: string;
        text: string;
        tags: string[];
      };
    };
  };
  cost: {
    badge: string;
    title: string;
  };
  team: {
    legacyTag: string;
    smartTag: string;
    legacyRoles: string[];
    smartCaps: string[];
    legacyMeta: string;
    smartMeta: string;
    savingsLabel: string;
  };
  performance: {
    tag: string;
    title: string;
    description: string;
    subscribeTab: string;
    publishTab: string;
    subscribeTitle: string;
    subscribeSubtitle: string;
    publishTitle: string;
    publishSubtitle: string;
    metrics: {
      subscribeThroughput: {
        label: string;
        description: string;
        tag: string;
        compareValue: string;
        compareLabel: string;
      };
      publishThroughput: {
        label: string;
        description: string;
        tag: string;
        compareValue: string;
      };
      latency: {
        label: string;
        description: string;
        tag: string;
        compareValue: string;
      };
      qos1Subscribe: {
        label: string;
        description: string;
        tag: string;
        compareValue: string;
        compareLabel: string;
      };
      qos2Subscribe: {
        label: string;
        description: string;
        tag: string;
        compareValue: string;
        compareLabel: string;
      };
      reliability: {
        label: string;
        description: string;
        tag: string;
        compareValue: string;
      };
    };
    benchmarkEnv: string;
    benchmarkSource: string;
  };
  deployment: {
    tag: string;
    title: string;
    description: string;
    dockerTab: string;
    manualTab: string;
    docker: {
      compose: {
        title: string;
        desc: string;
      };
      direct: {
        title: string;
        desc: string;
        mqttPort: string;
        dashboardPort: string;
      };
    };
    manual: {
      download: {
        title: string;
        desc: string;
      };
      start: {
        title: string;
        desc: string;
        note: string;
      };
    };
    copy: string;
    copied: string;
  };
  verification: {
    tag: string;
    title: string;
    description: string;
    serviceCheck: {
      title: string;
      step1Title: string;
      step2Title: string;
    };
    mqttTest: {
      title: string;
      step1Title: string;
      step2Title: string;
      clientLinks: string;
    };
    dashboard: {
      title: string;
      urlLabel: string;
      usernameLabel: string;
      passwordLabel: string;
      securityNote: string;
    };
  };
  enterprise: {
    tag: string;
    title: string;
    description: string;
    features: {
      support: {
        title: string;
        desc: string;
      };
      custom: {
        title: string;
        desc: string;
      };
      cluster: {
        title: string;
        desc: string;
      };
      sla: {
        title: string;
        desc: string;
      };
    };
    contactBtn: string;
  };
  cta: {
    title: string;
    description: string;
    github: string;
    gitee: string;
    trust1: string;
    trust2: string;
    trust3: string;
    qrTitle: string;
    qrDesc: string;
    qrWorkHours: string;
  };
  footer: {
    brand: string;
    desc: string;
    ecosystem: string;
    developer: string;
    resources: string;
    about: string;
    docs: string;
    plugin: string;
    changelog: string;
    guide: string;
    copyright: string;
    stars: string;
  };
}

export const translations: Record<Language, Translation> = {
  zh: {
    nav: {
      features: '产品特性',
      cost: '成本优势',
      performance: '性能表现',
      deployment: '部署方式',
      enterprise: '企业服务'
    },
    hero: {
      badge: '开源 MQTT Broker',
      title1: '让物联网基建成本',
      title2: '降至原先的 1/10',
      desc: '企业级 MQTT Broker，单机支持 10万+ 设备连接。代码全开源，让企业以更低成本构建高可用物联网基础设施。',
      download: '下载发行包',
      getStarted: '立即开始',
      requirements: '环境要求'
    },
    features: {
      tag: '为什么选择 smart-mqtt',
      title: '企业级 MQTT Broker 的核心能力',
      description: '专为大规模物联网场景打造，提供高性能、高可用、易扩展的消息服务',
      cards: {
        performance: {
          metric: '10万+',
          name: '超高性能',
          text: '单机支持10万+并发连接，毫秒级消息延迟，满足高吞吐场景需求。',
          tags: ['高并发', '毫秒级延迟']
        },
        security: {
          metric: '99.99%',
          name: '企业安全',
          text: '支持 MQTT over TLS/SSL、用户名密码认证、ACL权限控制，确保数据传输安全。',
          tags: ['TLS/SSL', 'ACL控制']
        },
        management: {
          metric: '实时监控',
          name: '可视化管理',
          text: '内置 Web Dashboard，实时监控连接、订阅、消息流量，在线管理客户端会话。',
          tags: ['Dashboard', '实时监控']
        },
        cluster: {
          metric: '百万级',
          name: '水平扩展',
          text: '内置集群插件，支持多节点部署实现百万级并发连接，提供负载均衡和高可用能力，支撑大规模设备接入。',
          tags: ['百万级连接', '高可用']
        },
        plugin: {
          metric: '灵活扩展',
          name: '插件化架构',
          text: '灵活的插件机制，支持认证、桥接、存储、集群等扩展，满足定制化需求。',
          tags: ['认证插件', '桥接插件']
        },
        developer: {
          metric: '多语言 SDK',
          name: '开发友好',
          text: '提供 Java 客户端 SDK，支持 MQTT 3.1/3.1.1/5.0 协议，简单易用的 API 设计。',
          tags: ['Java SDK', 'MQTT 5.0']
        }
      }
    },
    cost: {
      badge: '成本优势',
      title: '实现成本降至原先的'
    },
    team: {
      legacyTag: '传统Broker产品',
      smartTag: 'smart-mqtt',
      legacyRoles: ['产品经理', '后端开发', '前端开发', '测试工程师', '运维工程师', '技术支持'],
      smartCaps: ['全栈开发', '架构设计', '运维部署'],
      legacyMeta: '多角色专业团队',
      smartMeta: 'AI 加持单人模式',
      savingsLabel: '人力成本'
    },
    performance: {
      tag: '性能表现',
      title: '超越同类产品的性能指标',
      description: '经过严格压测验证的数据表现，为大规模物联网场景提供坚实保障',
      subscribeTab: '消息订阅吞吐',
      publishTab: '消息发布吞吐',
      subscribeTitle: '消息订阅吞吐能力对比',
      subscribeSubtitle: '2000订阅者 · 10发布者 · 128主题 · 128字节payload',
      publishTitle: '消息发布吞吐能力',
      publishSubtitle: '2000发布者 · 128主题 · 128字节payload',
      metrics: {
        subscribeThroughput: {
          label: '峰值订阅吞吐',
          description: 'QoS 0 · 2000订阅者 · 10发布者',
          tag: '订阅场景',
          compareValue: '+25%',
          compareLabel: 'vs 其他Broker'
        },
        publishThroughput: {
          label: '峰值发布吞吐',
          description: 'QoS 0 · 2000发布者',
          tag: '发布场景',
          compareValue: '行业领先'
        },
        latency: {
          label: '平均消息延迟',
          description: 'P99 延迟 · 消息投递速度',
          tag: '延迟表现',
          compareValue: '毫秒级响应'
        },
        qos1Subscribe: {
          label: '可靠订阅吞吐',
          description: 'AtLeastOnce 消息质量保证',
          tag: 'QoS 1',
          compareValue: '+35%',
          compareLabel: 'vs 其他Broker'
        },
        qos2Subscribe: {
          label: '最高可靠吞吐',
          description: 'ExactlyOnce 消息质量保证',
          tag: 'QoS 2',
          compareValue: '+60%',
          compareLabel: 'vs 其他Broker'
        },
        reliability: {
          label: '消息投递成功率',
          description: 'QoS 1 & 2 消息保证',
          tag: '可靠性',
          compareValue: '企业级'
        }
      },
      benchmarkEnv: '测试环境：8核16G 云服务器 · 2000 并发连接 · 128 字节 payload',
      benchmarkSource: '数据来源于 bench-plugin 官方压测插件 · 支持横向对比测试 EMQX、Mosquitto 等主流 Broker'
    },
    deployment: {
      tag: '快速部署',
      title: '选择适合你的部署方式',
      description: '提供 Docker 和手动部署两种方式，5 分钟内即可启动服务',
      dockerTab: 'Docker 部署（推荐）',
      manualTab: '手动部署',
      docker: {
        compose: {
          title: '使用 Docker Compose',
          desc: '最简单的部署方式，一键启动完整服务'
        },
        direct: {
          title: '直接运行 Docker',
          desc: '快速启动单个容器',
          mqttPort: 'MQTT 协议端口',
          dashboardPort: 'Dashboard 端口'
        }
      },
      manual: {
        download: {
          title: '下载发行包',
          desc: '从 Gitee 或 GitHub Releases 下载最新版本'
        },
        start: {
          title: '启动服务',
          desc: '执行启动脚本运行 smart-mqtt',
          note: '确保已安装 JDK 8 或更高版本'
        }
      },
      copy: '复制',
      copied: '已复制'
    },
    verification: {
      tag: '验证部署',
      title: '验证服务运行状态',
      description: '通过多种方式验证 smart-mqtt 是否成功运行',
      serviceCheck: {
        title: '检查服务状态',
        step1Title: '查看进程',
        step2Title: '查看端口监听'
      },
      mqttTest: {
        title: 'MQTT 客户端测试',
        step1Title: '订阅主题（终端 1）',
        step2Title: '发布消息（终端 2）',
        clientLinks: '推荐客户端：'
      },
      dashboard: {
        title: '访问 Dashboard（企业版）',
        urlLabel: '访问地址',
        usernameLabel: '默认账号',
        passwordLabel: '默认密码',
        securityNote: '出于安全考虑，生产环境请尽快修改成安全性更高的密码'
      }
    },
    enterprise: {
      tag: '企业服务',
      title: '为大型企业提供专业服务',
      description: '企业版在专业版基础上，提供更高级的功能和专属技术支持',
      features: {
        support: {
          title: '专属技术支持',
          desc: '7x24小时响应，专属技术顾问一对一服务'
        },
        custom: {
          title: '定制化开发',
          desc: '根据业务需求定制功能，满足特殊场景需求'
        },
        cluster: {
          title: '集群部署方案',
          desc: '专业的集群架构设计，支持高可用负载均衡'
        },
        sla: {
          title: 'SLA 服务保障',
          desc: '99.99% 服务可用性保障，完善的灾备方案'
        }
      },
      contactBtn: '联系我们获取方案'
    },
    cta: {
      title: '代码开源，即刻体验',
      description: 'AGPL 协议开源，商业授权可选。Docker 一键部署，5分钟完成接入。',
      github: '访问 GitHub',
      gitee: '访问 Gitee',
      trust1: '企业级服务保障',
      trust2: '专业技术服务',
      trust3: '社区支持',
      qrTitle: '联系销售团队',
      qrDesc: '扫描二维码，添加微信咨询',
      qrWorkHours: '工作时间：周一至周五 9:00-18:00'
    },
    footer: {
      brand: 'smart-mqtt',
      desc: '企业级物联网消息中间件',
      ecosystem: '开源生态',
      developer: '开发者',
      resources: '资源',
      about: '关于',
      docs: '使用文档',
      plugin: '关于Plugin',
      changelog: '更新日志',
      guide: '选型指南',
      copyright: '© 2024 smartboot. All rights reserved.',
      stars: 'Stars'
    }
  },
  en: {
    nav: {
      features: 'Features',
      cost: 'Cost',
      performance: 'Performance',
      deployment: 'Deployment',
      enterprise: 'Enterprise'
    },
    hero: {
      badge: 'Open Source MQTT Broker',
      title1: 'Reduce IoT Infrastructure Cost',
      title2: 'to 1/10 of Original',
      desc: 'Enterprise-grade MQTT Broker supporting 100K+ concurrent connections. Fully open-source, enabling enterprises to build high-availability IoT infrastructure at lower cost.',
      download: 'Download',
      getStarted: 'Get Started',
      requirements: 'Requirements'
    },
    features: {
      tag: 'Why Choose smart-mqtt',
      title: 'Core Capabilities of Enterprise MQTT Broker',
      description: 'Built for large-scale IoT scenarios, providing high-performance, high-availability, and scalable messaging services',
      cards: {
        performance: {
          metric: '100K+',
          name: 'Ultra Performance',
          text: 'Supports 100K+ concurrent connections on a single node with millisecond latency, meeting high-throughput requirements.',
          tags: ['High Concurrency', 'Millisecond Latency']
        },
        security: {
          metric: '99.99%',
          name: 'Enterprise Security',
          text: 'Supports MQTT over TLS/SSL, username/password authentication, and ACL permission control to ensure secure data transmission.',
          tags: ['TLS/SSL', 'ACL Control']
        },
        management: {
          metric: 'Real-time',
          name: 'Visual Management',
          text: 'Built-in Web Dashboard for real-time monitoring of connections, subscriptions, and message traffic, with online client session management.',
          tags: ['Dashboard', 'Real-time Monitoring']
        },
        cluster: {
          metric: 'Million+',
          name: 'Horizontal Scaling',
          text: 'Built-in cluster plugin supports multi-node deployment for million-level concurrent connections, providing load balancing and high availability for large-scale device access.',
          tags: ['Million Connections', 'High Availability']
        },
        plugin: {
          metric: 'Flexible',
          name: 'Plugin Architecture',
          text: 'Flexible plugin mechanism supporting authentication, bridging, storage, and cluster extensions to meet customization needs.',
          tags: ['Auth Plugin', 'Bridge Plugin']
        },
        developer: {
          metric: 'Multi-language SDK',
          name: 'Developer Friendly',
          text: 'Provides Java client SDK with support for MQTT 3.1/3.1.1/5.0 protocols and simple, easy-to-use API design.',
          tags: ['Java SDK', 'MQTT 5.0']
        }
      }
    },
    cost: {
      badge: 'Cost Advantage',
      title: 'Reduce Implementation Cost to'
    },
    team: {
      legacyTag: 'Traditional Broker',
      smartTag: 'smart-mqtt',
      legacyRoles: ['Product Manager', 'Backend Dev', 'Frontend Dev', 'QA Engineer', 'DevOps', 'Tech Support'],
      smartCaps: ['Full-stack Dev', 'Architecture', 'Deployment'],
      legacyMeta: 'Multi-role Professional Team',
      smartMeta: 'AI-Powered Single Person Mode',
      savingsLabel: 'Labor Cost'
    },
    performance: {
      tag: 'Performance',
      title: 'Performance Metrics Beyond Competitors',
      description: 'Rigorously tested data performance, providing solid guarantees for large-scale IoT scenarios',
      subscribeTab: 'Subscribe Throughput',
      publishTab: 'Publish Throughput',
      subscribeTitle: 'Message Subscribe Throughput Comparison',
      subscribeSubtitle: '2000 subscribers · 10 publishers · 128 topics · 128 bytes payload',
      publishTitle: 'Message Publish Throughput',
      publishSubtitle: '2000 publishers · 128 topics · 128 bytes payload',
      metrics: {
        subscribeThroughput: {
          label: 'Peak Subscribe Throughput',
          description: 'QoS 0 · 2000 subscribers · 10 publishers',
          tag: 'Subscribe Scenario',
          compareValue: '+25%',
          compareLabel: 'vs Other Brokers'
        },
        publishThroughput: {
          label: 'Peak Publish Throughput',
          description: 'QoS 0 · 2000 publishers',
          tag: 'Publish Scenario',
          compareValue: 'Industry Leading'
        },
        latency: {
          label: 'Average Message Latency',
          description: 'P99 Latency · Message Delivery Speed',
          tag: 'Latency Performance',
          compareValue: 'Millisecond Response'
        },
        qos1Subscribe: {
          label: 'Reliable Subscribe Throughput',
          description: 'AtLeastOnce Message Quality Guarantee',
          tag: 'QoS 1',
          compareValue: '+35%',
          compareLabel: 'vs Other Brokers'
        },
        qos2Subscribe: {
          label: 'Highest Reliable Throughput',
          description: 'ExactlyOnce Message Quality Guarantee',
          tag: 'QoS 2',
          compareValue: '+60%',
          compareLabel: 'vs Other Brokers'
        },
        reliability: {
          label: 'Message Delivery Success Rate',
          description: 'QoS 1 & 2 Message Guarantee',
          tag: 'Reliability',
          compareValue: 'Enterprise-grade'
        }
      },
      benchmarkEnv: 'Test Environment: 8-core 16G Cloud Server · 2000 Concurrent Connections · 128 bytes payload',
      benchmarkSource: 'Data from bench-plugin official benchmark plugin · Supports cross-comparison testing with mainstream brokers like EMQX, Mosquitto'
    },
    deployment: {
      tag: 'Quick Deployment',
      title: 'Choose Your Deployment Method',
      description: 'Docker and manual deployment options, start service within 5 minutes',
      dockerTab: 'Docker Deployment (Recommended)',
      manualTab: 'Manual Deployment',
      docker: {
        compose: {
          title: 'Use Docker Compose',
          desc: 'Simplest deployment method, start complete service with one command'
        },
        direct: {
          title: 'Run Docker Directly',
          desc: 'Quickly start a single container',
          mqttPort: 'MQTT Protocol Port',
          dashboardPort: 'Dashboard Port'
        }
      },
      manual: {
        download: {
          title: 'Download Release Package',
          desc: 'Download latest version from Gitee or GitHub Releases'
        },
        start: {
          title: 'Start Service',
          desc: 'Run startup script to start smart-mqtt',
          note: 'Ensure JDK 8 or higher is installed'
        }
      },
      copy: 'Copy',
      copied: 'Copied'
    },
    verification: {
      tag: 'Verify Deployment',
      title: 'Verify Service Status',
      description: 'Verify smart-mqtt is running successfully through multiple methods',
      serviceCheck: {
        title: 'Check Service Status',
        step1Title: 'View Process',
        step2Title: 'View Port Listening'
      },
      mqttTest: {
        title: 'MQTT Client Test',
        step1Title: 'Subscribe to Topic (Terminal 1)',
        step2Title: 'Publish Message (Terminal 2)',
        clientLinks: 'Recommended Clients:'
      },
      dashboard: {
        title: 'Access Dashboard (Enterprise Edition)',
        urlLabel: 'Access URL',
        usernameLabel: 'Default Username',
        passwordLabel: 'Default Password',
        securityNote: 'For security, please change to a more secure password in production environment'
      }
    },
    enterprise: {
      tag: 'Enterprise Services',
      title: 'Professional Services for Large Enterprises',
      description: 'Enterprise edition provides advanced features and dedicated technical support',
      features: {
        support: {
          title: 'Dedicated Technical Support',
          desc: '7x24 response, dedicated technical consultant one-on-one service'
        },
        custom: {
          title: 'Custom Development',
          desc: 'Custom features based on business needs, meeting special scenario requirements'
        },
        cluster: {
          title: 'Cluster Deployment Solution',
          desc: 'Professional cluster architecture design, supporting high availability load balancing'
        },
        sla: {
          title: 'SLA Service Guarantee',
          desc: '99.99% service availability guarantee, comprehensive disaster recovery solution'
        }
      },
      contactBtn: 'Contact Us for Solution'
    },
    cta: {
      title: 'Open Source Code, Experience Now',
      description: 'AGPL open source license, optional commercial license. Docker one-click deployment, complete integration in 5 minutes.',
      github: 'Visit GitHub',
      gitee: 'Visit Gitee',
      trust1: 'Enterprise-grade Service Guarantee',
      trust2: 'Professional Technical Service',
      trust3: 'Community Support',
      qrTitle: 'Contact Sales Team',
      qrDesc: 'Scan QR code to add WeChat for consultation',
      qrWorkHours: 'Working Hours: Monday to Friday 9:00-18:00'
    },
    footer: {
      brand: 'smart-mqtt',
      desc: 'Enterprise IoT Message Middleware',
      ecosystem: 'Open Source Ecosystem',
      developer: 'Developer',
      resources: 'Resources',
      about: 'About',
      docs: 'Documentation',
      plugin: 'About Plugin',
      changelog: 'Changelog',
      guide: 'Selection Guide',
      copyright: '© 2024 smartboot. All rights reserved.',
      stars: 'Stars'
    }
  }
};

export function getTranslation(lang: Language): Translation {
  return translations[lang];
}
