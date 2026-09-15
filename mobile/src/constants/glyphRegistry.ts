// 单色品牌字形注册表。
//
// 这些字形来自 simple-icons（monochrome，24×24 viewBox），构建期打包进产物，
// 运行时零网络请求。每个条目给出 slug（对应 assets/brands-glyph/{slug}.svg）、
// 中文/英文别名（用于把用户输入的公司名匹配到字形）、以及品牌主色（字形衬在品牌色底上）。
//
// 维护：要加一家公司，从 simple-icons 下载 {slug}.svg 丢进 brands-glyph/，
// 再在这里加一行即可；不需要改任何组件。

export interface GlyphEntry {
  slug: string
  /** 别名：用户输入里出现任意一个即命中。中英文都写，覆盖「腾讯」与「Tencent」两种输入。 */
  keys: string[]
  /** 品牌主色，用作芯片底色；字形用白/深色衬在上面。 */
  color: string
  /**
   * 字形前景色覆盖（可选）。整体填充型的 App 图标（path 铺满整个 viewBox、
   * 细节靠镂空呈现，如美团）必须强制白色——它们的「底色」由芯片透出，
   * 若按芯片亮度走通用明暗规则会被染成深色，黄底上就成了黑块。
   */
  fg?: string
}

export const GLYPH_REGISTRY: GlyphEntry[] = [
  { slug: 'accenture', keys: ['埃森哲', 'Accenture'], color: '#A100FF' },
  { slug: 'adidas', keys: ['阿迪达斯', 'Adidas'], color: '#000000' },
  { slug: 'adobe', keys: ['Adobe'], color: '#FF0000' },
  { slug: 'airbnb', keys: ['爱彼迎', 'Airbnb'], color: '#FF5A5F' },
  { slug: 'alibabacloud', keys: ['阿里云', 'Alibaba Cloud', 'AlibabaCloud'], color: '#FF6A00' },
  { slug: 'amazon', keys: ['亚马逊', 'Amazon'], color: '#FF9900' },
  { slug: 'amd', keys: ['AMD'], color: '#ED1C24' },
  { slug: 'americanexpress', keys: ['美国运通', 'American Express', 'Amex'], color: '#2E77BC' },
  { slug: 'apple', keys: ['苹果', 'Apple'], color: '#1D1D1F' },
  { slug: 'atlassian', keys: ['Atlassian'], color: '#0052CC' },
  { slug: 'audi', keys: ['奥迪', 'Audi'], color: '#BB0A30' },
  { slug: 'baidu', keys: ['百度', 'Baidu'], color: '#2932E1' },
  { slug: 'bankofamerica', keys: ['美国银行', 'Bank of America'], color: '#000000' },
  { slug: 'barclays', keys: ['巴克莱', 'Barclays'], color: '#00A6A6' },
  { slug: 'bilibili', keys: ['哔哩哔哩', 'B站', 'bilibili', 'Bilibili'], color: '#FB7299' },
  { slug: 'bmw', keys: ['宝马', 'BMW'], color: '#0066B1' },
  { slug: 'bytedance', keys: ['字节跳动', '抖音', '今日头条', 'TikTok', 'ByteDance', 'Douyin'], color: '#00C8D2' },
  { slug: 'cisco', keys: ['思科', 'Cisco'], color: '#1BA0D7' },
  { slug: 'cloudflare', keys: ['Cloudflare'], color: '#F38020' },
  { slug: 'cocacola', keys: ['可口可乐', 'Coca-Cola', 'CocaCola'], color: '#F40009' },
  { slug: 'coinbase', keys: ['Coinbase'], color: '#0052FF' },
  { slug: 'databricks', keys: ['Databricks'], color: '#FF3621' },
  { slug: 'dell', keys: ['戴尔', 'Dell'], color: '#0085C3' },
  { slug: 'discord', keys: ['Discord'], color: '#5865F2' },
  { slug: 'dji', keys: ['大疆', 'DJI'], color: '#111111' },
  { slug: 'dropbox', keys: ['Dropbox'], color: '#0061FF' },
  { slug: 'ebay', keys: ['eBay', 'Ebay'], color: '#E53238' },
  { slug: 'elastic', keys: ['Elastic'], color: '#FEC514' },
  { slug: 'ericsson', keys: ['爱立信', 'Ericsson'], color: '#0064A4' },
  { slug: 'expedia', keys: ['Expedia'], color: '#F36B08' },
  { slug: 'figma', keys: ['Figma'], color: '#F24E1E' },
  { slug: 'ford', keys: ['福特', 'Ford'], color: '#003478' },
  { slug: 'generalmotors', keys: ['通用汽车', 'General Motors', 'GM'], color: '#000000' },
  { slug: 'github', keys: ['GitHub', 'Github'], color: '#181717' },
  { slug: 'gitlab', keys: ['GitLab', 'Gitlab'], color: '#FC6D26' },
  { slug: 'goldmansachs', keys: ['高盛', 'Goldman Sachs', 'GoldmanSachs'], color: '#E8932B' },
  { slug: 'google', keys: ['谷歌', 'Google'], color: '#4285F4' },
  { slug: 'honda', keys: ['本田', 'Honda'], color: '#CC0000' },
  { slug: 'hp', keys: ['惠普', 'HP', 'Hewlett-Packard'], color: '#0096D6' },
  { slug: 'hsbc', keys: ['汇丰', 'HSBC'], color: '#DB0011' },
  { slug: 'huawei', keys: ['华为', 'Huawei'], color: '#CF0A2C' },
  { slug: 'ikea', keys: ['宜家', 'IKEA'], color: '#0058A3' },
  { slug: 'intel', keys: ['英特尔', 'Intel'], color: '#0071C5' },
  { slug: 'jetbrains', keys: ['JetBrains'], color: '#000000' },
  { slug: 'kuaishou', keys: ['快手', 'Kuaishou'], color: '#FF4906' },
  { slug: 'lenovo', keys: ['联想', 'Lenovo'], color: '#E60012' },
  { slug: 'lg', keys: ['LG'], color: '#A50034' },
  { slug: 'linkedin', keys: ['领英', 'LinkedIn', 'Linkedin'], color: '#0A66C2' },
  { slug: 'lyft', keys: ['Lyft'], color: '#00C2A8' },
  { slug: 'mastercard', keys: ['万事达', 'Mastercard', 'MasterCard'], color: '#EB001B' },
  { slug: 'mcdonalds', keys: ['麦当劳', 'McDonald', 'McDonald’s', 'McDonalds'], color: '#FFC72C' },
  { slug: 'meituan', keys: ['美团', '大众点评', 'Meituan'], color: '#FFC300', fg: '#ffffff' },
  { slug: 'mercedes', keys: ['梅赛德斯', '奔驰', 'Mercedes', 'Mercedes-Benz'], color: '#111111' },
  { slug: 'meta', keys: ['脸书', 'Facebook', 'Meta'], color: '#0866FF' },
  { slug: 'mihoyo', keys: ['米哈游', 'miHoYo', 'Mihoyo'], color: '#33CCFF' },
  { slug: 'mongodb', keys: ['MongoDB'], color: '#47A248' },
  { slug: 'netflix', keys: ['奈飞', 'Netflix'], color: '#E50914' },
  { slug: 'nike', keys: ['耐克', 'Nike'], color: '#111111' },
  { slug: 'nokia', keys: ['诺基亚', 'Nokia'], color: '#124191' },
  { slug: 'notion', keys: ['Notion'], color: '#111111' },
  { slug: 'nvidia', keys: ['英伟达', 'NVIDIA', 'Nvidia'], color: '#76B900' },
  { slug: 'oppo', keys: ['OPPO', 'OPPO'], color: '#2E7CEF' },
  { slug: 'oracle', keys: ['甲骨文', 'Oracle'], color: '#F80000' },
  { slug: 'panasonic', keys: ['松下', 'Panasonic'], color: '#0C5AAB' },
  { slug: 'paypal', keys: ['贝宝', 'PayPal', 'Paypal'], color: '#003087' },
  { slug: 'qualcomm', keys: ['高通', 'Qualcomm'], color: '#3253DC' },
  { slug: 'redhat', keys: ['红帽', 'Red Hat', 'RedHat'], color: '#EE0000' },
  { slug: 'redis', keys: ['Redis'], color: '#DC382D' },
  { slug: 'salesforce', keys: ['赛富时', 'Salesforce'], color: '#00A1E0' },
  { slug: 'samsung', keys: ['三星', 'Samsung'], color: '#1428A0' },
  { slug: 'sap', keys: ['SAP'], color: '#0FAAFF' },
  { slug: 'shopify', keys: ['Shopify'], color: '#95BF47' },
  { slug: 'slack', keys: ['Slack'], color: '#4A154B' },
  { slug: 'snowflake', keys: ['Snowflake'], color: '#29B5E8' },
  { slug: 'sony', keys: ['索尼', 'Sony'], color: '#000000' },
  { slug: 'spotify', keys: ['声田', 'Spotify'], color: '#1DB954' },
  { slug: 'starbucks', keys: ['星巴克', 'Starbucks'], color: '#00704A' },
  { slug: 'stripe', keys: ['Stripe'], color: '#635BFF' },
  { slug: 'tesla', keys: ['特斯拉', 'Tesla'], color: '#E82127' },
  { slug: 'toshiba', keys: ['东芝', 'Toshiba'], color: '#A31E00' },
  { slug: 'toyota', keys: ['丰田', 'Toyota'], color: '#EB0A1E' },
  { slug: 'twitch', keys: ['Twitch'], color: '#9146FF' },
  { slug: 'uber', keys: ['优步', 'Uber'], color: '#000000' },
  { slug: 'unilever', keys: ['联合利华', 'Unilever'], color: '#1F36C7' },
  { slug: 'visa', keys: ['维萨', 'Visa'], color: '#1A1F71' },
  { slug: 'vmware', keys: ['VMware'], color: '#607078' },
  { slug: 'volkswagen', keys: ['大众', 'Volkswagen'], color: '#041E42' },
  { slug: 'wellsfargo', keys: ['富国银行', 'Wells Fargo', 'WellsFargo'], color: '#D81E05' },
  { slug: 'x', keys: ['X', 'Twitter'], color: '#000000' },
  { slug: 'xiaohongshu', keys: ['小红书', 'RED', 'Xiaohongshu'], color: '#FF2442' },
  { slug: 'xiaomi', keys: ['小米', '红米', 'Xiaomi'], color: '#FF6900' },
  { slug: 'zoom', keys: ['Zoom', '瞩目'], color: '#2D8CFF' },
]
