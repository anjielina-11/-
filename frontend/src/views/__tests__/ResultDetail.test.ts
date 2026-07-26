import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readSource = () => readFileSync(resolve(process.cwd(), 'src/views/ResultDetail.vue'), 'utf8')

describe('ResultDetail history list', () => {
  it('formats API timestamps before display', () => {
    expect(readSource()).toContain('formatDateTime(row.createdAt)')
  })

  it('renders diagnosis context and the multi-agent decision trace', () => {
    const source = readSource()

    expect(source).toContain('contextSummary')
    expect(source).toContain('agentTrace')
    expect(source).toContain('诊断上下文')
    expect(source).toContain('Agent 决策轨迹')
    expect(source).toContain('agentName')
    expect(source).toContain('agentStatusLabel')
  })

  it('loads and renders the uploaded disease image in result details', () => {
    const source = readSource()

    expect(source).toContain('病害原图')
    expect(source).toContain('`/diagnosis/${diagnosisId}/image`')
    expect(source).toContain("responseType: 'blob'")
    expect(source).toContain('diagnosisImageUrl')
    expect(source).toContain('URL.revokeObjectURL')
  })
})
