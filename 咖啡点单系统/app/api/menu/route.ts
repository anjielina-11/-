import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function GET(req: NextRequest) {
  const category = req.nextUrl.searchParams.get('category')
  const where = category && category !== 'all' ? { category, isAvailable: true } : { isAvailable: true }
  const items = await prisma.menu.findMany({ where, orderBy: { createdAt: 'asc' } })
  return NextResponse.json(items)
}
