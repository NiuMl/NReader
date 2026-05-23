const themes: Record<string, {
  prefix: string[]
  settings: string[]
  conflicts: string[]
  resolutions: string[]
  endings: string[]
}> = {
  xianxia: {
    prefix: [
      '苍茫云海间，',
      '昆仑之巅上，',
      '九天星河畔，',
      '蓬莱仙岛中，',
      '蜀山剑派内，',
      '太玄圣地中，',
      '紫霄宫阙里，',
      '混沌秘境中'
    ],
    settings: [
      '灵气充沛，仙鹤翱翔，仙雾缭绕，一派仙家气象。',
      '古木参天，灵泉喷涌，奇花异草遍地皆是。',
      '琼楼玉宇，雕梁画栋，尽显仙家风范。',
      '剑气纵横，符文闪烁，蕴含无尽奥秘。',
      '云海翻腾，日月同辉，天地间一片祥和。'
    ],
    conflicts: [
      '魔族入侵，生灵涂炭，正道岌岌可危。',
      '上古秘境开启，各方势力纷争不断。',
      '仙道衰落，魔修猖獗，天下陷入大乱。',
      '神秘遗迹现世，引得无数修士争夺。',
      '宗门内乱，外敌虎视眈眈，危机四伏。'
    ],
    resolutions: [
      '主角挺身而出，力挽狂澜，拯救苍生。',
      '历经千难万险，终于寻得破局之法。',
      '联合各大宗门，共抗强敌，守护正道。',
      '领悟无上大道，突破境界，斩杀邪魔。',
      '揭开惊天秘密，化解千古谜团。'
    ],
    endings: [
      '最终成就仙位，逍遥天地间。',
      '开创一代传奇，名垂青史。',
      '破碎虚空，追寻更高境界。',
      '归隐山林，逍遥自在。',
      '执掌宗门，传承大道。'
    ]
  },
  wuxia: {
    prefix: [
      '江湖风起云涌，',
      '武林暗流涌动，',
      '大漠孤烟直，',
      '江南烟雨朦胧，',
      '塞北风雪交加，',
      '蜀中栈道险峻，',
      '东海波涛汹涌，',
      '西域黄沙漫天'
    ],
    settings: [
      '刀光剑影，恩怨情仇，尽显江湖本色。',
      '英雄辈出，豪杰并起，谱写壮丽篇章。',
      '门派林立，高手如云，群雄逐鹿天下。',
      '恩怨情仇，血雨腥风，江湖路漫漫。',
      '侠骨柔情，义薄云天，尽显英雄本色。'
    ],
    conflicts: [
      '魔教横行，残害武林，正义之士奋起反抗。',
      '藏宝图现世，引发江湖血雨腥风。',
      '武林大会召开，各方势力暗流涌动。',
      '神秘杀手组织出现，搅乱江湖秩序。',
      '多年恩怨情仇，终须做个了断。'
    ],
    resolutions: [
      '少侠仗剑天涯，惩奸除恶，维护正义。',
      '联合正道力量，共同对抗邪恶势力。',
      '历经生死考验，终成一代大侠。',
      '化解江湖恩怨，开创和平盛世。',
      '揭露惊天阴谋，还武林一片清明。'
    ],
    endings: [
      '成为武林盟主，号令天下群雄。',
      '与心爱之人携手归隐，逍遥江湖。',
      '开创一代武学传奇，流传千古。',
      '云游四海，行侠仗义，快意人生。',
      '功成身退，留下一段传奇佳话。'
    ]
  },
  fantasy: {
    prefix: [
      '魔法大陆上，',
      '巨龙山脉中，',
      '精灵森林深处，',
      '亡灵荒原上，',
      '元素神殿前，',
      '魔法学院内，',
      '暗影王国中，',
      '光明圣城旁'
    ],
    settings: [
      '魔法元素充盈，神秘力量涌动，奇幻世界异彩纷呈。',
      '巨龙翱翔天际，精灵穿梭林间，矮人锻造神兵。',
      '魔法塔高耸入云，传送门连接四方，魔法无处不在。',
      '兽人部落崛起，亡灵军团复苏，大陆风云变幻。',
      '古老遗迹沉睡，神秘力量等待觉醒。'
    ],
    conflicts: [
      '黑暗势力复苏，魔法世界陷入危机。',
      '元素失衡，灾难频发，大陆岌岌可危。',
      '古老封印松动，邪恶力量蠢蠢欲动。',
      '魔法学院遭遇危机，学员们挺身而出。',
      '种族纷争不断，和平岌岌可危。'
    ],
    resolutions: [
      '年轻魔法师踏上征途，寻找拯救世界的方法。',
      '联合各族力量，共同对抗黑暗势力。',
      '揭开古老预言，找到救世之道。',
      '觉醒神秘力量，成为传奇英雄。',
      '重建破碎的魔法世界，带来新的希望。'
    ],
    endings: [
      '成为传奇法师，守护魔法大陆。',
      '建立新的魔法秩序，开创和平时代。',
      '与龙族结为盟友，共同守护大陆。',
      '传承魔法知识，培养新一代魔法师。',
      '踏上新的冒险，探索更广阔的世界。'
    ]
  },
  mystery: {
    prefix: [
      '繁华都市中，',
      '古老庄园里，',
      '迷雾小镇上，',
      '阴森古堡中，',
      '神秘孤岛中，',
      '雨夜公寓里，',
      '列车车厢中，',
      '深山别墅里'
    ],
    settings: [
      '迷雾笼罩，悬疑丛生，真相隐藏在黑暗之中。',
      '神秘事件接连发生，谜团层层递进。',
      '线索扑朔迷离，真相令人难以置信。',
      '阴谋诡计交织，人心叵测难料。',
      '尘封的秘密逐渐揭开，真相令人震惊。'
    ],
    conflicts: [
      '离奇命案发生，侦探展开调查。',
      '失踪案背后隐藏着巨大的阴谋。',
      '神秘组织浮出水面，真相扑朔迷离。',
      '连环杀手作案，警方全力追捕。',
      '尘封多年的悬案重新浮出水面。'
    ],
    resolutions: [
      '侦探抽丝剥茧，逐渐接近真相。',
      '揭开层层迷雾，找到关键线索。',
      '与凶手斗智斗勇，最终将其绳之以法。',
      '揭露背后的阴谋，还受害者一个公道。',
      '解开多年谜团，真相大白于天下。'
    ],
    endings: [
      '罪犯伏法，正义得到伸张。',
      '真相大白，沉冤昭雪。',
      '侦探声名远扬，成为传奇。',
      '案件告破，但新的谜团又悄然浮现。',
      '正义战胜邪恶，世界恢复平静。'
    ]
  }
}

