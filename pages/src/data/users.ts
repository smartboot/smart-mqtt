/**
 * 企业用户案例数据
 * 
 * 添加新企业只需在数组中添加新对象即可
 * iconType 可选值: 'sensor' | 'farm' | 'data' | 'tech' | 'logistics' | 'automotive' | 'custom'
 * 如果选择 'custom'，需要提供 customIcon SVG 路径
 */
export interface UserCompany {
  name: string;
  fullName: string;
  iconType?: 'sensor' | 'farm' | 'data' | 'tech' | 'logistics' | 'automotive' | 'custom';
  customIcon?: string;
}

export const userCompanies: UserCompany[] = [
  {
    name: '深圳市比亚迪锂电池有限公司坑梓分公司',
    fullName: '深圳市比亚迪锂电池有限公司坑梓分公司',
    customIcon: '/smart-mqtt/user_byd.jpg'
  },
  {
    name: '上海极锐星瀚传感',
    fullName: '上海极锐星瀚传感技术有限公司',
    iconType: 'sensor'
  },
  {
    name: '宿迁市裕百信养殖',
    fullName: '宿迁市裕百信养殖服务有限公司',
    iconType: 'farm'
  },
  {
    name: '数研院(云南)信息',
    fullName: '数研院（云南）信息产业发展有限公司',
    iconType: 'data'
  },
  {
    name: '武汉睿友科技',
    fullName: '武汉睿友科技有限公司',
    iconType: 'tech'
  },
  {
    name: '顺丰科技',
    fullName: '顺丰科技有限公司',
    iconType: 'logistics'
  }
];

/**
 * 根据图标类型获取 SVG 内容
 */
export function getIconSvg(iconType: string): string {
  const icons: Record<string, string> = {
    sensor: `
      <circle cx="15" cy="20" r="8" fill="none" stroke="currentColor" stroke-width="2"/>
      <line x1="15" y1="12" x2="15" y2="20" stroke="currentColor" stroke-width="2"/>
      <line x1="15" y1="20" x2="21" y2="20" stroke="currentColor" stroke-width="2"/>
      <circle cx="15" cy="20" r="2" fill="currentColor"/>
    `,
    farm: `
      <ellipse cx="18" cy="20" rx="12" ry="10" fill="none" stroke="currentColor" stroke-width="2"/>
      <path d="M12 16 Q18 12 24 16" stroke="currentColor" stroke-width="2" fill="none"/>
    `,
    data: `
      <rect x="5" y="10" width="10" height="20" rx="2"/>
      <rect x="18" y="15" width="10" height="15" rx="2"/>
      <rect x="31" y="5" width="10" height="25" rx="2"/>
    `,
    tech: `
      <path d="M5 25 L15 15 L25 25 L15 35 Z" fill="none" stroke="currentColor" stroke-width="2"/>
      <circle cx="15" cy="25" r="3"/>
    `,
    logistics: `
      <path d="M5 15 L15 15 L15 25 L25 25" stroke="currentColor" stroke-width="2" fill="none"/>
      <circle cx="28" cy="25" r="3"/>
      <path d="M10 20 L20 20" stroke="currentColor" stroke-width="2"/>
    `,
    automotive: `
      <rect x="5" y="12" width="16" height="16" rx="3"/>
      <circle cx="9" cy="28" r="3" fill="none" stroke="currentColor" stroke-width="1.5"/>
      <circle cx="17" cy="28" r="3" fill="none" stroke="currentColor" stroke-width="1.5"/>
    `
  };
  
  return icons[iconType] || icons.tech;
}