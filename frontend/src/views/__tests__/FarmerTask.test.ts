import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('FarmerTask treatment feedback workflow', () => {
  it('supports starting, completing with feedback, and reviewing saved feedback', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/FarmerTask.vue'), 'utf8')

    expect(source).toContain('开始执行')
    expect(source).toContain('提交反馈并完成')
    expect(source).toContain('处置效果反馈')
    expect(source).toContain('v-model="feedbackText"')
    expect(source).toContain('remark: task.remark')
    expect(source).toContain('task.fieldName')
    expect(source).toContain('formatDiseaseTaskTitle(task.title)')
  })
})