const chapterTemplates = [
  (title: string, chapterNum: number): string => `第${chapterNum}章 ${title}`,
  (title: string, chapterNum: number): string => `第${chapterNum}回 ${title}`,
  (title: string, chapterNum: number): string => `卷${Math.ceil(chapterNum / 10)} 第${chapterNum}章 ${title}`,
  (title: string, chapterNum: number): string => `【第${chapterNum}章】${title}`,
  (title: string, chapterNum: number): string => `Chapter ${chapterNum}: ${title}`
]

const chapterTitles: Record<string, string[]> = {
  xianxia: ['初入仙途', '奇遇连连', '秘境探险', '修为突破', '宗门大会', '外出历练', '遭遇强敌', '获得传承', '秘境寻宝', '突破瓶颈'],
  wuxia: ['初出茅庐', '江湖历练', '结识知己', '遭遇追杀', '习得神功', '报仇雪恨', '威震江湖', '隐姓埋名', '重出江湖', '功成身退'],
  fantasy: ['魔法觉醒', '学院生活', '冒险启程', '结识伙伴', '巨龙传说', '秘境探险', '对抗黑暗', '获得神器', '拯救世界', '新的征程'],
  mystery: ['案件发生', '线索浮现', '疑点重重', '调查深入', '真相渐显', '揭开谜底', '追捕真凶', '水落石出', '正义伸张', '新的挑战']
}

