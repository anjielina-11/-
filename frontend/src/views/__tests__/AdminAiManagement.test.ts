import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readView = (name: string) => readFileSync(resolve(process.cwd(), `src/views/${name}.vue`), 'utf8')

describe('admin AI management integration', () => {
  it('archives and synchronizes managed knowledge with duplicate-submit guards', () => {
    const source = readView('AdminKnowledge')

    expect(source).toContain("'/knowledge/documents/' + id + '/archive'")
    expect(source).toContain("'/knowledge/documents/sync'")
    expect(source).toContain("archived: '已归档'")
    expect(source).toContain(':loading="syncing"')
    expect(source).toContain(':loading="submitting"')
  })

  it('shows runtime artifacts, precision, and deploys through backend runtime', () => {
    const source = readView('AdminModels')

    expect(source).toContain("'/model-versions/runtime'")
    expect(source).toContain('model_name: string')
    expect(source).toContain('runtime.model_name')
    expect(source).toContain("'/model-versions/' + id + '/deploy'")
    expect(source).toContain('precisionVal: percentToRatio(formData.value.precision)')
    expect(source).toContain('modelPath')
    expect(source).toContain('classMappingPath')
    expect(source).toContain('numClasses')
    expect(source).toContain(':loading="deployingId === row.id"')
    expect(source).toContain('当前 Runtime')
  })
})