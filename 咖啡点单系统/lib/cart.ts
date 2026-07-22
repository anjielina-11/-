import { prisma } from './prisma'

const SIZE_PRICE_MAP: Record<string, number> = { S: 0, M: 3, L: 6 }

export function calcUnitPrice(basePrice: number, cupSize: string): number {
  return basePrice + (SIZE_PRICE_MAP[cupSize] || 0)
}

export async function getOrCreateCart(phone: string | null, sessionId: string) {
  if (phone) {
    // 登录用户：按 phone 查找或创建
    let cart = await prisma.cart.findUnique({ where: { phone } })
    if (!cart) {
      cart = await prisma.cart.create({
        data: { phone, sessionId: `phone_${phone}` },
      })
    }
    return cart
  }
  // 未登录：按 sessionId
  let cart = await prisma.cart.findUnique({ where: { sessionId } })
  if (!cart) {
    cart = await prisma.cart.create({ data: { sessionId } })
  }
  return cart
}

export async function getCartWithItems(cartId: number) {
  return prisma.cart.findUnique({
    where: { id: cartId },
    include: { items: { include: { menu: true } } },
  })
}

export async function getCartItemsCount(cartId: number): Promise<number> {
  const items = await prisma.cartItem.findMany({
    where: { cartId },
    select: { quantity: true },
  })
  return items.reduce((sum, i) => sum + i.quantity, 0)
}