function getTheme(title: string): string {
  const keywords: Record<string, string[]> = {
    xianxia: ['剑', '仙', '道', '修真', '飞升', '灵气', '宗门', '秘境', '渡劫', '神器'],
    wuxia: ['江湖', '侠', '剑', '武功', '门派', '恩怨', '豪杰', '英雄', '秘籍', '盟主'],
    fantasy: ['魔法', '龙', '精灵', '骑士', '王国', '法师', '冒险', '传奇', '史诗', '魔法学院'],
    mystery: ['谜', '案', '侦探', '悬疑', '真相', '凶手', '推理', '破案', '秘密', '线索']
  }
  
  for (const [theme, words] of Object.entries(keywords)) {
    if (words.some(word => title.includes(word))) {
      return theme
    }
  }
  
  return 'fantasy'
}

function getRandomElement<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

function generateParagraph(theme: string, chapterNum: number): string {
  const themeData = themes[theme] || themes.fantasy
  const title = getRandomElement(chapterTitles[theme] || chapterTitles.fantasy)
  
  let paragraph = ''
  
  if (chapterNum === 1) {
    paragraph = `${getRandomElement(themeData.prefix)}${getRandomElement(themeData.settings)}`
  } else if (chapterNum % 5 === 0) {
    paragraph = `${getRandomElement(themeData.prefix)}${getRandomElement(themeData.resolutions)}`
  } else if (chapterNum % 3 === 0) {
    paragraph = `${getRandomElement(themeData.prefix)}${getRandomElement(themeData.conflicts)}`
  } else {
    const contents = [
      '故事从这里开始，主人公踏上了一段不平凡的旅程。',
      '命运的齿轮开始转动，一切都将变得不同。',
      '前路漫漫，充满未知和挑战，但也充满机遇。',
      '在这片神秘的土地上，传奇即将诞生。',
      '主人公将面对种种考验，逐渐成长和蜕变。',
      '每一步都充满艰辛，但也收获满满。',
      '友情、爱情、背叛、忠诚，交织成一幅壮丽的画卷。',
      '在这个世界里，实力决定一切，弱者只能被淘汰。',
      '阴谋与阳谋交织，真相隐藏在重重迷雾之中。',
      '唯有坚持不懈，才能到达彼岸。',
      '强者之路注定孤独，但也充满荣耀。',
      '每一次战斗都是一次成长，每一次挑战都是一次机遇。',
      '在这个充满危险的世界里，只有不断变强才能生存。',
      '机遇总是留给有准备的人，命运掌握在自己手中。',
      '踏上征程，不问归途，只为心中的信念。',
      '经历过风雨才能见到彩虹，经历过磨难才能成长。',
      '在这片广袤的天地间，每个人都有自己的使命。',
      '英雄的传说永不落幕，传奇的故事永远流传。',
      '当黑暗降临，总有人挺身而出，照亮前行的道路。',
      '希望永远存在，只要心中有光，就不怕黑暗。'
    ]
    paragraph = getRandomElement(contents)
  }
  
  return paragraph
}

export function generateNovelContent(title: string): string[] {
  const theme = getTheme(title)
  const lines: string[] = []
  
  lines.push(`《${title}》`)
  lines.push('')
  
  const targetWords = 2000
  let currentWords = title.length + 2
  
  let chapterNum = 1
  while (currentWords < targetWords) {
    const chapterTitle = `${chapterTemplates[chapterNum % chapterTemplates.length](chapterTitles[theme][chapterNum % 10], chapterNum)}`
    lines.push(chapterTitle)
    currentWords += chapterTitle.length
    
    for (let i = 0; i < 4 && currentWords < targetWords; i++) {
      const paragraph = generateParagraph(theme, chapterNum)
      lines.push(paragraph)
      currentWords += paragraph.length
    }
    
    lines.push('')
    currentWords += 2
    chapterNum++
  }
  
  const themeData = themes[theme] || themes.fantasy
  lines.push(getRandomElement(themeData.endings))
  
  return lines
}
