import { prisma } from '@/lib/prisma'
import MenuDetailClient from './MenuDetailClient'
import { notFound } from 'next/navigation'

export const dynamic = 'force-dynamic'

export default async function MenuDetailPage({ params }: { params: { id: string } }) {
  const item = await prisma.menu.findUnique({ where: { id: parseInt(params.id) } })
  if (!item || !item.isAvailable) notFound()

  return <MenuDetailClient item={item} />
}
