import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'
import { seedDatabase } from './lib/db'

// Hide the boot splash once React has painted.
function hideBoot() {
  const el = document.getElementById('boot')
  if (!el) return
  el.style.transition = 'opacity .25s ease'
  el.style.opacity = '0'
  setTimeout(() => el.remove(), 260)
}

async function start() {
  try {
    await seedDatabase()
  } catch (err) {
    console.error('Seed error', err)
  }
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
  requestAnimationFrame(hideBoot)
}

void start()
