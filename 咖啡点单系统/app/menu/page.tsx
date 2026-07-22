import { prisma } from '@/lib/prisma'
import MenuPageClient from './MenuPageClient'

export const dynamic = 'force-dynamic'

export default async function MenuPage() {
  const items = await prisma.menu.findMany({
    where: { isAvailable: true },
    orderBy: { createdAt: 'asc' },
  })

  // Get unique categories
  const categories = Array.from(new Set(items.map((i) => i.category)))

  return <MenuPageClient items={items} categories={categories} />
}
