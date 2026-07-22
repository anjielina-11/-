import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  const item = await prisma.menu.findUnique({ where: { id: parseInt(params.id) } })
  if (!item) return NextResponse.json({ error: '商品不存在' }, { status: 404 })
  return NextResponse.json(item)
}
