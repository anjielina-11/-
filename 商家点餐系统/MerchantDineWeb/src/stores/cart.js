import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  const items = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

  const totalCount = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const totalAmount = computed(() => items.value.reduce((sum, i) => sum + i.price * i.quantity, 0))

  function addItem(product, quantity = 1) {
    const exist = items.value.find(i => i.productId === product.id)
    if (exist) {
      exist.quantity += quantity
    } else {
      items.value.push({
        productId: product.id,
        productName: product.name,
        productPrice: product.price,
        productImage: product.image,
        price: product.price,
        name: product.name,
        image: product.image,
        quantity
      })
    }
    saveCart()
  }

  function removeItem(productId) {
    items.value = items.value.filter(i => i.productId !== productId)
    saveCart()
  }

  function updateQuantity(productId, quantity) {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      item.quantity = Math.max(1, quantity)
      saveCart()
    }
  }

  function clearCart() {
    items.value = []
    saveCart()
  }

  function clearCartOnLogout() {
    items.value = []
    localStorage.removeItem('cart')
  }

  function saveCart() {
    localStorage.setItem('cart', JSON.stringify(items.value))
  }

  return { items, totalCount, totalAmount, addItem, removeItem, updateQuantity, clearCart, clearCartOnLogout }
})
