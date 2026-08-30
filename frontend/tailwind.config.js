/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        taxoryn: {
          navy: '#082E5B',
          darkNavy: '#07152B',
          obsidian: '#070C1A',
          teal: '#00D1A3',
          tealDark: '#00B388',
          emerald: '#059669',
          cyan: '#0EA5E9',
          cyanLight: '#38BDF8',
          slate: '#F8FAFC',
        },
        obsidian: {
          950: '#070C1A',
          900: '#082E5B',
          800: '#0B132B',
          700: '#1C2541',
        },
        brand: {
          50: '#E6FBF6',
          100: '#CCF7ED',
          500: '#00D1A3',
          600: '#00B388',
          700: '#059669',
          800: '#047857',
          900: '#082E5B',
          navy: '#082E5B',
        },
        compliance: {
          filed: '#00D1A3',
          filedLight: '#E6FBF6',
          pending: '#D97706',
          pendingLight: '#FFFBEB',
          overdue: '#DC2626',
          overdueLight: '#FEF2F2',
          review: '#0EA5E9',
          reviewLight: '#F0F9FF',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgb(0 0 0 / 0.05), 0 1px 2px -1px rgb(0 0 0 / 0.05)',
        'card-hover': '0 4px 6px -1px rgb(0 0 0 / 0.07), 0 2px 4px -2px rgb(0 0 0 / 0.07)',
        'modal': '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
      }
    },
  },
  plugins: [],
}
