import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const read = (path: string) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('safe delete and lifecycle controls', () => {
  it('uses enable and disable semantics for users instead of a delete action', () => {
    const source = read('src/views/AdminUsers.vue')

    expect(source).not.toContain('handleDelete')
    expect(source).not.toContain(':icon="Delete"')
    expect(source).toContain('禁用用户')
    expect(source).toContain('启用用户')
  })

  it('lets farmers archive and restore farms', () => {
    const source = read('src/views/FarmList.vue')

    expect(source).toContain('管理农场')
    expect(source).toContain('includeArchived: true')
    expect(source).toContain('`/farms/${farm.id}/status`')
    expect(source).toContain("status: 'archived'")
    expect(source).toContain("status: 'active'")
  })

  it('provides an admin crop lifecycle page', () => {
    expect(existsSync(resolve(process.cwd(), 'src/views/AdminCrops.vue'))).toBe(true)
    const source = read('src/views/AdminCrops.vue')
    const routes = read('src/router/index.ts')
    const userStore = read('src/stores/user.ts')
    const adminLayout = read('src/views/Admin.vue')

    expect(source).toContain('作物品种管理')
    expect(adminLayout).toContain("'AdminCrops': '作物品种管理'")
    expect(source).toContain('`/crops/${row.id}/status`')
    expect(routes).toContain("path: 'crops'")
    expect(userStore).toContain("path: '/admin/crops'")
  })

  it('supports cancelling pending tasks without deleting history', () => {
    const source = read('src/views/FarmerTask.vue')

    expect(source).toContain("task.status === 'cancelled' ? '已取消'")
    expect(source).toContain("status: 'cancelled'")
    expect(source).toContain('取消任务')
    expect(source).toContain('{{ cancelledCount }}')
  })

  it('shows explicit relation-protection messages for fields and planting cycles', () => {
    const fields = read('src/views/FarmList.vue')
    const crops = read('src/views/FarmerCrops.vue')

    expect(fields).toContain("error instanceof Error ? error.message : '删除地块失败'")
    expect(crops).toContain("error instanceof Error ? error.message : '删除种植记录失败'")
  })
})
