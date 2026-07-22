import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        coffee: {
          50: '#f7f6f4',
          100: '#e8e4de',
          200: '#d2c8bc',
          300: '#b8a695',
          400: '#a48873',
          500: '#967762',
          600: '#7b5e4e',
          700: '#644c41',
          800: '#553f38',
          900: '#4a3732',
        },
      },
    },
  },
  plugins: [],
}
export default config
