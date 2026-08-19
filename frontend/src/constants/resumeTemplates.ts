export interface ResumeTemplateOption {
  key: string
  label: string
  badge: string
  description: string
}

export const resumeTemplateOptions: ResumeTemplateOption[] = [
  {
    key: 'classic',
    label: '经典正式',
    badge: '正式',
    description: '稳重的黑白结构，适合校招、国企和传统岗位投递。',
  },
  {
    key: 'blue',
    label: '科技蓝',
    badge: '推荐',
    description: '蓝色强调线突出技术层级，适合后端、算法和工程岗位。',
  },
  {
    key: 'minimal',
    label: '极简留白',
    badge: '清爽',
    description: '减少装饰，信息密度高，适合经历较多的一页简历。',
  },
  {
    key: 'emerald',
    label: '墨绿专业',
    badge: '沉稳',
    description: '绿色强调行动感，与职达工作台主色保持一致。',
  },
  {
    key: 'graphite',
    label: '石墨灰',
    badge: '克制',
    description: '低饱和灰阶风格，适合产品、运营和综合岗位。',
  },
  {
    key: 'sidebar',
    label: '左栏信息',
    badge: '双栏',
    description: '侧栏突出个人信息和技能，主体承载项目与经历。',
  },
  {
    key: 'compact',
    label: '紧凑工程',
    badge: '高密',
    description: '压缩间距和字号，让技术经历、项目亮点更集中。',
  },
  {
    key: 'elegant',
    label: '雅致 serif',
    badge: '精致',
    description: '标题使用衬线气质，适合研究、文案和偏表达型岗位。',
  },
  {
    key: 'warm',
    label: '暖色亲和',
    badge: '亲和',
    description: '暖棕强调稳定与沟通感，适合教育、咨询和运营类场景。',
  },
  {
    key: 'terminal',
    label: '开发者黑',
    badge: '酷炫',
    description: '深色边框与代码感强调工程身份，适合开发者作品展示。',
  },
]

export const defaultResumeTemplateKey = 'blue'

export function isValidResumeTemplateKey(templateKey: string) {
  return resumeTemplateOptions.some((template) => template.key === templateKey)
}
