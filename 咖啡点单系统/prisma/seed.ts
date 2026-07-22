import { PrismaClient } from '@prisma/client'

const menuItems = [
  {
    name: '经典美式',
    category: 'coffee',
    price: 18,
    imageUrl: 'https://images.unsplash.com/photo-1551030173-122aabc4489c?w=400&h=400&fit=crop',
    description: '精选阿拉比卡咖啡豆，热水缓慢萃取，口感清爽纯粹',
    isAvailable: true,
  },
  {
    name: '拿铁咖啡',
    category: 'coffee',
    price: 22,
    imageUrl: 'https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?w=400&h=400&fit=crop',
    description: '浓缩咖啡与蒸汽牛奶的完美融合，丝滑醇香',
    isAvailable: true,
  },
  {
    name: '卡布奇诺',
    category: 'coffee',
    price: 24,
    imageUrl: 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=400&h=400&fit=crop',
    description: '意式浓缩打底，厚实奶泡覆盖，口感绵密丰富',
    isAvailable: true,
  },
  {
    name: '抹茶拿铁',
    category: 'tea',
    price: 26,
    imageUrl: 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=400&h=400&fit=crop',
    description: '日本宇治抹茶搭配绵密奶泡，清新回甘',
    isAvailable: true,
  },
  {
    name: '伯爵红茶',
    category: 'tea',
    price: 16,
    imageUrl: 'https://images.unsplash.com/photo-1597318181409-cf64d0b5d8a2?w=400&h=400&fit=crop',
    description: '经典英式伯爵茶，佛手柑香气悠然',
    isAvailable: true,
  },
  {
    name: '黄油可颂',
    category: 'pastry',
    price: 15,
    imageUrl: 'https://images.unsplash.com/photo-1623334044303-241021148842?w=400&h=400&fit=crop',
    description: '法式酥脆可颂，层层黄油香气，外酥内软',
    isAvailable: true,
  },
  {
    name: '提拉米苏',
    category: 'pastry',
    price: 28,
    imageUrl: 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=400&h=400&fit=crop',
    description: '经典意式甜点，马斯卡彭芝士与咖啡的完美邂逅',
    isAvailable: true,
  },
  {
    name: '冷萃冰咖啡',
    category: 'special',
    price: 28,
    imageUrl: 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=400&h=400&fit=crop',
    description: '12小时低温冷萃，口感顺滑，酸度极低',
    isAvailable: true,
  },
]

const prisma = new PrismaClient()

async function main() {
  console.log('Seeding database...')

  for (const item of menuItems) {
    await prisma.menu.upsert({
      where: { id: menuItems.indexOf(item) + 1 },
      update: item,
      create: item,
    })
  }

  console.log(`Seeded ${menuItems.length} menu items`)
}

main()
  .catch((e) => {
    console.error(e)
    process.exit(1)
  })
  .finally(async () => {
    await prisma.$disconnect()
  })
