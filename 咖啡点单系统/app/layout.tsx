import type { Metadata, Viewport } from 'next'
import './globals.css'
import AppProvider from '@/components/AppProvider'

export const metadata: Metadata = {
  title: 'CoffeeShop - 精品咖啡点单',
  description: '随时随地享受精品咖啡',
}

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-amber-50">
        <AppProvider>{children}</AppProvider>
      </body>
    </html>
  )
}
