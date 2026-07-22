import { NextRequest, NextResponse } from 'next/server'
import { getLoginPhone } from '@/lib/auth'
import { getOrCreateCart, getCartItemsCount } from '@/lib/cart'
import { prisma } from '@/lib/prisma'

export async function GET(req: NextRequest) {
  const phone = await getLoginPhone()
  const sessionId = req.headers.get('x-session-id') || 'default'

  const cart = await getOrCreateCart(phone, sessionId)
  const full = await prisma.cart.findUnique({
    where: { id: cart.id },
    include: {
      items: {
        include: { menu: true },
        orderBy: { createdAt: 'asc' },
      },
    },
  })

  if (!full) {
    return NextResponse.json({ cart: null, items: [], totalCount: 0 })
  }

  // Filter out items whose menu no longer exists or is unavailable
  const validItems = full.items.filter((item) => item.menu && item.menu.isAvailable)

  return NextResponse.json({
    cart: { id: full.id, phone: full.phone },
    items: validItems.map((i) => ({
      id: i.id,
      menuId: i.menuId,
      name: i.name,
      imageUrl: i.imageUrl,
      cupSize: i.cupSize,
      temperature: i.temperature,
      quantity: i.quantity,
      unitPrice: i.unitPrice,
    })),
    totalCount: validItems.reduce((s, i) => s + i.quantity, 0),
  })
}

export async function DELETE(req: NextRequest) {
  const phone = await getLoginPhone()
  const sessionId = req.headers.get('x-session-id') || 'default'
  const cart = await getOrCreateCart(phone, sessionId)
  await prisma.cartItem.deleteMany({ where: { cartId: cart.id } })
  return NextResponse.json({ ok: true })
}
