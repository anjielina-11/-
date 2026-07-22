import { cookies } from 'next/headers'
import { prisma } from './prisma'

const COOKIE_NAME = 'coffeeshop_phone'
const MAX_AGE = 30 * 24 * 60 * 60 // 30 days

export async function getLoginPhone(): Promise<string | null> {
  const cookieStore = await cookies()
  return cookieStore.get(COOKIE_NAME)?.value || null
}

export async function setLoginCookie(phone: string) {
  const cookieStore = await cookies()
  cookieStore.set(COOKIE_NAME, phone, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: MAX_AGE,
    path: '/',
  })
}

export async function clearLoginCookie() {
  const cookieStore = await cookies()
  cookieStore.delete(COOKIE_NAME)
}

/** 确保用户存在，返回用户信息 */
export async function ensureUser(phone: string) {
  return prisma.user.upsert({
    where: { phone },
    update: {},
    create: { phone },
  })
}

/** 从 cookie 获取当前用户 */
export async function getCurrentUser() {
  const phone = await getLoginPhone()
  if (!phone) return null
  return prisma.user.findUnique({ where: { phone } })
}
