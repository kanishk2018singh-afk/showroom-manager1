#!/usr/bin/env node
/**
 * Preflight check for the smoke test.
 * Sabse common problem: webapp folder me `npm install` nahi chalaya gaya ho,
 * ya script repo root se chala diya gaya ho. Us case me saaf-saaf bata dete hain.
 */
import { existsSync } from 'node:fs'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { dirname, join, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const webappDir = resolve(here, '..')
const require = createRequire(import.meta.url)

const required = ['esbuild', 'jsdom', 'fake-indexeddb']
const missing = []
for (const mod of required) {
  try {
    require.resolve(mod)
  } catch {
    missing.push(mod)
  }
}

const hasNodeModules = existsSync(join(webappDir, 'node_modules'))

if (missing.length || !hasNodeModules) {
  console.error('\n❌ Smoke test ke liye zaroori packages nahi mile:', missing.join(', ') || 'node_modules')
  console.error('\nIse aise theek karein (repo ke andar se):\n')
  console.error('   cd webapp        # zaroori — root se nahi chalega')
  console.error('   npm install      # ek baar hi karna hai')
  console.error('   npm run smoke\n')
  console.error('Agar aap repo root par hain to seedha ye bhi chalega:  npm run smoke   (root script khud webapp me jaata hai)\n')
  process.exit(1)
}

const nodeMajor = Number(process.versions.node.split('.')[0])
if (nodeMajor < 20) {
  console.error(`\n❌ Node ${process.versions.node} mila — smoke test ke liye Node 20+ chahiye (Node 22 recommended).\n`)
  process.exit(1)
}

console.log(`  (node ${process.versions.node} • packages theek hain)`)
