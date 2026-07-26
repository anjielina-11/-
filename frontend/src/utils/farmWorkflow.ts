export interface FarmOption {
  id: string
  name: string
}

export const parsePositiveArea = (value: string | number): number => {
  const area = Number(value)
  if (!Number.isFinite(area) || area <= 0) throw new Error('\u9762\u79ef\u5fc5\u987b\u5927\u4e8e0')
  return area
}

export const ensureFarmAvailable = async (
  farms: readonly FarmOption[],
  createFarm: () => Promise<FarmOption>
): Promise<FarmOption> => {
  const farm = farms[0] ?? await createFarm()
  if (!farm.id) throw new Error('\u521b\u5efa\u519c\u573a\u540e\u672a\u8fd4\u56de\u6709\u6548 ID')
  return farm
}
