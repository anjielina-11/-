import { describe, expect, it } from 'vitest'
import { unwrapApiResponse } from '../request'

describe('API 响应解包', () => {
  it('成功码为 0 时返回业务 data', () => {
    expect(unwrapApiResponse({ code: 0, data: { id: 'farm-1' }, message: 'ok' }))
      .toEqual({ id: 'farm-1' })
  })

  it('非 0 业务码时抛出后端消息', () => {
    expect(() => unwrapApiResponse({ code: 40001, data: null, message: '用户名或密码错误' }))
      .toThrow('用户名或密码错误')
  })

  it('兼容无统一包装的文件流等响应', () => {
    const blobLike = { size: 12, type: 'image/png' }
    expect(unwrapApiResponse(blobLike)).toBe(blobLike)
  })
})
