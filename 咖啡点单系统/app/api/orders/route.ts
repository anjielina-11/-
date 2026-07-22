import { NextRequest, NextResponse } from 'next/server'
import { getLoginPhone } from '@/lib/auth'
import { getOrCreateCart } from '@/lib/cart'
import { prisma } from '@/lib/prisma'

export async function POST(req: NextRequest) {
  const phone = await getLoginPhone()
  const sessionId = req.headers.get('x-session-id') || 'default'
  const { customerName } = await req.json()

  if (!phone) {
    return NextResponse.json({ error: '请先登录' }, { status: 401 })
  }

  const cart = await getOrCreateCart(phone, sessionId)
  const cartItems = await prisma.cartItem.findMany({
    where: { cartId: cart.id },
    include: { menu: true },
  })

  if (cartItems.length === 0) {
    return NextResponse.json({ error: '购物车为空' }, { status: 400 })
  }

  // Validate all items
  for (const ci of cartItems) {
    if (!ci.menu || !ci.menu.isAvailable) {
      return NextResponse.json({ error: `商品「${ci.name}」已下架，请重新选择` }, { status: 400 })
    }
  }

  const totalAmount = cartItems.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0)

  const order = await prisma.order.create({
    data: {
      customerName: customerName || '顾客',
      phone,
      totalAmount,
      status: 'pending',
      items: {
        create: cartItems.map((ci) => ({
          menuId: ci.menuId,
          quantity: ci.quantity,
          price: ci.unitPrice,
          cupSize: ci.cupSize,
          temperature: ci.temperature,
        })),
      },
    },
    include: { items: true },
  })

  // Clear cart after order
  await prisma.cartItem.deleteMany({ where: { cartId: cart.id } })
  await prisma.cart.update({ where: { id: cart.id }, data: { updatedAt: new Date() } })

  return NextResponse.json({ order })
}

export async function GET(req: NextRequest) {
  const phone = req.nextUrl.searchParams.get('phone')
  const all = req.nextUrl.searchParams.get('all')

  // Admin: fetch all orders
  if (phone === 'admin' && all === 'true') {
    const orders = await prisma.order.findMany({
      include: { items: true },
      orderBy: { createdAt: 'desc' },
      take: 100,
    })
    return NextResponse.json(orders)
  }

  if (!phone) return NextResponse.json({ error: '手机号必填' }, { status: 400 })

  const orders = await prisma.order.findMany({
    where: { phone },
    include: { items: true },
    orderBy: { createdAt: 'desc' },
  })
  return NextResponse.json(orders)
}
