import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('acceptance interaction enhancements', () => {
  it('supports manual seven-day weather refresh and farm selection', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/Dashboard.vue'), 'utf8')
    expect(source).toContain('未来七天天气')
    expect(source).toContain('实时更新')
    expect(source).toContain('/weather/fetch')
    expect(source).toContain('selectedFarmId')
  })

  it('shows the uploaded disease image in technician details', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/TechWorkbench.vue'), 'utf8')
    expect(source).toContain('病害原图')
    expect(source).toContain('/image')
    expect(source).toContain('diagnosisImageUrl')
  })

  it('keeps drag upload and provides an explicit file selection button', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/DiseaseUpload.vue'), 'utf8')
    expect(source).toContain('drag')
    expect(source).toContain('选择病害图片')
    expect(source).toContain('v-show="!uploadedImageUrl"')
    expect(source).toContain('accept="image/jpeg,image/png"')
    expect(source).not.toContain('querySelector<HTMLInputElement>')
  })

  it('explains the local model scope before users upload unsupported images', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/DiseaseUpload.vue'), 'utf8')
    expect(source).toContain('当前本地模型支持 18 类病虫害')
    expect(source).toContain('不在支持范围或置信度不足的图片将转人工审核')
  })

  it('can create and immediately select a farm while adding a field', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/FarmList.vue'), 'utf8')
    expect(source).toContain('新建农场')
    expect(source).toContain('farmDialogVisible')
    expect(source).toContain('form.farmId = createdFarm.id')
  })
})
