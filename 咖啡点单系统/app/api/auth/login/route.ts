import { NextRequest, NextResponse } from 'next/server'
import { ensureUser, setLoginCookie } from '@/lib/auth'

export async function POST(req: NextRequest) {
  const { phone } = await req.json()
  if (!phone || !/^1\d{10}$/.test(phone)) {
    return NextResponse.json({ error: '请输入有效的手机号' }, { status: 400 })
  }
  await ensureUser(phone)
  await setLoginCookie(phone)
  return NextResponse.json({ ok: true, phone })
}
