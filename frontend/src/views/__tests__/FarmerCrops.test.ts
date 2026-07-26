import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readSource = () => readFileSync(resolve(process.cwd(), 'src/views/FarmerCrops.vue'), 'utf8')

describe('FarmerCrops planting form', () => {
  it('submits calendar dates without UTC timezone conversion', () => {
    const dateOnlyFormats = readSource().match(/value-format="YYYY-MM-DD"/g) ?? []

    expect(dateOnlyFormats).toHaveLength(2)
  })

  it('allows farmers to create a crop variety and selects it immediately', () => {
    const source = readSource()

    expect(source).toContain('新建作物品种')
    expect(source).toContain("request.post<CropOption>('/crops'")
    expect(source).toContain("cropOptions.value.unshift({ ...createdCrop, status: createdCrop.status || 'active' })")
    expect(source).toContain('formData.value.cropId = createdCrop.id')
  })

  it('selects and submits a canonical growth stage', () => {
    const source = readSource()

    expect(source).toContain('growthStage')
    expect(source).toContain("value: 'tillering'")
    expect(source).toContain('growthStage: formData.value.growthStage')
    expect(source).toContain('生育期')
  })

  it('ships multiple common crop choices for first-time use', () => {
    const migration = readFileSync(
      resolve(process.cwd(), '../backend/src/main/resources/db/migration/V4__seed_common_crops.sql'),
      'utf8'
    )

    for (const cropName of ['水稻', '玉米', '小麦', '马铃薯', '番茄', '辣椒']) {
      expect(migration).toContain(cropName)
    }
  })
})
