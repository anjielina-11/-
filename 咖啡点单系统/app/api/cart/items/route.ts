import { NextRequest, NextResponse } from 'next/server'
import { getLoginPhone } from '@/lib/auth'
import { getOrCreateCart, calcUnitPrice } from '@/lib/cart'
import { prisma } from '@/lib/prisma'

export async function POST(req: NextRequest) {
  const phone = await getLoginPhone()
  const sessionId = req.headers.get('x-session-id') || 'default'
  const { menuId, cupSize, temperature } = await req.json()

  const menu = await prisma.menu.findUnique({ where: { id: menuId } })
  if (!menu || !menu.isAvailable) {
    return NextResponse.json({ error: '商品不存在或已下架' }, { status: 400 })
  }

  const unitPrice = calcUnitPrice(menu.price, cupSize || 'M')
  const cart = await getOrCreateCart(phone, sessionId)

  // 查找是否已有相同商品+规格的项
  const existing = await prisma.cartItem.findFirst({
    where: { cartId: cart.id, menuId, cupSize: cupSize || 'M', temperature: temperature || 'hot' },
  })

  if (existing) {
    await prisma.cartItem.update({
      where: { id: existing.id },
      data: { quantity: existing.quantity + 1 },
    })
  } else {
    await prisma.cartItem.create({
      data: {
        cartId: cart.id,
        menuId,
        name: menu.name,
        imageUrl: menu.imageUrl,
        cupSize: cupSize || 'M',
        temperature: temperature || 'hot',
        quantity: 1,
        unitPrice,
      },
    })
  }

  // Update cart timestamp for SSE
  await prisma.cart.update({ where: { id: cart.id }, data: { updatedAt: new Date() } })

  return NextResponse.json({ ok: true })
}
