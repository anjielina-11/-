import { getCurrentUser } from '@/lib/auth'
import Link from 'next/link'

export default async function HomePage() {
  const user = await getCurrentUser()

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-6 text-center">
      <div className="text-8xl mb-6">☕</div>
      <h1 className="text-3xl md:text-5xl font-bold text-green-900 mb-3">CoffeeShop</h1>
      <p className="text-gray-500 text-sm md:text-base mb-8 max-w-md">
        精品咖啡，随时随地。选你所爱，即刻下单。
      </p>

      <div className="flex flex-col sm:flex-row gap-3">
        {user ? (
          <>
            <Link
              href="/menu"
              className="px-8 py-3 bg-green-800 text-white rounded-full font-medium hover:bg-green-700 transition-colors shadow-lg"
            >
              开始点单
            </Link>
            <Link
              href="/orders"
              className="px-8 py-3 bg-white text-green-800 border-2 border-green-800 rounded-full font-medium hover:bg-green-50 transition-colors"
            >
              我的订单
            </Link>
          </>
        ) : (
          <Link
            href="/login"
            className="px-8 py-3 bg-green-800 text-white rounded-full font-medium hover:bg-green-700 transition-colors shadow-lg"
          >
            手机号登录
          </Link>
        )}
      </div>

      {user && (
        <p className="mt-6 text-sm text-gray-400">当前账号：{user.phone}</p>
      )}
    </div>
  )
}
