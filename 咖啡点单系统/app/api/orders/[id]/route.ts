import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  const order = await prisma.order.findUnique({
    where: { id: parseInt(params.id) },
    include: { items: { include: { menu: true } } },
  })
  if (!order) return NextResponse.json({ error: '订单不存在' }, { status: 404 })
  return NextResponse.json(order)
}

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const { status } = await req.json()
  const validStatuses = ['pending', 'paid', 'making', 'done']
  if (!validStatuses.includes(status)) {
    return NextResponse.json({ error: '无效状态' }, { status: 400 })
  }

  const order = await prisma.order.update({
    where: { id: parseInt(params.id) },
    data: { status },
    include: { items: true },
  })
  return NextResponse.json(order)
}
