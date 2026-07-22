import { NextRequest, NextResponse } from 'next/server'
import { getLoginPhone } from '@/lib/auth'
import { getOrCreateCart } from '@/lib/cart'
import { prisma } from '@/lib/prisma'

export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  const phone = await getLoginPhone()
  const sessionId = req.headers.get('x-session-id') || 'default'
  const { quantity } = await req.json()
  const itemId = parseInt(params.id)

  const cart = await getOrCreateCart(phone, sessionId)
  const item = await prisma.cartItem.findFirst({ where: { id: itemId, cartId: cart.id } })
  if (!item) return NextResponse.json({ error: '购物车项不存在' }, { status: 404 })

  if (quantity <= 0) {
    await prisma.cartItem.delete({ where: { id: itemId } })
  } else {
    await prisma.cartItem.update({ where: { id: itemId }, data: { quantity } })
  }

  await prisma.cart.update({ where: { id: cart.id }, data: { updatedAt: new Date() } })
  return NextResponse.json({ ok: true })
}

export async function DELETE(_req: NextRequest, { params }: { params: { id: string } }) {
  const itemId = parseInt(params.id)
  const item = await prisma.cartItem.findUnique({ where: { id: itemId } })
  if (!item) return NextResponse.json({ error: '购物车项不存在' }, { status: 404 })

  const cart = await prisma.cart.findUnique({ where: { id: item.cartId } })
  if (cart) {
    await prisma.cart.update({ where: { id: cart.id }, data: { updatedAt: new Date() } })
  }
  await prisma.cartItem.delete({ where: { id: itemId } })
  return NextResponse.json({ ok: true })
}
