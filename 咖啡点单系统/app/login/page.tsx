'use client'

import { useState, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import { useApp } from '@/components/AppProvider'

export default function LoginPage() {
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const router = useRouter()
  const { login, phone: currentPhone } = useApp()

  // If already logged in, redirect
  if (currentPhone && typeof window !== 'undefined') {
    router.replace('/menu')
    return null
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    if (!/^1\d{10}$/.test(phone)) {
      setError('请输入有效的 11 位手机号')
      return
    }

    setSubmitting(true)
    try {
      await login(phone)
      router.push('/menu')
    } catch {
      setError('登录失败，请重试')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-6">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="text-6xl mb-4">☕</div>
          <h2 className="text-2xl font-bold text-green-900">欢迎来到 CoffeeShop</h2>
          <p className="text-sm text-gray-400 mt-2">输入手机号即可登录</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-600 mb-1.5">手机号</label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 11))}
              placeholder="请输入 11 位手机号"
              maxLength={11}
              className="w-full px-4 py-3 rounded-xl border-2 border-gray-200 focus:border-green-800 focus:outline-none text-lg tracking-wider transition-colors"
              autoFocus
            />
            {error && <p className="text-red-500 text-xs mt-1">{error}</p>}
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3 bg-green-800 text-white rounded-xl font-medium hover:bg-green-700 disabled:opacity-50 transition-colors shadow-lg"
          >
            {submitting ? '登录中...' : '登录'}
          </button>
        </form>

        <p className="text-xs text-gray-300 text-center mt-6">
          登录即表示同意服务条款和隐私政策
        </p>
      </div>
    </div>
  )
}
