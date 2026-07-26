export interface PlantingValidationInput {
  plantingDate: string
  expectedHarvestDate: string
  areaMu: string | number
  fieldAreaMu: string | number
  occupiedAreaMu: string | number
  today?: string
  occupiesField?: boolean
}

const asPositiveNumber = (value: string | number, label: string): number => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue) || numberValue <= 0) throw new Error(`${label}\u5fc5\u987b\u5927\u4e8e0`)
  return numberValue
}

export const localToday = (date = new Date()): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const validateFarmArea = (proposedArea: string | number, existingFieldArea = 0): number => {
  const farmArea = asPositiveNumber(proposedArea, '\u519c\u573a\u9762\u79ef')
  if (farmArea < existingFieldArea) throw new Error(`\u519c\u573a\u9762\u79ef\u4e0d\u80fd\u5c0f\u4e8e\u5df2\u6709\u5730\u5757\u603b\u9762\u79ef ${existingFieldArea} \u4ea9`)
  return farmArea
}

export const validateFieldArea = (
  proposedArea: string | number,
  farmArea: string | number,
  otherFieldArea = 0,
  occupiedPlantingArea = 0
): number => {
  const fieldArea = asPositiveNumber(proposedArea, '\u5730\u5757\u9762\u79ef')
  const farmCapacity = asPositiveNumber(farmArea, '\u6240\u5c5e\u519c\u573a\u9762\u79ef')
  if (fieldArea < occupiedPlantingArea) throw new Error(`\u5730\u5757\u9762\u79ef\u4e0d\u80fd\u5c0f\u4e8e\u5f53\u524d\u672a\u6536\u83b7\u79cd\u690d\u9762\u79ef ${occupiedPlantingArea} \u4ea9`)
  if (otherFieldArea + fieldArea > farmCapacity) throw new Error(`\u5730\u5757\u603b\u9762\u79ef\u4e0d\u80fd\u8d85\u8fc7\u519c\u573a\u9762\u79ef ${farmCapacity} \u4ea9`)
  return fieldArea
}

export const validatePlantingInput = (input: PlantingValidationInput): number => {
  if (!input.plantingDate || !input.expectedHarvestDate) throw new Error('\u8bf7\u586b\u5199\u79cd\u690d\u65e5\u671f\u548c\u9884\u8ba1\u6536\u83b7\u65e5\u671f')
  const today = input.today ?? localToday()
  if (input.plantingDate > today) throw new Error('\u79cd\u690d\u65e5\u671f\u4e0d\u80fd\u665a\u4e8e\u4eca\u5929')
  if (input.expectedHarvestDate < input.plantingDate) throw new Error('\u9884\u8ba1\u6536\u83b7\u65e5\u671f\u4e0d\u80fd\u65e9\u4e8e\u79cd\u690d\u65e5\u671f')
  const area = asPositiveNumber(input.areaMu, '\u79cd\u690d\u9762\u79ef')
  const fieldArea = asPositiveNumber(input.fieldAreaMu, '\u6240\u5c5e\u5730\u5757\u9762\u79ef')
  const occupiedArea = Number(input.occupiedAreaMu)
  if (!Number.isFinite(occupiedArea) || occupiedArea < 0) throw new Error('\u5df2\u5360\u7528\u79cd\u690d\u9762\u79ef\u4e0d\u80fd\u5c0f\u4e8e0')
  if (input.occupiesField !== false && occupiedArea + area > fieldArea) {
    const remaining = Math.max(0, fieldArea - occupiedArea)
    throw new Error(`\u79cd\u690d\u9762\u79ef\u4e0d\u80fd\u8d85\u8fc7\u5730\u5757\u5269\u4f59\u9762\u79ef ${remaining} \u4ea9`)
  }
  return area
}
