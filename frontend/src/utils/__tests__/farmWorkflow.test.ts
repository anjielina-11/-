import { describe, expect, it, vi } from 'vitest'
import { ensureFarmAvailable, parsePositiveArea, type FarmOption } from '@/utils/farmWorkflow'

describe('ensureFarmAvailable', () => {
  it('returns the first existing farm without creating another one', async () => {
    const existing: FarmOption = { id: 'farm-1', name: '\u5df2\u6709\u519c\u573a' }
    const createFarm = vi.fn()

    await expect(ensureFarmAvailable([existing], createFarm)).resolves.toEqual(existing)
    expect(createFarm).not.toHaveBeenCalled()
  })

  it('creates and returns the first farm when the list is empty', async () => {
    const created: FarmOption = { id: 'farm-new', name: '\u65b0\u519c\u573a' }
    const createFarm = vi.fn().mockResolvedValue(created)

    await expect(ensureFarmAvailable([], createFarm)).resolves.toEqual(created)
    expect(createFarm).toHaveBeenCalledOnce()
  })

  it('rejects when farm creation does not return a valid id', async () => {
    const createFarm = vi.fn().mockResolvedValue({ id: '', name: '\u5f02\u5e38\u519c\u573a' })

    await expect(ensureFarmAvailable([], createFarm)).rejects.toThrow('\u521b\u5efa\u519c\u573a\u540e\u672a\u8fd4\u56de\u6709\u6548 ID')
  })

  it('converts a positive numeric string to an area number', () => {
    expect(parsePositiveArea('12.5')).toBe(12.5)
  })

  it.each(['', '0', '-1', 'abc'])('rejects invalid area: %s', value => {
    expect(() => parsePositiveArea(value)).toThrow('\u9762\u79ef\u5fc5\u987b\u5927\u4e8e0')
  })
})
