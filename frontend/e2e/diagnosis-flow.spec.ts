import { expect, test, type Page } from '@playwright/test'

const baseUrl = process.env.BASE_URL || 'http://localhost'
const diagnosisId = process.env.DIAGNOSIS_ID || '1c6b6f7b-a398-4fc8-87d2-9679e034f8f3'

const login = async (page: Page, username: string, password: string) => {
  await page.goto(`${baseUrl}/login`)
  await page.getByRole('textbox', { name: '请输入用户名' }).fill(username)
  await page.getByRole('textbox', { name: '请输入密码' }).fill(password)
  await page.getByRole('button', { name: '登 录' }).click()
}

test('农技员可查询真实诊断列表和详情', async ({ page }) => {
  await login(page, 'tech', 'tech123')
  await expect(page).toHaveURL(/\/tech\/review/)

  await page.goto(`${baseUrl}/tech/results`)
  await expect(page.getByRole('heading', { name: '识别结果查询' })).toBeVisible()
  await expect(page.getByText('水稻稻瘟病').first()).toBeVisible()

  await page.goto(`${baseUrl}/tech/results?id=${diagnosisId}`)
  await expect(page.getByRole('heading', { name: '水稻稻瘟病' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '防治建议', exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: '引用来源', exact: true })).toBeVisible()
})

test('四类演示账号进入各自默认页面', async ({ page }) => {
  const accounts = [
    ['farmer', 'farmer123', /\/farmer\/fields/],
    ['tech', 'tech123', /\/tech\/review/],
    ['coop', 'coop123', /\/coop\/dashboard/],
    ['admin', 'admin123', /\/admin\/users/]
  ] as const

  for (const [username, password, expectedUrl] of accounts) {
    await page.goto(baseUrl)
    await page.evaluate(() => localStorage.clear())
    await login(page, username, password)
    await expect(page).toHaveURL(expectedUrl)
  }
})
